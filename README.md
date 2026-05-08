<p align="center">
  <img src="assets/smart-nutri-stock-banner.png" alt="Smart Nutri-Stock Banner" width="100%">
</p>

# Smart Nutri-Stock 🟢🟡🔴

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Architecture](https://img.shields.io/badge/Architecture-Clean--Arch-blue)](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
[![CI/CD](https://img.shields.io/badge/CI--CD-GitHub--Actions-2088FF?logo=github-actions&logoColor=white)](https://github.com/features/actions)

**Smart Nutri-Stock** es una solución nativa de alto rendimiento diseñada para la optimización de inventario perecedero. Nacida en el corazón de **Decathlon Gandía**, esta herramienta elimina el error humano y maximiza la productividad mediante la automatización inteligente del control de caducidades.

---

## 💡 Origen y Propósito: El Valor de la Subsidiariedad

Esta aplicación nace de una necesidad real detectada en el terreno: la gestión ineficiente del stock de Nutrición. El proyecto se basa en el **Principio de Subsidiariedad**, que defiende que las soluciones más efectivas son aquellas ideadas por quienes están más cerca de la acción.

Al empoderar al colaborador de tienda con tecnología avanzada, Smart Nutri-Stock busca:

- **Minimizar la Merma y el "Decot"**: Identificación proactiva de productos antes de su vencimiento.
- **Maximizar Ventas**: Implementación ágil de flujos de descuento (-20%) para asegurar la rotación.
- **Productividad Real**: Ganancia de tiempo para el equipo, permitiendo un enfoque total en el asesoramiento al deportista.

---

## ✨ Key Features (v2.7.0)

- **OCR Date Scanning**: Extracción automatizada de fechas de vencimiento mediante visión artificial (**Google ML Kit**), con soporte para normativa europea y años bisiestos.
- **High-Speed Barcode Recognition**: Escaneo masivo de productos para entradas de stock ultra rápidas.
- **Smart Semaphore System**: Cálculo dinámico del estado del lote:
  - 🔴 **Expirado/Hoy**: Prioridad crítica de retirada.
  - 🟡 **Próximo a Vencer**: Alerta de acción comercial (1-7 días).
  - 🟢 **Seguro**: Rotación controlada.
- **Workflow de Acciones**: Gestión con un solo toque (Aplicar descuento, Retirar, Eliminar).
- **Reporting Nativo**: Exportación de auditorías en **PDF con código de colores** y **CSV (RFC 4180)**.
- **Rugged Hardware Support**: Optimizado específicamente para dispositivos **Samsung Galaxy XCover7**.
- **Local Push Notifications (v2.6.0)**: WorkManager alerts para vencimientos diarios (POST_NOTIFICATIONS runtime permission).
- **Auth (v2.7.0)**: Integración Supabase Auth, sesión persistente via DataStore, logout UI en Dashboard.
- **Cloud Sync (v2.7.0)**: Sincronización inmediata post-login, arquitectura offline-first, catálogo de productos en pull.

---

## 🛠 Tech Stack

- **Language**: Kotlin + Coroutines & Flow.
- **UI Framework**: Jetpack Compose (Material 3).
- **Local Storage**: Room Persistence Library (Offline-first).
- **Intelligence**: Google ML Kit (Barcode & Text Recognition API).
- **Dependency Injection**: Hilt.
- **Cloud Backend**: Supabase Kotlin SDK (Gotrue for Auth, PostgREST for Sync).
- **Local Persistence**: DataStore (session tokens), Room (offline-first database).
- **Testing**: 130+ Unit & Integration Tests (TDD approach).

## 🏗 Architecture

Implementación estricta de **Clean Architecture** para garantizar escalabilidad hacia todas las tiendas de **Decathlon España**:

- **Domain Layer**: Lógica de negocio pura y casos de uso de alta cobertura.
- **Data Layer**: Patrón Repository con fuentes de datos locales y lógica de persistencia robusta.
- **Presentation Layer**: Patrón MVVM con gestión de estado reactiva (StateFlow).

---

## 🚀 Phase 2 Roadmap (Fase de Escalamiento)

- [x] **Full CRUD**: Edición y borrado completo de lotes.
- [x] **Workflow Actions**: Botones de acción dinámica en tarjetas de producto.
- [x] **Reporting**: Exportación automatizada a PDF y CSV.
- [x] **OCR Expiry Scanning**: Automatización total de la entrada de fechas (v2.5.0).
- [x] **Local Push Notifications**: Alertas proactivas diarias mediante WorkManager (v2.6.0).
- [x] **Cloud Sync**: Sincronización inmediata post-login, offline-first, catálogo de productos (v2.7.0).

---

<p align="center">
  Desarrollado con 💙 por <a href="https://github.com/AndresDev28"><b>AndresDev</b></a> <br>
  <b>Decathlon Gandía</b> - 2026
</p>
