package com.example.waiterapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Order extends AppCompatActivity {

    // NumberPickers för rätter
    NumberPicker npAppetizer1, npAppetizer2, npAppetizer3;
    NumberPicker npMain1, npMain2, npMain3;
    NumberPicker npDessert1, npDessert2, npDessert3;

    // TextView för att visa valt bord (finns i headern i din layout)
    TextView tvTableNumber;
    Button btnSubmitOrder;
    List<TakeOrder> orders = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order); // Se till att filnamnet stämmer

        // Initiera vyerna
        tvTableNumber = findViewById(R.id.tvTableNumber);
        npAppetizer1 = findViewById(R.id.npAppetizer1);
        npAppetizer2 = findViewById(R.id.npAppetizer2);
        npAppetizer3 = findViewById(R.id.npAppetizer3);
        npMain1 = findViewById(R.id.npMain1);
        npMain2 = findViewById(R.id.npMain2);
        npMain3 = findViewById(R.id.npMain3);
        npDessert1 = findViewById(R.id.npDessert1);
        npDessert2 = findViewById(R.id.npDessert2);
        npDessert3 = findViewById(R.id.npDessert3);
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder);

        // Hämta bordnummer från Intent och visa det (t.ex. "Bord: 3")
        String tableNumber = getIntent().getStringExtra("TABLE_NUMBER");
        if (tableNumber != null) {
            tvTableNumber.setText("Bord: " + tableNumber);
        }

        // Konfigurera NumberPickers
        configNumberPicker(npAppetizer1);
        configNumberPicker(npAppetizer2);
        configNumberPicker(npAppetizer3);
        configNumberPicker(npMain1);
        configNumberPicker(npMain2);
        configNumberPicker(npMain3);
        configNumberPicker(npDessert1);
        configNumberPicker(npDessert2);
        configNumberPicker(npDessert3);

        // Hämta Retrofit-instansen för API-anrop
        apiService = Retrofit.getInstance().getApi();
    }

    private void configNumberPicker(NumberPicker picker) {
        picker.setMinValue(0);
        picker.setMaxValue(9);
        picker.setWrapSelectorWheel(true);
    }

    // Anropas via android:onClick="sendOrder" i layouten
    public void sendOrder(View view) {
        orders.clear();

        // 1. Kolla att vi har ett bordnummer
        String tableText = tvTableNumber.getText().toString();
        if (tableText.isEmpty()) {
            Toast.makeText(this, "Bordnummer saknas!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Extrahera siffra ("Bord: 3" -> "3")
        int tableInt = Integer.parseInt(tableText.replaceAll("[^0-9]", ""));

        // 2. Läs antal valda för varje NumberPicker
        List<Integer> vals = new ArrayList<>();
        vals.add(npAppetizer1.getValue());
        vals.add(npAppetizer2.getValue());
        vals.add(npAppetizer3.getValue());
        vals.add(npMain1.getValue());
        vals.add(npMain2.getValue());
        vals.add(npMain3.getValue());
        vals.add(npDessert1.getValue());
        vals.add(npDessert2.getValue());
        vals.add(npDessert3.getValue());

        // 3. Summera valen per kategori
        int totalAppetizers = npAppetizer1.getValue() + npAppetizer2.getValue() + npAppetizer3.getValue();
        int totalMains      = npMain1.getValue() + npMain2.getValue() + npMain3.getValue();
        int totalDesserts   = npDessert1.getValue() + npDessert2.getValue() + npDessert3.getValue();

        // 4. Kontrollera hur många kategorier som valts
        int categoriesChosen = 0;
        if (totalAppetizers > 0) categoriesChosen++;
        if (totalMains > 0) categoriesChosen++;
        if (totalDesserts > 0) categoriesChosen++;

        if (categoriesChosen == 0) {
            Toast.makeText(this, "Välj minst en rätt i en kategori!", Toast.LENGTH_SHORT).show();
            return;
        } else if (categoriesChosen > 1) {
            Toast.makeText(this, "Endast EN kategori åt gången är tillåtet!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Bygg ordern endast om exakt en kategori är vald
        List<String> cat = Arrays.asList("Förrätt", "Huvudrätt", "Efterrätt");
        List<String> avail_meals = Arrays.asList(
                "Prawn Chips", "Garlic Bread", "Baked Parmesan Tomato",
                "Smoked Salmon", "Fish and Chips", "Extreme Burger",
                "Pudding", "Ice cream", "Apple Pie"
        );

        Boolean isEmpty = true;
        TakeOrder order = new TakeOrder();
        order.setTable(tableInt);

        // Loopar igenom alla 9 NumberPickers
        for (int i = 0; i < vals.size(); i++) {
            if (vals.get(i) > 0) {
                OrderSpecs meals = new OrderSpecs();
                meals.setCategory(cat.get(i / 3));
                meals.setMeal(avail_meals.get(i));
                meals.setCount(vals.get(i));
                order.addOrderSpec(meals);
                isEmpty = false;
            }
        }

        order.setComplete(false);

        if (isEmpty) {
            Toast.makeText(this, "Inga rätter valda!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 6. Skicka ordern till servern
        Call<TakeOrder> call = apiService.sendOrder(order);
        call.enqueue(new Callback<TakeOrder>() {
            @Override
            public void onResponse(Call<TakeOrder> call, Response<TakeOrder> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(Order.this, "Order skickad!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Order.this, MainActivity.class));
                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        Toast.makeText(Order.this, "Kunde inte skicka order: " + response.code() + " " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(Order.this, "Fel vid hämtning av errorBody", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<TakeOrder> call, Throwable t) {
                Log.e("API_ERROR", "Fel vid API-anrop", t);
                Toast.makeText(Order.this, "Fel: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }
}
