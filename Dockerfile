# syntax=docker/dockerfile:1
#
# HJO baseline 앱 이미지. 공통 인프라(bench-infra)의 compose가 이 이미지로 앱 컨테이너를 띄운다.
# - JDK는 Temurin 25로 고정한다(로컬 빌드와 같은 벤더). 태그는 패치 버전까지 고정하고 latest는 쓰지 않는다.
# - 빌드도 컨테이너 안에서 해서, 누가 어디서 빌드해도 같은 JDK로 jar가 만들어진다.
# 빌드: docker build -t hjo-app:<버전> .

# ---- 1단계: 빌드 (JDK) ----
FROM eclipse-temurin:25.0.4_7-jdk-noble AS build
WORKDIR /workspace

# 의존성 레이어를 먼저 받아 두면 소스만 바뀔 때 다시 받지 않는다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*.jar /workspace/app.jar

# ---- 2단계: 실행 (JRE) ----
FROM eclipse-temurin:25.0.4_7-jre-noble

# root가 아닌 사용자로 실행한다. GC 로그는 /logs에 쓴다(인프라가 볼륨으로 꺼내 분석)
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app /logs \
    && chown app:app /app /logs
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar
USER app

# JVM 옵션은 JAVA_OPTS로 주입한다. 값을 주면 아래 기본값을 **대체**하므로, 기본 옵션(G1, GC 로그)도 함께 넣어야 한다.
# 힙 크기와 -XX:ActiveProcessorCount는 코어·메모리 배분에 맞춰 인프라가 넣는다.
ENV JAVA_OPTS="-XX:+UseG1GC -Xlog:gc*:file=/logs/gc.log:time,uptime"

EXPOSE 8080

# exec로 java를 PID 1로 띄워 docker stop의 SIGTERM이 앱에 바로 전달되게 한다(graceful shutdown)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
