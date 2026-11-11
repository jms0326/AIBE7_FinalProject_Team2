#!/bin/sh
# 운영 Nginx 설정(frontend/nginx)을 Render 단일 컨테이너에 맞게 바꾼다. 이미지 빌드 때 한 번 실행된다
set -eu
CONF=/etc/nginx/conf.d/default.conf
APP=/etc/nginx/snippets/app.conf
PROXY=/etc/nginx/snippets/api-proxy.conf

# 1) Render가 넘기는 포트(10000)로 듣는다
sed -i 's/listen 80 default_server;/listen 10000 default_server;/' "$CONF"

# 2) 백엔드는 같은 컨테이너의 127.0.0.1:8080. Docker DNS(127.0.0.11)를 쓰지 않는다
sed -i '/^\s*resolver /d' "$APP"
sed -i 's#set $backend_upstream http://backend:8080;#set $backend_upstream http://127.0.0.1:8080;#' "$APP"

# 3) TLS는 Render가 앞에서 끝낸다 → 원래 scheme은 Render가 보낸 X-Forwarded-Proto로 본다
#    (백엔드 카카오 Redirect URI · Origin 검증 · HSTS가 https를 봐야 한다)
cat >> "$CONF" <<'CONF'

# ---------- Render (deploy/render) ----------
# Render 앞단이 TLS를 끝내고 X-Forwarded-Proto · X-Forwarded-For를 붙여 넘긴다
map $http_x_forwarded_proto $render_proto {
    ""      $scheme;
    default $http_x_forwarded_proto;
}
# 실제 사용자 IP: Render 내부 대역에서 온 X-Forwarded-For만 믿는다 (요청 제한 · 백엔드 감사 로그용)
set_real_ip_from 10.0.0.0/8;
set_real_ip_from 172.16.0.0/12;
set_real_ip_from 192.168.0.0/16;
set_real_ip_from 100.64.0.0/10;
real_ip_header X-Forwarded-For;
real_ip_recursive on;
CONF
sed -i 's/proxy_set_header X-Forwarded-Proto $scheme;/proxy_set_header X-Forwarded-Proto $render_proto;/' "$PROXY"
sed -i 's/map $scheme $hsts_header {/map $render_proto $hsts_header {/' "$CONF"
# 백엔드에는 Render 포트(10000)가 아니라 공개 포트(443)를 알린다
sed -i 's/proxy_set_header X-Forwarded-Port $server_port;/proxy_set_header X-Forwarded-Port 443;/' "$PROXY"

# 4) 사진은 S3가 아니라 Supabase Storage에서 받는다 → CSP img-src의 S3 도메인을 바꾼다
sed -i 's#https://\*\.s3\.ap-northeast-2\.amazonaws\.com#https://*.supabase.co#g' /etc/nginx/snippets/security-headers.conf

# 5) Let's Encrypt 경로는 쓰지 않는다 (Render가 인증서를 관리)
sed -i '\#include /etc/nginx/snippets/acme-challenge.conf;#d' "$CONF"

grep -q 'listen 10000' "$CONF"
grep -q '127.0.0.1:8080' "$APP"
grep -q 'render_proto' "$PROXY"
grep -q 'supabase.co' /etc/nginx/snippets/security-headers.conf
