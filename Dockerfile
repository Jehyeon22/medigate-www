# ===========================================
# Dockerfile for Spring Boot Application
# Java 11 + JAR 직접 실행 방식
# ===========================================

# 빌드 스테이지
FROM gradle:7.5-jdk11 AS build
WORKDIR /app

# Gradle 캐시 최적화를 위해 의존성 먼저 복사
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# ===========================================
# 실행 스테이지
# ===========================================
# amazoncorretto:11-alpine 이미지는 가볍지만, 운영 편의를 위해 'full' 태그를 사용하거나, 
# 'debian' 기반 이미지를 사용하는 것을 고려할 수 있습니다. 여기서는 alpine을 유지합니다.
FROM amazoncorretto:11-alpine

WORKDIR /app

# 기본 패키지 및 타임존 설정
# curl을 추가하여 HEALTHCHECK에 wget 대신 사용 (더 흔한 방식)
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# EFS 액세스 포인트와 UID/GID를 일치시키기 위해 명시적으로 1000으로 설정 (핵심 수정)
# EFS 액세스 포인트 설정: POSIX 사용자 1000:1000과 일치해야 합니다.
RUN addgroup -S spring -g 1000 && adduser -S spring -G spring -u 1000

# EFS 마운트 포인트 디렉토리 생성 및 소유권 변경
RUN mkdir -p /mnt/efs/uploads && chown -R spring:spring /mnt/efs

# JAR 파일 복사 및 소유권 변경
COPY --from=build /app/build/libs/app.jar app.jar
RUN chown spring:spring app.jar

# spring 사용자로 전환
USER spring:spring

# 환경변수 (민감 정보는 제거하고, ECS 태스크 정의를 통해 Secrets Manager로 주입)
# JVM 최적화: 컨테이너 메모리를 자동으로 인식하도록 설정
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC"
ENV EFS_MOUNT_PATH=/mnt/efs/uploads

# DB 관련 환경변수 (제거됨): ECS 태스크 정의의 'secrets' 섹션을 통해 주입해야 합니다.
# ENV DB_HOST=...
# ENV DB_PASSWORD=...

# 헬스체크: curl을 사용하여 더 신뢰성 높은 체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl --fail http://localhost:8080/api/health || exit 1

EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
