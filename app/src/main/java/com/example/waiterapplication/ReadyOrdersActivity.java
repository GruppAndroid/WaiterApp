package com.example.waiterapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
            refreshHandler.postDelayed(this, 10000);  // Run every 10 seconds
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
                    List<TakeOrder> newReadyOrders = response.body();
                    Log.d("ReadyOrdersActivity", "Hämtade " + newReadyOrders.size() + " färdiga ordrar från API");

                    // First update the adapter with the new data
                    if (adapter == null) {
                        Log.d("ReadyOrdersActivity", "Skapar ny adapter med " + newReadyOrders.size() + " ordrar");
                        adapter = new ReadyOrdersAdapter(newReadyOrders, ReadyOrdersActivity.this);
                        recyclerView.setAdapter(adapter);
                    } else {
                        Log.d("ReadyOrdersActivity", "Uppdaterar befintlig adapter med " + newReadyOrders.size() + " ordrar");
                        adapter.updateOrders(newReadyOrders);
                    }

                    // Then handle notification logic separately
                    int currentOrderCount = newReadyOrders.size();
                    Log.d("ReadyOrdersActivity", "Nuvarande: " + currentOrderCount + ", Tidigare: " + previousOrderCount);

                    if (currentOrderCount > previousOrderCount) {
                        Log.d("ReadyOrdersActivity", "Nya ordrar upptäckta, spelar notifikation");
                        playNotificationSound();
                        vibrate();
                    }

                    previousOrderCount = currentOrderCount;

                    Log.d("ReadyOrdersActivity", "Antal färdiga ordrar: " + newReadyOrders.size());
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
    // Play notification sound
    private void playNotificationSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.order_ready_sound);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        } catch (Exception e) {
            Log.e("Sound Error", "Could not play notification sound", e);
        }
    }

    // Vibrate the device
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }
}