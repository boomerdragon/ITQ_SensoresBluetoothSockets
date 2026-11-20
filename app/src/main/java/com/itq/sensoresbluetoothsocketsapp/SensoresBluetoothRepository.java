package com.itq.sensoresbluetoothsocketsapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 🗄️ Repositorio Central Singleton (SensoresBluetoothRepository)
 *
 * ¿Qué es un Singleton?
 * Es un patrón de diseño que garantiza que SOLO EXISTA UNA INSTANCIA de esta clase en toda la app.
 * Esto es ideal para gestionar los sensores físicos del teléfono y la infraestructura de red
 * de manera centralizada sin duplicar conexiones ni hilos de ejecución.
 *
 * Responsabilidades:
 * 1. Capturar datos del Acelerómetro y Giroscopio mediante SensorEventListener.
 * 2. Ejecutar un Servidor TCP local (Puerto 8888).
 * 3. Ejecutar un Servidor Web HTTP embebido (Puerto 8080) con API JSON y Dashboard web.
 * 4. Ejecutar un Servidor/Cliente Bluetooth SPP (Serial Port Profile - UUID 00001101...).
 */
public class SensoresBluetoothRepository implements SensorEventListener {

    private static final String TAG = "SENSORES_P2P_REPO";
    private static final int PUERTO_TCP = 8888;
    private static final int PUERTO_HTTP = 8080;

    // 📶 UUID Estándar de Android para perfil de puerto serie Bluetooth Classic (SPP/RFCOMM)
    public static final UUID UUID_SPP_DEFAULT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // 🔒 Instancia única Singleton
    private static SensoresBluetoothRepository instancia;

    // 📱 Administradores del Hardware de Android
    private final SensorManager sensorManager;
    private final Sensor acelerometro;
    private final Sensor giroscopio;
    private final BluetoothManager bluetoothManager;

    // 🧵 Pool de Hilos (ExecutorService) para ejecutar operaciones de red fuera del Hilo Principal (UI Thread)
    private final ExecutorService executorRed = Executors.newCachedThreadPool();

    // 📊 Flujos de Datos Observables (MutableLiveData interna, LiveData externa)
    private final MutableLiveData<LecturaSensorDto> acelerometroLiveData = new MutableLiveData<>();
    private final MutableLiveData<LecturaSensorDto> giroscopioLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<PaqueteP2PDto>> mensajesP2PLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> estadoConexionLiveData = new MutableLiveData<>("Modo P2P En Espera");
    private final MutableLiveData<String> estadoServidorWebLiveData = new MutableLiveData<>("Servidor Web HTTP En Espera");
    private final MutableLiveData<String> estadoBluetoothLiveData = new MutableLiveData<>("Bluetooth SPP En Espera");

    // 🚦 Banderas booleanas de control para los hilos del servidor
    private boolean servidorActivo = false;
    private boolean servidorHttpActivo = false;
    private boolean servidorBluetoothActivo = false;

