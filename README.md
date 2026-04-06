<p align="center">
  <img src="assets/smart-nutri-stock-banner.png" alt="Smart Nutri-Stock Banner" width="100%">
</p>

# Smart Nutri-Stock 🟢🟡🔴

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-Clean--Arch-blue)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![CI/CD](https://img.shields.io/badge/CI--CD-GitHub--Actions-2088FF?logo=github-actions&logoColor=white)](https://github.com/features/actions)

**Smart Nutri-Stock** is a high-performance, B2B native Android solution specifically engineered for **Decathlon Gandía**. It streamlines the management of food expiry dates, eliminating manual errors and optimizing stock rotation through intelligent automation.

---

## 🎯 The Vision

Retail efficiency meets food sustainability. By leveraging **Google ML Kit**, this application transforms the traditional "daily check" into a seamless, high-speed digital process, ensuring that no product expires on the shelf and reducing food waste to near-zero.

## ✨ Key Features (MVP v2.1.0)

- **High-Speed Scanning**: Native barcode recognition for rapid stock intake.
- **Smart Semaphore System**: Real-time dynamic calculation of product status:
  - 🔴 **Expired/Today**: Critical priority.
  - 🟡 **Warning**: Expiring within 3 days.
  - 🟢 **Safe**: Controlled rotation.
- **Real-Time Dashboard**: Comprehensive stock health overview at a glance.
- **Advanced History**: Filterable ledger of all registered batches with dynamic status updates.
- **Rugged Hardware Support**: Fully optimized for **Samsung Galaxy XCover7** enterprise devices.

## 🛠 Tech Stack

- **Language**: Kotlin + Coroutines & Flow.
- **UI Framework**: Jetpack Compose (Declarative UI).
- **Local Storage**: Room Persistence Library (Offline-first).
- **Intelligence**: Google ML Kit (Barcode & Vision API).
- **Dependency Injection**: Koin / Hilt.
- **Testing**: 80+ Unit & Integration Tests (TDD approach).

## 🏗 Architecture

Built upon **Clean Architecture** principles to ensure scalability and maintainability:

- **Domain Layer**: Pure Kotlin logic with high-coverage use cases.
- **Data Layer**: Repository pattern with local data sources.
- **Presentation Layer**: MVVM pattern with state-driven UI.

## 🚀 Phase 2 Roadmap (Fase 2)

- [ ] **Full CRUD**: Edit and delete existing batches from history.
- [ ] **Workflow Actions**: One-tap "Discount Applied" (-20%) or "Removed" flags.
- [ ] **OCR Expiry Scanning**: Fully automated date extraction via computer vision.
- [ ] **Cloud Sync**: Multi-device synchronization via Supabase/Firebase.
- [ ] **Reporting**: Automated CSV/PDF export for inventory management.

---

_Developed with ❤️ for Decathlon Gandía - 2026_
