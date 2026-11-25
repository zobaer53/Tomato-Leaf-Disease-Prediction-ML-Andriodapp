# %% [markdown]
# # Tomato leaf disease model v2
#
# Trains a 10-class tomato leaf classifier for the Android app and exports a `.tflite` that
# drops into `core/data/src/main/assets/`.
#
# * **Data:** PlantVillage tomato (lab photos, ~18k) + PlantDoc tomato (real field photos, CC BY 4.0).
#   PlantDoc has no Target Spot class, so that class is learned from PlantVillage only.
# * **Model:** MobileNetV3-Large (ImageNet weights) fine-tuned, heavy augmentation for field robustness.
# * **Contract (same as the app expects):** input float32 `[1,224,224,3]` RGB **0..255** (scaling is
#   inside the model, so the app uses mean=0, std=1), output `[1,10]` softmax in the app's label order.
# * **Evaluation:** held-out PlantVillage test split + PlantDoc field test split, scored with the
#   app's preprocessing (bilinear stretch to 224x224, no EXIF rotation), new model vs the current
#   app model (`[-1,1]` input).
#
# Runtime: **T4 GPU** (Runtime > Change runtime type). Then Runtime > Run all. Takes ~30-45 min.

# %%
import os, sys, subprocess, json, random, pathlib, time, zipfile, urllib.request
import numpy as np
import tensorflow as tf
import keras
from PIL import Image

print("TF", tf.__version__, "| Keras", keras.__version__, "| Python", sys.version.split()[0])
gpus = tf.config.list_physical_devices("GPU")
print("GPU:", gpus)
assert gpus, "No GPU: Runtime > Change runtime type > T4 GPU, then Run all again."

# %%
SEED = 42
IMG = 224
BATCH = 32
HEAD_EPOCHS = 6
FINE_EPOCHS = 14
PLANTDOC_OVERSAMPLE = 4  # field photos are few but the most valuable signal

random.seed(SEED); np.random.seed(SEED); keras.utils.set_random_seed(SEED)

# Index order MUST match core/data/.../classifier/ModelLabels.kt
LABELS = ["healthy", "Bacterial_spot", "Early_blight", "Late_blight", "Leaf_Mold",
          "Septoria_leaf_spot", "Spider_mites", "Target_Spot", "mosaic_virus", "Yellow_Leaf_Curl"]
SHORT = ["Hea", "Bac", "EBl", "LBl", "Mol", "Sep", "Spi", "Tgt", "Mos", "YLC"]

PV_FOLDERS = {
    "Tomato___healthy": "healthy",
    "Tomato___Bacterial_spot": "Bacterial_spot",
    "Tomato___Early_blight": "Early_blight",
    "Tomato___Late_blight": "Late_blight",
    "Tomato___Leaf_Mold": "Leaf_Mold",
    "Tomato___Septoria_leaf_spot": "Septoria_leaf_spot",
    "Tomato___Spider_mites Two-spotted_spider_mite": "Spider_mites",
    "Tomato___Target_Spot": "Target_Spot",
    "Tomato___Tomato_mosaic_virus": "mosaic_virus",
    "Tomato___Tomato_Yellow_Leaf_Curl_Virus": "Yellow_Leaf_Curl",
}
PD_FOLDERS = {
    "Tomato leaf": "healthy",
    "Tomato leaf bacterial spot": "Bacterial_spot",
    "Tomato Early blight leaf": "Early_blight",
    "Tomato leaf late blight": "Late_blight",
    "Tomato mold leaf": "Leaf_Mold",
    "Tomato Septoria leaf spot": "Septoria_leaf_spot",
    "Tomato two spotted spider mites leaf": "Spider_mites",
    "Tomato leaf mosaic virus": "mosaic_virus",
    "Tomato leaf yellow virus": "Yellow_Leaf_Curl",
}
# Baseline to compare against: the v1 model file, pinned to the last commit that shipped it.
OLD_MODEL_URL = ("https://raw.githubusercontent.com/zobaer53/Tomato-Leaf-Disease-Prediction-ML-Andriodapp/"
                 "652819b7f281cf7ca40b23a4b9e475ebaa283858/core/data/src/main/assets/tomato_leaf_disease.tflite")
os.makedirs("data", exist_ok=True); os.makedirs("out", exist_ok=True)

