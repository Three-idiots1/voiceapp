package com.example.voiceapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnectionManager {
    private static final String TAG = "BluetoothConnManager";
    // 표준 SPP UUID (직렬 포트 프로필)
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected = false;
    private Thread connectionThread;
    private Thread dataListenerThread;
    private boolean isListening = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BluetoothCallback callback;

    public interface BluetoothCallback {
        void onConnectionStatus(boolean isConnected, String message);
        void onDataReceived(String message);
    }

    @SuppressLint("NewApi")
    public BluetoothConnectionManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setCallback(BluetoothCallback callback) {
        this.callback = callback;
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    @SuppressLint("NewApi")
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return isConnected;
    }

    @SuppressLint("NewApi")
    public void connectToDevice(String deviceName) {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "블루투스가 비활성화되어 있습니다");
            notifyConnectionStatus(false, "블루투스가 비활성화되어 있습니다");
            return;
        }

        // 이미 실행 중인 연결 스레드가 있다면 중단
        if (connectionThread != null && connectionThread.isAlive()) {
            connectionThread.interrupt();
        }

        connectionThread = new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "블루투스 연결 권한이 필요합니다");
                    notifyConnectionStatus(false, "블루투스 연결 권한이 필요합니다");
                    return;
                }

                // 현재 연결 상태라면 먼저 연결 해제
                if (isConnected) {
                    Log.d(TAG, "기존 연결 해제 시도");
                    disconnect();
                    // 연결 해제 후 잠시 대기
                    Thread.sleep(1000);
                }

                // 페어링된 모든 기기 확인 및 로깅
                @SuppressLint({"NewApi", "LocalSuppress"})
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                bluetoothDevice = null;

                Log.d(TAG, "페어링된 기기 목록 확인:");
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        Log.d(TAG, "페어링된 기기: " + device.getName() + " [" + device.getAddress() + "]");
                        if (device.getName() != null && device.getName().equals(deviceName)) {
                            bluetoothDevice = device;
                            Log.d(TAG, "대상 기기 발견: " + device.getName());
                        }
                    }
                } else {
                    Log.e(TAG, "페어링된 기기가 없습니다");
                    notifyConnectionStatus(false, "페어링된 기기가 없습니다. 먼저 블루투스 설정에서 디바이스를 페어링하세요.");
                    return;
                }

                if (bluetoothDevice == null) {
                    Log.e(TAG, "지정된 기기(" + deviceName + ")를 찾을 수 없습니다");
                    notifyConnectionStatus(false, "지정된 기기(" + deviceName + ")를 찾을 수 없습니다. 기기 이름이 정확한지 확인하세요.");
                    return;
                }

                // 블루투스 검색 중지 (검색 중이면 연결 속도가 느려짐)
                if (bluetoothAdapter.isDiscovering()) {
                    Log.d(TAG, "블루투스 검색 중지");
                    bluetoothAdapter.cancelDiscovery();
                }

                // 기존 소켓이 있다면 닫기
                if (bluetoothSocket != null) {
                    try {
                        Log.d(TAG, "기존 소켓 닫기");
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "소켓 닫기 실패", e);
                    }
                    bluetoothSocket = null;
                }

                Log.d(TAG, "소켓 생성 시도 - 기기: " + bluetoothDevice.getName());
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_MODULE_UUID);

                // 연결 시도 전 짧은 지연
                Thread.sleep(500);

                Log.d(TAG, "소켓 연결 시도");
                notifyConnectionStatus(false, "연결 시도 중: " + bluetoothDevice.getName());

                // 연결 시도 (타임아웃 설정을 위해 별도 스레드에서 실행)
                final boolean[] connectionSuccess = {false};
                Thread connectThread = new Thread(() -> {
                    try {
                        bluetoothSocket.connect();
                        connectionSuccess[0] = true;
                    } catch (IOException e) {
                        Log.e(TAG, "소켓 연결 실패", e);
                    }
                });

                connectThread.start();

                // 최대 10초 대기
                connectThread.join(10000);

                if (!connectionSuccess[0]) {
                    if (connectThread.isAlive()) {
                        connectThread.interrupt();
                    }
                    throw new IOException("연결 시간 초과");
                }

                Log.d(TAG, "소켓 연결 성공");

                // 입출력 스트림 설정
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();
                isConnected = true;

                Log.d(TAG, "연결 완료: " + bluetoothDevice.getName());
                notifyConnectionStatus(true, bluetoothDevice.getName());

                // 데이터 수신 리스너 시작
                listenForData();

                // 연결 상태 테스트 위해 간단한 데이터 전송
                sendData("CONNECT_TEST");

            } catch (IOException e) {
                Log.e(TAG, "Bluetooth 연결 실패: ", e);
                notifyConnectionStatus(false, "연결 실패: " + e.getMessage());
                closeConnection();
            } catch (InterruptedException e) {
                Log.e(TAG, "연결 스레드 중단됨", e);
                notifyConnectionStatus(false, "연결 스레드 중단됨");
                closeConnection();
            }
        });

        connectionThread.start();
    }

    private void notifyConnectionStatus(boolean isConnected, String message) {
        if (callback != null) {
            handler.post(() -> callback.onConnectionStatus(isConnected, message));
        }
    }

    @SuppressLint("NewApi")
    private void closeConnection() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "소켓 닫기 실패", e);
            }
            bluetoothSocket = null;
            outputStream = null;
            inputStream = null;
            isConnected = false;
        }
    }

    private void listenForData() {
        // 이미 리스너 스레드가 실행 중이라면 중단
        if (dataListenerThread != null && dataListenerThread.isAlive()) {
            isListening = false;
            dataListenerThread.interrupt();
            try {
                dataListenerThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "리스너 스레드 중단 대기 실패", e);
            }
        }

        isListening = true;
        dataListenerThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;

            Log.d(TAG, "데이터 리스너 시작");

            while (isListening && isConnected) {
                try {
                    if (inputStream != null && inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            final String message = new String(buffer, 0, bytes);
                            Log.d(TAG, "데이터 수신: " + message);
                            if (callback != null) {
                                handler.post(() -> callback.onDataReceived(message));
                            }
                        }
                    }
                    // 짧은 지연으로 CPU 사용률 감소
                    Thread.sleep(50);
                } catch (IOException e) {
                    Log.e(TAG, "데이터 수신 오류", e);
                    if (isConnected) {
                        notifyConnectionStatus(false, "데이터 수신 오류: " + e.getMessage());
                        isConnected = false;
                    }
                    break;
                } catch (InterruptedException e) {
                    Log.d(TAG, "데이터 리스너 스레드 중단됨");
                    break;
                }
            }
            Log.d(TAG, "데이터 리스너 종료");
        });

        dataListenerThread.start();
    }

    public void sendData(String message) {
        if (isConnected && outputStream != null) {
            try {
                outputStream.write((message + "\n").getBytes()); // 종료 문자 추가
                Log.d(TAG, "LED 제어 명령 전송: " + message);
            } catch (IOException e) {
                Log.e(TAG, "데이터 전송 실패", e);
            }
        }
    }

    @SuppressLint("NewApi")
    public void disconnect() {
        Log.d(TAG, "블루투스 연결 해제 시도");

        // 데이터 리스너 중지
        isListening = false;
        if (dataListenerThread != null && dataListenerThread.isAlive()) {
            dataListenerThread.interrupt();
        }

        // 소켓 및 스트림 정리
        if (bluetoothSocket != null) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                bluetoothSocket.close();
                Log.d(TAG, "블루투스 소켓 닫힘");
            } catch (IOException e) {
                Log.e(TAG, "소켓 닫기 실패", e);
            } finally {
                bluetoothSocket = null;
                inputStream = null;
                outputStream = null;
                isConnected = false;
                notifyConnectionStatus(false, "연결 해제됨");
            }
        } else {
            Log.d(TAG, "이미 연결 해제된 상태");
        }
    }

    // 기기 재연결 메소드
    public void reconnect() {
        if (bluetoothDevice != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    // 현재 연결된 기기 이름을 final 변수에 저장
                    @SuppressLint({"NewApi", "LocalSuppress"}) final String finalDeviceName = bluetoothDevice.getName();
                    if (finalDeviceName != null) {
                        Log.d(TAG, "기기 재연결 시도: " + finalDeviceName);
                        disconnect();
                        // 1초 지연 후 재연결 (final 변수 사용)
                        handler.postDelayed(() -> connectToDevice(finalDeviceName), 1000);
                    } else {
                        Log.e(TAG, "재연결할 기기 이름을 가져올 수 없습니다");
                        Toast.makeText(context, "재연결할 기기 이름을 가져올 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "블루투스 연결 권한이 없습니다");
                    Toast.makeText(context, "블루투스 연결 권한이 필요합니다", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "기기 이름 가져오기 실패", e);
                Toast.makeText(context, "기기 정보 가져오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "재연결할 기기 정보가 없습니다");
            Toast.makeText(context, "재연결할 기기 정보가 없습니다", Toast.LENGTH_SHORT).show();
        }
    }
}
