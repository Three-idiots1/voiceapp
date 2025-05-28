package com.example.voiceapp.network;

import com.example.voiceapp.model.ResponseObject;
import com.example.voiceapp.model.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // 사용자 생성
    @POST("/api/users")  // 백엔드 컨트롤러에 맞게 수정
    Call<User> createUser(@Body User user);

    // ID로 사용자 조회
    @GET("/api/users/{id}")
    Call<User> getUserById(@Path("id") Long id);

    // 이름으로 사용자 조회 - 백엔드에 구현 필요
    @GET("/api/users/findByName")
    Call<User> getUserByName(@Query("name") String name);

    // 사용자 존재 여부 확인 - 백엔드에 구현 필요
    @GET("/api/users/exists")
    Call<Boolean> checkUserExists(@Query("name") String name);
}