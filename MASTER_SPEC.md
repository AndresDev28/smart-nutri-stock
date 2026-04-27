Este documento (SSOT) es el que deberás "alimentar" a cualquier agente de IA (como Cursor, Windsurf o el propio Claude/Gemini) cuando llegue el momento de generar el código, ya que contiene las reglas de negocio y la arquitectura de datos sin ambigüedades.

# 📄 MASTER SPEC: Smart Nutri-Stock v2.7.0 (Decathlon Gandía)

**Tipo de Documento:** PRD & Data Architecture (Fuente Única de Verdad)
**Propósito:** Contexto base para desarrollo SDD mediante agentes de IA.
**Stack Objetivo:** Android Nativo, Kotlin, Jetpack Compose, Room (SQLite), Google ML Kit (Barcode + OCR), WorkManager, Supabase (Auth + Postgrest), minSdk 26 (Optimized for Android 14 (API 34) on XCover7 devices).

---

## 1. CONTEXTO DE NEGOCIO Y BUSINESS CASE

Aplicación interna B2B para optimizar el control de caducidades en la sección de Nutrición.

- **Volumen a gestionar:** +2.600 unidades/mes.
- **Facturación del Universo:** ~25.000 €/mes (Datos Reales Enero 2026).
- **Problema:** Revisión manual lenta (16h/mes) y pérdida de producto por caducidad no detectada (estimado 2% merma = ~500€/mes).
- **Solución:** Digitalización híbrida (EAN + OCR) con sistema de alertas por "Semáforo".
- **ROI Esperado:** Recuperación estimada de 360€/mes en margen mediante acción comercial anticipada y liberación de tiempo operativo.

---

## 2. FLUJO DE USUARIO (USER JOURNEY)

1.  **Recepción:** El usuario abre la app e inicia "Escanear Producto".
2.  **Identificación (Barcode):** Escanea el EAN. La app identifica el producto y su tamaño de pack (ej: caja x6, caja x20) consultando el catálogo.
3.  **Captura de Fecha (OCR):** La cámara enfoca la fecha. La IA extrae el texto mediante Google ML Kit Text Recognition (Play Services version), disponible tanto en el BottomSheet de registro como en el ScannerScreen (BatchInputStep). Para formatos reducidos (ej: "09/26"), el sistema utiliza `YearMonth.atEndOfMonth()` para asignar la fecha al último día del mes.
4.  **Pantalla de Confirmación:** La app muestra el producto, la fecha extraída por OCR y solicita confirmar la cantidad de unidades/packs ingresados.
5.  **Guardado:** El stock pasa al inventario activo y se clasifica automáticamente en el semáforo.

---

## 3. REQUISITOS FUNCIONALES (CORE FEATURES)

- **FR-01 (Escáner Híbrido):** Integración de Google ML Kit para leer EAN y texto secuencialmente o en la misma vista de cámara.
- **FR-02 (Multiplicador Inteligente):** Autocompletado de cantidades sugeridas según el `packSize` del catálogo maestro.
- **FR-03 (Fallback Manual):** Formulario UI completo para introducir EAN, Nombre y Fecha (vía DatePicker) si el hardware de cámara falla.
- **FR-04 (Gestión de Lotes):** Lógica de "Upsert". Si se escanea un EAN con una fecha que ya existe en el stock activo, se actualiza la cantidad (`Update`) en lugar de crear un duplicado (`Insert`).
- **FR-05 (Dashboard Semáforo):** Vista principal con contadores dinámicos:
  - 🟢 **Verde:** (>3 meses)
  - 🟡 **Amarillo:** (<=30 días)
  - 🔴 **Rojo:** (<=15 días)

---

## 4. ARQUITECTURA DE DATOS Y LÓGICA (DATA PIPELINE)

El sistema utiliza un patrón de repositorio único _offline-first_ con persistencia en Room.

### 4.1 Esquema de Base de Datos Local (Room)

