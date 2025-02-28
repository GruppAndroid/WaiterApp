package com.example.waiterapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.waiterapplication.api.ApiService;
import com.example.waiterapplication.api.Retrofit;

public class MainActivity extends AppCompatActivity {

    // Referenser till bords-knapparna
    Button b1, b2, b3, b4, b5, b6, b7, b8;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_selection); // Kontrollera att layoutfilens namn stämmer

        // Initiera knapparna
        b1 = findViewById(R.id.btnTable1);
        b2 = findViewById(R.id.btnTable2);
        b3 = findViewById(R.id.btnTable3);
        b4 = findViewById(R.id.btnTable4);
        b5 = findViewById(R.id.btnTable5);
        b6 = findViewById(R.id.btnTable6);
        b7 = findViewById(R.id.btnTable7);
        b8 = findViewById(R.id.btnTable8);

        apiService = Retrofit.getInstance().getApi();
    }

    // Dessa metoder anropas via android:onClick i din layout
    public void tableOne(View view) {
        startOrderActivity("1");
    }

    public void tableTwo(View view) {
        startOrderActivity("2");
    }

    public void tableThree(View view) {
        startOrderActivity("3");
    }

    public void tableFour(View view) {
        startOrderActivity("4");
    }

    public void tableFive(View view) {
        startOrderActivity("5");
    }

    public void tableSix(View view) {
        startOrderActivity("6");
    }

    public void tableSeven(View view) {
        startOrderActivity("7");
    }

    public void tableEight(View view) {
        startOrderActivity("8");
    }

    private void startOrderActivity(String tableNumber) {
        Intent intent = new Intent(MainActivity.this, Order.class);
        intent.putExtra("TABLE_NUMBER", tableNumber);
        startActivity(intent);
    }
}
