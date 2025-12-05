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
FROM amazoncorretto:11-alpine

WORKDIR /app

# 타임존 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 보안: non-root 사용자로 실행
RUN addgroup -S spring && adduser -S spring -G spring

# EFS 마운트 포인트 디렉토리 생성
RUN mkdir -p /mnt/efs/uploads && chown -R spring:spring /mnt/efs

# JAR 파일 복사
COPY --from=build /app/build/libs/app.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

# 환경변수 (기본값)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV DB_HOST=localhost
ENV DB_PORT=3306
ENV DB_NAME=sampledb
ENV DB_USERNAME=admin
ENV DB_PASSWORD=password
ENV EFS_MOUNT_PATH=/mnt/efs/uploads

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