    /**
     * Constructor privado del Singleton. Inicializa los gestores de sensores y Bluetooth.
     */
    private SensoresBluetoothRepository(Context context) {
        Context appContext = context.getApplicationContext();
        bluetoothManager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        sensorManager = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            giroscopio = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } else {
            acelerometro = null;
            giroscopio = null;
        }
    }

    /**
     * Accesor estático thread-safe para obtener la instancia única del Repositorio.
     */
    public static synchronized SensoresBluetoothRepository getInstancia(Context context) {
        if (instancia == null) {
            instancia = new SensoresBluetoothRepository(context);
        }
        return instancia;
    }

    // --- Métodos de Acceso a LiveData ---

    public LiveData<LecturaSensorDto> getAcelerometroData() { return acelerometroLiveData; }
    public LiveData<LecturaSensorDto> getGiroscopioData() { return giroscopioLiveData; }
    public LiveData<List<PaqueteP2PDto>> getMensajesP2P() { return mensajesP2PLiveData; }
    public LiveData<String> getEstadoConexion() { return estadoConexionLiveData; }
    public LiveData<String> getEstadoServidorWeb() { return estadoServidorWebLiveData; }
    public LiveData<String> getEstadoBluetooth() { return estadoBluetoothLiveData; }

    /**
     * Obtiene el adaptador Bluetooth de manera moderna (BluetoothManager en API 31+).
     */
    @SuppressWarnings("deprecation")
    public BluetoothAdapter obtenerBluetoothAdapter() {
        if (bluetoothManager != null) {
            return bluetoothManager.getAdapter();
        }
        return BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Retorna el conjunto de dispositivos Bluetooth previamente vinculados.
     */
    public Set<BluetoothDevice> getDispositivosEmparejados() {
        try {
            BluetoothAdapter adapter = obtenerBluetoothAdapter();
            if (adapter != null && adapter.isEnabled()) {
                return adapter.getBondedDevices();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Permiso denegado al leer dispositivos emparejados", e);
        }
        return Collections.emptySet();
    }

    /**
     * Obtiene la dirección IP local asignada al teléfono dentro de la red Wi-Fi.
     */
    public String obtenerIpLocal() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "127.0.0.1";
    }

    /**
     * Registra la escucha de eventos de hardware para Acelerómetro y Giroscopio.
     */
    public void registrarSensores() {
        if (sensorManager == null) return;
        if (acelerometro != null) {
            sensorManager.registerListener(this, acelerometro, SensorManager.SENSOR_DELAY_UI);
        }
        if (giroscopio != null) {
            sensorManager.registerListener(this, giroscopio, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Desregistra los eventos de sensores para liberar recursos cuando la vista se pausa.
     */
    public void desregistrarSensores() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // --- Implementación de SensorEventListener (Captura Inercial) ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        long ts = System.currentTimeMillis();
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Magnitud escalar del vector aceleración: ||A|| = √(x² + y² + z²)
            float mag = (float) Math.sqrt(x * x + y * y + z * z);
            LecturaSensorDto dto = new LecturaSensorDto("ACELERÓMETRO", x, y, z, mag, ts);
            acelerometroLiveData.postValue(dto);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float mag = (float) Math.sqrt(x * x + y * y + z * z);
            LecturaSensorDto dto = new LecturaSensorDto("GIROSCAPIO", x, y, z, mag, ts);
            giroscopioLiveData.postValue(dto);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // --- Servidor P2P Socket TCP/IP (Puerto 8888) ---

    public void iniciarServidorTcp() {
        if (servidorActivo) return;
        servidorActivo = true;
        estadoConexionLiveData.postValue("Servidor TCP Escuchando en Puerto " + PUERTO_TCP);

        executorRed.execute(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PUERTO_TCP);
                while (servidorActivo && !serverSocket.isClosed()) {
                    Socket clientSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String linea = reader.readLine();

                    if (linea != null) {
                        PaqueteP2PDto paquete = new PaqueteP2PDto(
                                String.valueOf(System.currentTimeMillis()),
                                clientSocket.getInetAddress().getHostAddress(),
                                "SOCKET_TCP",
                                linea,
                                System.currentTimeMillis()
                        );
                        agregarMensajeP2P(paquete);
                    }
                    clientSocket.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Excepción en Servidor TCP P2P", e);
                estadoConexionLiveData.postValue("Servidor TCP Detenido");
            }
        });
    }

    // --- Servidor Web HTTP Embebido (Puerto 8080) ---

    public void iniciarServidorHttp() {
        if (servidorHttpActivo) return;
        servidorHttpActivo = true;

        String ipLocal = obtenerIpLocal();
        String urlServidor = "http://" + ipLocal + ":" + PUERTO_HTTP;
        estadoServidorWebLiveData.postValue("Servidor Web HTTP Activo: " + urlServidor);

        executorRed.execute(() -> {
            try {
                ServerSocket serverSocketHttp = new ServerSocket(PUERTO_HTTP);
                while (servidorHttpActivo && !serverSocketHttp.isClosed()) {
                    Socket clientSocket = serverSocketHttp.accept();
                    executorRed.execute(() -> atenderPeticionHttp(clientSocket));
                }
            } catch (Exception e) {
                Log.e(TAG, "Excepción en Servidor HTTP Web", e);
                estadoServidorWebLiveData.postValue("Servidor Web HTTP Detenido");
            }
        });
    }

    private void atenderPeticionHttp(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            String lineaInicial = reader.readLine();
            if (lineaInicial == null) {
                socket.close();
                return;
            }

            if (lineaInicial.startsWith("GET /api/telemetria") || lineaInicial.startsWith("GET /api/telemetry")) {
                responderJsonTelemetria(out);
            } else if (lineaInicial.startsWith("POST /api/transmitir") || lineaInicial.startsWith("GET /api/transmitir")) {
                atenderTransmitirP2PWeb(out);
            } else {
                responderHtmlDashboard(out);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error atendiendo cliente HTTP", e);
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void atenderTransmitirP2PWeb(OutputStream out) throws Exception {
        LecturaSensorDto acc = acelerometroLiveData.getValue();
        String lecturaStr = acc != null ? String.format(Locale.US, "X: %.2f | Y: %.2f | Z: %.2f", acc.getX(), acc.getY(), acc.getZ()) : "Sin Datos";
        enviarMensajeTcp("127.0.0.1", "Captura Web Congelada: " + lecturaStr);

        String json = "{\"status\":\"OK\",\"mensaje\":\"Lectura congelada transmitida a P2P\"}";
        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private void responderJsonTelemetria(OutputStream out) throws Exception {
        LecturaSensorDto acc = acelerometroLiveData.getValue();
        LecturaSensorDto gyro = giroscopioLiveData.getValue();

        String accJson = acc != null ? String.format(Locale.US,
                "{\"tipoSensor\":\"%s\",\"x\":%.4f,\"y\":%.4f,\"z\":%.4f,\"magnitud\":%.4f,\"timestamp\":%d}",
                acc.getTipoSensor(), acc.getX(), acc.getY(), acc.getZ(), acc.getMagnitud(), acc.getTimestamp())
                : "null";

        String gyroJson = gyro != null ? String.format(Locale.US,
                "{\"tipoSensor\":\"%s\",\"x\":%.4f,\"y\":%.4f,\"z\":%.4f,\"magnitud\":%.4f,\"timestamp\":%d}",
                gyro.getTipoSensor(), gyro.getX(), gyro.getY(), gyro.getZ(), gyro.getMagnitud(), gyro.getTimestamp())
                : "null";

        String json = String.format(Locale.US,
                "{\"ipLocal\":\"%s\",\"timestamp\":%d,\"acelerometro\":%s,\"giroscopio\":%s}",
                obtenerIpLocal(), System.currentTimeMillis(), accJson, gyroJson);

        byte[] bodyBytes = json.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    private void responderHtmlDashboard(OutputStream out) throws Exception {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Telemetría en Vivo - Android App</title>\n" +
                "    <style>\n" +
                "        body { background-color: #001A33; color: #FFFFFF; font-family: 'Segoe UI', Tahoma, sans-serif; padding: 20px; margin: 0; }\n" +
                "        h1 { color: #FF6600; text-align: center; font-size: 1.8rem; margin-bottom: 5px; }\n" +
                "        .subtitle { text-align: center; color: #00E5FF; margin-bottom: 20px; font-size: 0.95rem; }\n" +
                "        .status { text-align: center; color: #FFD700; font-weight: bold; margin-bottom: 20px; font-size: 1.1rem; }\n" +
                "        .btn-container { text-align: center; margin-bottom: 25px; }\n" +
                "        .btn-transmit { background: linear-gradient(135deg, #FF6600, #FF3300); color: #FFFFFF; border: none; padding: 14px 28px; border-radius: 8px; font-size: 1rem; font-weight: bold; cursor: pointer; box-shadow: 0 4px 15px rgba(255,102,0,0.4); transition: transform 0.1s ease, box-shadow 0.1s ease; }\n" +
                "        .btn-transmit:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(255,102,0,0.6); }\n" +
                "        .btn-transmit:active { transform: translateY(1px); }\n" +
                "        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; max-width: 900px; margin: 0 auto 20px auto; }\n" +
                "        .card { background-color: #002244; border-radius: 12px; padding: 20px; border: 1px solid #00E5FF33; box-shadow: 0 4px 15px rgba(0,229,255,0.1); }\n" +
                "        .card h2 { color: #00E5FF; margin-top: 0; font-size: 1.3rem; border-bottom: 2px solid #00E5FF; padding-bottom: 8px; }\n" +
                "        .value { font-size: 1.3rem; font-weight: bold; margin: 15px 0; color: #00FFCC; font-family: monospace; }\n" +
                "        .magnitude { color: #FFD700; font-size: 1.1rem; font-weight: bold; margin-bottom: 10px; }\n" +
                "        .gauge-bar { height: 12px; background: #003366; border-radius: 6px; overflow: hidden; margin-top: 10px; }\n" +
                "        .gauge-fill { height: 100%; width: 0%; background: linear-gradient(90deg, #00E5FF, #FF6600); transition: width 0.15s ease; }\n" +
                "        .badge { background: #FF6600; color: #FFFFFF; padding: 4px 10px; border-radius: 12px; font-weight: bold; font-size: 0.85rem; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>🚀 Telemetría Inercial en Vivo</h1>\n" +
                "    <div class=\"subtitle\">Sensores Bluetooth Sockets App</div>\n" +
                "    <div class=\"status\" id=\"status\">Conectando con Android App...</div>\n" +
                "\n" +
                "    <div class=\"btn-container\">\n" +
                "        <button class=\"btn-transmit\" onclick=\"congelarYTransmitir()\">\n" +
                "            📸 TRANSMITIR Y CONGELAR LECTURA INERCIAL\n" +
                "        </button>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"card\" id=\"frozen-card\" style=\"display: none; max-width: 900px; margin: 0 auto 25px auto; border-color: #FF6600;\">\n" +
                "        <div style=\"display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #FF6600; padding-bottom: 8px;\">\n" +
                "            <h2 style=\"color: #FF6600; margin: 0; border: none;\">❄️ Lectura Congelada (Snapshot)</h2>\n" +
                "            <span class=\"badge\" id=\"frozen-time\">--:--:--</span>\n" +
                "        </div>\n" +
                "        <div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 15px; margin-top: 15px;\">\n" +
                "            <div>\n" +
                "                <div style=\"color: #00E5FF; font-weight: bold;\">📐 Acelerómetro Capturado</div>\n" +
                "                <div class=\"value\" id=\"frozen-acc\" style=\"color: #FFD700; font-size: 1.1rem;\">--</div>\n" +
                "                <div class=\"magnitude\" id=\"frozen-acc-mag\" style=\"color: #FF6600;\">--</div>\n" +
                "            </div>\n" +
                "            <div>\n" +
                "                <div style=\"color: #00E5FF; font-weight: bold;\">🔄 Giroscopio Capturado</div>\n" +
                "                <div class=\"value\" id=\"frozen-gyro\" style=\"color: #FFD700; font-size: 1.1rem;\">--</div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"grid\">\n" +
                "        <div class=\"card\">\n" +
                "            <h2>📊 Acelerómetro</h2>\n" +
                "            <div class=\"value\" id=\"acc-values\">X: -- | Y: -- | Z: --</div>\n" +
                "            <div class=\"magnitude\" id=\"acc-mag\">Magnitud: -- m/s²</div>\n" +
                "            <div class=\"gauge-bar\"><div class=\"gauge-fill\" id=\"acc-bar\"></div></div>\n" +
                "        </div>\n" +
                "        <div class=\"card\">\n" +
                "            <h2>🌀 Giroscopio</h2>\n" +
                "            <div class=\"value\" id=\"gyro-values\">X: -- | Y: -- | Z: --</div>\n" +
                "            <div class=\"gauge-bar\"><div class=\"gauge-fill\" id=\"gyro-bar\"></div></div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let currentData = null;\n" +
                "\n" +
                "        async function fetchTelemetry() {\n" +
                "            try {\n" +
                "                const res = await fetch('/api/telemetria');\n" +
                "                if (!res.ok) throw new Error('HTTP ' + res.status);\n" +
                "                const data = await res.json();\n" +
                "                currentData = data;\n" +
                "                document.getElementById('status').innerText = '🟢 En vivo - IP: ' + data.ipLocal + ' | Actualizado: ' + new Date(data.timestamp).toLocaleTimeString();\n" +
                "                if (data.acelerometro) {\n" +
                "                    const acc = data.acelerometro;\n" +
                "                    document.getElementById('acc-values').innerText = `X: ${acc.x.toFixed(2)} | Y: ${acc.y.toFixed(2)} | Z: ${acc.z.toFixed(2)}`;\n" +
                "                    document.getElementById('acc-mag').innerText = `Magnitud: ${acc.magnitud.toFixed(2)} m/s²`;\n" +
                "                    const pct = Math.min(100, (acc.magnitud / 25) * 100);\n" +
                "                    document.getElementById('acc-bar').style.width = pct + '%';\n" +
                "                }\n" +
                "                if (data.giroscopio) {\n" +
                "                    const gyro = data.giroscopio;\n" +
                "                    document.getElementById('gyro-values').innerText = `X: ${gyro.x.toFixed(2)} | Y: ${gyro.y.toFixed(2)} | Z: ${gyro.z.toFixed(2)}`;\n" +
                "                    const mag = Math.sqrt(gyro.x*gyro.x + gyro.y*gyro.y + gyro.z*gyro.z);\n" +
                "                    const pctGyro = Math.min(100, (mag / 10) * 100);\n" +
                "                    document.getElementById('gyro-bar').style.width = pctGyro + '%';\n" +
                "                }\n" +
                "            } catch (err) {\n" +
                "                document.getElementById('status').innerText = '🔴 Reintentando conexión...';\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        async function congelarYTransmitir() {\n" +
                "            if (!currentData) return;\n" +
                "            const frozenCard = document.getElementById('frozen-card');\n" +
                "            frozenCard.style.display = 'block';\n" +
                "            const now = new Date(currentData.timestamp || Date.now());\n" +
                "            document.getElementById('frozen-time').innerText = 'CAPTURADO: ' + now.toLocaleTimeString();\n" +
                "            if (currentData.acelerometro) {\n" +
                "                const acc = currentData.acelerometro;\n" +
                "                document.getElementById('frozen-acc').innerText = `X: ${acc.x.toFixed(2)} | Y: ${acc.y.toFixed(2)} | Z: ${acc.z.toFixed(2)}`;\n" +
                "                document.getElementById('frozen-acc-mag').innerText = `Magnitud: ${acc.magnitud.toFixed(2)} m/s²`;\n" +
                "            }\n" +
                "            if (currentData.giroscopio) {\n" +
                "                const gyro = currentData.giroscopio;\n" +
                "                document.getElementById('frozen-gyro').innerText = `X: ${gyro.x.toFixed(2)} | Y: ${gyro.y.toFixed(2)} | Z: ${gyro.z.toFixed(2)}`;\n" +
                "            }\n" +
                "            try {\n" +
                "                await fetch('/api/transmitir', { method: 'POST' });\n" +
                "            } catch (e) {}\n" +
                "        }\n" +
                "\n" +
                "        setInterval(fetchTelemetry, 150);\n" +
                "        fetchTelemetry();\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        byte[] bodyBytes = html.getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
        out.flush();
    }

    // --- Emisión de Socket TCP ---

    public void enviarMensajeTcp(String destinoIp, String mensaje) {
        executorRed.execute(() -> {
            try (Socket socket = new Socket(destinoIp, PUERTO_TCP);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(mensaje);
                PaqueteP2PDto paquete = new PaqueteP2PDto(
                        String.valueOf(System.currentTimeMillis()),
                        "LOCAL_HOST",
                        "SOCKET_TCP_OUT",
                        "Enviado a " + destinoIp + ": " + mensaje,
                        System.currentTimeMillis()
                );
                agregarMensajeP2P(paquete);
            } catch (Exception e) {
                Log.e(TAG, "Error enviando Socket TCP a " + destinoIp, e);
                estadoConexionLiveData.postValue("Fallo al enviar Socket TCP");
            }
        });
    }

    // --- Servidor y Cliente Bluetooth SPP (Serial Port Profile) ---

    public void iniciarServidorBluetooth() {
        if (servidorBluetoothActivo) return;

        BluetoothAdapter adapter = obtenerBluetoothAdapter();
        if (adapter == null) {
            estadoBluetoothLiveData.postValue("Bluetooth No Soportado");
            return;
        }
        if (!adapter.isEnabled()) {
            estadoBluetoothLiveData.postValue("Bluetooth Desactivado");
            return;
        }

        servidorBluetoothActivo = true;
        estadoBluetoothLiveData.postValue("Servidor BT SPP Escuchando");

        executorRed.execute(() -> {
            try (BluetoothServerSocket serverSocketBluetooth = adapter.listenUsingRfcommWithServiceRecord("SensoresBluetoothP2P", UUID_SPP_DEFAULT)) {
                while (servidorBluetoothActivo) {
                    BluetoothSocket socket = serverSocketBluetooth.accept();
                    if (socket != null) {
                        executorRed.execute(() -> atenderClienteBluetooth(socket));
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Permiso Bluetooth denegado al iniciar servidor", e);
                estadoBluetoothLiveData.postValue("Permiso BT Requerido");
                servidorBluetoothActivo = false;
            } catch (Exception e) {
                Log.e(TAG, "Excepción en Servidor Bluetooth SPP", e);
                estadoBluetoothLiveData.postValue("Servidor BT SPP Detenido");
                servidorBluetoothActivo = false;
            }
        });
    }

    private void atenderClienteBluetooth(BluetoothSocket socket) {
        String nombreRemoto = "Dispositivo BT";
        try {
            nombreRemoto = socket.getRemoteDevice().getName();
        } catch (SecurityException ignored) {}

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                PaqueteP2PDto paquete = new PaqueteP2PDto(
                        String.valueOf(System.currentTimeMillis()),
                        nombreRemoto != null ? nombreRemoto : "REMOTOBT",
                        "BLUETOOTH_SPP",
                        linea,
                        System.currentTimeMillis()
                );
                agregarMensajeP2P(paquete);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error recibiendo datos por Bluetooth", e);
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    public void enviarMensajeBluetooth(BluetoothDevice device, String mensaje) {
        if (device == null) return;
        executorRed.execute(() -> {
            String nombreDestino = "Dispositivo BT";
            try {
                nombreDestino = device.getName();
            } catch (SecurityException ignored) {}

            try (BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID_SPP_DEFAULT)) {
                socket.connect();
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(mensaje);

                PaqueteP2PDto paquete = new PaqueteP2PDto(
                        String.valueOf(System.currentTimeMillis()),
                        "LOCAL_HOST",
                        "BT_SPP_OUT",
                        "Enviado a " + (nombreDestino != null ? nombreDestino : device.getAddress()) + ": " + mensaje,
                        System.currentTimeMillis()
                );
                agregarMensajeP2P(paquete);
                estadoBluetoothLiveData.postValue("Enviado por BT a " + (nombreDestino != null ? nombreDestino : "Dispositivo"));
            } catch (SecurityException e) {
                Log.e(TAG, "Permiso denegado al transmitir Bluetooth", e);
                estadoBluetoothLiveData.postValue("Fallo BT: Permiso denegado");
            } catch (Exception e) {
                Log.e(TAG, "Error enviando vía Bluetooth SPP", e);
                estadoBluetoothLiveData.postValue("Error al conectar por Bluetooth");
            }
        });
    }

    /**
     * Agrega un nuevo paquete recibido a la lista en memoria y actualiza el LiveData.
     */
    private synchronized void agregarMensajeP2P(PaqueteP2PDto paquete) {
        List<PaqueteP2PDto> lista = mensajesP2PLiveData.getValue();
        if (lista == null) lista = new ArrayList<>();
        lista.add(0, paquete);
        if (lista.size() > 20) lista.remove(lista.size() - 1);
        mensajesP2PLiveData.postValue(lista);
    }
}