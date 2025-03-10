package com.example.waiterapplication;

import android.media.AudioManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderMonitorService extends Service {
    private static final String TAG = "OrderMonitorService";
    private static final long CHECK_INTERVAL = 5000;  // Check every 5 seconds

    private Handler handler;
    private ApiService apiService;
    private List<Integer> knownTableNumbers = new ArrayList<>();
    private boolean isFirstCheck = true;  // To avoid sound on first check
    private boolean isReadyOrdersActivityActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OrderMonitorService created");
        handler = new Handler(Looper.getMainLooper());
        apiService = Retrofit.getInstance().getApi();
        startPolling();  // Start polling immediately
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OrderMonitorService started");

        if (intent != null && intent.hasExtra("readyOrdersActivityActive")) {
            isReadyOrdersActivityActive = intent.getBooleanExtra("readyOrdersActivityActive", false);
            Log.d(TAG, "ReadyOrdersActivity status updated: " + (isReadyOrdersActivityActive ? "active" : "inactive"));
        }

        return START_STICKY;
    }

    // Start polling to check for ready orders
    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForReadyOrders();  // Check for ready orders
                handler.postDelayed(this, CHECK_INTERVAL);  // Run again after 5 seconds
            }
        }, CHECK_INTERVAL);
    }

    // Check if there are new ready orders
    // Check if there are new ready orders
    private void checkForReadyOrders() {
        Log.d(TAG, "Checking for ready orders...");

        apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TakeOrder> allOrders = response.body();
                    Log.d(TAG, "Received " + allOrders.size() + " orders from API");

                    // Filter to only include orders that are marked as finished
                    List<TakeOrder> readyOrders = new ArrayList<>();
                    for (TakeOrder order : allOrders) {
                        if (order.getComplete() != null && order.getComplete()) {
                            readyOrders.add(order);
                            Log.d(TAG, "Order for table " + order.getTable() + " is finished");
                        } else {
                            Log.d(TAG, "Order for table " + order.getTable() + " is NOT finished yet");
                        }
                    }

                    List<Integer> currentTableNumbers = new ArrayList<>();
                    for (TakeOrder order : readyOrders) {
                        currentTableNumbers.add(order.getTable());
                    }

                    if (isFirstCheck) {
                        Log.d(TAG, "First check, just storing table numbers without notification");
                        knownTableNumbers = new ArrayList<>(currentTableNumbers);
                        isFirstCheck = false;
                        return;
                    }

                    // Look for new ready orders (tables that weren't in our known list)
                    boolean hasNewReadyOrders = false;
                    for (Integer tableNumber : currentTableNumbers) {
                        if (!knownTableNumbers.contains(tableNumber)) {
                            hasNewReadyOrders = true;
                            Log.d(TAG, "NEW ready order detected for table: " + tableNumber);
                        }
                    }

                    if (hasNewReadyOrders) {
                        Log.d(TAG, "New ready orders detected, playing notification");
                        playNotificationSound();  // Play sound

                        // Only navigate if we're not already on the ReadyOrdersActivity
                        if (!isReadyOrdersActivityActive) {
                            Log.d(TAG, "Navigating to ReadyOrdersActivity");
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Intent readyOrdersIntent = new Intent(OrderMonitorService.this, ReadyOrdersActivity.class);
                                readyOrdersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(readyOrdersIntent);
                            }, 1500);  // Wait 1.5 seconds for sound to play
                        } else {
                            Log.d(TAG, "ReadyOrdersActivity already active, not navigating");
                            // Send a broadcast to notify ReadyOrdersActivity to refresh
                            Intent refreshIntent = new Intent("com.example.waiterapplication.REFRESH_READY_ORDERS");
                            sendBroadcast(refreshIntent);
                        }
                    }

                    // Update our known table numbers for next check
                    knownTableNumbers = new ArrayList<>(currentTableNumbers);
                } else {
                    Log.d(TAG, "No ready orders found or API error");
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Log.e(TAG, "Network error when checking for ready orders", t);
            }
        });
    }

    // Play notification sound
    private void playNotificationSound() {
        Log.d(TAG, " ATTEMPTING TO PLAY SOUND ");

        try {
            // Force max volume
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                Log.d(TAG, "Volume set to maximum: " + maxVolume);
            }

            // Use simple MediaPlayer directly
            final MediaPlayer mp = new MediaPlayer();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setDataSource(getApplicationContext(),
                    android.net.Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.order_ready_sound));

            mp.setOnPreparedListener(player -> {
                Log.d(TAG, " MEDIA PLAYER PREPARED, PLAYING NOW ");
                player.start();
            });

            mp.setOnCompletionListener(player -> {
                Log.d(TAG, "Sound finished playing");
                player.release();
            });

            mp.setOnErrorListener((player, what, extra) -> {
                Log.e(TAG, " MEDIA PLAYER ERROR: " + what + ", " + extra + " ");
                return false;
            });

            Log.d(TAG, "Preparing MediaPlayer...");
            mp.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, " ERROR PLAYING SOUND: " + e.getMessage() + " ", e);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);  // Remove all callbacks
        Log.d(TAG, "OrderMonitorService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Update status for ReadyOrdersActivity
    public static void setReadyOrdersActivityActive(Context context, boolean isActive) {
        Intent intent = new Intent(context, OrderMonitorService.class);
        intent.putExtra("readyOrdersActivityActive", isActive);
        context.startService(intent);
    }
}