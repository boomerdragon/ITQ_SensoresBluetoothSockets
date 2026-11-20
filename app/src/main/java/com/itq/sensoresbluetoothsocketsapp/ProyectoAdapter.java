package com.itq.sensoresbluetoothsocketsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔄 Adaptador de RecyclerView (ProyectoAdapter)
 * 
 * En Android, un RecyclerView se encarga de mostrar listas dinámicas reciclando las vistas
 * visibles en pantalla para optimizar la memoria RAM. El Adaptador actúa como puente
 * entre la fuente de datos (List<PaqueteP2PDto>) y los elementos visuales en pantalla.
 */
public class ProyectoAdapter extends RecyclerView.Adapter<ProyectoAdapter.PaqueteViewHolder> {

    // 📋 Colección interna que almacena los paquetes P2P recibidos o enviados
    private List<PaqueteP2PDto> listaPaquetes = new ArrayList<>();

    /**
     * Actualiza la lista de paquetes del adaptador y notifica al RecyclerView para redibujar la pantalla.
     * 
     * @param nuevosPaquetes Nueva lista de paquetes enviada por el ViewModel.
     */
    public void setPaquetes(List<PaqueteP2PDto> nuevosPaquetes) {
        this.listaPaquetes = nuevosPaquetes;
        notifyDataSetChanged(); // Notifica al RecyclerView que los datos cambiaron
    }

    /**
     * 🛠️ Crea una nueva casilla visual (ViewHolder) inflando el diseño XML 'item_nodo_red.xml'.
     */
    @NonNull
    @Override
    public PaqueteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nodo_red, parent, false);
        return new PaqueteViewHolder(view);
    }

    /**
     * 🎨 Enlaza (bind) los datos de un paquete específico con los componentes de texto de la vista.
     * 
     * @param holder   Instancia del ViewHolder que contiene las referencias visuales.
     * @param position Índice del elemento dentro de la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull PaqueteViewHolder holder, int position) {
        PaqueteP2PDto dto = listaPaquetes.get(position);
        
        // Asigna el nombre del nodo emisor formateado desde recursos de cadenas
        holder.tvEmisor.setText(holder.itemView.getContext().getString(R.string.node_emitter_format, dto.getEmisor()));
        
        // Asigna el tipo de canal de red (TCP / BLUETOOTH)
        holder.tvTipo.setText(holder.itemView.getContext().getString(R.string.channel_type_format, dto.getTipoTransmision()));
        
        // Muestra la carga telemétrica útil recibida
        holder.tvContenido.setText(dto.getContenidoData());
    }

    /**
     * 📊 Retorna la cantidad total de elementos presentes en la lista.
     */
    @Override
    public int getItemCount() {
        return listaPaquetes.size();
    }

    /**
     * 🧩 ViewHolder (Contenedor de Vistas)
     * 
     * Guarda las referencias a los TextView de cada fila del RecyclerView para evitar
     * búsquedas lentas repetidas con 'findViewById()' durante el desplazamiento (scroll).
     */
    static class PaqueteViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmisor;
        TextView tvTipo;
        TextView tvContenido;

        public PaqueteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmisor = itemView.findViewById(R.id.tvEmisorNodo);
            tvTipo = itemView.findViewById(R.id.tvTipoCanal);
            tvContenido = itemView.findViewById(R.id.tvContenidoPaquete);
        }
    }
}