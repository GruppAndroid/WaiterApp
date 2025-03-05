package com.example.waiterapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Order extends AppCompatActivity {
    private static final String TAG = "Order";
    private TextView tvTableNumber;
    private Button btnSubmitOrder;
    private LinearLayout dishContainer;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        tvTableNumber = findViewById(R.id.tvTableNumber);
        btnSubmitOrder = findViewById(R.id.btnSubmitOrder);
        dishContainer = findViewById(R.id.dishContainer);

        apiService = Retrofit.getInstance().getApi();

        String tableNumber = getIntent().getStringExtra("TABLE_NUMBER");
        if (tableNumber != null) {
            tvTableNumber.setText("Bord: " + tableNumber);
        }

        btnSubmitOrder.setOnClickListener(v -> sendOrder());
        fetchMenuData();
    }

    private void fetchMenuData() {
        Call<List<Map<String, Object>>> menuCall = apiService.getMenu();
        menuCall.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateDishContainer(response.body());
                } else {
                    Toast.makeText(Order.this, "Kunde inte hämta menyn", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(Order.this, "Fel vid hämtning av meny", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDishContainer(List<Map<String, Object>> dishes) {
        dishContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        Map<String, List<Map<String, Object>>> categorizedDishes = new HashMap<>();
        for (Map<String, Object> dish : dishes) {
            String category = (String) dish.get("DISH_TYPE_NAME");
            if (category == null || category.isEmpty()) category = "Okänd kategori";

            categorizedDishes.computeIfAbsent(category, k -> new java.util.ArrayList<>()).add(dish);
        }

        for (String category : categorizedDishes.keySet()) {
            TextView categoryHeader = new TextView(this);
            categoryHeader.setText(category);
            categoryHeader.setTextSize(22);
            categoryHeader.setPadding(0, 20, 0, 10);
            dishContainer.addView(categoryHeader);

            for (Map<String, Object> dish : categorizedDishes.get(category)) {
                View dishRow = inflater.inflate(R.layout.dish_row, dishContainer, false);

                TextView dishNameTextView = dishRow.findViewById(R.id.dishNameTextView);
                NumberPicker dishQuantityPicker = dishRow.findViewById(R.id.dishQuantityPicker);

                dishNameTextView.setText((String) dish.get("DISH_NAME"));
                dishQuantityPicker.setMinValue(0);
                dishQuantityPicker.setMaxValue(9);
                dishQuantityPicker.setWrapSelectorWheel(true);
                dishRow.setTag(dish);
                dishContainer.addView(dishRow);
            }
        }
    }

    private void sendOrder() {
        int childCount = dishContainer.getChildCount();
        TakeOrder order = new TakeOrder();

        String tableNumberStr = tvTableNumber.getText().toString().replace("Bord: ", "");
        order.setTable(Integer.parseInt(tableNumberStr));
        order.setComplete(false);

        boolean hasOrderedItems = false;
        for (int i = 0; i < childCount; i++) {
            View dishRow = dishContainer.getChildAt(i);

            if (dishRow instanceof TextView) continue;

            NumberPicker quantityPicker = dishRow.findViewById(R.id.dishQuantityPicker);
            if (quantityPicker == null) continue;

            int quantity = quantityPicker.getValue();
            if (quantity > 0) {
                hasOrderedItems = true;
                Map<String, Object> dishInfo = (Map<String, Object>) dishRow.getTag();
                String dishName = (String) dishInfo.get("DISH_NAME");
                String dishCategory = (String) dishInfo.get("DISH_TYPE_NAME");
                if (dishCategory == null || dishCategory.isEmpty()) dishCategory = "Okänd kategori";

                OrderSpecs orderSpec = new OrderSpecs();
                orderSpec.setMeal(dishName);
                orderSpec.setCategory(dishCategory);
                orderSpec.setCount(quantity);
                order.addOrderSpec(orderSpec);
            }
        }

        if (!hasOrderedItems) {
            Toast.makeText(this, "Välj minst en rätt!", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.sendOrder(order).enqueue(new Callback<TakeOrder>() {
            @Override
            public void onResponse(Call<TakeOrder> call, Response<TakeOrder> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(Order.this, "Order skickad!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(Order.this, "Kunde inte skicka order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TakeOrder> call, Throwable t) {
                Toast.makeText(Order.this, "Nätverksfel: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
