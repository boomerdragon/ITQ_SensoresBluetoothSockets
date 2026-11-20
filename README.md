# 🎮 Sensores Bluetooth Sockets App 🚀

Una aplicación Android robusta desarrollada en **Java** utilizando la arquitectura **MVVM (Model-View-ViewModel)** para la captura inercial de hardware (Acelerómetro y Giroscopio) y su transmisión inalámbrica P2P a través de múltiples protocolos de comunicación: **Sockets TCP/IP**, **Servidor Web HTTP embebido** y **Bluetooth Classic SPP (Serial Port Profile)**.

---

## 🌟 Características Principales

### 📐 1. Telemetría Inercial en Tiempo Real (IMU)
- **Acelerómetro ($m/s^2$)**: Captura constante de ejes $X, Y, Z$ y cálculo dinámico de magnitud $||A|| = \sqrt{x^2 + y^2 + z^2}$.
- **Giroscopio ($rad/s$)**: Monitoreo de la velocidad angular tridimensional del dispositivo.

### 🌐 2. Servidor Web HTTP Embebido (Puerto 8080)
- **Dashboard Web Interactivo**: La aplicación incluye su propio servidor HTTP interno. Al abrir `http://<IP_CELULAR>:8080` en cualquier navegador (PC, tablet o smartphone) en la misma red Wi-Fi, se despliega una interfaz web moderna en tema oscuro con barras indicadoras animadas.
- **API REST JSON (`/api/telemetria`)**: Endpoint JSON de alta velocidad consultado automáticamente por el dashboard web vía `fetch()`.
- **📸 Botón "Congelar Lectura"**: Permite realizar una captura estática (*snapshot*) de la telemetría actual directamente en la página web y enviar una notificación P2P a la app Android.

### 🔌 3. Redes P2P por Sockets TCP/IP (Puerto 8888)
- **Servidor/Cliente Socket TCP**: Escucha y transmisión telemétrica local en segundo plano mediante `ServerSocket` y `Socket` en hilos de ejecución dedicados.

### 📶 4. Conectividad Bluetooth Classic SPP (Serial Port Profile)
- **Servidor y Cliente RFCOMM**: Comunicación canalizada mediante el UUID estándar SPP (`00001101-0000-1000-8000-00805F9B34FB`).
- **Transmisión Móvil a Móvil**: Permite conectar dos teléfonos Android para intercambiar paquetes telemétricos en tiempo real.
- **Transmisión Móvil a PC**: Transmite la lectura inercial directamente a una computadora emparejada por Bluetooth (Puerto COM virtual).

### 🐍 5. Script Receptor en Python para PC (`recibir_telemetria_pc.py`)
- Script independiente en Python usando `pyserial` para escuchar y desplegar la telemetría en tiempo real desde la consola de comandos de Windows, macOS o Linux.

---

## 🏗️ Arquitectura del Proyecto

El proyecto sigue las mejores prácticas de desarrollo Android moderno bajo el patrón **MVVM**:

```
com.itq.sensoresbluetoothsocketsapp/
│
├── 📜 LecturaSensorDto.java          # DTO para lecturas de Acelerómetro y Giroscopio
├── 📜 PaqueteP2PDto.java            # DTO para empaquetado de mensajes P2P (TCP / BT)
├── 📜 ProyectoAdapter.java           # Adapter optimizado para el RecyclerView de paquetes
├── 📜 SensoresBluetoothRepository.java # Repositorio Singleton (IMU, Sockets TCP, HTTP, BT SPP)
├── 📜 SensoresBluetoothViewModel.java  # ViewModel desacoplado de la UI (LiveData)
└── 📜 MainActivity.java             # Control visual Arcade HUD y gestión de permisos
```

---

## 🛠️ Requisitos e Instalación

### Requisitos del Sistema
- **Android Studio**: Ladybug / 2024.2.1 o superior.
- **Min SDK**: API 24 (Android 7.0 Nougat).
- **Target SDK**: API 35 (Android 15) / Compile SDK 37.
- **JDK**: Java 17.

### Pasos para Ejecutar en Android Studio
1. Clona este repositorio o abre la carpeta en Android Studio:
   ```bash
   git clone https://github.com/tu-usuario/SensoresBluetoothSockets.git
   ```
2. Abre Android Studio y permite que **Gradle** sincronice el proyecto.
3. Conecta tu dispositivo físico mediante Depuración USB (recomendado para probar sensores físicos de hardware).
4. Haz clic en **Run 'app'** (`Shift + F10`).

---

## 📱 Guía de Uso

### 1. Servidor Web HTTP (Visualización en Navegador)
1. Conecta tu celular a la red Wi-Fi local.
2. Abre la app en tu teléfono; en la pantalla principal observarás la etiqueta **"Servidor Web HTTP Activo:"** seguida de la dirección (ejemplo: `http://192.168.1.50:8080`).
3. Abre esa URL en el navegador de tu computadora o teléfono.
4. Presiona el botón **"📸 TRANSMITIR Y CONGELAR LECTURA INERCIAL"** en la web para capturar una lectura fija en pantalla.

### 2. Transmisión Bluetooth a otro Celular Android
1. Empareja ambos teléfonos Android desde **Ajustes -> Bluetooth**.
2. Abre la app en ambos teléfonos. Verás el estado **"Servidor BT SPP Escuchando"**.
3. En el teléfono emisor, presiona **"TRANSMITIR VÍA BLUETOOTH SPP"**.
4. Selecciona el segundo celular de la lista de dispositivos emparejados.
5. Los datos inerciales aparecerán al instante en el historial del celular receptor.

### 3. Transmisión Bluetooth a una PC (Python)
1. Empareja tu celular Android con tu PC por Bluetooth (**Configuración de Windows -> Bluetooth**).
2. En Windows, revisa el puerto COM asignado al celular (ejemplo: `COM3`).
3. Instala la dependencia de Python:
   ```bash
   pip install pyserial
   ```
4. Ejecuta el script receptor incluido en el proyecto:
   ```bash
   python recibir_telemetria_pc.py --puerto COM3
   ```
5. En la app Android, presiona **"TRANSMITIR VÍA BLUETOOTH SPP"**, elige tu PC y observa los datos recibidos en la terminal de Python.

---

## 🔐 Permisos Declarados (`AndroidManifest.xml`)

Para garantizar compatibilidad con Android 12+ (API 31+), la app gestiona los siguientes permisos de red y conectividad Bluetooth:
- `INTERNET` y `ACCESS_NETWORK_STATE` (Para sockets TCP y servidor HTTP embebido).
- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (Para comunicación RFCOMM/SPP).
- `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` (Requeridos para el descubrimiento de hardware de red en Android).

---

## 🤝 Contribuciones y Licencia

Desarrollado como proyecto académico de sistemas embebidos, redes P2P y procesamiento inercial de hardware. ¡Las contribuciones y sugerencias son bienvenidas!

Distribuido bajo la Licencia **MIT**. Consulta el archivo `LICENSE` para más información.