# %%
def sparse_clone(url, dest, patterns):
    if os.path.isdir(dest):
        print("exists:", dest); return
    t = time.time()
    subprocess.run(["git", "clone", "--depth", "1", "--filter=blob:none", "--sparse", "-q", url, dest], check=True)
    subprocess.run(["git", "-C", dest, "sparse-checkout", "set", "--no-cone", *patterns], check=True)
    print(f"cloned {dest} in {time.time() - t:.0f}s")

sparse_clone("https://github.com/spMohanty/PlantVillage-Dataset.git", "data/pv", ["/raw/color/Tomato___*"])
sparse_clone("https://github.com/pratikkayal/PlantDoc-Dataset.git", "data/pd", ["/train/Tomato*", "/test/Tomato*"])
urllib.request.urlretrieve(OLD_MODEL_URL, "data/old_model.tflite")
print("old model bytes:", os.path.getsize("data/old_model.tflite"))

# %%
IMG_EXTS = (".jpg", ".jpeg", ".png")

def list_images(d):
    return sorted(str(p) for p in pathlib.Path(d).iterdir() if p.suffix.lower() in IMG_EXTS)

def clean_copy(src, dst_dir, idx):
    """PlantDoc is scraped from the web: normalise to RGB JPEG (<=1024 px) and drop unreadable files."""
    try:
        with Image.open(src) as im:
            im = im.convert("RGB")
            im.thumbnail((1024, 1024), Image.BILINEAR)
            dst = os.path.join(dst_dir, f"{idx:05d}.jpg")
            im.save(dst, quality=95)
            return dst
    except Exception as e:
        print("skip", src, type(e).__name__)
        return None

rng = random.Random(SEED)
pv_train, pv_val, pv_test = [], [], []
for folder, label in PV_FOLDERS.items():
    files = list_images(f"data/pv/raw/color/{folder}")
    rng.shuffle(files)
    a, b = int(len(files) * 0.8), int(len(files) * 0.9)
    y = LABELS.index(label)
    pv_train += [(f, y) for f in files[:a]]
    pv_val += [(f, y) for f in files[a:b]]
    pv_test += [(f, y) for f in files[b:]]

pd_train, pd_val, pd_test = [], [], []
for split in ("train", "test"):
    for folder, label in PD_FOLDERS.items():
        src_dir = f"data/pd/{split}/{folder}"
        if not os.path.isdir(src_dir):
            print("missing", src_dir); continue
        dst_dir = f"data/pd_clean/{split}/{label}"; os.makedirs(dst_dir, exist_ok=True)
        srcs = sorted(str(p) for p in pathlib.Path(src_dir).iterdir() if p.is_file())
        files = [c for i, s in enumerate(srcs) if (c := clean_copy(s, dst_dir, i))]
        y = LABELS.index(label)
        if split == "test":
            pd_test += [(f, y) for f in files]
        else:
            rng.shuffle(files)
            k = max(1, int(len(files) * 0.15))
            pd_val += [(f, y) for f in files[:k]]
            pd_train += [(f, y) for f in files[k:]]

def counts(items):
    c = np.bincount([y for _, y in items], minlength=len(LABELS)); return c

print(f"{'class':<20}" + "".join(f"{n:>9}" for n in ["pv_train", "pv_val", "pv_test", "pd_train", "pd_val", "pd_test"]))
cols = [counts(s) for s in (pv_train, pv_val, pv_test, pd_train, pd_val, pd_test)]
for i, l in enumerate(LABELS):
    print(f"{l:<20}" + "".join(f"{c[i]:>9}" for c in cols))
print(f"{'TOTAL':<20}" + "".join(f"{c.sum():>9}" for c in cols))

# %%
AUTOTUNE = tf.data.AUTOTUNE

def decode(path):
    img = tf.io.decode_image(tf.io.read_file(path), channels=3, expand_animations=False)  # ignores EXIF, like the app
    img.set_shape([None, None, 3])
    return tf.image.resize(tf.cast(img, tf.float32), (IMG, IMG), method="bilinear")  # 0..255, stretched like the app

def color_jitter(x):
    x = x / 255.0
    x = tf.image.random_saturation(x, 0.7, 1.3)
    x = tf.image.random_hue(x, 0.03)
    return tf.clip_by_value(x, 0.0, 1.0) * 255.0

def make_ds(items, training):
    paths = [p for p, _ in items]; ys = [y for _, y in items]
    ds = tf.data.Dataset.from_tensor_slices((paths, ys))
    if training:
        ds = ds.shuffle(len(items), seed=SEED, reshuffle_each_iteration=True)
    ds = ds.map(lambda p, y: (decode(p), tf.one_hot(y, len(LABELS))), num_parallel_calls=AUTOTUNE)
    if training:
        ds = ds.map(lambda x, y: (color_jitter(x), y), num_parallel_calls=AUTOTUNE)
    else:
        ds = ds.cache()
    return ds.batch(BATCH).prefetch(AUTOTUNE)

