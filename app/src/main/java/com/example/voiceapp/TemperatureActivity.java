package com.example.voiceapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Locale;

public class TemperatureActivity extends AppCompatActivity {

    private static final String TAG = "TemperatureActivity";
    private SeekBar seekBarTemperature;
    private TextView tvCurrentTemperature, tvStatus;
    private Button btnSetTemperature, btnBack;
    private int currentTemperature = 45; // 기본 온도 값 (45도)

    // TTS 관련 변수
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private String pendingUtterance = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        // UI 요소 초기화
        seekBarTemperature = findViewById(R.id.seekbar_temperature);
        tvCurrentTemperature = findViewById(R.id.tv_current_temperature);
        tvStatus = findViewById(R.id.tv_status);
        btnSetTemperature = findViewById(R.id.btn_set_temperature);
        btnBack = findViewById(R.id.btn_back);

        // SeekBar 설정 (30~60도 범위)
        seekBarTemperature.setMax(30); // 60-30=30 범위

        // TTS 초기화
        initTextToSpeech();

        // 인텐트에서 온도 정보 가져오기
        Intent intent = getIntent();
        if (intent.hasExtra("TEMPERATURE")) {
            currentTemperature = intent.getIntExtra("TEMPERATURE", 45);
            // SeekBar 초기값 설정 (30도를 0으로 시작)
            seekBarTemperature.setProgress(currentTemperature - 30);
            updateTemperatureDisplay();

            // 온도 설정 메시지 표시 및 TTS 실행 (1초 지연)
            tvStatus.setText(currentTemperature + "도로 온도 설정을 하겠습니다");
            handler.postDelayed(() -> {
                speak(currentTemperature + "도로 온도 설정을 하겠습니다");
            }, 1000);
        }

        // SeekBar 이벤트 처리
        seekBarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTemperature = progress + 30; // 30도부터 시작
                updateTemperatureDisplay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // 온도 설정 버튼 클릭 이벤트
        btnSetTemperature.setOnClickListener(v -> {
            setTemperature(currentTemperature);
        });

        // 뒤로 가기 버튼 클릭 이벤트
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    // 온도 표시 업데이트
    private void updateTemperatureDisplay() {
        tvCurrentTemperature.setText("현재 설정 온도: " + currentTemperature + "°C");
    }

    // 온도 설정 메소드
    private void setTemperature(int temperature) {
        tvStatus.setText(temperature + "도로 온도 설정을 하겠습니다");
        speak(temperature + "도로 온도 설정을 하겠습니다");

        // 아두이노로 온도 설정 명령 전송
        String command = "TEMP_" + temperature;
        sendDataToArduino(command);

        Toast.makeText(this, temperature + "도로 온도를 설정했습니다.", Toast.LENGTH_SHORT).show();
    }

    // 블루투스 데이터 전송
    private void sendDataToArduino(String command) {
        // MainActivity의 블루투스 연결 매니저를 통해 명령 전송
        if (MainActivity.bluetoothConnection != null && MainActivity.bluetoothConnection.isConnected()) {
            MainActivity.bluetoothConnection.sendData(command);
            Log.d(TAG, "아두이노로 명령 전송: " + command);
        } else {
            Log.e(TAG, "블루투스 연결이 없습니다");
            Toast.makeText(this, "아두이노에 연결되어 있지 않습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // TTS 초기화 메서드
    @SuppressLint("NewApi")
    private void initTextToSpeech() {
        Log.d(TAG, "TTS 초기화 시작");
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS 엔진 초기화 성공");
                int result = textToSpeech.setLanguage(Locale.KOREAN);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "한국어가 지원되지 않습니다");
                    Toast.makeText(TemperatureActivity.this, "TTS에서 한국어를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "TTS 한국어 설정 성공");
                    isTtsReady = true;

                    // TTS 음성 속성 설정
                    textToSpeech.setPitch(1.0f); // 기본 피치
                    textToSpeech.setSpeechRate(1.0f); // 기본 속도

                    // 진행 상황 리스너 설정
                    textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {
                            Log.d(TAG, "TTS 발화 시작: " + utteranceId);
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            Log.d(TAG, "TTS 발화 완료: " + utteranceId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            Log.e(TAG, "TTS 발화 오류: " + utteranceId);
                        }
                    });

                    // 보류 중인 발화가 있다면 지금 말하기
                    if (pendingUtterance != null) {
                        Log.d(TAG, "보류 중인 TTS 발화 실행: " + pendingUtterance);
                        speakNow(pendingUtterance);
                        pendingUtterance = null;
                    }
                }
            } else {
                Log.e(TAG, "TextToSpeech 초기화 실패: " + status);
                Toast.makeText(TemperatureActivity.this, "TTS 초기화에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 즉시 말하기 - TTS가 준비되었을 때만 호출
    @SuppressLint("NewApi")
    private void speakNow(String text) {
        if (textToSpeech != null && isTtsReady) {
            Log.d(TAG, "TTS 즉시 말하기: " + text);

            // 안드로이드 버전에 따라 다른 방식으로 호출
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                String utteranceId = "utteranceId_" + System.currentTimeMillis();
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId_" + System.currentTimeMillis());
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
            }
        } else {
            Log.e(TAG, "TTS가 준비되지 않았습니다. 텍스트 보류: " + text);
        }
    }

    // 음성으로 안내하는 메서드 - 외부에서 호출
    private void speak(String text) {
        if (isTtsReady) {
            speakNow(text);
        } else {
            Log.d(TAG, "TTS 준비 안됨, 텍스트 보류: " + text);
            pendingUtterance = text;

            // TTS가 준비되지 않은 상태에서 5초 후에 다시 시도
            handler.postDelayed(() -> {
                if (!isTtsReady && pendingUtterance != null) {
                    Log.d(TAG, "5초 후 재시도: " + pendingUtterance);
                    if (textToSpeech != null) {
                        speakNow(pendingUtterance);
                    }
                }
            }, 5000);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 핸들러 콜백 제거
        handler.removeCallbacksAndMessages(null);

        // TTS 리소스 해제
        if (textToSpeech != null) {
            Log.d(TAG, "TTS 리소스 해제");
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}