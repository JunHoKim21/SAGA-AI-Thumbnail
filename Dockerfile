# 1단계: 빌드 환경 (Java 17)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
# 빌드 필수 파일 복사
COPY backend/gradlew .
COPY backend/gradle/ gradle/
COPY backend/build.gradle .
COPY backend/settings.gradle .
COPY backend/src/ src/
# Windows 환경에서 작성된 gradlew 실행 권한 부여 및 줄바꿈 오류 방지
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
# Spring Boot 프로젝트 빌드
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 환경 (Java 17 JRE + FFmpeg)
FROM eclipse-temurin:17-jre
WORKDIR /app

# FFmpeg 설치 (매우 중요 ⭐️)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# 1단계에서 빌드된 jar 파일만 가져오기
COPY --from=builder /app/build/libs/*.jar app.jar

# 8081 포트 노출 (application.properties 기본 포트)
EXPOSE 8081

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dserver.port=${PORT:8081}", "-jar", "app.jar"]
