# Guía de Despliegue - Smart Nutri-Stock

Este documento explica cómo descargar e instalar la aplicación Smart Nutri-Stock en los dispositivos de las tiendas Decathlon.

---

## 📥 Instrucciones de Descarga

Los archivos APK se generan automáticamente a través de GitHub Actions cada vez que se hace merge a la rama `main`.

### Pasos para descargar:

1. **Acceder al repositorio**: Ve al repositorio de Smart Nutri-Stock en GitHub

2. **Ir a la pestaña "Actions"**:
   - Haz clic en la pestaña "Actions" en la parte superior del repositorio

3. **Seleccionar un workflow**:
   - Busca el workflow llamado "Android CI (Quality, Testing & Build)"
   - Verás una lista de ejecuciones recientes (selecciona la más reciente con el icono ✓ verde)

4. **Descargar el artifact**:
   - Baja hasta la sección "Artifacts" en la parte inferior de la página del workflow
   - Haz clic en el archivo llamado `smart-nutri-stock-snapshot-apk`
   - El archivo se descargará como un archivo ZIP comprimido

5. **Extraer el APK**:
   - Descomprime el archivo ZIP
   - Dentro encontrarás el archivo `app-debug.apk` (este es el instalable)

> **⚠️ Importante**: Los archivos APK se mantienen disponibles por **30 días** después de la generación. Si el artifact ha expirado, contacta al equipo de desarrollo para generar una nueva versión.

---

## 📱 Instalación en Dispositivo

Antes de instalar, asegúrate de habilitar la instalación de aplicaciones desconocidas.

### Paso 1: Habilitar aplicaciones desconocidas

1. Ve a **Ajustes** > **Seguridad y privacidad** (o **Seguridad** dependiendo del dispositivo)
2. Busca la opción **"Instalar apps desconocidas"** o **"Fuentes desconocidas"**
3. Selecciona la app que usarás para instalar (por ejemplo: **Archivos**, **Gestor de archivos**, o tu navegador)
4. Activa el permiso: **"Permitir la instalación de aplicaciones"**

### Paso 2: Permitir en Play Protect (si aplica)

Es posible que Google Play Protect bloquee la instalación al detectar que es un APK interno:

1. Si ves una alerta de **Play Protect** diciendo "No se pudo analizar el paquete" o similar
2. Toca **"Más información"** o **"Detalles"**
3. Selecciona **"Instalar de todos modos"** o **"Permitir"**
4. Es posible que necesites confirmar una vez más

> **💡 Nota**: Esta alerta es normal para aplicaciones que no provienen de Google Play Store. Smart Nutri-Stock es segura y ha pasado todos los tests de calidad y seguridad.

### Paso 3: Instalar el APK

1. Abre el archivo `app-debug.apk` desde tu gestor de archivos o navegador
2. Toca **"Instalar"**
3. Espera a que finalice el proceso de instalación
4. Toca **"Abrir"** para iniciar la aplicación o **"Hecho"** para salir

### Paso 4: Configurar permisos iniciales

La primera vez que abras la aplicación, se te pedirán permisos necesarios:

- **Cámara**: Necesario para escanear códigos de barras de productos
- **Almacenamiento**: Necesario para guardar capturas de pantalla y exportar datos

Selecciona **"Permitir"** en ambos casos para usar todas las funcionalidades.

---

## 📲 Dispositivos Objetivo

Smart Nutri-Stock está optimizada para uso en tiendas Decathlon.

### Dispositivos Recomendados

| Dispositivo | Pantalla | Observaciones |
|------------|----------|---------------|
| **Samsung XCover7** | 6.3 pulgadas | Dispositivo principal - optimizado para uso con una mano |
| **Xiaomi (varios modelos)** | Variable | Probado y compatible |

### Requisitos Mínimos

- **Versión de Android**: Android 14 (API 34) o superior
- **Memoria RAM**: 4 GB recomendados
- **Espacio de almacenamiento**: 100 MB libres

> **📐 Diseño UI**: La interfaz está diseñada con targets de toque mínimo de 48dp para facilitar el uso con una sola mano, especialmente en el Samsung XCover7.

---

## 🔧 Solución de Problemas

### Problema: La aplicación no solicita el permiso de cámara

**Causa posible**: El permiso fue denegado previamente.

**Solución**:
1. Ve a **Ajustes** > **Aplicaciones** > **Smart Nutri-Stock**
2. Toca **Permisos**
3. Busca **Cámara**
4. Cambia a **"Permitir"**

### Problema: "Play Protect no pudo analizar el paquete"

**Causa posible**: Google Play Protec bloquea APKs que no vienen de Play Store.

**Solución**:
1. Toca **"Más información"**
2. Selecciona **"Instalar de todos modos"**
3. Si la opción no aparece, desactiva temporalmente **Mejora de detección de aplicaciones dañinas** en Google Play Protect

### Problema: La aplicación no aparece después de la instalación

**Causa posible**: Instalada en un perfil de trabajo o carpeta segura.

**Solución**:
1. Busca en todas las pantallas de inicio (puede estar en una carpeta)
2. Verifica en **Ajustes** > **Aplicaciones** > **Smart Nutri-Stock** si está instalada
3. Si aparece un icono de "maletín" o "oficina", es un perfil de trabajo

### Problema: El escaneo de códigos de barras no funciona

**Causa posible**: Permiso de cámara no concedido o hardware incompatible.

**Solución**:
1. Verifica que el permiso de cámara esté concedido (ver tabla anterior)
2. Asegúrate de que el dispositivo tenga una cámara funcional
3. Prueba en un entorno con buena iluminación

### Problema: La aplicación se cierra inesperadamente

**Causa posible**: Versión de Android incompatible o dispositivo sin recursos suficientes.

**Solución**:
1. Verifica que el dispositivo tenga Android 14 o superior
2. Cierra otras aplicaciones para liberar memoria
3. Si el problema persiste, reporta el issue al equipo de desarrollo

---

## 📋 Notas Adicionales

### Sobre el Workflow de CI/CD

El archivo `DEPLOYMENT.md` es parte del pipeline automatizado:

- **Workflow**: `.github/workflows/android-check.yml`
- **Nombre del artifact**: `smart-nutri-stock-snapshot-apk`
- **Retención**: 30 días
- **Trigger**: Merge a la rama `main` (después de pasar todos los tests y quality checks)

### Actualizaciones

Cada vez que se fusiona código a `main`:
1. Se ejecutan automáticamente los tests unitarios (60 escenarios TDD)
2. Se ejecutan las comprobaciones de calidad (Lint)
3. Se genera un nuevo APK de debug
4. El artifact se sube a GitHub Actions y está disponible para descarga

### Soporte

Si encuentras problemas no descritos en esta guía:
- Contacta al equipo de desarrollo interno
- Abre un issue en el repositorio de GitHub con:
  - Modelo del dispositivo
  - Versión de Android
  - Descripción detallada del problema
  - Capturas de pantalla si es posible

---

**Última actualización**: Marzo 2026
**Versión de documento**: 1.0