train_items = pv_train + pd_train * PLANTDOC_OVERSAMPLE
val_items = pv_val + pd_val
train_ds = make_ds(train_items, training=True)
val_ds = make_ds(val_items, training=False)

# Square-root balanced class weights: lifts rare classes (mosaic, target spot) without overfitting them.
cnt = counts(train_items).astype(np.float64)
w = np.sqrt(cnt.max() / np.maximum(cnt, 1)); w = w / w.mean()
class_weight = {i: float(w[i]) for i in range(len(LABELS))}
print("class weights:", {LABELS[i]: round(v, 2) for i, v in class_weight.items()})

# %%
augment = keras.Sequential([
    keras.layers.RandomFlip("horizontal_and_vertical"),
    keras.layers.RandomRotation(0.15, fill_mode="reflect"),
    keras.layers.RandomZoom((-0.3, 0.1), fill_mode="reflect"),
    keras.layers.RandomTranslation(0.1, 0.1, fill_mode="reflect"),
    keras.layers.RandomContrast(0.3),
    keras.layers.RandomBrightness(0.25, value_range=(0, 255)),
], name="augment")

backbone = keras.applications.MobileNetV3Large(
    input_shape=(IMG, IMG, 3), include_top=False, weights="imagenet",
    include_preprocessing=True,  # rescaling lives in the model: the app feeds raw 0..255
    pooling="avg")
backbone.trainable = False

inp = keras.Input((IMG, IMG, 3), name="image")
x = backbone(inp, training=False)  # BatchNorm stays in inference mode during fine-tuning
x = keras.layers.Dropout(0.3)(x)
out = keras.layers.Dense(len(LABELS), activation="softmax", name="probs")(x)
model = keras.Model(inp, out, name="tomato_leaf_v2")          # exported (no augmentation)
train_model = keras.Sequential([keras.Input((IMG, IMG, 3)), augment, model], name="train")

loss = keras.losses.CategoricalCrossentropy(label_smoothing=0.1)
train_model.compile(optimizer=keras.optimizers.Adam(1e-3), loss=loss, metrics=["accuracy"])
t = time.time()
h1 = train_model.fit(train_ds, validation_data=val_ds, epochs=HEAD_EPOCHS, class_weight=class_weight, verbose=2)
print(f"head training {time.time() - t:.0f}s")

# %%
backbone.trainable = True
steps = FINE_EPOCHS * len(train_ds)
lr = keras.optimizers.schedules.CosineDecay(1e-4, decay_steps=steps, alpha=0.05)
train_model.compile(optimizer=keras.optimizers.Adam(lr), loss=loss, metrics=["accuracy"])
stop = keras.callbacks.EarlyStopping(monitor="val_accuracy", patience=4, restore_best_weights=True)
t = time.time()
h2 = train_model.fit(train_ds, validation_data=val_ds, epochs=FINE_EPOCHS, class_weight=class_weight,
                     callbacks=[stop], verbose=2)
print(f"fine-tuning {time.time() - t:.0f}s")
model.save("out/tomato_leaf_v2.keras")

# %%
def to_tflite(fp16):
    export_dir = "out/saved_model"
    archive = keras.export.ExportArchive()
    archive.track(model)
    archive.add_endpoint(name="serve", fn=lambda x: model(x, training=False),
                         input_signature=[tf.TensorSpec([1, IMG, IMG, 3], tf.float32, name="image")])
    archive.write_out(export_dir)
    conv = tf.lite.TFLiteConverter.from_saved_model(export_dir)
    if fp16:
        conv.optimizations = [tf.lite.Optimize.DEFAULT]
        conv.target_spec.supported_types = [tf.float16]
    return conv.convert()

for name, fp16 in (("tomato_leaf_v2_fp16.tflite", True), ("tomato_leaf_v2_fp32.tflite", False)):
    with open(f"out/{name}", "wb") as fh:
        fh.write(to_tflite(fp16))
    it = tf.lite.Interpreter(model_path=f"out/{name}"); it.allocate_tensors()
    i, o = it.get_input_details()[0], it.get_output_details()[0]
    print(name, os.path.getsize(f"out/{name}") // 1024, "KB | in", i["shape"], i["dtype"].__name__,
          "| out", o["shape"], o["dtype"].__name__)
    assert list(i["shape"]) == [1, IMG, IMG, 3] and list(o["shape"]) == [1, len(LABELS)]

