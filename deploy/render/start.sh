#!/bin/sh
# 백엔드(Spring Boot)와 Nginx를 함께 띄운다. 둘 중 하나가 죽으면 컨테이너를 끝내 Render가 다시 띄우게 한다
set -u

# Render 무료 인스턴스 메모리 512MB에 맞춘다 (Nginx 몫을 남기고 JVM 전체를 약 400MB 안으로)
: "${JAVA_OPTS:=-Xmx256m -Xss512k -XX:MaxMetaspaceSize=160m -XX:ReservedCodeCacheSize=48m -XX:+UseSerialGC -XX:TieredStopAtLevel=1}"

java $JAVA_OPTS -jar /app/app.jar &
JAVA_PID=$!

nginx -g 'daemon off;' &
NGINX_PID=$!

# 어느 쪽이든 먼저 끝나면 나머지도 정리하고 같은 종료 코드로 끝낸다
while kill -0 "$JAVA_PID" 2>/dev/null && kill -0 "$NGINX_PID" 2>/dev/null; do
  sleep 5
done
kill "$JAVA_PID" "$NGINX_PID" 2>/dev/null
wait
exit 1
