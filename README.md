<h1 align="center">Tomato leaf disease prediction machine learning app</h1>

<p align="center">
  Pick a photo of a tomato leaf. The app tells you whether it looks healthy or which of nine diseases it most likely has. Everything runs on your phone.
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4.21-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.11.01-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="LiteRT" src="https://img.shields.io/badge/LiteRT-2.4.0-FF6F00?logo=tensorflow&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-24%20(Android%207.0)-3DDC84?logo=android&logoColor=white">
</p>

---

## Demo

<p align="center">
  <img src="docs/demo.gif" alt="Picking a tomato leaf photo, analysing it as early blight at 94% confidence, opening the disease info screen, then analysing a healthy leaf at 96%" width="320">
</p>

v2 running on a Pixel 6a (Android 17): pick a leaf photo, analyse it, open the disease info screen, then check a second, healthy leaf. The video is sped up where nothing changes on screen. There is also an [MP4 version](docs/demo.mp4). Leaf photos are from the [PlantVillage dataset](https://github.com/spMohanty/PlantVillage-Dataset).

<details>
<summary>v1 demo (Java + XML views)</summary>

https://user-images.githubusercontent.com/61627836/186446347-5a76f82d-5000-4fe1-96ef-8fc3d0ce913d.mp4

v2 keeps the same flow (pick → predict → learn more) but rebuilds it in Kotlin and Compose. It also adds a confidence percentage, the top 3 results and a "not sure" state.
</details>

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Requirements](#requirements)
- [Build and test](#build-and-test)
- [Model contract](#model-contract)
- [Training the model](#training-the-model)
- [Privacy](#privacy)
- [Disclaimer](#disclaimer)
- [Known limitations and open questions](#known-limitations-and-open-questions)
- [Device verification](#device-verification)

## Features

- **On-device classification** into 10 classes: healthy plus 9 tomato leaf diseases.
- **Confidence and ranking:** the top prediction as a percentage, plus the top 3 classes with confidence bars.
- **"Not confident" state** when the top score is below a threshold (0.60 by default).
- **Disease info:** a short summary for each disease and a link to an extension-service guide, opened in a Custom Tab.
- **System photo picker:** no storage permission is needed.

## Tech stack

| Area | Library | Version |
|---|---|---|
| Language | Kotlin (K2, built into AGP 9) | 2.4.21 |
| UI | Jetpack Compose BOM, Material 3 | 2026.11.01 (Compose 1.13, Material 3 1.5) |
| Navigation | Navigation 3 (typed keys, app-owned back stack) | 1.2.2 |
| State | Lifecycle ViewModel, `StateFlow`, kotlinx.coroutines | 2.11.1 / 1.11.0 |
| DI | Hilt + KSP | 2.60.1 / 2.3.14 |
| ML runtime | LiteRT `CompiledModel` API, CPU, bundled | 2.4.0 |
| Images | Coil (preview only) | 3.6.3 |
| Other | core-splashscreen, AndroidX Browser (Custom Tabs), kotlinx.serialization | 1.2.0 / 1.10.0 / 1.13.0 |
| Build | Android Gradle Plugin, Gradle | 9.5.3 / 9.9.2 |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, `java-test-fixtures` fakes | 4.13.2 / 1.11.0 / 1.2.1 |
| SDK levels | compileSdk / targetSdk / minSdk | 37.2 / 37 / 24 |

## Architecture

Clean architecture with four Gradle modules. Dependencies point inwards only:

```mermaid
flowchart TD
    app[":app<br/><i>Activity, Nav3 host, theme, DI wiring</i>"]
    feature[":feature:diagnosis<br/><i>Compose screens, ViewModels</i>"]
    data[":core:data<br/><i>LiteRT, image decoding, disease content</i>"]
    domain[":core:domain<br/><i>pure Kotlin/JVM: models, interfaces, use cases</i>"]

    app --> feature
    app --> data
    app --> domain
    feature --> domain
    data --> domain
```

**Dependency rule**
- `:core:domain` depends on nothing. It is a plain Kotlin JVM module, so importing an Android class there is a compile error, not just a convention.
- `:core:data` and `:feature:diagnosis` depend only on `:core:domain`, never on each other.
- `:app` is the only module that sees everything. It wires implementations to interfaces.

### How the layers map to SOLID

| Principle | Where it shows up |
|---|---|
| **SRP** (single responsibility) | Each class has one job. `DiagnoseLeafUseCase` ranks predictions and applies the confidence policy. `PixelTensorConverter` turns pixels into the input tensor. `ScoreMapper` turns raw scores into `Prediction`s. `LiteRtInferenceEngine` only runs the model. `StaticDiseaseInfoRepository` only serves content. `DiagnosisViewModel` only owns screen state. |
| **OCP** (open/closed) | `ConfidencePolicy` is a `fun interface`. A different rule, such as a margin between the top two scores, is a new class. `DiagnoseLeafUseCase` does not change. Normalization lives in a single `ModelSpec`, so changing the preprocessing does not touch the classifier. |
| **LSP** (Liskov substitution) | `LiteRtLeafDiseaseClassifier` and the test `FakeLeafDiseaseClassifier` both honour the `LeafDiseaseClassifier` contract: one `Prediction` per class, `ClassificationException` for expected failures, and `CancellationException` always propagates. Either can be given to `DiagnoseLeafUseCase`, and the domain tests rely on that. |
| **ISP** (interface segregation) | Interfaces are small and specific to the client. `LeafDiseaseClassifier`, `DiseaseInfoRepository` and `ConfidencePolicy` each have one method. Inside data, `ImageTensorSource` and `InferenceEngine` split "get pixels" from "run model". The feature module sees only use cases, never LiteRT or `Bitmap` APIs. |
| **DIP** (dependency inversion) | The domain owns the abstractions. Data implements them, and Hilt binds them in `DataModule`. The use cases have no DI annotations; `:app`'s `DomainModule` builds them. High-level policy (`DiagnoseLeafUseCase`) never refers to a LiteRT type. |

### Module layout

```
core/domain/        model/, classifier/, repository/, usecase/   (+ src/testFixtures fakes)
core/data/          classifier/, inference/, image/, info/, di/  (+ assets/tomato_leaf_disease_v2.tflite)
feature/diagnosis/  DiagnosisScreen + ViewModel, info/ DiseaseInfoScreen + ViewModel, components/
app/                MainActivity, navigation/, di/DomainModule, ui/theme/
```

Inference runs on one dedicated single-thread dispatcher (`litert-inference`), which creates, runs and closes the `CompiledModel`. Image decoding runs on `Dispatchers.IO`; resizing and tensor conversion run on `Dispatchers.Default`. The model is loaded lazily on the first analysis and kept for the life of the process.

## Requirements

- **JDK 17.** AGP 9.5 and Gradle 9.9 need it.
- **Android Studio** with AGP 9.5 support. See the [AGP and Android Studio compatibility table](https://developer.android.com/build/releases/gradle-plugin#android_gradle_plugin_and_android_studio_compatibility).
- **Android SDK platform 37.2** (`compileSdk 37`, `compileSdkMinor 2`). Compose 1.13 requires it.
- **Device or emulator on Android 7.0 (API 24) or newer.** LiteRT ABIs: arm64-v8a, armeabi-v7a, x86_64 (no 32-bit x86).

> **minSdk went from 21 to 24 in v2.** LiteRT 2.x, Compose 1.13 and Material 3 1.5 all declare minSdk 24. Devices on Android 5.0–6.0 stay on v1.

## Build and test

```bash
# Build the debug APK and run every unit test, including the pure-JVM domain module
./gradlew assembleDebug testDebugUnitTest

# Domain tests only
./gradlew :core:domain:test

# Instrumented tests (needs a device or emulator)
./gradlew :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

`:core:domain` is a JVM module, so it has no `testDebugUnitTest` task of its own. An alias task depends on `test`, so the command above covers it.

## Model contract

The model is `core/data/src/main/assets/tomato_leaf_disease_v2.tflite`. It has no TFLite metadata, so the contract lives in code (`ModelSpec.TomatoLeafV2`, `ModelLabels`) and in [`tools/model-training/metrics_v2.json`](tools/model-training/metrics_v2.json).

| | |
|---|---|
| Architecture | MobileNetV3-Large (ImageNet weights), fine-tuned. Float16 weights. |
| File | 6,056,228 bytes, sha256 prefix `3d3cee732509f292`, TFL3 |
| Ops | Builtins only: CONV_2D, DEPTHWISE_CONV_2D, HARD_SWISH, MUL, ADD, MEAN, FULLY_CONNECTED, SOFTMAX, plus DEQUANTIZE for the float16 weights. No custom or Flex ops. |
| Input | `serving_default_image:0`, float32 `[1, 224, 224, 3]` (NHWC, RGB, **raw 0–255**) |
| Output | `StatefulPartitionedCall_1:0`, float32 `[1, 10]`, softmax |
| Signature | `serving_default` (input `image`, output `output_0`) |

**Label order** (output index → class). Training writes the same order to `labels.txt`.

| # | Label | # | Label |
|---|---|---|---|
| 0 | Healthy | 5 | Septoria leaf spot |
| 1 | Bacterial spot | 6 | Spider mites |
| 2 | Early blight | 7 | Target spot |
| 3 | Late blight | 8 | Tomato mosaic virus |
| 4 | Leaf mold | 9 | Tomato yellow leaf curl virus |

**Preprocessing:**
1. Decode the image at full size, without applying EXIF rotation. Images over 4096 px on a side are subsampled to avoid running out of memory.
2. Bilinear-resize to 224×224 by stretching. No crop.
3. Write R, G, B as float32 **0–255** (`ModelSpec` mean 0, std 1), row-major. Alpha is dropped. The model rescales to [-1, 1] itself, so the app cannot get the scaling wrong. v1 did: it fed 0–255 to a model that expected [-1, 1].

### Accuracy

Measured on photos held out from training, with the app's preprocessing. Full confusion matrices are in [`metrics_v2.json`](tools/model-training/metrics_v2.json).

| Test set | Previous model (v1 file, [-1, 1]) | **Current model (v2)** |
|---|---|---|
| PlantVillage lab photos (1,821; one leaf, plain background) | 95.1% | **99.0%** |
| PlantDoc field photos (69; real gardens, cluttered backgrounds) | 21.7% | **55.1%** |

- **The previous model fell apart on field photos.** It labelled almost every field photo "Late blight", at 99% confidence. Its PlantVillage score is also probably optimistic, because it was most likely trained on PlantVillage.
- **The v2 model is honest about field photos.** Its mean confidence there is 0.70, and 36% of its field predictions fall below the 0.60 threshold, so the app says "not sure" instead of guessing.
- **Field accuracy is still weak and the test set is small.** v2 got 0/10 mosaic virus and 2/9 bacterial spot photos right. The field set has no spider-mite or target-spot photos, because PlantDoc has none for those classes.

## Training the model

[`tools/model-training/train_tomato_model.ipynb`](tools/model-training/train_tomato_model.ipynb) retrains the model from scratch in Google Colab (T4 GPU, about 45 minutes). The same code is in `train_tomato_model.py` for diffs.

- **Data:** [PlantVillage](https://github.com/spMohanty/PlantVillage-Dataset) tomato photos (`raw/color`, split 80/10/10 with seed 42), plus the tomato classes of [PlantDoc](https://github.com/pratikkayal/PlantDoc-Dataset) (CC BY 4.0). PlantDoc's training photos are oversampled 4×, because field photos are scarce.
- **Training:** heavy augmentation (flips, rotation, zoom, translation, contrast, brightness, saturation, hue) and square-root class weights. The new classification layer is trained for 6 epochs with the backbone frozen. Then the whole network is fine-tuned for up to 14 epochs with cosine learning-rate decay, keeping the epoch with the best validation accuracy.
- **Output:** it scores both the new model and the current app model (the v1 file, pinned to a commit) on the held-out sets, then downloads `tomato_leaf_v2.zip`. The zip holds float16 and float32 `.tflite` files, `labels.txt` and `metrics.json`.

To ship a retrained model, copy the float16 file to `core/data/src/main/assets/` and update `ModelSpec`. If you change the label order or the input size, update `ModelLabels` too. [`tools/model-eval/evaluate.py`](tools/model-eval/evaluate.py) checks a `.tflite` file locally with the app's preprocessing.

## Privacy

- **Inference is fully on-device.** Photos never leave the phone. The app has no backend, analytics or accounts.
- **The app's own manifest requests no permissions, and the app never asks for a runtime (dangerous) permission.** The system photo picker replaces the old storage permission. The app does not hold `INTERNET`. The "Open treatment guide" link on the disease info screen opens in your browser through a Custom Tab.
- **Some permissions are merged in by dependencies.** LiteRT pulls in Play AI-pack delivery, which uses WorkManager and merges three normal-level, install-time permissions: `WAKE_LOCK`, `ACCESS_NETWORK_STATE` and `RECEIVE_BOOT_COMPLETED`. AndroidX Core adds its signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. The `FOREGROUND_SERVICE` permissions that LiteRT also merges are stripped, because this app ships no AI packs.

## Disclaimer

This app is **informational only and is not professional agronomic or plant-pathology advice.** Predictions can be wrong, especially on photos unlike the training data (other plants, poor lighting, several leaves in one frame). Before you treat or remove plants, confirm a diagnosis with your local agricultural extension service or a qualified expert.

## Known limitations and open questions

- **Field accuracy is only 55%, measured on 69 photos.** More real field photos, especially of mosaic virus, bacterial spot, spider mites and target spot, are the most useful next step. PlantDoc photos often show several leaves or a whole plant, so cropping to one leaf before analysis would probably help too.
- **EXIF rotation is not applied** before inference, the same as v1. The Coil preview shows rotated photos upright, but the model sees them unrotated. Applying EXIF would change the model's input, so it is left as an explicit decision.
- **The 0.60 confidence threshold is uncalibrated.** It is a starting point (`ThresholdConfidencePolicy.DEFAULT_MIN_CONFIDENCE`), not a value tuned on a validation set.
- **No golden-output test.** The model was replaced (see [Accuracy](#accuracy)), so matching v1's output is no longer the goal. `ModelInferenceInstrumentedTest` can compare against a stored output of the current model if one is added.
- **CPU only.** v1 declared a GPU dependency but never used it. GPU acceleration is not enabled.
- **The launcher icon is still the Android Studio template icon** (green grid and Android robot), the same as in v1. The splash screen shows it too. v1's unused "Plant ML app" artwork (`mipmap-*/ic_launcher_foreground.png`) was removed as dead code in v2; it can be restored from git history if you want to turn it into a proper adaptive icon.

## Device verification

One row per device. Max abs difference is measured against a v1 golden output; Top-1 is the top class the app reported for the test image. There is no v1 golden yet (see above), so max abs difference is n/a.

| Device | Android | LiteRT version | Accelerator | Max abs difference | Top-1 |
|---|---|---|---|---|---|
| Pixel 10 Pro emulator (AVD, arm64-v8a) | 17 | 2.4.0 | CPU (XNNPACK, 71/71 ops delegated) | n/a (no golden) | Septoria leaf spot, 100% (synthetic 640×640 leaf drawing with brown ringed spots, not a real photo; raw 0–255 build) |
| Pixel 6a (arm64-v8a) | 17 | 2.4.0 | CPU | n/a (no golden) | v1 model: 4/4 correct on PlantVillage photos with [-1, 1] scaling, 0/4 with raw 0–255. **v2 model, 4 held-out PlantDoc field photos:** yellow leaf curl 93% ✓, late blight 94% ✓, Septoria → leaf mold 87% ✗, early blight 53% (shown as "not sure"). Same top-1 as the offline Python check (the old model called all 4 "Late blight"). |

On the same emulator, `:core:data:connectedDebugAndroidTest` ran 5 tests: 4 passed and 1 was skipped (the golden comparison, because no golden file exists). `:app:connectedDebugAndroidTest` ran 1 test, which passed.

---

<div align="right">
  <b><a href="#readme">↥ back to top</a></b>
</div>