# %%
def app_preprocess(path):
    # Same as the app / tools/model-eval/evaluate.py: no EXIF rotation, bilinear stretch, RGB floats.
    with Image.open(path) as im:
        return np.asarray(im.convert("RGB").resize((IMG, IMG), Image.BILINEAR), dtype=np.float32)

def evaluate_tflite(model_path, items, mean, std):
    it = tf.lite.Interpreter(model_path=model_path); it.allocate_tensors()
    i, o = it.get_input_details()[0]["index"], it.get_output_details()[0]["index"]
    conf_m = np.zeros((len(LABELS), len(LABELS)), dtype=int); confs = []
    for path, truth in items:
        it.set_tensor(i, ((app_preprocess(path) - mean) / std)[None, ...])
        it.invoke()
        p = it.get_tensor(o)[0]; pred = int(np.argmax(p))
        conf_m[truth, pred] += 1; confs.append(float(p[pred]))
    confs = np.array(confs)
    return {
        "n": len(items),
        "accuracy": float(np.trace(conf_m) / conf_m.sum()),
        "mean_top1_confidence": float(confs.mean()),
        "share_top1_ge_0_60": float((confs >= 0.6).mean()),
        "per_class_recall": {LABELS[k]: (float(conf_m[k, k] / conf_m[k].sum()) if conf_m[k].sum() else None)
                             for k in range(len(LABELS))},
        "confusion": conf_m.tolist(),
    }

MODELS = {
    "old v1 ([-1,1])": ("data/old_model.tflite", 127.5, 127.5),
    "new v2 fp16": ("out/tomato_leaf_v2_fp16.tflite", 0.0, 1.0),
}
SETS = {"PlantVillage test (lab)": pv_test, "PlantDoc test (field)": pd_test}
results = {}
for m, (path, mean, std) in MODELS.items():
    for s, items in SETS.items():
        r = evaluate_tflite(path, items, mean, std)
        results[f"{m} | {s}"] = r
        print(f"\n=== {m} | {s}: {r['accuracy']:.1%} of {r['n']}, mean conf {r['mean_top1_confidence']:.2f}, "
              f">=0.60: {r['share_top1_ge_0_60']:.0%}")
        print("      " + " ".join(f"{x:>4}" for x in SHORT))
        for k, row in enumerate(r["confusion"]):
            if sum(row): print(f"  {SHORT[k]} " + " ".join(f"{v:>4}" for v in row))

print("\nSUMMARY")
for k, r in results.items():
    print(f"  {k:<50} {r['accuracy']:.1%}")

# %%
hist = {k: [float(v) for v in vals] for k, vals in {**{f"head_{a}": b for a, b in h1.history.items()},
                                                    **{f"fine_{a}": b for a, b in h2.history.items()}}.items()}
meta = {
    "labels_in_output_order": LABELS,
    "input": "float32 [1,224,224,3] RGB 0..255 (mean=0, std=1 in the app; scaling is inside the model)",
    "output": "float32 [1,10] softmax",
    "backbone": "MobileNetV3Large imagenet, fine-tuned",
    "data": {"plantvillage": "github.com/spMohanty/PlantVillage-Dataset raw/color Tomato___* (80/10/10 split, seed 42)",
             "plantdoc": "github.com/pratikkayal/PlantDoc-Dataset (CC BY 4.0) train (85/15) + test"},
    "counts": {n: counts(s).tolist() for n, s in zip(["pv_train", "pv_val", "pv_test", "pd_train", "pd_val", "pd_test"],
                                                    [pv_train, pv_val, pv_test, pd_train, pd_val, pd_test])},
    "versions": {"tensorflow": tf.__version__, "keras": keras.__version__},
    "history": hist,
    "results": results,
}
with open("out/metrics.json", "w") as fh:
    json.dump(meta, fh, indent=1)
with open("out/labels.txt", "w") as fh:
    fh.write("\n".join(LABELS) + "\n")

with zipfile.ZipFile("tomato_leaf_v2.zip", "w", zipfile.ZIP_DEFLATED) as z:
    for f in ("tomato_leaf_v2_fp16.tflite", "tomato_leaf_v2_fp32.tflite", "metrics.json", "labels.txt"):
        z.write(f"out/{f}", f)
print("zip bytes:", os.path.getsize("tomato_leaf_v2.zip"))
print("ALL DONE")

from google.colab import files
files.download("tomato_leaf_v2.zip")
