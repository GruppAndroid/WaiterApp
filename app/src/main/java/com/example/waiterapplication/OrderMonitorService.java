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
    private static final long CHECK_INTERVAL = 5000;  // Kontrollera var 5:e sekund

    private Handler handler;
    private ApiService apiService;
    private List<Integer> knownTableNumbers = new ArrayList<>();
    private boolean isFirstCheck = true;  // För att undvika ljud vid första kontrollen
    private boolean isReadyOrdersActivityActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        apiService = Retrofit.getInstance().getApi();
        startPolling();  // Starta polling direkt
        Log.d(TAG, "Order monitor service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Order monitor service started");

        if (intent != null && intent.hasExtra("readyOrdersActivityActive")) {
            isReadyOrdersActivityActive = intent.getBooleanExtra("readyOrdersActivityActive", false);
            Log.d(TAG, "ReadyOrdersActivity status updated: " + (isReadyOrdersActivityActive ? "active" : "inactive"));
        }

        return START_STICKY;
    }

    // Starta polling för att kontrollera färdiga ordrar
    private void startPolling() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkForReadyOrders();  // Kontrollera färdiga ordrar
                handler.postDelayed(this, CHECK_INTERVAL);  // Kör igen efter 5 sekunder
            }
        }, CHECK_INTERVAL);
    }

    // Kontrollera om det finns nya färdiga ordrar
    private void checkForReadyOrders() {
        apiService.getReadyOrders().enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TakeOrder> readyOrders = response.body();
                    List<Integer> currentTableNumbers = new ArrayList<>();

                    for (TakeOrder order : readyOrders) {
                        currentTableNumbers.add(order.getTable());
                    }

                    if (isFirstCheck) {
                        knownTableNumbers = new ArrayList<>(currentTableNumbers);
                        isFirstCheck = false;
                        return;
                    }

                    boolean hasNewReadyOrders = false;
                    for (Integer tableNumber : currentTableNumbers) {
                        if (!knownTableNumbers.contains(tableNumber)) {
                            hasNewReadyOrders = true;
                        }
                    }

                    if (hasNewReadyOrders) {
                        playNotificationSound();  // 🔊 Spela ljud

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            // 📋 Automatiskt navigera till sidan för färdiga ordrar
                            Intent readyOrdersIntent = new Intent(OrderMonitorService.this, ReadyOrdersActivity.class);
                            readyOrdersIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(readyOrdersIntent);
                        }, 1500);  // Vänta 1,5 sekunder för ljuduppspelning
                    }

                    knownTableNumbers = new ArrayList<>(currentTableNumbers);
                } else {
                    Log.d(TAG, "Inga färdiga ordrar hittades");
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Log.e(TAG, "Network error", t);
            }
        });
    }

    // Spela notifikationsljud
    private void playNotificationSound() {
        ensureAudioVolumeIsUp();

        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.order_ready_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing notification sound", e);
        }
    }

    // Justera ljudvolymen om den är för låg
    private void ensureAudioVolumeIsUp() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                if (currentVolume < (maxVolume * 0.8)) {
                    int newVolume = (int) (maxVolume * 0.8);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting volume", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);  // Ta bort alla callbacks
        Log.d(TAG, "Order monitor service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Uppdatera status för ReadyOrdersActivity
    public static void setReadyOrdersActivityActive(Context context, boolean isActive) {
        Intent intent = new Intent(context, OrderMonitorService.class);
        intent.putExtra("readyOrdersActivityActive", isActive);
        context.startService(intent);
    }
}
