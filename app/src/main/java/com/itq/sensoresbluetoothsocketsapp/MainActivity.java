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

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private SensoresBluetoothViewModel viewModel;

    private TextView tvValoresAcelerometro;
    private TextView tvMagnitudAcelerometro;
    private TextView tvValoresGiroscopio;
    private TextView tvEstadoServidor;
    private TextView tvEstadoServidorWeb;
    private TextView tvEstadoBluetooth;
    private Button btnEnviarP2P;
    private Button btnEnviarBluetooth;

    private String ultimaLecturaAcelerometro = "Sin Datos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvValoresAcelerometro = findViewById(R.id.tvValoresAcelerometro);
        tvMagnitudAcelerometro = findViewById(R.id.tvMagnitudAcelerometro);
        tvValoresGiroscopio = findViewById(R.id.tvValoresGiroscopio);
        tvEstadoServidor = findViewById(R.id.tvEstadoServidor);
        tvEstadoServidorWeb = findViewById(R.id.tvEstadoServidorWeb);
        tvEstadoBluetooth = findViewById(R.id.tvEstadoBluetooth);
        btnEnviarP2P = findViewById(R.id.btnEnviarP2P);
        btnEnviarBluetooth = findViewById(R.id.btnEnviarBluetooth);
        RecyclerView rvMensajesP2P = findViewById(R.id.rvMensajesP2P);

        rvMensajesP2P.setLayoutManager(new LinearLayoutManager(this));
        ProyectoAdapter adapter = new ProyectoAdapter();
        rvMensajesP2P.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SensoresBluetoothViewModel.class);

        viewModel.getAcelerometroData().observe(this, dto -> {
            if (dto != null) {
                String txt = String.format(Locale.getDefault(), "X: %.2f | Y: %.2f | Z: %.2f", dto.getX(), dto.getY(), dto.getZ());
                tvValoresAcelerometro.setText(txt);
                tvMagnitudAcelerometro.setText(String.format(Locale.getDefault(), "Magnitud: %.2f m/s²", dto.getMagnitud()));
                ultimaLecturaAcelerometro = txt;
            }
        });

        viewModel.getGiroscopioData().observe(this, dto -> {
            if (dto != null) {
                tvValoresGiroscopio.setText(String.format(Locale.getDefault(), "X: %.2f | Y: %.2f | Z: %.2f", dto.getX(), dto.getY(), dto.getZ()));
            }
        });

        viewModel.getMensajesP2P().observe(this, adapter::setPaquetes);

        viewModel.getEstadoConexion().observe(this, estado -> tvEstadoServidor.setText(estado));
        viewModel.getEstadoServidorWeb().observe(this, estadoWeb -> tvEstadoServidorWeb.setText(estadoWeb));
        viewModel.getEstadoBluetooth().observe(this, estadoBt -> tvEstadoBluetooth.setText(estadoBt));

        verificarYSolicitarPermisos();

        viewModel.iniciarServidorTcp();
        viewModel.iniciarServidorHttp();
        viewModel.iniciarServidorBluetooth();

        btnEnviarP2P.setOnClickListener(v -> viewModel.transmitirPaqueteP2P("127.0.0.1", "Telemetría Acelerómetro: " + ultimaLecturaAcelerometro));

        btnEnviarBluetooth.setOnClickListener(v -> mostrarDialogoSeleccionBluetooth());
    }

    private void verificarYSolicitarPermisos() {
        List<String> permisosRequeridos = new ArrayList<>();
        permisosRequeridos.add(Manifest.permission.ACCESS_FINE_LOCATION);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            viewModel.iniciarServidorBluetooth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.activarSensores();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.desactivarSensores();
    }
}