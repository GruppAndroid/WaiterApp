package com.example.waiterapplication.api;

import com.example.waiterapplication.TakeOrder;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/api/orders")
    Call<List<TakeOrder>> sendOrder(@Body List<TakeOrder> orders);

    @POST("kitchen/order")
    Call<TakeOrder> sendOrder(@Body TakeOrder order);

    @GET("kitchen/order")
    Call<List<TakeOrder>> getOrder();
}
