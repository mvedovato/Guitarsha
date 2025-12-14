# Changelog – GuitarSHA 🎸

Todas las notas importantes sobre cambios y mejoras en el proyecto.

---

## [1.0.0] – 2025-12-14
### Añadido
- Implementación básica de controles para **Volume, Drive y Tone** con CircularSeekBar.
- Botones `+` y `-` para cada parámetro.
- Envío de cambios a ESP32 vía **Bluetooth Classic (SPP)** con checksum XOR.
- Indicador de estado Bluetooth (`Conectado` / `Desconectado`).

### Mejorado
- Animación suave en los knobs al moverlos.
- Guardado y carga de **presets P1…P5** con SharedPreferences.
- Validaciones para evitar envíos innecesarios de datos cuando no cambian los valores.

### Corregido
- Manejo de permisos **BLUETOOTH_CONNECT** en Android 12+.
- Errores previos de referencias de `R` en botones y TextViews.

---

## [0.9.0] – 2025-12-10
### Añadido
- Prototipo inicial de UI con knobs y botones.
- Conexión Bluetooth básica.
- Envío de datos de parámetros en tiempo real.

---

## Notas
- Próximas mejoras: feedback desde microcontrolador, modo sincronizado, más presets, optimización del protocolo.
- Proyecto pensado para **ESP32 / HC-05 / HC-06** y control de pedal de guitarra en tiempo real.

