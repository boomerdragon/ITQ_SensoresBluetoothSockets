package com.itq.sensoresbluetoothsocketsapp;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Set;

/**
 * ViewModel que conserva el estado de telemetría inercial y mensajes P2P
 * desacoplándolos de la interfaz visual durante cambios de configuración.
 */
public class SensoresBluetoothViewModel extends AndroidViewModel {

    private final SensoresBluetoothRepository repository;

    public SensoresBluetoothViewModel(@NonNull Application application) {
        super(application);
        repository = SensoresBluetoothRepository.getInstancia(application);
    }

    public LiveData<LecturaSensorDto> getAcelerometroData() {
        return repository.getAcelerometroData();
    }

    public LiveData<LecturaSensorDto> getGiroscopioData() {
        return repository.getGiroscopioData();
    }

    public LiveData<List<PaqueteP2PDto>> getMensajesP2P() {
        return repository.getMensajesP2P();
    }

    public LiveData<String> getEstadoConexion() {
        return repository.getEstadoConexion();
    }

    public LiveData<String> getEstadoServidorWeb() {
        return repository.getEstadoServidorWeb();
    }

    public LiveData<String> getEstadoBluetooth() {
        return repository.getEstadoBluetooth();
    }

    public Set<BluetoothDevice> getDispositivosEmparejados() {
        return repository.getDispositivosEmparejados();
    }

    public void activarSensores() {
        repository.registrarSensores();
    }

    public void desactivarSensores() {
        repository.desregistrarSensores();
    }

    public void iniciarServidorTcp() {
        repository.iniciarServidorTcp();
    }

    public void iniciarServidorHttp() {
        repository.iniciarServidorHttp();
    }

    public void iniciarServidorBluetooth() {
        repository.iniciarServidorBluetooth();
    }

    public void transmitirPaqueteP2P(String destinoIp, String dato) {
        repository.enviarMensajeTcp(destinoIp, dato);
    }

    public void transmitirPaqueteBluetooth(BluetoothDevice device, String dato) {
        repository.enviarMensajeBluetooth(device, dato);
    }
}