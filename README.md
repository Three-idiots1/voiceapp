# [말하는 대로] 대구가톨릭대학교 캡스톤디자인 과제

## 🛏️ 말하는 대로 - 사용자 맞춤 스마트 지압 침대 시스템
'말하는 대로'는 노약자와 일반 사용자를 위한 **맞춤형 지압 및 온도 조절 기능**을 제공하는 스마트 침대 시스템입니다.  
AI 기반 음성인식 기능과 사용자 데이터를 활용한 건강관리 서비스를 통해  
**편리하고 건강한 수면 환경**을 목표로 합니다.

<div align=center>

| 기능 | 설명 |
|------|------|
| 🗣️ 음성 제어 | STT/TTS 기술로 리모컨 없이 간편한 제어 |
| 🔥 지압/온도 조절 | 신체 부위별 맞춤형 지압 및 온도 조절 |
| 📊 BMI 기반 개인화 | 사용자 DB를 바탕으로 맞춤 기능 제공 |
| 📡 블루투스 연동 | 컨트롤러와 침대 간 무선 통신 구현 |

</div>

### 🎬 말하는 대로 설명 영상
[![아이편해 설명 영상](http://img.youtube.com/vi/K0pShYf_NuY/0.jpg)](https://www.youtube.com/watch?v=K0pShYf_NuY)


## 🗒️목차
- [프로젝트 소개](#프로젝트-소개)
- [😥 아쉬웠던 부분](#😥-아쉬웠던-부분)

## 💻 프로젝트 소개

**프로젝트 이름**: 말하는 대로  
**프로젝트 주제**: 음성인식을 통한 스마트 침대 제어  
**개발 기간**: 2024.09.02 ~ 2025.05.10  
**개발 인원**: 4명 ([오경석](#), [박경운](https://github.com/gyngxn), [이범수](#), [김유찬](#))  
**캡스톤 디자인 전시회**: 2024.12.03  |  2025.06.03


> **기존의 문제점**
&nbsp; 기존 리모컨 방식은 버튼이 복잡하고 직관적이지 않아, 노약자나 기기에 익숙하지 않은 사용자가 조작하기 어렵습니다. 또한 등, 어깨, 허리 등 각 부위에 맞춘 지압 강도나 온도 조절 기능이 없어, 사용자에게 맞는 피로 회복이나 건강 관리가 어려운 점이 문제였습니다.
> 

> **해결 방법**
&nbsp; 어깨, 등, 허리 등 부위별 지압 강도와 온도 조절 기능을 갖춘 스마트 침대를 구현합니다. 사용자의 신체 정보와 BMI 데이터를 기반으로 개인 맞춤형 설정을 제공하며, 블루투스 통신과 음성인식을 통해 침대 기능을 간편하게 제어할 수 있도록 설계했습니다.
또한, 사용자 편의성을 고려한 직관적인 UI를 통해 누구나 쉽게 사용할 수 있도록 하였습니다.
> 

> **기대 효과**
&nbsp; 음성 명령만으로 침대의 다양한 기능을 제어할 수 있어 리모컨 없이도 손쉽게 사용할 수 있습니다.
특히 노약자나 임산부처럼 조작이 불편한 사용자도 부담 없이 접근할 수 있도록 설계되었습니다.
음성 인식 기반의 제어 시스템은 직관적인 UI와 결합되어 누구나 쉽게 사용할 수 있는 환경을 제공합니다.
복잡한 조작 없이 간단한 명령만으로 맞춤형 지압과 온도 조절이 가능하며,
이로 인해 연령이나 기술 숙련도에 관계없이 폭넓은 사용자층을 만족시킬 수 있습니다.
> 

## ⚙️기능 설명

### 📚 사용 기술 스택

<div align=center> 
  <img src="https://img.shields.io/badge/androidstudio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white">
  <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> 
  <img src="https://img.shields.io/badge/postgresql-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img src="https://img.shields.io/badge/postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white">
  <br>
  
  <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/restapi-000000?style=for-the-badge&logo=api&logoColor=white">
</div>

### 🧩 메인 제공 기능

<div align="center">

| 메인 화면 | 자동 모드 | 수동 모드 | 온도 모드 | BMI |
|:--:|:--:|:--:|:--:|:--:|
| <img src="https://github.com/Three-idiots1/voiceapp/blob/master/images/%EB%A9%94%EC%9D%B8%20%ED%99%94%EB%A9%B4.png?raw=true" width="170"/> | <img src="https://github.com/Three-idiots1/voiceapp/blob/master/images/%EC%9E%90%EB%8F%99%20%EB%AA%A8%EB%93%9C.png?raw=true" width="170"/> | <img src="https://github.com/Three-idiots1/voiceapp/blob/master/images/%EC%88%98%EB%8F%99%20%EB%AA%A8%EB%93%9C.png?raw=true" width="170"/> | <img src="https://github.com/Three-idiots1/voiceapp/blob/master/images/%EC%98%A8%EB%8F%84%20%EB%AA%A8%EB%93%9C.png?raw=true" width="170"/> | <img src="https://github.com/Three-idiots1/voiceapp/blob/master/images/BMI.png?raw=true" width="170"/> |
| 간단한 상태 확인 | 자동 시작/종료 시간 설정 | 사용자가 직접 수동 조작 | 온도 조절 및 확인 | BMI 기반 기능 제공 |

</div>



## 🔧 아쉬웠던 부분
- 음성인식 기능의 정확도가 낮아, 소음 환경에서는 명령 인식률이 떨어짐
- 블루투스 통신이 간헐적으로 끊기거나 지연되어, 실시간 제어에 어려움
- 제한된 개발 기간으로 인해 체형 인식이나 자세 분석 같은 고급 기능은 구현 미흡
- 지압 모듈을 실제 침대에 적용하기 위한 하드웨어 제작에 비용과 자재 한계
- 다양한 사용자층을 대상으로 한 충분한 사용성 테스트를 진행하지 못한 점

