package com.example.voiceapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voiceapp.model.User;
import com.example.voiceapp.network.ApiService;
import com.example.voiceapp.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final String PREF_NAME = "UserPrefs";

    private EditText etUserName, etUserAge, etUserHeight, etUserWeight;
    private RadioGroup radioGroupGender;
    private RadioButton radioButtonMale, radioButtonFemale;
    private Button btnSaveUser, btnGoBack;
    private ApiService apiService;
    private SharedPreferences prefs;
    private boolean isProcessing = false; // 중복 클릭 방지용 플래그

    @SuppressLint({"MissingInflatedId", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // SharedPreferences 초기화
        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // UI 요소 연결
        etUserName = findViewById(R.id.et_user_name);
        etUserAge = findViewById(R.id.et_user_age);
        etUserHeight = findViewById(R.id.et_user_Height);
        etUserWeight = findViewById(R.id.et_user_Weight); // 몸무게 입력 필드 연결
        radioGroupGender = findViewById(R.id.radio_group_gender);
        radioButtonMale = findViewById(R.id.radio_button_male);
        radioButtonFemale = findViewById(R.id.radio_button_female);
        btnSaveUser = findViewById(R.id.btn_save_user);
        btnGoBack = findViewById(R.id.btn_go_back);

        // Retrofit 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 힌트 동작 설정
        setupHintBehavior();

        // EditText에 터치 리스너 추가
        setupTouchListeners();

        // 저장 버튼 클릭 이벤트
        btnSaveUser.setOnClickListener(v -> {
            if (!isProcessing && validateInputs()) {
                isProcessing = true;
                btnSaveUser.setEnabled(false);
                checkUserAndSave();
            }
        });

        // 뒤로가기 버튼 클릭 이벤트
        btnGoBack.setOnClickListener(v -> finish());

        // 첫 번째 필드에 포커스 요청 및 키보드 표시
        etUserName.requestFocus();
        @SuppressLint({"NewApi", "LocalSuppress"}) InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etUserName, InputMethodManager.SHOW_IMPLICIT);
    }

    // EditText 터치 리스너 설정
    private void setupTouchListeners() {
        @SuppressLint({"NewApi", "LocalSuppress"}) View.OnTouchListener touchListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setFocusableInTouchMode(true);
                v.requestFocus();
                @SuppressLint({"NewApi", "LocalSuppress"}) InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            }
            return false;
        };

        etUserName.setOnTouchListener(touchListener);
        etUserAge.setOnTouchListener(touchListener);
        etUserHeight.setOnTouchListener(touchListener);
        etUserWeight.setOnTouchListener(touchListener);
    }

    // 힌트 동작 설정
    private void setupHintBehavior() {
        setHintBehavior(etUserName, "이름 입력");
        setHintBehavior(etUserAge, "나이 입력");
        setHintBehavior(etUserHeight, "신장(키) 입력 (cm)");
        setHintBehavior(etUserWeight, "몸무게 입력 (kg)");
    }

    @SuppressLint("NewApi")
    private void setHintBehavior(EditText editText, String hint) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ((EditText) v).setHint("");
            } else {
                if (((EditText) v).getText().toString().trim().isEmpty()) {
                    ((EditText) v).setHint(hint);
                }
            }
        });
    }

    // 입력 데이터 유효성 검사
    @SuppressLint("NewApi")
    private boolean validateInputs() {
        String name = etUserName.getText().toString().trim();
        String age = etUserAge.getText().toString().trim();
        String height = etUserHeight.getText().toString().trim();
        String weight = etUserWeight.getText().toString().trim();

        if (name.isEmpty()) {
            showError(etUserName, "이름을 입력하세요");
            return false;
        }

        if (age.isEmpty()) {
            showError(etUserAge, "나이를 입력하세요");
            return false;
        }

        try {
            Integer.parseInt(age);
        } catch (NumberFormatException e) {
            showError(etUserAge, "나이는 숫자로 입력하세요");
            return false;
        }

        // 성별 선택 확인
        if (radioGroupGender.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (height.isEmpty()) {
            showError(etUserHeight, "신장을 입력하세요");
            return false;
        }

        try {
            Double.parseDouble(height);
        } catch (NumberFormatException e) {
            showError(etUserHeight, "신장은 숫자로 입력하세요");
            return false;
        }

        if (weight.isEmpty()) {
            showError(etUserWeight, "몸무게를 입력하세요");
            return false;
        }

        try {
            Double.parseDouble(weight);
        } catch (NumberFormatException e) {
            showError(etUserWeight, "몸무게는 숫자로 입력하세요");
            return false;
        }

        return true;
    }

    // 오류 메시지 표시 및 포커스 설정
    @SuppressLint("NewApi")
    private void showError(EditText editText, String message) {
        editText.setError(message);
        editText.requestFocus();

        // 키보드 표시
        @SuppressLint({"NewApi", "LocalSuppress"}) InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
    }

    // 사용자 중복 확인 및 저장
    private void checkUserAndSave() {
        String name = etUserName.getText().toString().trim();

        // 사용자 중복 체크 API 호출
        Call<Boolean> checkUserCall = apiService.checkUserExists(name);
        checkUserCall.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body()) {
                        // 중복된 사용자명이 존재하는 경우
                        Toast.makeText(ScanActivity.this, "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
                        isProcessing = false;
                        btnSaveUser.setEnabled(true);

                        // 중복된 사용자의 정보를 가져와 다음 화면으로 이동
                        getUserDataAndMoveToNextScreen(name);
                    } else {
                        // 중복이 없으면 새 사용자로 저장
                        saveNewUser();
                    }
                } else {
                    // API 응답 실패 시, 일단 새 사용자로 저장 시도
                    Log.e(TAG, "사용자 중복 체크 API 응답 실패: " + response.code());
                    saveNewUser();
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e(TAG, "사용자 중복 체크 API 호출 실패", t);
                Toast.makeText(ScanActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isProcessing = false;
                btnSaveUser.setEnabled(true);

                // 오류 발생 시에도 다음 화면으로 이동 (로컬 데이터 사용)
                moveToHealthResultActivity();
            }
        });
    }

    // 기존 사용자 정보 가져오기
    private void getUserDataAndMoveToNextScreen(String name) {
        Call<User> call = apiService.getUserByName(name);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                isProcessing = false;
                btnSaveUser.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();

                    // 가져온 사용자 정보로 다음 화면으로 이동
                    Intent intent = new Intent(ScanActivity.this, HealthResultActivity.class);
                    intent.putExtra("userId", user.getId());
                    intent.putExtra("userName", user.getName());
                    intent.putExtra("userAge", user.getAge());
                    intent.putExtra("userHeight", user.getHeightCm());
                    intent.putExtra("userWeight", user.getWeight());
                    intent.putExtra("userGender", user.getGender());

                    startActivity(intent);
                    finish();
                } else {
                    // 사용자 정보를 가져오지 못했을 때, 현재 입력된 정보로 다음 화면으로 이동
                    Log.e(TAG, "사용자 정보 가져오기 실패: " + response.code());
                    moveToHealthResultActivity();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "사용자 정보 가져오기 API 호출 실패", t);
                isProcessing = false;
                btnSaveUser.setEnabled(true);
                moveToHealthResultActivity();
            }
        });
    }

    // 새 사용자 정보 저장
    private void saveNewUser() {
        // 사용자 입력값 가져오기
        String name = etUserName.getText().toString().trim();
        int age = Integer.parseInt(etUserAge.getText().toString().trim());
        int height = Integer.parseInt(etUserHeight.getText().toString().trim());
        double weight = Double.parseDouble(etUserWeight.getText().toString().trim());
        String gender = radioButtonMale.isChecked() ? "남" : "여";

        // 사용자 객체 생성
        User user = new User(name, age, gender, height, weight);

        // 서버에 저장 API 호출
        Call<User> callSave = apiService.createUser(user);
        callSave.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                isProcessing = false;
                btnSaveUser.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    // 저장 성공
                    User savedUser = response.body();
                    Toast.makeText(ScanActivity.this, "사용자 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();

                    // 서버에 저장된 사용자 정보로 다음 화면으로 이동
                    Intent intent = new Intent(ScanActivity.this, HealthResultActivity.class);
                    intent.putExtra("userId", savedUser.getId());
                    intent.putExtra("userName", savedUser.getName());
                    intent.putExtra("userAge", savedUser.getAge());
                    intent.putExtra("userHeight", savedUser.getHeightCm());
                    intent.putExtra("userWeight", savedUser.getWeight());
                    intent.putExtra("userGender", savedUser.getGender());

                    startActivity(intent);
                    finish();
                } else {
                    // 저장 실패
                    Log.e(TAG, "사용자 저장 실패: " + response.code());
                    Toast.makeText(ScanActivity.this, "사용자 정보 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();

                    // 저장 실패해도 다음 화면으로 이동 (현재 입력 데이터 사용)
                    moveToHealthResultActivity();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Log.e(TAG, "사용자 저장 API 호출 실패", t);
                Toast.makeText(ScanActivity.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                isProcessing = false;
                btnSaveUser.setEnabled(true);

                // 오류 발생 시에도 다음 화면으로 이동 (로컬 데이터 사용)
                moveToHealthResultActivity();
            }
        });
    }

    // 현재 입력된 데이터로 HealthResultActivity로 이동
    @SuppressLint("NewApi")
    private void moveToHealthResultActivity() {
        // 현재 입력값 가져오기
        String name = etUserName.getText().toString().trim();
        int age = Integer.parseInt(etUserAge.getText().toString().trim());
        int height = Integer.parseInt(etUserHeight.getText().toString().trim());
        double weight = Double.parseDouble(etUserWeight.getText().toString().trim());
        String gender = radioButtonMale.isChecked() ? "남" : "여";

        // 로컬 SharedPreferences에 저장
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("lastUserName", name);
        editor.apply();

        // HealthResultActivity로 데이터 전달
        Intent intent = new Intent(ScanActivity.this, HealthResultActivity.class);
        intent.putExtra("userName", name);
        intent.putExtra("userAge", age);
        intent.putExtra("userHeight", height);
        intent.putExtra("userWeight", weight);
        intent.putExtra("userGender", gender);

        startActivity(intent);
        finish();
    }
}