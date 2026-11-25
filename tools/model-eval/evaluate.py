"""Evaluate the app's tomato_leaf_disease.tflite on labelled PlantVillage images.

Mirrors the app's preprocessing (core/data UriImageTensorSource + PixelTensorConverter):
decode without EXIF rotation, bilinear stretch to 224x224, RGB floats, then (v - mean) / std.

Setup (Python 3.11-3.13; ai-edge-litert has no wheels for newer versions yet):
    python3.11 -m venv .venv && .venv/bin/pip install ai-edge-litert numpy pillow

Data: <data_dir> holds PlantVillage folders named like raw/color/Tomato___* in
https://github.com/spMohanty/PlantVillage-Dataset (see FOLDER_TO_LABEL below).

Usage: .venv/bin/python -I evaluate.py core/data/src/main/assets/tomato_leaf_disease.tflite <data_dir> [--dump <csv>]
"""
import sys
import os
import csv
import numpy as np
from PIL import Image
from ai_edge_litert.interpreter import Interpreter

# App label order: core/data/.../classifier/ModelLabels.kt (index -> class)
APP_LABELS = [
    "healthy", "Bacterial_spot", "Early_blight", "Late_blight", "Leaf_Mold",
    "Septoria_leaf_spot", "Spider_mites", "Target_Spot", "mosaic_virus", "Yellow_Leaf_Curl",
]
# PlantVillage folder -> app label
FOLDER_TO_LABEL = {
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
SCHEMES = {"raw_0_255 (app today)": (0.0, 1.0), "minus1_1": (127.5, 127.5), "zero_1": (0.0, 255.0)}
SHORT = ["Hea", "Bac", "EBl", "LBl", "Mol", "Sep", "Spi", "Tgt", "Mos", "YLC"]


def load(path):
    img = Image.open(path)  # PIL does not apply EXIF rotation, matching the app
    img = img.convert("RGB").resize((224, 224), Image.BILINEAR)
    return np.asarray(img, dtype=np.float32)


def main():
    model, data_dir = sys.argv[1], sys.argv[2]
    dump = sys.argv[sys.argv.index("--dump") + 1] if "--dump" in sys.argv else None
    interp = Interpreter(model_path=model)
    interp.allocate_tensors()
    inp, out = interp.get_input_details()[0], interp.get_output_details()[0]

    samples = []
    for folder, label in FOLDER_TO_LABEL.items():
        d = os.path.join(data_dir, folder)
        for f in sorted(os.listdir(d)):
            if f.lower().endswith((".jpg", ".jpeg", ".png")):
                samples.append((os.path.join(d, f), APP_LABELS.index(label)))

    rows = []
    for name, (mean, std) in SCHEMES.items():
        confusion = np.zeros((10, 10), dtype=int)
        correct, confs = 0, []
        for path, truth in samples:
            x = ((load(path) - mean) / std)[None, ...]
            interp.set_tensor(inp["index"], x)
            interp.invoke()
            p = interp.get_tensor(out["index"])[0]
            pred = int(np.argmax(p))
            confusion[truth, pred] += 1
            correct += pred == truth
            confs.append(float(p[pred]))
            rows.append([name, os.path.relpath(path, data_dir), APP_LABELS[truth], APP_LABELS[pred], f"{p[pred]:.4f}"])
        print(f"\n=== {name}: accuracy {correct}/{len(samples)} = {correct / len(samples):.1%}, "
              f"mean top-1 confidence {np.mean(confs):.3f}, top-1 >= 0.60: {np.mean(np.array(confs) >= 0.6):.0%}")
        print("rows = true class, cols = predicted index (app label order)")
        print("      " + " ".join(f"{s:>3}" for s in SHORT))
        for i, s in enumerate(SHORT):
            print(f"  {s} " + " ".join(f"{v:>3}" for v in confusion[i]))
    if dump:
        with open(dump, "w", newline="") as fh:
            csv.writer(fh).writerows([["scheme", "file", "truth", "pred", "conf"]] + rows)


if __name__ == "__main__":
    main()
