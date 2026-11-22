# Paper Calendar — Lenovo Y700용 TRMNL 감성 캘린더

## 무엇이 포함되나요?

- `app/src/main/assets/index.html`: 실제 화면을 그리는 HTML/CSS/JS. 웹 브라우저에서 열면 **데모 일정**으로 미리 볼 수 있습니다.
- `app/src/main/java/com/example/papercalendar/MainActivity.java`: Y700 Android 캘린더의 일정에 연결하는 네이티브 코드.
- `.github/workflows/build-apk.yml`: GitHub Actions에서 디버그 APK를 만드는 워크플로.
- `AndroidManifest.xml`: `READ_CALENDAR` 읽기 권한만 요청합니다. 일정 수정 권한이나 인터넷 권한은 요청하지 않습니다.

## 주요 기능

- TRMNL에서 영감을 얻은 무채색·여백 중심 달력
- 월간 / 주간 / 월간 일정 목록
- 이전 달·다음 달 / 이전 주·다음 주 / 오늘 버튼
- 날짜 터치 → 해당 날짜의 일정 목록
- 기기에 동기화된 구글 캘린더 및 다른 로컬 캘린더 읽기
- 일정 제목의 한국어 표시
- 캘린더별 표시/숨김
- 앱 사용 중 60초 간격으로 화면 데이터 재확인, 앱 복귀 시 새로고침

## 구글 캘린더 연결 방식

이 앱은 Google OAuth를 직접 구현하지 않습니다. 대신 Android 공식 Calendar Provider를 통해 **Y700에 이미 동기화된 일정**을 읽습니다. 기기에 Google Calendar 앱과 Google 계정의 캘린더 동기화가 설치·활성화되어 있어야 합니다.

Y700이 중국 내수롬이거나 Google Services / 캘린더 저장소가 없으면 직접 연결되지 않을 수 있습니다. Google Calendar 앱에서 실제 일정이 표시되는지 먼저 확인해 주세요.

## 안드로이드 APK 빌드

### 방법 1: Android Studio (권장)

1. Android Studio를 설치합니다. JDK 17 사용.
2. `TRMNL_Y700_Calendar` 폴더를 **프로젝트로 열기** 합니다.
3. Android Gradle Plugin 8.7.3, Gradle 8.9, Android SDK Platform 35가 설치되도록 안내를 따릅니다.
4. **Build → Build App Bundle(s) / APK(s) → Build APK(s)** 실행.
5. `app/build/outputs/apk/debug/app-debug.apk`를 Y700으로 복사하고 설치합니다.
6. 처음 실행 시 캘린더 접근 권한을 허용합니다.

이 소스 ZIP에 Gradle Wrapper 실행파일/라이브러리는 포함되지 않습니다. Android Studio가 프로젝트 동기화를 위해 Gradle 8.9를 사용하도록 설정하세요. 또는 아래 GitHub Actions 빌드를 이용하세요.

### 방법 2: GitHub Actions (PC에서 빌드 도구 설치 불필요)

1. GitHub 저장소를 만들고 이 ZIP 내용물(최상위 `settings.gradle` 포함)을 저장소에 올립니다.
2. **Actions → Build Android APK → Run workflow**를 실행하거나 `main`에 push합니다.
3. 작업이 완료되면 **Artifacts → paper-calendar-y700-debug-apk**를 다운로드합니다.
4. `app-debug.apk`를 Y700에 복사해 설치합니다.

## 프리뷰 사용

`index.html`을 Y700이나 PC 브라우저에서 직접 열어 볼 수 있습니다. 다만 브라우저는 Android의 READ_CALENDAR에 직접 접근할 수 없으므로 **데모 일정만** 표시됩니다. 실제 데이터 연동은 위 APK 빌드 후 가능합니다.

## 주의

- 현재는 개인용 프로토타입으로, Y700 기기 실물에서 설치·동기화 검증을 진행하지 않았습니다.
- 앱은 읽기 전용입니다. 일정 편집은 Google Calendar에서 하세요.
- 최신 데이터 반영 속도는 Google 계정의 Android 동기화 주기에도 영향을 받습니다.
- 화면을 계속 켜두고 사용하려면 Android 디스플레이 설정에서 화면 꺼짐 시간을 조정하세요. 상시 켜두면 발열과 배터리 소모가 발생합니다.
- TRMNL 공식 앱/플러그인을 사용하거나 복제하는 것이 아니며, 미니멀한 전자잉크 대시보드의 디자인 언어를 참고한 독립 화면입니다.
