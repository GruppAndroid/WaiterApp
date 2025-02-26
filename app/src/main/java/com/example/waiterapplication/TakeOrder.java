package com.example.waiterapplication;

import com.google.gson.annotations.SerializedName;

public class TakeOrder {

    @SerializedName("tableNr")
    private int table;

    @SerializedName("category")
    private String category;

    @SerializedName("meal")
    private String meal;

    @SerializedName("time")
    private String time;

    @SerializedName("complete")
    private String complete;

    public int getTable() {
        return table;
    }

    public String getCategory() {
        return category;
    }

    public String getMeal() {
        return meal;
    }

    public String getTime() {
        return time;
    }

    public String getComplete() {
        return complete;
    }

    public void setTable(int table) {
        this.table = table;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMeal(String meal) {
        this.meal = meal;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setComplete(String complete) {
        this.complete = complete;
    }
}
