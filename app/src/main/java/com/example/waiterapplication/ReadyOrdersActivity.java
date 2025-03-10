package com.example.waiterapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReadyOrdersAdapter adapter;
    private ApiService apiService;
    private Handler refreshHandler = new Handler();
    private int previousOrderCount = 0;

    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchReadyOrders();
            refreshHandler.postDelayed(this, 5000);  // Run every 10 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready_orders);

        recyclerView = findViewById(R.id.recyclerViewReadyOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = Retrofit.getInstance().getApi();

        // Tell the service this activity is now active
        OrderMonitorService.setReadyOrdersActivityActive(this, true);

        fetchReadyOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tell the service this activity is now active
        OrderMonitorService.setReadyOrdersActivityActive(this, true);
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Tell the service this activity is now inactive
        OrderMonitorService.setReadyOrdersActivityActive(this, false);
    }

    private void fetchReadyOrders() {
        apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TakeOrder> newReadyOrders = response.body();
                    Log.d("ReadyOrdersActivity", "Hämtade " + newReadyOrders.size() + " färdiga ordrar från API");

                    // Update the adapter with the new data
                    if (adapter == null) {
                        Log.d("ReadyOrdersActivity", "Skapar ny adapter med " + newReadyOrders.size() + " ordrar");
                        adapter = new ReadyOrdersAdapter(newReadyOrders, ReadyOrdersActivity.this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.d("ReadyOrdersActivity", "Uppdaterar befintlig adapter med " + newReadyOrders.size() + " ordrar");
                        adapter.updateOrders(newReadyOrders);
                    }

                    // Update previous count (no need to play sound here)
                    previousOrderCount = newReadyOrders.size();
                } else {
                    // Don't show toast for no orders, just log it
                    Log.d("ReadyOrdersActivity", "Inga färdiga ordrar hittades eller svarskod: " + response.code());

                    // Update adapter with empty list if already initialized
                    if (adapter != null) {
                        adapter.updateOrders(java.util.Collections.emptyList());
                    } else {
                        Toast.makeText(ReadyOrdersActivity.this, "Inga färdiga ordrar hittades", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Log.e("ReadyOrdersActivity", "Nätverksfel vid hämtning av ordrar", t);
                Toast.makeText(ReadyOrdersActivity.this, "Fel vid hämtning: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markOrderAsDelivered(int orderId) {
        apiService.markOrderDelivered(orderId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    fetchReadyOrders();  // Uppdatera listan
                    Toast.makeText(ReadyOrdersActivity.this, "Order markerad som levererad!", Toast.LENGTH_SHORT).show();

                    // Gå tillbaka till huvudmenyn efter leverans
                    new Handler().postDelayed(() -> {
                        Intent mainIntent = new Intent(ReadyOrdersActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(mainIntent);
                        finish();
                    }, 1000);  // Vänta 1 sekund för en bättre upplevelse
                } else {
                    Toast.makeText(ReadyOrdersActivity.this, "Misslyckades med att markera som levererad", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ReadyOrdersActivity.this, "Nätverksfel", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getPreviousOrderCount() {
        return previousOrderCount;
    }

    public void setPreviousOrderCount(int previousOrderCount) {
        this.previousOrderCount = previousOrderCount;
    }
}