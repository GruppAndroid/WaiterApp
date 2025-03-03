package com.example.waiterapplication;
import com.example.waiterapplication.OrderSpecs;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TakeOrder {

    @SerializedName("tableNumber")
    private int table;

    @SerializedName("orderSpecs")
    List<OrderSpecs> orderSpecs;


    @SerializedName("isFinished")
    private Boolean complete;

    public TakeOrder(){ orderSpecs = new ArrayList<>();}
    public int getTable() {
        return table;
    }


    public Boolean getComplete() {
        return complete;
    }

    public void setTable(int table) {
        this.table = table;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }
    public void addOrderSpec(OrderSpecs orderspec){
        orderSpecs.add(orderspec);
    }

    public List<OrderSpecs> getOrderSpecs() {
        return orderSpecs;
    }

}
