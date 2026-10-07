# SAGA AI Thumbnail Studio

AI 기반 썸네일 자동 생성 스튜디오입니다.
영상을 업로드하면 AI가 핵심 장면을 분석하고, 어울리는 카피와 함께 썸네일 및 하이라이트 쇼츠를 자동으로 합성해 줍니다.

## 프로젝트 구조
- \/backend\: Spring Boot 기반 핵심 서버 (FFmpeg 및 Gemini AI 연동)
- \/frontend\: Vue 3 기반 스튜디오 UI 원본 소스

## 실행 방법 (Backend)
1. \ackend\ 폴더로 이동합니다.
2. \pplication.yml\에 Gemini API 키를 입력합니다.
3. \./gradlew bootRun\ 명령어로 실행합니다.
4. 브라우저에서 \http://localhost:8081\로 접속합니다.
