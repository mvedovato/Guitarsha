# GuitarSHA 🎸

Controlador Bluetooth para pedal de guitarra basado en ESP32 / HC-06.

La app permite controlar en tiempo real los parámetros:
- **Volume**
- **Drive**
- **Tone**

mediante perillas circulares (knobs) y botones incrementales, enviando los cambios por Bluetooth de forma eficiente.

---

## 📱 Características

- Conexión Bluetooth clásica (SPP)
- Envío incremental de parámetros (solo cuando cambian)
- Controles tipo pedal real
- Indicador de estado Bluetooth
- Compatible con módulos HC-05 / HC-06
- Arquitectura simple y extensible

---

## 🎛️ Controles

Cada parámetro tiene:
- Perilla circular táctil
- Botones `+` y `-`
- Valor numérico visible en el centro

Parámetros:
- `VOL` → Volume
- `DRV` → Drive
- `TON` → Tone

---

## 📡 Protocolo Bluetooth

Cada cambio se envía como frame ASCII con checksum XOR:

V,50,<CHK<
D,30,<CHK<
T,75,<CHK<


Donde:
- `V / D / T` = parámetro
- valor = 0–100
- `CHK` = XOR de los bytes anteriores

---

## 🔐 Permisos

En Android 12+ se solicita:
- `BLUETOOTH_CONNECT`

La app maneja la solicitud de permisos en tiempo de ejecución.

---

## 🧱 Tecnologías

- Kotlin
- Android SDK
- Bluetooth Classic (SPP)
- CircularSeekBar (me.tankery.lib)

---

## 🚀 Próximas mejoras

- Presets (guardar / cargar)
- Animaciones suaves en knobs
- Feedback desde el microcontrolador
- Modo sincronizado con hardware

---

## 🛠️ Hardware esperado

- ESP32 o Arduino compatible
- Módulo Bluetooth HC-05 / HC-06
- Pedal analógico o digital

---

## 👤 Autor

Proyecto personal / experimental  
Pensado para control de efectos de guitarra en tiempo real 🎶

