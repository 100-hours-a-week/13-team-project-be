# =============================================================================
# Stage 1: Build — JDK + Gradle로 애플리케이션 빌드
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY gradlew ./
COPY gradle/ gradle/

RUN chmod +x gradlew

# [캐시 최적화] 빌드 설정 파일 복사 → 의존성 변경 시에만 재다운로드
COPY build.gradle settings.gradle ./

# [캐시 최적화] 의존성만 먼저 다운로드 (소스 변경과 무관하게 캐싱)
RUN ./gradlew dependencies --no-daemon

# [마지막] 자주 변경되는 소스 코드 복사
COPY src ./src

# bootJar 빌드 (테스트·린트·정적분석은 CI에서 수행하므로 제외)
RUN ./gradlew bootJar -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest --no-daemon

# =============================================================================
# Stage 2: Runtime — JRE만으로 실행 (빌드 도구·소스 코드 제외)
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# [보안] 루트가 아닌 전용 사용자로 실행
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

# 빌드 스테이지에서 JAR만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 소유권 변경 후 사용자 전환
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# JVM 튜닝: 컨테이너 메모리 인식 활성화
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
