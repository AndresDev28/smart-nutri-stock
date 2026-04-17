# Changelog

## [2.6.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.5.0...v2.6.0) (2026-04-17)


### Features

* add local push notifications for expiry alerts (Feature 5) ([8828536](https://github.com/AndresDev28/smart-nutri-stock/commit/8828536e00d690cbeceab31e2f5fa3556f2bae01))


### Bug Fixes

* add POST_NOTIFICATIONS runtime permission check before notify() in NotificationHelper ([10c42a1](https://github.com/AndresDev28/smart-nutri-stock/commit/10c42a1562be309e532c08031384f97420e33d2b))

## [2.5.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.4.0...v2.5.0) (2026-04-09)


### Features

* implement sequential ML Kit OCR scanning with proper CameraX lifecycle handoff and batch separation logic ([f2bce32](https://github.com/AndresDev28/smart-nutri-stock/commit/f2bce321d65d3948668b360ec868d4ad63c0efb5))

## [2.4.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.3.0...v2.4.0) (2026-04-08)


### Features

* implement inventory workflow buttons and action filtering v2.3.0 ([596323b](https://github.com/AndresDev28/smart-nutri-stock/commit/596323b5e7acf27e4ba940b8d925bc0afdd7c028))
* implement report export system (CSV/PDF) v2.4.0 ([da45951](https://github.com/AndresDev28/smart-nutri-stock/commit/da45951715ca61181d3eb924926a93e75d8d7e5e))


### Bug Fixes

* add missing ExportInventoryUseCase to HistoryViewModelFilterTest ([4e8312d](https://github.com/AndresDev28/smart-nutri-stock/commit/4e8312df3c20d97b64d8b1ea7de61f967dea2a7f))

## [2.3.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.2.0...v2.3.0) (2026-04-06)


### Features

* **batch:** add edit and soft-delete with undo support ([0bf45b4](https://github.com/AndresDev28/smart-nutri-stock/commit/0bf45b402c51fd0fa066433b38b20441cb242874))

## [2.2.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.1.1...v2.2.0) (2026-04-06)


### Features

* **branding:** implement premium adaptive launcher icons v2.1.0 ([38c3284](https://github.com/AndresDev28/smart-nutri-stock/commit/38c32849d0561e92620f658cb808a9ab82efadc1))

## [2.1.1](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.1.0...v2.1.1) (2026-04-03)


### Bug Fixes

* **dashboard:** correct expired counter displaying wrong field ([19bb096](https://github.com/AndresDev28/smart-nutri-stock/commit/19bb09680bc6b1a687c6929a43583ae5e792f1b7))

## [2.1.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.0.1...v2.1.0) (2026-03-27)


### Features

* **ci:** implement APK build and artifact upload for EPIC-08 ([5449991](https://github.com/AndresDev28/smart-nutri-stock/commit/54499919b0f2c7ecb0849d3352280bff34d52863))

## [2.0.1](https://github.com/AndresDev28/smart-nutri-stock/compare/v2.0.0...v2.0.1) (2026-03-26)


### Bug Fixes

* **history:** migrate HistoryScreen to use active_stocks data source ([7cf3a78](https://github.com/AndresDev28/smart-nutri-stock/commit/7cf3a78e5689b1b3e548e3672fe6c27f988c6bcc))
* **scanner:** chain product registration to batch input flow ([a81d601](https://github.com/AndresDev28/smart-nutri-stock/commit/a81d60173bf53fc7d6c43db5a380f75a3d9b2682))
* **semaphore:** use LocalDate comparison to fix precision bug ([eb0c2e6](https://github.com/AndresDev28/smart-nutri-stock/commit/eb0c2e6d4a4fc4d6ebb387364122e12e14b18524))
* **test:** implement missing findAllWithProductInfo() in DummyStockRepository ([b06b4c9](https://github.com/AndresDev28/smart-nutri-stock/commit/b06b4c957ffc33397d839388f73d5d7d239fec97))

## [2.0.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v1.1.0...v2.0.0) (2026-03-25)


### ⚠ BREAKING CHANGES

* **data:** Database schema update from v2 to v3 with active_stocks table
* **domain:** SemaphoreStatus enum values and CalculateStatusUseCase signature changed

### Features

* add haptic feedback on barcode detection ([e1b3b41](https://github.com/AndresDev28/smart-nutri-stock/commit/e1b3b41c3359906d62472be6664c16064114813e))
* **data:** restore Phase 1.5 Data Layer - ActiveStockEntity, StockDao, Database v3 ([261d000](https://github.com/AndresDev28/smart-nutri-stock/commit/261d0006e07b33b1c6171839405bd931f0836920))
* **di:** complete Phase 2.3 Hilt Implementation ([626a8a5](https://github.com/AndresDev28/smart-nutri-stock/commit/626a8a572c61296614200db4acd0c39f0df15271))
* implement HistoryScreen for EPIC-05 ([9108373](https://github.com/AndresDev28/smart-nutri-stock/commit/9108373be193b99375f25982e2ee97bd1b94e136))
* **scanner:** implement ProductRegistrationBottomSheet with thumb zone optimization ([ca0adb9](https://github.com/AndresDev28/smart-nutri-stock/commit/ca0adb91d457855f2d81b5e2ac8f07dcf9b79564))
* **scanner:** implement ScannerScreen with CameraX and ML Kit ([da4b4be](https://github.com/AndresDev28/smart-nutri-stock/commit/da4b4be06a5d0006c950b14e43fe8ce5255d507c))
* **scanner:** implement ScannerViewModel with state management ([96acae5](https://github.com/AndresDev28/smart-nutri-stock/commit/96acae56725539362e72c0965ea7ec732c29c365))
* **ui:** add Decathlon corporate color and Scaffold structure to MainActivity ([2c30cb7](https://github.com/AndresDev28/smart-nutri-stock/commit/2c30cb7b5fe77e2e7400c86f61ac35bc9a8b6d07))


### Bug Fixes

* enable ML Kit barcode detection with ImageAnalysis connection ([fa033bd](https://github.com/AndresDev28/smart-nutri-stock/commit/fa033bd3746b1eb4ea7279bdc639f166a53e9226))
* **phase2.4:** resolve verification warnings ([e838154](https://github.com/AndresDev28/smart-nutri-stock/commit/e838154f35bbe92053ca5e41cbc78f611815e071))
* resolve compilation errors and implement scanner navigation ([2958371](https://github.com/AndresDev28/smart-nutri-stock/commit/2958371d7ee6c9e6cfc0b3059703bd4e5afa55a6))
* resolve HistoryScreen recomposition loop and add history navigation ([6d9f989](https://github.com/AndresDev28/smart-nutri-stock/commit/6d9f989c566cdce5356d3004a01507f76b80274b))


### Code Refactoring

* **domain:** unify SemaphoreStatus and refactor use cases for batch management ([a582831](https://github.com/AndresDev28/smart-nutri-stock/commit/a5828314c13dca267540698d04850648b15a6920))

## [1.1.0](https://github.com/AndresDev28/smart-nutri-stock/compare/v1.0.0...v1.1.0) (2026-03-19)


### Features

* **EPIC-05:** implement dashboard with semaphore counters ([1a47e92](https://github.com/AndresDev28/smart-nutri-stock/commit/1a47e92538bb906d4973692769acc5a1e8427626))

## 1.0.0 (2026-03-19)


### Features

* **epic-02:** implement data layer with Room, repository pattern and validation ([927e9f1](https://github.com/AndresDev28/smart-nutri-stock/commit/927e9f102fe94e75ca4136c3393026dceacc1524))
* **epic-03:** implement RegisterProductUseCase with TDD ([a042770](https://github.com/AndresDev28/smart-nutri-stock/commit/a042770ca338d3f4b490d4d2997908f0609a4130))
* **EPIC-04:** implement 4 domain use cases with TDD ([b9f2e90](https://github.com/AndresDev28/smart-nutri-stock/commit/b9f2e90fd3019fd6bb92f2eaf5e8d217e3156873))
* initial project skeleton with clean architecture, hilt setup and verified build ([6f8688a](https://github.com/AndresDev28/smart-nutri-stock/commit/6f8688a735581efc98fc796a69d987c2565fcfba))
