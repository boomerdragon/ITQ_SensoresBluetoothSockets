package com.itq.sensoresbluetoothsocketsapp;

import androidx.annotation.Keep;

/**
 * Modelo DTO para estructurar los mensajes transmitidos entre dispositivos
 * mediante Bluetooth Classic (SPP) o Sockets TCP/IP.
 */
@Keep
public class PaqueteP2PDto {

    private String idMensaje;
    private String emisor;
    private String tipoTransmision;
    private String contenidoData;
    private long timestamp;

    public PaqueteP2PDto() {
    }

    public PaqueteP2PDto(String idMensaje, String emisor, String tipoTransmision, String contenidoData, long timestamp) {
        this.idMensaje = idMensaje;
        this.emisor = emisor;
        this.tipoTransmision = tipoTransmision;
        this.contenidoData = contenidoData;
        this.timestamp = timestamp;
    }

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