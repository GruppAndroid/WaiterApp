package com.example.waiterapplication.api;

import com.example.waiterapplication.TakeOrder;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // Skicka en order
    @POST("api/kitchen/orders")
    Call<TakeOrder> sendOrder(@Body TakeOrder order);

    // Hämta färdiga ordrar
    @GET("api/kitchen/orders/ready")
    Call<List<TakeOrder>> getReadyOrders();

    // Markera en order som levererad
    @PUT("api/kitchen/orders/{tableNumber}/delivered")
    Call<Void> markOrderDelivered(@Path("tableNumber") int tableNumber);

}
