package com.example.myapplication.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Body;

public interface ApiService {
    
    // 确保路径与 BASE_URL 拼接后为 http://47.96.93.30:8000/api/v1/travel/plan
    @POST("api/v1/travel/plan")
    Call<ResponseBody> planTravel(@Body PlanTravelRequest request);
}
