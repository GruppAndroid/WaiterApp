package com.example.waiterapplication.api;

import com.example.waiterapplication.TakeOrder;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("kitchen/order")
    Call<TakeOrder> sendOrder(@Body TakeOrder order);

    @GET("kitchen/order")
    Call<List<TakeOrder>> getOrder();
}
