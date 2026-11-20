package com.itq.sensoresbluetoothsocketsapp;

import android.app.Application;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Set;

/**
 * 🏛️ ViewModel (Arquitectura MVVM - Model-View-ViewModel)
 * 
 * ¿Por qué usar un ViewModel en Android?
 * En Android, las Actividades (MainActivity) se destruyen y vuelven a crear cuando el usuario
 * gira la pantalla o cambia la configuración. El ViewModel conserva los datos de estado y la lógica
 * de negocio sobreviviente a esos cambios de ciclo de vida.
 * 
 * Además, expone los datos mediante 'LiveData', lo que permite a la Activity observar cambios
 * de manera reactiva sin acoplar la interfaz gráfica con el acceso a los sensores o la red.
 */
public class SensoresBluetoothViewModel extends AndroidViewModel {

    // 🗄️ Referencia al Repositorio central de datos
    private final SensoresBluetoothRepository repository;

    /**
     * Constructor del ViewModel. Obtiene la instancia Singleton del Repositorio.
     */
    public SensoresBluetoothViewModel(@NonNull Application application) {
        super(application);
        repository = SensoresBluetoothRepository.getInstancia(application);
    }

    // --- Métodos de Exposición de Datos (LiveData Observables) ---

    /** Retorna el flujo Observable con las lecturas en tiempo real del Acelerómetro. */
    public LiveData<LecturaSensorDto> getAcelerometroData() {
        return repository.getAcelerometroData();
    }

    /** Retorna el flujo Observable con las lecturas en tiempo real del Giroscopio. */
    public LiveData<LecturaSensorDto> getGiroscopioData() {
        return repository.getGiroscopioData();
    }

    /** Retorna la lista Observable de paquetes P2P (TCP / Bluetooth) para el RecyclerView. */
    public LiveData<List<PaqueteP2PDto>> getMensajesP2P() {
        return repository.getMensajesP2P();
    }

    /** Retorna el estado actual del Servidor TCP (Puerto 8888). */
    public LiveData<String> getEstadoConexion() {
        return repository.getEstadoConexion();
    }

    /** Retorna el estado actual del Servidor Web HTTP (Puerto 8080). */
    public LiveData<String> getEstadoServidorWeb() {
        return repository.getEstadoServidorWeb();
    }

    /** Retorna el estado actual del Servidor Bluetooth SPP. */
    public LiveData<String> getEstadoBluetooth() {
        return repository.getEstadoBluetooth();
    }

    /** Obtiene el conjunto de dispositivos Bluetooth vinculados en el sistema. */
    public Set<BluetoothDevice> getDispositivosEmparejados() {
        return repository.getDispositivosEmparejados();
    }

    // --- Métodos de Control y Acciones solicitadas desde la Interfaz de Usuario ---

    /** Activa la escucha de sensores físicos (Acelerómetro y Giroscopio). */
    public void activarSensores() {
        repository.registrarSensores();
    }

    /** Desactiva los sensores para ahorrar energía cuando la app no está visible. */
    public void desactivarSensores() {
        repository.desregistrarSensores();
    }

    /** Inicia el servidor de Sockets TCP en segundo plano. */
    public void iniciarServidorTcp() {
        repository.iniciarServidorTcp();
    }

    /** Inicia el servidor Web HTTP embebido (dashboard web). */
    public void iniciarServidorHttp() {
        repository.iniciarServidorHttp();
    }

    /** Inicia el servidor Bluetooth SPP escudriñador de conexiones. */
    public void iniciarServidorBluetooth() {
        repository.iniciarServidorBluetooth();
    }

    /** Transmite un paquete telemétrico por Sockets TCP/IP. */
    public void transmitirPaqueteP2P(String destinoIp, String dato) {
        repository.enviarMensajeTcp(destinoIp, dato);
    }

    /** Transmite un paquete telemétrico por Bluetooth SPP a un dispositivo seleccionado. */
    public void transmitirPaqueteBluetooth(BluetoothDevice device, String dato) {
        repository.enviarMensajeBluetooth(device, dato);
    }
}