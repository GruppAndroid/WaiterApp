package com.example.waiterapplication;

import com.google.gson.annotations.SerializedName;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class OrderSpecs {
    @SerializedName("category")
    private String category;
    @SerializedName("meal")
    private String meal;
    @SerializedName("count")
    private Integer count;
    public String getCategory(){return category;}
    public String getMeal(){return meal;}
    public Integer getCount(){return count;}
    public void setCategory(String category){
        this.category=category;
    }
    public void setMeal(String meal){this.meal=meal;}
    public void setCount(Integer count){this.count=count;}
}
