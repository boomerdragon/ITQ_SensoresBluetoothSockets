package com.itq.sensoresbluetoothsocketsapp;

import androidx.annotation.Keep;

/**
 * ✉️ Modelo DTO (Data Transfer Object) para Paquetes de Red P2P
 * 
 * Esta clase sirve como contenedor estandarizado para los mensajes intercambiados
 * entre dispositivos inalámbricos (ya sea mediante Sockets TCP/IP o Bluetooth SPP).
 */
@Keep
public class PaqueteP2PDto {

    // 🔑 Identificador único del mensaje (generalmente basado en timestamp)
    private String idMensaje;
    
    // 📱 Identificador o IP del dispositivo que envía el mensaje (Emisor/Nodo)
    private String emisor;
    
    // 🌐 Canal o tecnología utilizada ("SOCKET_TCP", "BLUETOOTH_SPP", etc.)
    private String tipoTransmision;
    
    // 📝 Contenido útil o carga del mensaje (Payload de telemetría inercial)
    private String contenidoData;
    
    // ⏱️ Estampa de tiempo en milisegundos cuando fue recibido o creado el paquete
    private long timestamp;

    /**
     * Constructor vacío sin parámetros (requerido para serialización).
     */
    public PaqueteP2PDto() {
    }

    /**
     * Constructor parametrizado para crear rápidamente un paquete de red P2P completo.
     */
    public PaqueteP2PDto(String idMensaje, String emisor, String tipoTransmision, String contenidoData, long timestamp) {
        this.idMensaje = idMensaje;
        this.emisor = emisor;
        this.tipoTransmision = tipoTransmision;
        this.contenidoData = contenidoData;
        this.timestamp = timestamp;
    }

    // --- Métodos de acceso Getters y Setters ---

    public String getIdMensaje() { return idMensaje; }
    public void setIdMensaje(String idMensaje) { this.idMensaje = idMensaje; }

    public String getEmisor() { return emisor; }
    public void setEmisor(String emisor) { this.emisor = emisor; }

    public String getTipoTransmision() { return tipoTransmision; }
    public void setTipoTransmision(String tipoTransmision) { this.tipoTransmision = tipoTransmision; }

    public String getContenidoData() { return contenidoData; }
    public void setContenidoData(String contenidoData) { this.contenidoData = contenidoData; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}