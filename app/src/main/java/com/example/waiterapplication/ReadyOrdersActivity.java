package com.example.waiterapplication;

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
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchReadyOrders();
            // Kör om efter exempelvis 10 sekunder (10000 ms)
            refreshHandler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready_orders);

        recyclerView = findViewById(R.id.recyclerViewReadyOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = Retrofit.getInstance().getApi();

        fetchReadyOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void fetchReadyOrders() {
        apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (adapter == null) {
                        adapter = new ReadyOrdersAdapter(response.body(), ReadyOrdersActivity.this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateOrders(response.body());
                    }
                    Log.d("ReadyOrdersActivity", "Antal färdiga ordrar: " + response.body().size());
                } else {
                    Toast.makeText(ReadyOrdersActivity.this, "Inga färdiga ordrar hittades", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Toast.makeText(ReadyOrdersActivity.this, "Fel vid hämtning: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