**Tabla 1: `ProductCatalog` (Catálogo Maestro)**

- `ean` (String, Primary Key): Identificador único del producto.
- `name` (String): Nombre comercial.
- `packSize` (Int): Cantidad de unidades por pack (Default: 1).

**Tabla 2: `ActiveStock` (Inventario Físico)**

- `batchId` (String, UUID, Primary Key): Identificador único del lote ingresado.
- `ean` (String, Foreign Key -> `ProductCatalog.ean`): Relación con el catálogo.
- `expiryDate` (Long): Timestamp en milisegundos de la fecha de caducidad.
- `quantity` (Int): Unidades totales disponibles.
- `status` (Int o Enum): Estado actual (0: GREEN, 1: YELLOW, 2: RED).

### 4.2 Reglas de Negocio Estrictas para Agentes IA

1.  **Regla de Fecha (Conservadora - EU Retail):** El sistema implementa la extracción de múltiples formatos (DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY, MM/YY, MM/YYYY, ISO YYYY-MM-DD). Si el OCR extrae el formato `MM/YY` (ej. 07/26), la lógica de dominio **debe** transformarlo obligatoriamente al último día válido de ese mes (`31/07/2026`) para el cálculo de días restantes, usando `YearMonth.of(year, month).atEndOfMonth()`.
2.  **Cálculo de Estado Dinámico (Semáforo):** Los estados se calculan en tiempo real mediante el `CalculateStatusUseCase` cada vez que se accede a la UI (Dashboard o Historia), asegurando una precisión del 100% basada en el reloj del sistema. No se requiere WorkManager ni actualizaciones programadas.
3.  **Unicidad de Lote (Batch Uniqueness):** Un registro en `ActiveStock` es único por la combinación de (EAN + expiryDate).
    - Mismo EAN + misma fecha $\rightarrow$ Sumar cantidades al registro existente.
    - Mismo EAN + fecha diferente $\rightarrow$ Crear nuevo registro de lote.

---

## 5. ALCANCE DEL SISTEMA (MVP + CORE PHASE 2)

### ✅ Implementado y Verificado (v2.7.0)

El sistema ha evolucionado desde el MVP inicial integrando las capacidades críticas de la Fase 2:

- **Core Inventory:** Escaneo de códigos de barras, gestión de lotes (Upsert), sistema de semáforo y Dashboard dinámico.
- **OCR de Fechas:** Extracción automática de fechas via ML Kit Text Recognition con normalización de formatos EU.
- **CRUD Completo:** Edición de lotes y Borrado Lógico (Soft Delete) con funcionalidad de Undo.
- **Auth & Cloud Sync:** Autenticación via Supabase Auth, persistencia de sesión y sincronización offline-first via SyncWorker.
- **Notificaciones Push:** Alertas diarias programadas via WorkManager para lotes amarillos y rojos.
- **Calidad:** Cobertura de pruebas TDD (150+ tests unitarios e instrumentados) y pipeline CI/CD.

### 🟡 Características Pendientes (Phase 2.4+)

Las siguientes funcionalidades están detalladas en `FUTURE_ROADMAP.md`:

- **Diseño Premium & UI:** Implementación de la paleta de colores premium, logo 'Barcode Leaf' y micro-interacciones.
- **E2E Validation:** Automatización de flujos completos en dispositivo físico Samsung XCover7.
- **Advanced Analytics:** Integración de Firebase Analytics y Crashlytics.
- **Accessibility:** Soporte para TalkBack y modo de alto contraste.

Nota: La aplicación implementa un acceso secuencial a la cámara: el escáner de códigos de barras debe liberar el hardware (`unbind()`) antes de que el módulo OCR se active, evitando conflictos de acceso al recurso.

Las herramientas de análisis de código que verifiquen el cumplimiento de este spec deben considerar las funcionalidades de la sección "Implementado" como requerimientos base del sistema.

---
