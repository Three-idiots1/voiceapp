package com.example.voiceapp.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {
    // 서버 주소 또는 엔드포인트
    private static final String BASE_URL = "http://10.32.31.156:8080";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // OkHttpClient 설정
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Gson 설정
            Gson gson = new GsonBuilder()
                    // 비공식 JSON 포맷을 허용해 파싱 가능하게 함
                    .setLenient()
                    // null 필드도 직렬화
                    //.serializeNulls()
                    // HTML 태그 이스케이프 비활성화
                    //.disableHtmlEscaping()
                    // 필요한 다른 설정들...
                    .create();

            // Retrofit 설정
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    // Gson 설정 반영
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
