package com.example.waiterapplication;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import android.media.AudioManager;
import android.media.MediaPlayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;
import com.google.gson.Gson;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadyOrdersActivity extends AppCompatActivity {
    private static final String TAG = "ReadyOrdersActivity";

    private RecyclerView recyclerView;
    private ReadyOrdersAdapter adapter;
    private ApiService apiService;
    private Handler refreshHandler = new Handler();
    private int previousOrderCount = 0;
    private boolean isActivityVisible = false;

    // Refresh every 10 seconds
    private Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchReadyOrders();
            refreshHandler.postDelayed(this, 10000);
        }
    };

    // Receiver for refresh broadcasts from OrderMonitorService
    private BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received refresh broadcast");
            fetchReadyOrders();
        }
    };


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ready_orders);

        // Your existing initialization code
        recyclerView = findViewById(R.id.recyclerViewReadyOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        apiService = Retrofit.getInstance().getApi();

        // Initialize adapter with empty list
        adapter = new ReadyOrdersAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Tell the service this activity is now active
        OrderMonitorService.setReadyOrdersActivityActive(this, true);

        // Register for refresh broadcasts
        IntentFilter filter = new IntentFilter("com.example.waiterapplication.REFRESH_READY_ORDERS");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(refreshReceiver, filter);
        }

        // Check if we should play sound immediately (launched by kitchen)
        if (getIntent().getBooleanExtra("PLAY_SOUND", false)) {
            int tableNumber = getIntent().getIntExtra("TABLE_NUMBER", -1);
            Log.d(TAG, "Launched by kitchen - playing sound for table " + tableNumber);
            playNotificationSound();
        }

        // Fetch ready orders immediately
        fetchReadyOrders();
    }

    // Add this method to ReadyOrdersActivity
    // Add this method to ReadyOrdersActivity
    private void playNotificationSound() {
        try {
            // Make sure volume is up
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                // Force volume to maximum for notification
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                Log.d(TAG, "Setting volume to maximum: " + maxVolume);
            }

            // Create the MediaPlayer
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.order_ready_sound);
            if (mediaPlayer == null) {
                Log.e(TAG, "Failed to create MediaPlayer - sound file might be missing");
                return;
            }

            // Set maximum volume for the player too
            mediaPlayer.setVolume(1.0f, 1.0f);

            // Add listeners for troubleshooting
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting sound");
                mp.start();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Sound playback completed");
                mp.release();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                return false;
            });

            // Try starting directly as well
            mediaPlayer.start();
            Log.d(TAG, "Sound playback started");

        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "ReadyOrdersActivity resumed");
        isActivityVisible = true;

        // Tell the service this activity is now active
        OrderMonitorService.setReadyOrdersActivityActive(this, true);

        // Start periodic refresh
        refreshHandler.post(refreshRunnable);

        debugJsonResponse();

        // Fetch data immediately
        fetchReadyOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "ReadyOrdersActivity paused");
        isActivityVisible = false;

        // Stop periodic refresh when activity is not visible
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ReadyOrdersActivity destroyed");

        // Tell the service this activity is no longer active
        OrderMonitorService.setReadyOrdersActivityActive(this, false);

        // Unregister receiver
        unregisterReceiver(refreshReceiver);

        // Stop all handlers
        refreshHandler.removeCallbacksAndMessages(null);
    }

    private void fetchReadyOrders() {
        Log.d(TAG, "Fetching ready orders...");

        apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TakeOrder> newReadyOrders = response.body();
                    Log.d(TAG, "Fetched " + newReadyOrders.size() + " ready orders from API");

                    // Log order details for debugging
                    for (TakeOrder order : newReadyOrders) {
                        Log.d(TAG, "Order for table " + order.getTable());

                        if (order.getOrderSpecs() != null) {
                            Log.d(TAG, "  Has " + order.getOrderSpecs().size() + " order specs");
                        } else {
                            Log.e(TAG, "  Order specs is NULL");
                        }
                    }

                    // Update the adapter with the new data
                    if (adapter != null) {
                        adapter.updateOrders(newReadyOrders);
                    }

                    // Update previous count
                    previousOrderCount = newReadyOrders.size();

                    // If no orders and we're visible, show a message
                    if (newReadyOrders.isEmpty() && isActivityVisible) {
                        Toast.makeText(ReadyOrdersActivity.this, "Inga färdiga ordrar just nu", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "No ready orders found or error code: " + response.code());

                    // Update adapter with empty list
                    if (adapter != null) {
                        adapter.updateOrders(new ArrayList<>());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Log.e(TAG, "Network error fetching orders", t);

                if (isActivityVisible) {
                    Toast.makeText(ReadyOrdersActivity.this,
                            "Fel vid hämtning: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Debug method to print raw JSON - call this if you're having parsing issues
    private void debugJsonResponse() {
        try {
            Log.d(TAG, "Debugging JSON response...");
            apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
                @Override
                public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                    try {
                        if (response.isSuccessful()) {
                            Gson gson = new Gson();
                            List<TakeOrder> orders = response.body();
                            if (orders != null) {
                                for (TakeOrder order : orders) {
                                    Log.d(TAG, "JSON for order: " + gson.toJson(order));
                                }
                            }
                        } else {
                            Log.e(TAG, "Error response: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing JSON", e);
                    }
                }

                @Override
                public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                    Log.e(TAG, "Network failure", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception in debugJsonResponse", e);
        }
    }
}