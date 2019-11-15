package com.agriexpertchat.NotificationManager;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Created by Admin on 9/2/2019.
 */

public interface APIService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAnZx0siA:APA91bGdsplAcOPSG4UBs1ohqTihbi-s6NzpLXbh23hkG2OwpO47xVEtBMBU-XabbDgbDPagZg359MShkCuzFSLE5Ggermm2hDE_16MSHyaHnKO0ZXBYgHajpKoC7ht-LwaPZi-pekfQ"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
