package com.example.waiterapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadyOrdersAdapter extends RecyclerView.Adapter<ReadyOrdersAdapter.ViewHolder> {

    private List<TakeOrder> orderList;
    private final Context context;
    private final ApiService apiService;

    public ReadyOrdersAdapter(List<TakeOrder> orderList, Context context) {
        this.orderList = orderList;
        this.context = context;
        this.apiService = Retrofit.getInstance().getApi();
    }


    @NonNull
    @Override
    public ReadyOrdersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ready_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadyOrdersAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        TakeOrder order = orderList.get(position);
        holder.tvTableNumber.setText("Bord: " + order.getTable());

        // Bygg en sträng med orderdetaljer
        StringBuilder details = new StringBuilder();
        if (order.getOrderSpecs() != null) {
            for (OrderSpecs spec : order.getOrderSpecs()) {
                details.append(spec.getMeal())
                        .append(" (")
                        .append(spec.getCount())
                        .append(") - ")
                        .append(spec.getCategory())
                        .append("\n");
            }
        }
        holder.tvOrderDetails.setText(details.toString());
        Log.d("API_REQUEST", "Försöker markera order som levererad för bord: " + order.getTable());

        // När användaren trycker på knappen, markera ordern som levererad
        holder.btnDelivered.setOnClickListener(v -> {
            apiService.markOrderDelivered(order.getTable()).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(context, "Order för bord " + order.getTable() + " markerad som levererad", Toast.LENGTH_SHORT).show();
                        // Ta bort ordern från listan och uppdatera adaptern
                        orderList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, orderList.size());
                    } else {
                        //för att se villken API fel det är
                        Log.e("API_ERROR", "Statuskod: " + response.code() + " | Meddelande: " + response.message());

                        Toast.makeText(context, "Kunde inte uppdatera ordern", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(context, "Fel vid uppdatering: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    // Metod för att uppdatera orderlistan med nya data
    public void updateOrders(List<TakeOrder> newOrders) {
        orderList.clear();
        orderList.addAll(newOrders);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTableNumber, tvOrderDetails;
        Button btnDelivered;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableNumber = itemView.findViewById(R.id.tvItemTableNumber);
            tvOrderDetails = itemView.findViewById(R.id.tvItemOrderDetails);
            btnDelivered = itemView.findViewById(R.id.btnDelivered);
        }
    }
}
