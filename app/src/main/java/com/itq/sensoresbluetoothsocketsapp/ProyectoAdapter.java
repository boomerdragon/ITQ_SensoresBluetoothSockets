package com.itq.sensoresbluetoothsocketsapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProyectoAdapter extends RecyclerView.Adapter<ProyectoAdapter.PaqueteViewHolder> {

    private List<PaqueteP2PDto> listaPaquetes = new ArrayList<>();

    public void setPaquetes(List<PaqueteP2PDto> nuevosPaquetes) {
        this.listaPaquetes = nuevosPaquetes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaqueteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nodo_red, parent, false);
        return new PaqueteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaqueteViewHolder holder, int position) {
        PaqueteP2PDto dto = listaPaquetes.get(position);
        holder.tvEmisor.setText(holder.itemView.getContext().getString(R.string.node_emitter_format, dto.getEmisor()));
        holder.tvTipo.setText(holder.itemView.getContext().getString(R.string.channel_type_format, dto.getTipoTransmision()));
        holder.tvContenido.setText(dto.getContenidoData());
    }

    @Override
    public int getItemCount() {
        return listaPaquetes.size();
    }

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