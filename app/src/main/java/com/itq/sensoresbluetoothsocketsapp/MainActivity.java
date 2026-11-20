package com.itq.sensoresbluetoothsocketsapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 📱 Pantalla Principal de la Aplicación (MainActivity)
 * 
 * En el patrón de diseño MVVM, la Actividad actúa únicamente como la "Vista" (View).
 * Su responsabilidad se limita a:
 * 1. Inicializar los componentes gráficos de la interfaz (TextView, Button, RecyclerView).
 * 2. Observar los cambios publicados por el ViewModel mediante LiveData y refrescar la pantalla.
 * 3. Gestionar permisos en tiempo de ejecución (Runtime Permissions) requeridos por el sistema.
 */
public class MainActivity extends AppCompatActivity {

    // 🔢 Código único de solicitud para identificar la respuesta de permisos de Android
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 🏛️ Instancia del ViewModel desacoplado
    private SensoresBluetoothViewModel viewModel;

    // 📺 Componentes visuales de la interfaz gráfica
    private TextView tvValoresAcelerometro;
    private TextView tvMagnitudAcelerometro;
    private TextView tvValoresGiroscopio;
    private TextView tvEstadoServidor;
    private TextView tvEstadoServidorWeb;
    private TextView tvEstadoBluetooth;
    private Button btnEnviarP2P;
    private Button btnEnviarBluetooth;

    // 💾 Almacena el último valor legible capturado del acelerómetro para ser transmitido
    private String ultimaLecturaAcelerometro = "Sin Datos";

