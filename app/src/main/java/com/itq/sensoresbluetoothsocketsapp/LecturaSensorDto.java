package com.itq.sensoresbluetoothsocketsapp;

import androidx.annotation.Keep;

/**
 * Modelo DTO (Data Transfer Object) para encapsular las lecturas
 * de hardware inercial provenientes del Acelerómetro y Giroscopio.
 */
@Keep
public class LecturaSensorDto {

    private String tipoSensor;
    private float x;
    private float y;
    private float z;
    private float magnitud;
    private long timestamp;

    public LecturaSensorDto() {
    }

    public LecturaSensorDto(String tipoSensor, float x, float y, float z, float magnitud, long timestamp) {
        this.tipoSensor = tipoSensor;
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitud = magnitud;
        this.timestamp = timestamp;
    }

    public String getTipoSensor() { return tipoSensor; }
    public void setTipoSensor(String tipoSensor) { this.tipoSensor = tipoSensor; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getZ() { return z; }
    public void setZ(float z) { this.z = z; }

    public float getMagnitud() { return magnitud; }
    public void setMagnitud(float magnitud) { this.magnitud = magnitud; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}