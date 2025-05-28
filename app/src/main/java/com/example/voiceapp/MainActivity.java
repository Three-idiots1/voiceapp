package com.example.voiceapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements BluetoothConnectionManager.BluetoothCallback, ModeManager.ModeCallback {
    private static final String TAG = "MainActivity";
    private static final String DEVICE_NAME = "D2"; // 연결할 블루투스 기기 이름
    public static BluetoothConnectionManager bluetoothConnection;
    private static final int SPEECH_REQUEST_CODE = 1000;
    private BluetoothConnectionManager bluetoothManager;
    private ModeManager modeManager;
    private SpeechRecognizer speechRecognizer;

    private Button btnVoiceRecognition, btnBluetoothConnect;
    private Button btnAutoMode, btnManualMode, btnScanMode, btnTemperatureMode;
    private TextView tvStatus;

    // 핸들러 추가: UI 스레드에서 작업을 실행하기 위함
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 블루투스 활성화 요청
    private final ActivityResultLauncher<Intent> enableBtResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    checkPermissionsAndConnect();
                } else {
                    Toast.makeText(MainActivity.this, "블루투스를 활성화해야 합니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI 초기화
        initializeViews();

        // 매니저 초기화
        bluetoothManager = new BluetoothConnectionManager(this);
        bluetoothManager.setCallback(this);
        bluetoothConnection = bluetoothManager; // 정적 참조 설정

        modeManager = new ModeManager(this);
        modeManager.setCallback(this);

        // 음성 인식기 초기화
        initializeSpeechRecognizer();

        // 블루투스 지원 여부 확인
        if (!bluetoothManager.isBluetoothSupported()) {
            Toast.makeText(this, "블루투스를 지원하지 않는 기기입니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 블루투스 활성화 확인
        if (!bluetoothManager.isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtResult.launch(enableBtIntent);
        }

        // 버튼 이벤트 설정
        setupButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱이 전면으로 돌아올 때마다 블루투스 연결 확인
        //mainHandler.postDelayed(this::checkBluetoothConnection, 1000);
    }

    // 블루투스 연결 상태 확인 메서드 추가
    private void checkBluetoothConnection() {
        if (bluetoothManager != null) {
            boolean isConnected = bluetoothManager.isConnected();
            Log.d(TAG, "블루투스 연결 상태 확인: " + (isConnected ? "연결됨" : "연결 안됨"));

            if (!isConnected) {
                // 상태 표시 업데이트
                tvStatus.setText("블루투스 연결 안됨. 재연결 중...");

                // 재연결 시도
                connectToArduino();

                // 3초 후 연결 확인 상태 업데이트
                mainHandler.postDelayed(() -> {
                    if (bluetoothManager.isConnected()) {
                        tvStatus.setText("블루투스 재연결 성공");
                        // 테스트 메시지 전송
                        sendTestMessage();
                    } else {
                        tvStatus.setText("블루투스 재연결 실패. 다시 시도하세요.");
                    }
                }, 3000);
            } else {
                // 연결 상태 표시
                tvStatus.setText("블루투스 연결 상태: 연결됨");
            }
        }
    }

    // 테스트 메시지 전송 메서드 추가
    private void sendTestMessage() {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            try {
                // 간단한 테스트 메시지 전송
                bluetoothManager.sendData("TEST");
                Log.d(TAG, "테스트 메시지 전송 성공");
            } catch (Exception e) {
                Log.e(TAG, "테스트 메시지 전송 실패: " + e.getMessage(), e);
            }
        }
    }

    @SuppressLint("NewApi")
    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("음성 인식 준비 중...");
            }

            @Override
            public void onBeginningOfSpeech() {
                tvStatus.setText("음성을 입력 중...");
            }

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {
                tvStatus.setText("음성 입력 완료");
            }

            @Override
            public void onError(int error) {
                tvStatus.setText("음성 인식 오류: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);  // 첫 번째 인식된 텍스트
                    tvStatus.setText("인식된 텍스트: " + recognizedText);

                    // 음성 인식 전에 블루투스 연결 확인
                    if (!bluetoothManager.isConnected()) {
                        tvStatus.setText("블루투스 연결이 끊겼습니다. 재연결 중...");
                        connectToArduino();
                        // 잠시 후에 명령 처리 시도
                        mainHandler.postDelayed(() -> {
                            if (bluetoothManager.isConnected()) {
                                handleVoiceCommand(recognizedText);
                            } else {
                                tvStatus.setText("블루투스 연결 실패. 명령을 전송할 수 없습니다.");
                            }
                        }, 2000); // 2초 후에 다시 시도
                    } else {
                        // 음성 명령 처리
                        handleVoiceCommand(recognizedText);
                    }
                } else {
                    tvStatus.setText("음성 인식 결과가 없습니다.");
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    // handleVoiceCommand 메서드 수정
    private void handleVoiceCommand(String command) {
        String lowerCommand = command.toLowerCase();
        Log.d(TAG, "음성 명령 처리: " + lowerCommand);

        // 프로토콜에 맞게 LED 제어 명령 수정
        if (lowerCommand.contains("전원 켜") ||
                lowerCommand.contains("켜") ||
                lowerCommand.toLowerCase().contains("on") ||
                lowerCommand.toLowerCase().contains("turn on")) {
            // 아두이노로 "1" 전송 (LED 켜기)
            sendDataToArduino("1");
            tvStatus.setText("LED 켜짐");
            return;
        } else if (lowerCommand.contains("전원 꺼") ||
                lowerCommand.contains("꺼") ||
                lowerCommand.toLowerCase().contains("off") ||
                lowerCommand.toLowerCase().contains("turn off")) {
            // 아두이노로 "0" 전송 (LED 끄기)
            sendDataToArduino("0");
            tvStatus.setText("LED 꺼짐");
            return;
        } else if (lowerCommand.contains("자동") || lowerCommand.contains("자동 모드")) {
            sendDataToArduino("2");
            tvStatus.setText("자동 모드");
            return;
        } else if (lowerCommand.contains("어깨") || lowerCommand.contains("어깨 모드")) {
            sendDataToArduino("3");
            tvStatus.setText("어깨 모드");
            return;
        } else if (lowerCommand.contains("등") || lowerCommand.contains("등 모드")) {
            sendDataToArduino("4");
            tvStatus.setText("등 모드");
            return;
        } else if (lowerCommand.contains("허리") || lowerCommand.contains("허리 모드")) {
            sendDataToArduino("5");
            tvStatus.setText("허리 모드");
            return;
        } else if (lowerCommand.contains("온도") || lowerCommand.contains("온도 모드")) {
            sendDataToArduino("6");
            tvStatus.setText("온도 모드");
            return;
        }
    }

    private String extractTemperature(String command) {
        // 숫자 추출 정규식 패턴
        Pattern pattern = Pattern.compile("(\\d+)\\s*도");
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            return matcher.group(1); // 첫 번째 캡처 그룹 (숫자 부분) 반환
        }
        return null;
    }

    private void initializeViews() {
        btnVoiceRecognition = findViewById(R.id.btn_voice_recognition);
        btnBluetoothConnect = findViewById(R.id.btnConnect);
        tvStatus = findViewById(R.id.tv_status);

        // 버튼 초기화 시 예외 처리 추가
        try {
            btnAutoMode = findViewById(R.id.btn_auto_mode);
            if (btnAutoMode == null) {
                Log.e(TAG, "btn_auto_mode를 찾을 수 없습니다!");
                Toast.makeText(this, "자동 모드 버튼을 찾을 수 없습니다. 레이아웃을 확인하세요.", Toast.LENGTH_SHORT).show();
            }

            btnManualMode = findViewById(R.id.btn_manual_mode);
            if (btnManualMode == null) {
                Log.e(TAG, "btn_manual_mode를 찾을 수 없습니다!");
            }

            btnScanMode = findViewById(R.id.btn_scan_mode);
            if (btnScanMode == null) {
                Log.e(TAG, "btn_scan_mode를 찾을 수 없습니다!");
            }

            // 온도 설정 버튼 초기화
            btnTemperatureMode = findViewById(R.id.btn_temperature_mode);
            if (btnTemperatureMode == null) {
                Log.e(TAG, "btn_temperature_mode를 찾을 수 없습니다!");
            }
        } catch (Exception e) {
            Log.e(TAG, "버튼 초기화 중 오류 발생: " + e.getMessage(), e);
            Toast.makeText(this, "UI 초기화 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonListeners() {
        btnBluetoothConnect.setOnClickListener(v -> {
            Log.d(TAG, "블루투스 연결 버튼 클릭됨");
            Toast.makeText(MainActivity.this, "연결 버튼 클릭됨", Toast.LENGTH_SHORT).show();
            checkPermissionsAndConnect();
        });

        btnVoiceRecognition.setOnClickListener(v -> {
            // 음성 인식 전에 블루투스 연결 확인
            if (!bluetoothManager.isConnected()) {
                tvStatus.setText("블루투스 연결이 끊겼습니다. 재연결 중...");
                connectToArduino();

                // 연결 상태 확인 후 음성 인식 시작
                mainHandler.postDelayed(() -> {
                    if (bluetoothManager.isConnected()) {
                        startVoiceRecognition();
                    } else {
                        tvStatus.setText("블루투스 연결 실패. 다시 연결 버튼을 눌러주세요.");
                    }
                }, 2000);
            } else {
                // 블루투스가 연결되어 있으면 음성 인식 시작
                startVoiceRecognition();
            }
        });

        // 모드 버튼 리스너 설정 - 화면 전환 로직 사용 및 디버깅 추가
        if (btnAutoMode != null) {
            btnAutoMode.setOnClickListener(v -> {
                Log.d(TAG, "자동 모드 버튼 클릭됨");
                try {
                    // 명령어 전송 후 화면 전환
                    if (bluetoothManager.isConnected()) {
                        sendDataToArduino("2");
                    }

                    // 자동 모드 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, AutoModeActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "자동 모드 화면 전환 시도 완료");
                } catch (Exception e) {
                    Log.e(TAG, "자동 모드 화면 전환 실패: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnManualMode != null) {
            btnManualMode.setOnClickListener(v -> {
                Log.d(TAG, "수동 모드 버튼 클릭됨");
                try {
                    // 수동 모드 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, ManualModeActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "수동 모드 화면 전환 시도 완료");
                } catch (Exception e) {
                    Log.e(TAG, "수동 모드 화면 전환 실패: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnScanMode != null) {
            btnScanMode.setOnClickListener(v -> {
                Log.d(TAG, "스캔 모드 버튼 클릭됨");
                try {
                    // 스캔 모드 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "스캔 모드 화면 전환 시도 완료");
                } catch (Exception e) {
                    Log.e(TAG, "스캔 모드 화면 전환 실패: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 온도 설정 버튼이 있다면 리스너 설정
        if (btnTemperatureMode != null) {
            btnTemperatureMode.setOnClickListener(v -> {
                Log.d(TAG, "온도 설정 버튼 클릭됨");
                try {
                    // 온도 모드 명령 전송
                    if (bluetoothManager.isConnected()) {
                        sendDataToArduino("6");
                    }

                    // 온도 설정 화면으로 이동
                    Intent intent = new Intent(MainActivity.this, TemperatureActivity.class);
                    startActivity(intent);
                    Log.d(TAG, "온도 설정 화면 전환 시도 완료");
                } catch (Exception e) {
                    Log.e(TAG, "온도 설정 화면 전환 실패: " + e.getMessage(), e);
                    Toast.makeText(MainActivity.this, "오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkPermissionsAndConnect() {
        String[] requiredPermissions = new String[] {
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, requiredPermissions, 1);
        } else {
            connectToArduino();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                connectToArduino();
            } else {
                Toast.makeText(this, "블루투스 기능을 사용하려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectToArduino() {
        Log.d(TAG, "아두이노 연결 시도...");
        bluetoothManager.connectToDevice(DEVICE_NAME);
    }

    // BluetoothConnectionManager.BluetoothCallback 구현
    @Override
    public void onConnectionStatus(boolean isConnected, String message) {
        if (isConnected) {
            tvStatus.setText("연결됨: " + message);
            Log.d(TAG, "블루투스 연결 성공: " + message);

            // 연결 확인을 위한 테스트 메시지 전송
            mainHandler.postDelayed(this::sendTestMessage, 1000);
        } else {
            tvStatus.setText("연결 상태: " + message);
            Log.e(TAG, "블루투스 연결 실패 또는 연결 해제: " + message);
        }
    }

    @Override
    public void onDataReceived(String message) {
        Log.d(TAG, "블루투스 데이터 수신: " + message);
        tvStatus.append("\n수신: " + message);
    }

    // ModeManager.ModeCallback 구현
    @Override
    public void onModeChanged(String mode) {
        tvStatus.setText("현재 모드: " + mode);
    }

    private void startVoiceRecognition() {
        // 음성 인식 권한 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 2);
            return;
        }

        // 로그 추가
        Log.d(TAG, "음성 인식 시작");

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR"); // 한국어로 설정
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "음성을 입력하세요.");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE); // 여기를 SPEECH_REQUEST_CODE로 변경
            Log.d(TAG, "음성 인식 인텐트 시작됨");
        } catch (ActivityNotFoundException a) {
            Log.e(TAG, "음성 인식 기능이 지원되지 않음", a);
            Toast.makeText(MainActivity.this, "음성 인식 기능을 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0).trim();
                Log.d(TAG, "인식된 음성: " + spokenText);

                // 음성 인식 전에 블루투스 연결 확인
                if (!bluetoothManager.isConnected()) {
                    tvStatus.setText("블루투스 연결이 끊겼습니다. 재연결 중...");
                    connectToArduino();

                    // 연결 확인 후 명령 처리
                    mainHandler.postDelayed(() -> {
                        if (bluetoothManager.isConnected()) {
                            processVoiceResult(spokenText);
                        } else {
                            tvStatus.setText("블루투스 연결 실패. 명령을 전송할 수 없습니다.");
                        }
                    }, 2000);
                } else {
                    processVoiceResult(spokenText);
                }
            }
        }
    }

    // 음성 인식 결과 처리 메서드 추가
    private void processVoiceResult(String spokenText) {
        // 프로토콜에 맞게 명령 수정
        String lowerText = spokenText.toLowerCase();
        Log.d(TAG, "음성 처리: " + lowerText);

        // 전원 켜기/끄기 명령 처리
        if (lowerText.contains("전원 켜") ||
                lowerText.contains("켜") ||
                lowerText.contains("on") ||
                lowerText.contains("turn on")) {
            sendDataToArduino("1");
            tvStatus.setText("LED 켜짐");
            return;
        } else if (lowerText.contains("전원 꺼") ||
                lowerText.contains("꺼") ||
                lowerText.contains("off") ||
                lowerText.contains("turn off")) {
            sendDataToArduino("0");
            tvStatus.setText("LED 꺼짐");
            return;
        }

        // 온도 설정 명령 처리
        if (lowerText.contains("도")) {
            String tempStr = extractTemperature(lowerText);
            if (tempStr != null) {
                int temperature = Integer.parseInt(tempStr);
                if (temperature >= 30 && temperature <= 60) {
                    Log.d(TAG, "온도 설정 인식됨: " + temperature + "도");
                    // 온도 모드 설정 후 온도 전송
                    sendDataToArduino("6");
                    // 온도값 전송을 위한 추가 코드가 필요할 수 있음
                    Intent intent = new Intent(MainActivity.this, TemperatureActivity.class);
                    intent.putExtra("TEMPERATURE", temperature);
                    startActivity(intent);
                    return;
                } else {
                    Toast.makeText(this, "유효한 온도 범위는 30~60도입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // 모드 명령 처리 및 화면 전환
        Intent intent = null;

        if (lowerText.contains("어깨") || lowerText.contains("어깨 부위 지압해줘") || lowerText.contains("어깨 모드")) {
            sendDataToArduino("4");
            intent = new Intent(MainActivity.this, ManualModeActivity.class);
            intent.putExtra("SELECTED_MODE", "SHOULDER");
        } else if (lowerText.contains("등") || lowerText.contains("등 부위 지압해줘") || lowerText.contains("등 모드")) {
            sendDataToArduino("5");
            intent = new Intent(MainActivity.this, ManualModeActivity.class);
            intent.putExtra("SELECTED_MODE", "BACK");
        } else if (lowerText.contains("허리") || lowerText.contains("허리 부위 지압해줘") || lowerText.contains("허리 모드")) {
            sendDataToArduino("6");
            intent = new Intent(MainActivity.this, ManualModeActivity.class);
            intent.putExtra("SELECTED_MODE", "WAIST");
        } else if (lowerText.contains("자동") || lowerText.contains("자동 모드")) {
            sendDataToArduino("2");
            intent = new Intent(MainActivity.this, AutoModeActivity.class);
        } else if (lowerText.contains("스캔") || lowerText.contains("스캔 모드")) {
            // 스캔 모드는 프로토콜에 정의되지 않았지만 기존 기능 유지
            intent = new Intent(MainActivity.this, ScanActivity.class);
        } else if (lowerText.contains("수동") || lowerText.contains("수동 모드")) {
            sendDataToArduino("3");
            intent = new Intent(MainActivity.this, ManualModeActivity.class);
        } else if (lowerText.contains("온도") || lowerText.contains("온도 설정")) {
            sendDataToArduino("7");
            intent = new Intent(MainActivity.this, TemperatureActivity.class);
        } else {
            Toast.makeText(this, "지원하지 않는 명령입니다: " + spokenText, Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            try {
                startActivity(intent);
                Log.d(TAG, "화면 전환 시도 완료: " + intent.getComponent().getClassName());
            } catch (Exception e) {
                Log.e(TAG, "화면 전환 실패: " + e.getMessage(), e);
                Toast.makeText(this, "화면 전환 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 아두이노로 데이터를 전송하는 메서드
    public void sendDataToArduino(String text) {
        if (bluetoothManager != null && bluetoothManager.isConnected()) {
            try {
                // 디버깅용 로그 추가
                Log.d(TAG, "블루투스 전송 시도: '" + text + "'");

                // 명령어 단순화 - 공백 제거
                String command = text.trim();

                // 명령어 직접 전송
                bluetoothManager.sendData(command);

                // 전송 성공 로그 추가
                Log.d(TAG, "블루투스 전송 완료: '" + command + "'");

                Toast.makeText(MainActivity.this, "전송: " + command, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "데이터 전송 오류: " + e.getMessage(), e);
                Toast.makeText(MainActivity.this, "데이터 전송 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "블루투스 연결 상태 오류: 연결되지 않음");
            Toast.makeText(MainActivity.this, "아두이노와 연결되지 않았습니다.", Toast.LENGTH_SHORT).show();

            // 재연결 시도
            connectToArduino();
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    // 디버깅을 위한 로그 기록 메소드
    private void logDebug(String message) {
        Log.d(TAG, message);
    }
}