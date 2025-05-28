package com.example.voiceapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AutoModeActivity extends AppCompatActivity {

    private static final String TAG = "AutoModeActivity";
    private int autoStep = 0; // 자동 실행 단계 (0: 어깨, 1: 등, 2: 허리)
    private String selectedIntensity = "N2"; // 기본 강도 (중간)
    private String selectedTime = "T3"; // 기본 시간 (3분)
    private CountDownTimer countDownTimer;
    private TextView tvCurrentBodyPart, tvRemainingTime, tvProgressStatus;
    // 블루투스 관련 변수
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private final String DEVICE_NAME = "D2"; // 아두이노 블루투스 모듈 이름 (실제 기기 이름으로 변경 필요)
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID
    private final int REQUEST_ENABLE_BT = 1;
    private boolean isConnected = false;
    private Handler handler = new Handler(Looper.getMainLooper());

    // TTS 추가
    private TextToSpeech textToSpeech;
    private boolean isTtsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_mode);

        // UI 초기화
        tvCurrentBodyPart = findViewById(R.id.tv_current_body_part);
        tvRemainingTime = findViewById(R.id.tv_remaining_time);
        tvProgressStatus = findViewById(R.id.tv_progress_status);

        // 상태 텍스트 초기화
        tvProgressStatus.setText("자동모드를 시작하겠습니다");

        // TTS 초기화
        initTextToSpeech();

        // 메인 화면에서 전달된 강도와 시간을 확인하여 설정
        Intent intent = getIntent();
        String intensity = intent.getStringExtra("selectedIntensity");
        if (intensity != null) {
            selectedIntensity = intensity;
        }

        String time = intent.getStringExtra("selectedTime");
        if (time != null) {
            selectedTime = time;
        }

        // 블루투스 초기화
        setupBluetooth();
    }

    // TTS 초기화 메서드
    @SuppressLint("NewApi")
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "한국어가 지원되지 않습니다");
                } else {
                    // TTS 초기화 성공 시 바로 시작 메시지 출력
                    isTtsReady = true;
                    speak("자동모드를 시작하겠습니다");
                    Log.d(TAG, "TTS 초기화 성공: 시작 메시지 재생");
                }
            } else {
                Log.e(TAG, "TextToSpeech 초기화 실패");
            }
        });
    }

    // 음성으로 안내하는 메서드
    @SuppressLint("NewApi")
    private void speak(String text) {
        if (textToSpeech != null && isTtsReady) {
            Log.d(TAG, "TTS 말하기: " + text);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
        } else {
            Log.e(TAG, "TTS가 초기화되지 않았거나 준비되지 않았습니다");
        }
    }

    // 블루투스 초기화 및 연결
    @SuppressLint("NewApi")
    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 블루투스를 지원하지 않는 경우
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 블루투스가 비활성화된 경우
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        // 블루투스 연결 시도
        connectToDevice();
    }

    // 블루투스 기기 연결
    @SuppressLint("NewApi")
    private void connectToDevice() {
        tvProgressStatus.setText("블루투스 연결 중...");
        speak("블루투스 연결 중입니다");

        // 연결은 백그라운드 스레드에서 처리
        new Thread(() -> {
            try {
                // 페어링된 장치 검색
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                @SuppressLint({"NewApi", "LocalSuppress"}) Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        // 설정된 이름의 기기 찾기 (실제 기기 이름으로 변경 필요)
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        if (device.getName().equals(DEVICE_NAME)) {
                            bluetoothDevice = device;
                            break;
                        }
                    }
                }

                // 기기를 찾지 못한 경우
                if (bluetoothDevice == null) {
                    showToast("연결할 블루투스 기기를 찾을 수 없습니다. 페어링을 확인해주세요.");
                    return;
                }

                // 소켓 연결 시도
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                showToast("블루투스 연결 성공: " + bluetoothDevice.getName());
                speak("블루투스 연결 성공");

                // UI 스레드에서 자동 모드 시작
                handler.post(() -> startAutoMassageSequence());

            } catch (IOException e) {
                isConnected = false;
                showToast("블루투스 연결 실패: " + e.getMessage());
                speak("블루투스 연결에 실패했습니다");
                e.printStackTrace();

                // 연결 실패 시 종료하거나 재시도 로직 추가
                try {
                    if (bluetoothSocket != null)
                        bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
        }).start();
    }

    // UI 스레드에서 Toast 메시지 표시
    private void showToast(final String message) {
        handler.post(() -> Toast.makeText(AutoModeActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    // **1. 자동 모드에서 어깨 → 등 → 허리 순서대로 실행**
    private void startAutoMassageSequence() {
        showToast("자동 모드 시작: 어깨 → 등 → 허리");
        speak("자동 모드가 시작됩니다. 어깨, 등, 허리 순으로 진행됩니다");
        autoStep = 0; // 처음부터 시작
        startAutoMassageWithDelay();
    }

    // **2. 자동 모드 실행 (딜레이 포함)**
    private void startAutoMassageWithDelay() {
        String[] bodyParts = {"어깨", "등", "허리"};

        if (autoStep >= bodyParts.length) {
            showToast("자동 모드 완료");
            speak("자동 모드가 완료되었습니다");
            return;
        }

        String part = bodyParts[autoStep];
        tvCurrentBodyPart.setText("현재 부위: " + part); // 진행 중인 부위 업데이트
        tvProgressStatus.setText(part + " 부위 지압 중...");
        speak(part + " 부위 지압을 시작합니다");

        // 블루투스 명령 전송
        String command = part + "_" + selectedIntensity + selectedTime;
        sendDataToArduino(command);

        int duration = getTimeInMilliseconds(Integer.parseInt(selectedTime.substring(1)));
        startCountdownTimer(duration, () -> {
            autoStep++; // 다음 부위로 이동
            startAutoMassageWithDelay(); // 다음 부위 실행
        });
    }

    // **3. 블루투스 데이터 전송 (실제 구현)**
    private void sendDataToArduino(String command) {
        if (!isConnected) {
            showToast("블루투스가 연결되어 있지 않습니다.");
            speak("블루투스가 연결되어 있지 않습니다");
            return;
        }

        // 백그라운드 스레드에서 데이터 전송
        new Thread(() -> {
            try {
                // 문자열 형태의 명령을 바이트 형태로 변환하여 전송
                outputStream.write((command + "\n").getBytes());
                outputStream.flush();
                Log.d(TAG, "블루투스 명령 전송: " + command);
            } catch (IOException e) {
                showToast("명령 전송 실패: " + e.getMessage());
                isConnected = false;
                e.printStackTrace();
            }
        }).start();
    }

    // **4. 타이머 시작 (남은 시간 표시 + 자동 종료 후 다음 단계 실행)**
    private void startCountdownTimer(int duration, Runnable onFinishAction) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvRemainingTime.setText("남은 시간: " + millisUntilFinished / 1000 + "초");
            }

            @Override
            public void onFinish() {
                tvRemainingTime.setText("지압 종료됨");
                tvProgressStatus.setText("지압이 완료되었습니다");
                speak("지압이 완료되었습니다");
                sendDataToArduino("STOP");
                if (onFinishAction != null) {
                    onFinishAction.run(); // 다음 부위 실행
                }
            }
        }.start();
    }

    // **5. 사용자가 선택한 시간(1, 3, 5분)을 밀리초 단위로 변환**
    private int getTimeInMilliseconds(int timeOption) {
        switch (timeOption) {
            case 1: return 60000;
            case 3: return 180000;
            case 5: return 300000;
            default: return 60000; // 기본값: 1분
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // 블루투스가 활성화되었으면 연결 시도
                connectToDevice();
            } else {
                // 사용자가 블루투스 활성화를 거부함
                Toast.makeText(this, "블루투스 활성화가 필요합니다.", Toast.LENGTH_SHORT).show();
                speak("블루투스 활성화가 필요합니다");
                finish();
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 타이머 해제
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // TTS 리소스 해제
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        // 블루투스 연결 해제
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}