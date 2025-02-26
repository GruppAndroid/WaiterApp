package com.example.waiterapplication;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Order extends AppCompatActivity {

    // NumberPickers för förrätter
    NumberPicker npAppetizer1, npAppetizer2, npAppetizer3;
    // NumberPickers för huvudrätter
    NumberPicker npMain1, npMain2, npMain3;
    // NumberPickers för efterrätter
    NumberPicker npDessert1, npDessert2, npDessert3;

    TextView tvTableNumber;
    Button btnSubmitOrder;
    List<TakeOrder> orders = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        String tableNumber = getIntent().getStringExtra("TABLE_NUMBER");

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

        if (tvTableNumber != null && tableNumber != null) {
            tvTableNumber.setText(tableNumber);
        }

        configNumberPicker(npAppetizer1);
        configNumberPicker(npAppetizer2);
        configNumberPicker(npAppetizer3);
        configNumberPicker(npMain1);
        configNumberPicker(npMain2);
        configNumberPicker(npMain3);
        configNumberPicker(npDessert1);
        configNumberPicker(npDessert2);
        configNumberPicker(npDessert3);

        // Använd den uppdaterade Retrofit-instansen för kökskommunikation
        apiService = Retrofit.getInstance().getApi();
    }

    private void configNumberPicker(NumberPicker picker) {
        picker.setMinValue(0);
        picker.setMaxValue(9);
        picker.setWrapSelectorWheel(true);
    }

    // Metod kopplad via XML: android:onClick="sendOrder"
    public void sendOrder(View view) {
        orders.clear();

        String tableNumber = tvTableNumber.getText().toString().trim();
        if (tableNumber.isEmpty()) {
            Toast.makeText(this, "Table number is missing!", Toast.LENGTH_SHORT).show();
            return;
        }
        int tableInt = Integer.parseInt(tableNumber);

        // Läs av antal valda för varje NumberPicker
        int qtyApp1 = npAppetizer1.getValue();
        int qtyApp2 = npAppetizer2.getValue();
        int qtyApp3 = npAppetizer3.getValue();

        int qtyMain1 = npMain1.getValue();
        int qtyMain2 = npMain2.getValue();
        int qtyMain3 = npMain3.getValue();

        int qtyDess1 = npDessert1.getValue();
        int qtyDess2 = npDessert2.getValue();
        int qtyDess3 = npDessert3.getValue();

        // Skapa beställningar endast om kvantiteten är > 0
        if (qtyApp1 > 0) {
            orders.add(createOrder("Appetizer", "Prawn Chips", tableInt));
        }
        if (qtyApp2 > 0) {
            orders.add(createOrder("Appetizer", "Garlic Bread", tableInt));
        }
        if (qtyApp3 > 0) {
            orders.add(createOrder("Appetizer", "Baked Parmesan Tomato", tableInt));
        }

        if (qtyMain1 > 0) {
            orders.add(createOrder("MainDish", "Smoked Salmon", tableInt));
        }
        if (qtyMain2 > 0) {
            orders.add(createOrder("MainDish", "Fish and Chips", tableInt));
        }
        if (qtyMain3 > 0) {
            orders.add(createOrder("MainDish", "Extreme Burger", tableInt));
        }

        if (qtyDess1 > 0) {
            orders.add(createOrder("Dessert", "Pudding", tableInt));
        }
        if (qtyDess2 > 0) {
            orders.add(createOrder("Dessert", "Ice Cream", tableInt));
        }
        if (qtyDess3 > 0) {
            orders.add(createOrder("Dessert", "Apple Pie", tableInt));
        }

        if (orders.isEmpty()) {
            Toast.makeText(this, "No dishes selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        sendOrderToServer();
        startActivity(new Intent(this, MainActivity.class));
    }

    private TakeOrder createOrder(String category, String meal, int table) {
        TakeOrder order = new TakeOrder();
        order.setCategory(category);
        order.setMeal(meal);
        order.setComplete("False");
        order.setTable(table);
        order.setTime(getCurrentTime());
        return order;
    }

    private void sendOrderToServer() {
        for (TakeOrder order : orders) {
            Call<TakeOrder> call = apiService.sendOrder(order);
            call.enqueue(new Callback<TakeOrder>() {
                @Override
                public void onResponse(Call<TakeOrder> call, Response<TakeOrder> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(Order.this, "Order sent successfully!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Order.this, "Failed to send order: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<TakeOrder> call, Throwable t) {
                    Toast.makeText(Order.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
        orders.clear();
    }

    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }
}
