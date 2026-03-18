Este documento (SSOT) es el que deberás "alimentar" a cualquier agente de IA (como Cursor, Windsurf o el propio Claude/Gemini) cuando llegue el momento de generar el código, ya que contiene las reglas de negocio y la arquitectura de datos sin ambigüedades.

# 📄 MASTER SPEC: Smart Nutri-Stock v1.0 (Decathlon Gandía)

**Tipo de Documento:** PRD & Data Architecture (Fuente Única de Verdad)
**Propósito:** Contexto base para desarrollo SDD mediante agentes de IA.
**Stack Objetivo:** Android Nativo, Kotlin, Jetpack Compose, Room (SQLite), Google ML Kit (Barcode + Text Recognition), WorkManager.

---

## 1. CONTEXTO DE NEGOCIO Y BUSINESS CASE
Aplicación interna B2B para optimizar el control de caducidades en la sección de Nutrición.

* **Volumen a gestionar:** +2.600 unidades/mes.
* **Facturación del Universo:** ~25.000 €/mes (Datos Reales Enero 2026).
* **Problema:** Revisión manual lenta (16h/mes) y pérdida de producto por caducidad no detectada (estimado 2% merma = ~500€/mes).
* **Solución:** Digitalización híbrida (EAN + OCR) con sistema de alertas por "Semáforo". 
* **ROI Esperado:** Recuperación estimada de 360€/mes en margen mediante acción comercial anticipada y liberación de tiempo operativo.

---

## 2. FLUJO DE USUARIO (USER JOURNEY)

1.  **Recepción:** El usuario abre la app e inicia "Escanear Producto".
2.  **Identificación (Barcode):** Escanea el EAN. La app identifica el producto y su tamaño de pack (ej: caja x6, caja x20) consultando el catálogo.
3.  **Captura de Fecha (OCR):** La cámara enfoca la fecha (ej: "09/26"). La IA extrae el texto mediante Google ML Kit.
4.  **Pantalla de Confirmación:** La app muestra el producto, propone el día 1 del mes detectado (01/09/2026) y solicita confirmar la cantidad de unidades/packs ingresados.
5.  **Guardado:** El stock pasa al inventario activo y se clasifica automáticamente en el semáforo.

---

## 3. REQUISITOS FUNCIONALES (CORE FEATURES)

* **FR-01 (Escáner Híbrido):** Integración de Google ML Kit para leer EAN y texto secuencialmente o en la misma vista de cámara.
* **FR-02 (Multiplicador Inteligente):** Autocompletado de cantidades sugeridas según el `packSize` del catálogo maestro.
* **FR-03 (Fallback Manual):** Formulario UI completo para introducir EAN, Nombre y Fecha (vía DatePicker) si el hardware de cámara falla.
* **FR-04 (Gestión de Lotes):** Lógica de "Upsert". Si se escanea un EAN con una fecha que ya existe en el stock activo, se actualiza la cantidad (`Update`) en lugar de crear un duplicado (`Insert`).
* **FR-05 (Dashboard Semáforo):** Vista principal con contadores dinámicos: 
    * 🟢 **Verde:** (>3 meses)
    * 🟡 **Amarillo:** (<=30 días)
    * 🔴 **Rojo:** (<=15 días)

---

## 4. ARQUITECTURA DE DATOS Y LÓGICA (DATA PIPELINE)

El sistema utiliza un patrón de repositorio único *offline-first* con persistencia en Room.

### 4.1 Esquema de Base de Datos Local (Room)

**Tabla 1: `ProductCatalog` (Catálogo Maestro)**
* `ean` (String, Primary Key): Identificador único del producto.
* `name` (String): Nombre comercial.
* `packSize` (Int): Cantidad de unidades por pack (Default: 1).

**Tabla 2: `ActiveStock` (Inventario Físico)**
* `batchId` (String, UUID, Primary Key): Identificador único del lote ingresado.
* `ean` (String, Foreign Key -> `ProductCatalog.ean`): Relación con el catálogo.
* `expiryDate` (Long): Timestamp en milisegundos de la fecha de caducidad.
* `quantity` (Int): Unidades totales disponibles.
* `status` (Int o Enum): Estado actual (0: GREEN, 1: YELLOW, 2: RED).

### 4.2 Reglas de Negocio Estrictas para Agentes IA

1.  **Regla de Fecha (Conservadora):** Si el OCR extrae el formato `MM/YY` (ej. 07/26), la lógica de dominio **debe** transformarlo obligatoriamente al día 1 de ese mes (`01/07/2026`) para el cálculo de días restantes.
2.  **Regla de Motor Asíncrono (Semáforo):** Los estados no se calculan "on-the-fly" únicamente. Debe implementarse un **Android WorkManager** diario (ej. 06:00 AM) que recorra la tabla `ActiveStock`, compare `expiryDate` con la fecha actual y actualice la columna `status` para asegurar que las alertas estén al día sin intervención del usuario.
