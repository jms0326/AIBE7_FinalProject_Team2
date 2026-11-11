# Render 무료 웹 서비스용 단일 이미지: Nginx(화면 · /api 프록시) + Spring Boot(백엔드)를 한 컨테이너에 둔다
# - 운영(EC2)과 같은 구조를 유지한다: 브라우저는 같은 도메인으로 화면과 /api를 부르고, Nginx가 /api만 백엔드로 넘긴다
#   (리프레시 쿠키 SameSite=Strict · Origin 검증 · 카카오 Redirect URI가 같은 도메인을 전제로 한다)
# - 서비스 하나로 띄우는 이유: Render 무료 인스턴스 시간(월 750시간)을 서비스 두 개가 나눠 쓰지 않게
# - 빌드 컨텍스트는 저장소 루트다 (Render 설정: Dockerfile Path = deploy/render/Dockerfile, Docker Context = .)

# ===== 1단계: 프론트 빌드 (frontend/Dockerfile과 같은 값) =====
FROM node:24-alpine AS web
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_API_MODE=real
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_API_MODE=$VITE_API_MODE
RUN npm run build

# ===== 2단계: 백엔드 빌드 (backend/Dockerfile과 같은 값) =====
FROM eclipse-temurin:17-jdk AS api
WORKDIR /app
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true
COPY backend/src ./src
RUN ./gradlew bootJar -x test --no-daemon

# ===== 3단계: 실행 =====
# 운영 백엔드와 같은 JRE 이미지(Ubuntu). alpine판은 arm64 이미지가 없어 로컬(Apple Silicon) 빌드가 안 된다
FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx \
    && rm -rf /var/lib/apt/lists/* /etc/nginx/sites-enabled/default

# 운영 Nginx 설정을 그대로 가져와 Render에 맞는 부분만 바꾼다 (nginx-render.sh 참고)
COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY frontend/nginx/snippets/ /etc/nginx/snippets/
COPY deploy/render/nginx-render.sh /tmp/nginx-render.sh
RUN sh /tmp/nginx-render.sh && rm /tmp/nginx-render.sh \
    && mkdir -p /var/log/nginx-ratelimit /run/nginx \
    && nginx -t

COPY --from=web /app/dist /usr/share/nginx/html
COPY --from=api /app/build/libs/*.jar /app/app.jar
COPY --chmod=755 deploy/render/start.sh /app/start.sh

# Render가 이 포트로 요청을 넘긴다 (Render 기본 PORT 10000)
EXPOSE 10000
CMD ["/app/start.sh"]
