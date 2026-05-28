# ============================================
# Stage 1: 빌드
# ============================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 의존성 파일 먼저 복사 → build.gradle 변경 없으면 레이어 캐시 재사용
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드 (테스트·spotless는 CI에서 검사하므로 스킵)
COPY src src
RUN ./gradlew bootJar -x test -x spotlessApply --no-daemon

# ============================================
# Stage 2: 런타임
# ============================================
FROM eclipse-temurin:21-jre

# 타임존
RUN apt-get update && \
    apt-get install -y --no-install-recommends tzdata && \
    rm -rf /var/lib/apt/lists/* && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 비root 유저 (보안)
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
