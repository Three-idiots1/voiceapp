package com.example.voiceapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Locale;
import android.os.CountDownTimer;

public class ManualModeActivity extends AppCompatActivity {

    private static final String TAG = "ManualModeActivity";
    private Button btnShoulder, btnBack, btnWaist;
    private TextView tvRemainingTime, tvStatus;
    private String selectedIntensity = "N2"; // 기본 강도 (중간)
    private String selectedTime = "T3"; // 기본 시간 (3분)
    private CountDownTimer countDownTimer;

    // TTS 추가
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;
    private String pendingUtterance = null;
    private Handler handler = new Handler();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_mode);

        // 버튼 ID와 연결
        btnShoulder = findViewById(R.id.btn_shoulder);
        btnBack = findViewById(R.id.btn_back);
        btnWaist = findViewById(R.id.btn_waist);
        tvRemainingTime = findViewById(R.id.tv_remaining_time);
        tvStatus = findViewById(R.id.tv_status);

        // TTS 초기화 - 개선된 방식
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            initTextToSpeech();
        }

        // 메인 화면에서 전달된 강도와 시간을 확인하여 설정
        Intent intent = getIntent();
        if (intent.hasExtra("selectedIntensity")) {
            selectedIntensity = intent.getStringExtra("selectedIntensity");
        }

        if (intent.hasExtra("selectedTime")) {
            selectedTime = intent.getStringExtra("selectedTime");
        }

        // 음성 명령으로 어깨/등/허리 모드가 선택되었는지 확인하고 화면에 표시
        String selectedMode = intent.getStringExtra("SELECTED_MODE");
        if (selectedMode != null) {
            // selectedMode가 비어있지 않은 경우에만 로그 출력
            Log.d(TAG, "음성으로 선택된 모드: " + selectedMode);

            // 화면에 표시하고 1초 후에 모드 시작 (TTS 초기화 시간 확보)
            if (selectedMode.equals("SHOULDER")) {
                if (tvStatus != null) {
                    tvStatus.setText("어깨모드를 시작하겠습니다");
                }
                // 1초 후 모드 시작 (TTS가 초기화되도록 약간의 지연 적용)
                handler.postDelayed(() -> startShoulderMode(), 1000);
            } else if (selectedMode.equals("BACK")) {
                if (tvStatus != null) {
                    tvStatus.setText("등모드를 시작하겠습니다");
                }
                handler.postDelayed(() -> startBackMode(), 1000);
            } else if (selectedMode.equals("WAIST")) {
                if (tvStatus != null) {
                    tvStatus.setText("허리모드를 시작하겠습니다");
                }
                handler.postDelayed(() -> startWaistMode(), 1000);
            } else {
                setupManualMode(); // 수동 모드 설정
            }
        } else {
            Log.d(TAG, "음성 선택 모드가 없음 - 수동 모드로 설정");
            setupManualMode(); // 수동 모드 설정
        }
    }

    // TTS 초기화 메서드 - 개선된 방식
    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void initTextToSpeech() {
        Log.d(TAG, "TTS 초기화 시작");
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS 엔진 초기화 성공");
                int result = textToSpeech.setLanguage(Locale.KOREAN);

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "한국어가 지원되지 않습니다");
                    Toast.makeText(ManualModeActivity.this, "TTS에서 한국어를 지원하지 않습니다", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ManualModeActivity.this, "TTS 초기화에 실패했습니다", Toast.LENGTH_SHORT).show();
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

    // 어깨 모드 시작 메서드
    private void startShoulderMode() {
        // 화면에 상태 표시
        if (tvStatus != null) {
            tvStatus.setText("어깨모드를 시작하겠습니다");
        } else {
            Log.e(TAG, "tvStatus가 null입니다");
        }

        // 음성으로 안내
        speak("어깨모드를 시작하겠습니다");

        Log.d(TAG, "어깨 모드 시작");

        // 바로 어깨 지압 시작 (강도와 시간은 기본값 사용)
        String command = "SHOULDER_" + selectedIntensity + selectedTime;
        sendDataToArduino(command);

        // 타이머 시작
        int duration = getTimeInMilliseconds(Integer.parseInt(selectedTime.substring(1)));
        startCountdownTimer(duration, null);
    }

    private void startBackMode() {
        // 화면에 상태 표시
        if (tvStatus != null) {
            tvStatus.setText("등모드를 시작하겠습니다");
        } else {
            Log.e(TAG, "tvStatus가 null입니다");
        }

        // 음성으로 안내
        speak("등모드를 시작하겠습니다");

        Log.d(TAG, "등 모드 시작");

        // 바로 등 지압 시작 (강도와 시간은 기본값 사용)
        String command = "BACK_" + selectedIntensity + selectedTime;
        sendDataToArduino(command);

        // 타이머 시작
        int duration = getTimeInMilliseconds(Integer.parseInt(selectedTime.substring(1)));
        startCountdownTimer(duration, null);
    }

    private void startWaistMode() {
        // 화면에 상태 표시
        if (tvStatus != null) {
            tvStatus.setText("허리모드를 시작하겠습니다");
        } else {
            Log.e(TAG, "tvStatus가 null입니다");
        }

        // 음성으로 안내
        speak("허리모드를 시작하겠습니다");

        Log.d(TAG, "허리 모드 시작");

        // 바로 허리 지압 시작 (강도와 시간은 기본값 사용)
        String command = "WAIST_" + selectedIntensity + selectedTime;
        sendDataToArduino(command);

        // 타이머 시작
        int duration = getTimeInMilliseconds(Integer.parseInt(selectedTime.substring(1)));
        startCountdownTimer(duration, null);
    }

    // 수동 모드 설정
    private void setupManualMode() {
        btnShoulder.setOnClickListener(v -> {
            // 어깨 버튼 클릭 시 어깨 모드 상태 표시 및 음성 안내
            if (tvStatus != null) {
                tvStatus.setText("어깨모드를 시작하겠습니다");
            }
            speak("어깨모드를 시작하겠습니다");
            showIntensityDialog("SHOULDER");
        });

        btnBack.setOnClickListener(v -> {
            // 등 버튼 클릭 시 등 모드 상태 표시 및 음성 안내
            if (tvStatus != null) {
                tvStatus.setText("등모드를 시작하겠습니다");
            }
            speak("등모드를 시작하겠습니다");
            showIntensityDialog("BACK");
        });

        btnWaist.setOnClickListener(v -> {
            // 허리 버튼 클릭 시 허리 모드 상태 표시 및 음성 안내
            if (tvStatus != null) {
                tvStatus.setText("허리모드를 시작하겠습니다");
            }
            speak("허리모드를 시작하겠습니다");
            showIntensityDialog("WAIST");
        });
    }

    // 지압 강도 선택 팝업 (수동 모드)
    private void showIntensityDialog(final String bodyPart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("지압 강도를 선택하세요")
                .setItems(new String[]{"약", "중", "강"}, (dialog, which) -> {
                    selectedIntensity = "N" + (which + 1);
                    showTimeDialog(bodyPart); // 강도 선택 후 시간 설정 팝업
                })
                .show();
    }

    // 지압 시간 선택 팝업 (수동 모드)
    private void showTimeDialog(final String bodyPart) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("지압 시간을 선택하세요")
                .setItems(new String[]{"1분", "3분", "5분"}, (dialog, which) -> {
                    int selectedMinutes = 1;
                    if (which == 1) selectedMinutes = 3;
                    else if (which == 2) selectedMinutes = 5;

                    selectedTime = "T" + selectedMinutes;
                    int duration = getTimeInMilliseconds(selectedMinutes);

                    // 선택한 부위에 맞는 명령어 생성
                    String command = bodyPart + "_" + selectedIntensity + selectedTime;
                    sendDataToArduino(command);

                    startCountdownTimer(duration, null);
                })
                .show();
    }

    // 타이머 시작 (남은 시간 표시)
    private void startCountdownTimer(int duration, Runnable onFinishAction) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvRemainingTime != null) {
                    tvRemainingTime.setText("남은 시간: " + millisUntilFinished / 1000 + "초");
                }
            }

            @Override
            public void onFinish() {
                if (tvRemainingTime != null) {
                    tvRemainingTime.setText("지압 종료됨");
                }
                if (tvStatus != null) {
                    tvStatus.setText("지압이 완료되었습니다");
                }
                speak("지압이 완료되었습니다");
                sendDataToArduino("STOP");
                if (onFinishAction != null) {
                    onFinishAction.run(); // 콜백 실행
                }
            }
        }.start();
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

    // 사용자가 선택한 시간(1, 3, 5분)을 밀리초 단위로 변환
    private int getTimeInMilliseconds(int timeOption) {
        switch (timeOption) {
            case 1: return 60000;
            case 3: return 180000;
            case 5: return 300000;
            default: return 60000;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 화면 복귀 시 필요한 처리
        if (!isTtsReady && textToSpeech != null) {
            // TTS 재초기화 시도
            Log.d(TAG, "화면 복귀 - TTS 상태 확인");
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

        // 타이머 취소
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}