package com.example.waiterapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

public class MainActivity extends AppCompatActivity {

    Button b1, b2, b3, b4, b5, b6, b7, b8;
    ListView lvNotifications;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_selection);

        b1 = findViewById(R.id.btnTable1);
        b2 = findViewById(R.id.btnTable2);
        b3 = findViewById(R.id.btnTable3);
        b4 = findViewById(R.id.btnTable4);
        b5 = findViewById(R.id.btnTable5);
        b6 = findViewById(R.id.btnTable6);
        b7 = findViewById(R.id.btnTable7);
        b8 = findViewById(R.id.btnTable8);
        //lvNotifications = findViewById(R.id.lvNotifications);

        // Använd den uppdaterade Retrofit-instansen för att kommunicera med köket
        apiService = Retrofit.getInstance().getApi();

        Call<List<TakeOrder>> call = apiService.getOrder();
        call.enqueue(new Callback<List<TakeOrder>>() {
            @Override
            public void onResponse(Call<List<TakeOrder>> call, Response<List<TakeOrder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TakeOrder> orders = response.body();
                    List<String> combinedList = new ArrayList<>();

                    for (TakeOrder order : orders) {
                        if (Objects.equals(order.getComplete(), "True")) {
                            String tableAndMeal = "Table: " + order.getTable() + " " + order.getMeal() + " | Completed";
                            combinedList.add(tableAndMeal);
                        }
                    }

                    ArrayAdapter<String> combinedAdapter = new ArrayAdapter<>(MainActivity.this, R.layout.activity_item_centered, combinedList);
                    lvNotifications.setAdapter(combinedAdapter);
                }
            }

            @Override
            public void onFailure(Call<List<TakeOrder>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "ERROR: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void tableOne(View view){
        startOrderActivity("1");
    }

    public void tableTwo(View view){
        startOrderActivity("2");
    }

    public void tableThree(View view){
        startOrderActivity("3");
    }

    public void tableFour(View view){
        startOrderActivity("4");
    }

    public void tableFive(View view){
        startOrderActivity("5");
    }

    public void tableSix(View view){
        startOrderActivity("6");
    }

    public void tableSeven(View view){
        startOrderActivity("7");
    }

    public void tableEight(View view){
        startOrderActivity("8");
    }

    private void startOrderActivity(String tableNumber) {
        Intent intent = new Intent(MainActivity.this, Order.class);
        intent.putExtra("TABLE_NUMBER", tableNumber);
        startActivity(intent);
    }
}
