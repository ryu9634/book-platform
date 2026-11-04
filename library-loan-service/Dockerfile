# syntax=docker/dockerfile:1
# 사용법: docker build --build-arg MODULE=library-user -t book-platform/library-user .
# 같은 Dockerfile로 user, admin 앱 이미지를 만들고 MODULE 값으로 구분합니다.

# ---------- 1단계: jar 빌드 ----------
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# Gradle 캐시를 빌드 사이에 재사용해 의존성을 매번 받지 않습니다. 테스트는 CI와 로컬에서 따로 돌립니다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :${MODULE}:bootJar -x test --no-daemon --console=plain \
    && cp ${MODULE}/build/libs/${MODULE}-*-SNAPSHOT.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ---------- 2단계: 실행 이미지 ----------
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
# 자주 바뀌지 않는 계층부터 복사해야 코드만 바뀌었을 때 의존성 계층을 다시 쓰지 않습니다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER app
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
