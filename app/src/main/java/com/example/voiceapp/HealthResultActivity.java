package com.example.voiceapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HealthResultActivity extends AppCompatActivity {
    private static final String TAG = "HealthResultActivity";

    // UI 요소
    private TextView textViewBmi;
    private TextView textViewBmiCategory;
    private TextView textViewIdealWeight;
    private TextView textViewDietAdvice;
    private TextView textViewExerciseAdvice;
    private TextView textViewLifestyleAdvice;
    private Button buttonRefresh;

    // 사용자 데이터
    private String userName;
    private int userAge;
    private int userHeight;
    private double userWeight;
    private String userGender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_result);

        // UI 요소 초기화
        initializeViews();

        // Intent에서 데이터 가져오기
        getDataFromIntent();

        // 데이터 표시 및 계산
        if (isUserDataComplete()) {
            // 사용자 데이터가 있으면 건강 지표 계산 및 표시
            calculateAndDisplayHealthData();
        } else {
            // 데이터가 없으면 오류 메시지 표시
            Toast.makeText(this, "사용자 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 새로 계산하기 버튼 클릭 이벤트
        buttonRefresh.setOnClickListener(v -> {
            if (isUserDataComplete()) {
                // 데이터가 있으면 새로 계산
                calculateAndDisplayHealthData();
                Toast.makeText(this, "건강 지표가 갱신되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // UI 요소 초기화
    private void initializeViews() {
        textViewBmi = findViewById(R.id.textViewBmi);
        textViewBmiCategory = findViewById(R.id.textViewBmiCategory);
        textViewIdealWeight = findViewById(R.id.textViewIdealWeight);
        textViewDietAdvice = findViewById(R.id.textViewDietAdvice);
        textViewExerciseAdvice = findViewById(R.id.textViewExerciseAdvice);
        textViewLifestyleAdvice = findViewById(R.id.textViewLifestyleAdvice);
        buttonRefresh = findViewById(R.id.buttonRefresh);
    }

    // Intent에서 데이터 가져오기
    private void getDataFromIntent() {
        Intent intent = getIntent();
        userName = intent.getStringExtra("userName");
        userAge = intent.getIntExtra("userAge", 0);
        userHeight = intent.getIntExtra("userHeight", 0);
        userWeight = intent.getDoubleExtra("userWeight", 0.0);
        userGender = intent.getStringExtra("userGender");

        Log.d(TAG, "받은 데이터: name=" + userName +
                ", age=" + userAge + ", height=" + userHeight +
                ", weight=" + userWeight + ", gender=" + userGender);
    }

    // 사용자 데이터가 완전한지 확인
    private boolean isUserDataComplete() {
        return userName != null && userAge > 0 &&
                userHeight > 0 && userWeight > 0 &&
                userGender != null;
    }

    // 건강 지표 계산 및 UI 표시
    private void calculateAndDisplayHealthData() {
        // BMI 계산
        double heightInMeter = userHeight / 100.0;
        double bmi = userWeight / (heightInMeter * heightInMeter);

        // BMI 카테고리 결정
        String bmiCategory;
        if (bmi < 18.5) {
            bmiCategory = "저체중";
        } else if (bmi < 23) {
            bmiCategory = "정상";
        } else if (bmi < 25) {
            bmiCategory = "과체중";
        } else {
            bmiCategory = "비만";
        }

        // 적정 체중 범위 계산
        double idealWeightLower = 18.5 * heightInMeter * heightInMeter;
        double idealWeightUpper = 23 * heightInMeter * heightInMeter;

        // UI에 기본 정보 업데이트
        textViewBmi.setText(String.format("BMI: %.1f", bmi));
        textViewBmiCategory.setText("분류: " + bmiCategory);
        textViewIdealWeight.setText(String.format("적정 체중 범위: %.1fkg ~ %.1fkg", idealWeightLower, idealWeightUpper));

        // BMI 카테고리에 따른 건강 조언 설정
        setHealthAdvice(bmiCategory);
    }

    // BMI 카테고리별 건강 조언 설정
    private void setHealthAdvice(String bmiCategory) {
        String dietAdvice;
        String exerciseAdvice;
        String lifestyleAdvice;

        switch (bmiCategory) {
            case "저체중":
                dietAdvice = "단백질과 탄수화물 섭취를 늘리세요. 하루에 5-6회 소량씩 자주 먹는 것이 좋습니다. 견과류, 아보카도, 치즈 등 건강한 지방이 포함된 음식을 섭취하세요.";
                exerciseAdvice = "근력 운동에 집중하세요. 체중 증가를 위해 유산소 운동보다 웨이트 트레이닝이 효과적입니다. 주 3-4회, 30분 이상의 근력 운동을 권장합니다.";
                lifestyleAdvice = "충분한 수면을 취하고 스트레스를 관리하세요. 수면 부족과 과도한 스트레스는 체중 증가를 방해할 수 있습니다. 하루 7-8시간의 수면을 목표로 하세요.";
                break;

            case "정상":
                dietAdvice = "균형 잡힌 식단을 유지하세요. 다양한 과일, 채소, 통곡물, 저지방 단백질을 섭취하고, 가공식품과 설탕 섭취를 제한하세요.";
                exerciseAdvice = "현재의 활동 수준을 유지하세요. 주 5회, 30분 이상의 중강도 유산소 운동과 주 2-3회의 근력 운동을 병행하는 것이 이상적입니다.";
                lifestyleAdvice = "건강한 생활 습관을 계속 유지하세요. 충분한 수면, 스트레스 관리, 정기적인 건강 검진을 통해 현재의 건강 상태를 지속하세요.";
                break;

            case "과체중":
                dietAdvice = "칼로리 섭취를 줄이고 식이 섬유가 풍부한 음식을 선택하세요. 과일, 채소, 통곡물을 늘리고, 가공식품, 설탕, 포화지방을 줄이세요.";
                exerciseAdvice = "유산소 운동을 늘리세요. 주 5회, 최소 30분의 걷기, 조깅, 수영, 자전거 타기와 같은 운동을 권장합니다. 근력 운동도 주 2회 병행하세요.";
                lifestyleAdvice = "식사 습관을 개선하세요. 천천히 먹고, 배고픔과 포만감을 인식하며, 감정적 섭취를 피하세요. 물을 충분히 마시고 규칙적인 식사 시간을 지키세요.";
                break;

            case "비만":
                dietAdvice = "건강한 식단 계획을 세우세요. 전문가의 도움을 받아 개인화된 식단을 구성하고, 칼로리와 포화지방을 줄이며, 단백질과 식이 섬유를 늘리세요.";
                exerciseAdvice = "점진적으로 운동 강도를 높이세요. 처음에는 하루 10-15분의 걷기부터 시작해서 점차 시간과 강도를 늘리세요. 근력 운동도 주 2-3회 추가하세요.";
                lifestyleAdvice = "작은 생활 습관 변화부터 시작하세요. 엘리베이터 대신 계단 이용, 짧은 거리는 걷기, 장시간 앉아있지 않기 등의 일상적인 활동을 늘리세요.";
                break;

            default:
                dietAdvice = "균형 잡힌 식단을 유지하세요. 다양한 과일, 채소, 통곡물, 저지방 단백질을 섭취하세요.";
                exerciseAdvice = "규칙적인 운동을 유지하세요. 주 3-5회, 30분 이상의 운동을 권장합니다.";
                lifestyleAdvice = "건강한 생활 습관을 유지하세요. 충분한 수면, 스트레스 관리가 중요합니다.";
                break;
        }

        // 건강 조언 UI 업데이트
        textViewDietAdvice.setText(dietAdvice);
        textViewExerciseAdvice.setText(exerciseAdvice);
        textViewLifestyleAdvice.setText(lifestyleAdvice);
    }
}