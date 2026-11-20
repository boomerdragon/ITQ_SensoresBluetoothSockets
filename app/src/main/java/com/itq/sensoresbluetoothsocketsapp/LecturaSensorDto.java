package com.itq.sensoresbluetoothsocketsapp;

import androidx.annotation.Keep;

/**
 * 📦 Modelo DTO (Data Transfer Object - Objeto de Transferencia de Datos)
 * 
 * ¿Qué es un DTO? 
 * Es una clase simple tipo "caja contenedor" cuyo único propósito es llevar datos de un lugar
 * a otro (por ejemplo, desde el sensor físico hasta la pantalla o la red) sin contener lógica compleja.
 * 
 * La anotación @Keep le dice al compilador de Android (ProGuard / R8) que NO reduzca ni cambie
 * los nombres de estas variables al compilar la app para producción.
 */
@Keep
public class LecturaSensorDto {

    // 🏷️ Nombre del tipo de sensor ("ACELERÓMETRO" o "GIROSCAPIO")
    private String tipoSensor;
    
    // 📐 Componentes cartesianos de movimiento en el espacio tridimensional (Ejes X, Y, Z)
    private float x;
    private float y;
    private float z;
    
    // ⚡ Magnitud vectorial calculada: √(x² + y² + z²)
    private float magnitud;
    
    // ⏱️ Estampa de tiempo (Timestamp en milisegundos) en que se realizó la lectura
    private long timestamp;

    /**
     * Constructor vacío requerido para marcos de trabajo y serialización JSON.
     */
    public LecturaSensorDto() {
    }

    /**
     * Constructor completo para instanciar una lectura inercial con todos sus parámetros.
     */
    public LecturaSensorDto(String tipoSensor, float x, float y, float z, float magnitud, long timestamp) {
        this.tipoSensor = tipoSensor;
        this.x = x;
        this.y = y;
        this.z = z;
        this.magnitud = magnitud;
        this.timestamp = timestamp;
    }

    // --- Métodos Getters y Setters para acceder y modificar las propiedades ---

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