    /**
     * 🚀 Método del Ciclo de Vida: onCreate
     * Se ejecuta una sola vez al construir e iniciar la Actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Enlaza el diseño XML 'activity_main.xml'

        // 1. Vinculación de vistas mediante sus IDs declarados en el XML
        tvValoresAcelerometro = findViewById(R.id.tvValoresAcelerometro);
        tvMagnitudAcelerometro = findViewById(R.id.tvMagnitudAcelerometro);
        tvValoresGiroscopio = findViewById(R.id.tvValoresGiroscopio);
        tvEstadoServidor = findViewById(R.id.tvEstadoServidor);
        tvEstadoServidorWeb = findViewById(R.id.tvEstadoServidorWeb);
        tvEstadoBluetooth = findViewById(R.id.tvEstadoBluetooth);
        btnEnviarP2P = findViewById(R.id.btnEnviarP2P);
        btnEnviarBluetooth = findViewById(R.id.btnEnviarBluetooth);
        RecyclerView rvMensajesP2P = findViewById(R.id.rvMensajesP2P);

        // 2. Configuración del RecyclerView para la lista de paquetes telemétricos
        rvMensajesP2P.setLayoutManager(new LinearLayoutManager(this));
        ProyectoAdapter adapter = new ProyectoAdapter();
        rvMensajesP2P.setAdapter(adapter);

        // 3. Inicialización del ViewModel mediante ViewModelProvider
        viewModel = new ViewModelProvider(this).get(SensoresBluetoothViewModel.class);

        // 4. Suscripción a Observadores LiveData (Actualización automática de la UI)
        
        // Observador de lecturas del Acelerómetro
        viewModel.getAcelerometroData().observe(this, dto -> {
            if (dto != null) {
                String txt = String.format(Locale.getDefault(), "X: %.2f | Y: %.2f | Z: %.2f", dto.getX(), dto.getY(), dto.getZ());
                tvValoresAcelerometro.setText(txt);
                tvMagnitudAcelerometro.setText(String.format(Locale.getDefault(), "Magnitud: %.2f m/s²", dto.getMagnitud()));
                ultimaLecturaAcelerometro = txt;
            }
        });

        // Observador de lecturas del Giroscopio
        viewModel.getGiroscopioData().observe(this, dto -> {
            if (dto != null) {
                tvValoresGiroscopio.setText(String.format(Locale.getDefault(), "X: %.2f | Y: %.2f | Z: %.2f", dto.getX(), dto.getY(), dto.getZ()));
            }
        });

        // Observador del historial de paquetes P2P recibidos
        viewModel.getMensajesP2P().observe(this, adapter::setPaquetes);

        // Observadores de estados de los servidores de red
        viewModel.getEstadoConexion().observe(this, estado -> tvEstadoServidor.setText(estado));
        viewModel.getEstadoServidorWeb().observe(this, estadoWeb -> tvEstadoServidorWeb.setText(estadoWeb));
        viewModel.getEstadoBluetooth().observe(this, estadoBt -> tvEstadoBluetooth.setText(estadoBt));

        // 5. Verificación de permisos en tiempo de ejecución
        verificarYSolicitarPermisos();

        // 6. Arranque de los servidores de comunicación inalámbrica
        viewModel.iniciarServidorTcp();
        viewModel.iniciarServidorHttp();
        viewModel.iniciarServidorBluetooth();

        // 7. Configuración de oyentes (Listeners) para los botones de acción
        btnEnviarP2P.setOnClickListener(v -> 
            viewModel.transmitirPaqueteP2P("127.0.0.1", "Telemetría Acelerómetro: " + ultimaLecturaAcelerometro)
        );

        btnEnviarBluetooth.setOnClickListener(v -> mostrarDialogoSeleccionBluetooth());
    }

    /**
     * 🛡️ Solicita permisos necesarios en tiempo de ejecución (Requerido en Android 12+ / API 31+)
     */
    private void verificarYSolicitarPermisos() {
        List<String> permisosRequeridos = new ArrayList<>();
        permisosRequeridos.add(Manifest.permission.ACCESS_FINE_LOCATION);

        // Android 12 introdujo permisos específicos explícitos para Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisosRequeridos.add(Manifest.permission.BLUETOOTH_CONNECT);
            permisosRequeridos.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        List<String> faltantes = new ArrayList<>();
        for (String p : permisosRequeridos) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                faltantes.add(p);
            }
        }

        if (!faltantes.isEmpty()) {
            ActivityCompat.requestPermissions(this, faltantes.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 📶 Muestra un cuadro de diálogo flotante (AlertDialog) con la lista de dispositivos Bluetooth emparejados.
     */
    private void mostrarDialogoSeleccionBluetooth() {
        Set<BluetoothDevice> dispositivos = viewModel.getDispositivosEmparejados();
        if (dispositivos.isEmpty()) {
            Toast.makeText(this, R.string.no_paired_devices, Toast.LENGTH_SHORT).show();
            return;
        }

        List<BluetoothDevice> listaDispositivos = new ArrayList<>(dispositivos);
        String[] nombres = new String[listaDispositivos.size()];

        for (int i = 0; i < listaDispositivos.size(); i++) {
            BluetoothDevice dev = listaDispositivos.get(i);
            String nombre = "Dispositivo Desconocido";
            try {
                nombre = dev.getName();
            } catch (SecurityException ignored) {}
            nombres[i] = (nombre != null ? nombre : "Dispositivo") + "\n" + dev.getAddress();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_bluetooth_device);
        builder.setItems(nombres, (dialog, which) -> {
            BluetoothDevice seleccionado = listaDispositivos.get(which);
            viewModel.transmitirPaqueteBluetooth(seleccionado, "Telemetría BT: " + ultimaLecturaAcelerometro);
        });
        builder.show();
    }

    /**
     * Callback invocado cuando el usuario responde al diálogo de concesión de permisos.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            viewModel.iniciarServidorBluetooth();
        }
    }

    /**
     * 🔄 Ciclo de vida: onResume
     * Se ejecuta cuando la app vuelve a estar visible en primer plano. Registra los sensores.
     */
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.activarSensores();
    }

    /**
     * ⏸️ Ciclo de vida: onPause
     * Se ejecuta cuando la app pasa a segundo plano. Desregistra los sensores para evitar consumo innecesario de batería.
     */
    @Override
    protected void onPause() {
        super.onPause();
        viewModel.desactivarSensores();
    }
}