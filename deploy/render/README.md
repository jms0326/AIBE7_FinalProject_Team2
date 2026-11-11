# Render 배포 (AWS 종료 후 이관)

학원 AWS(EC2 · RDS · S3)가 내려간 뒤 무료로 운영하기 위한 구성이다. 파이프라인(AI 판결 생성)은 끈다.

| 역할 | 서비스 |
| --- | --- |
| 화면 + API | Render Web Service 1개 (이 폴더의 Dockerfile: Nginx + Spring Boot) |
| DB | Supabase PostgreSQL (pgvector, Session pooler) |
| 사진 | Supabase Storage (S3 호환, 공개 버킷) |
| Redis | Render Key Value (같은 리전, 내부 주소) |

## Render 설정

- Runtime: Docker
- Dockerfile Path: `deploy/render/Dockerfile`
- Docker Build Context Directory: `.`
- Instance Type: Free
- Health Check Path: `/`
- 자동 배포: `main`에 push하면 배포된다. jms0326 계정에 Render GitHub 앱이 설치돼 있어야 한다(이 저장소만 허용)

## 환경변수

비밀값은 Render 대시보드에만 넣는다. 이 파일 · 코드 · 이슈에 적지 않는다.

| 이름 | 값 |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<pooler 호스트>:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `postgres.<project ref>` |
| `DB_PASSWORD` | (비밀) |
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | `5` |
| `REDIS_HOST` | Key Value 내부 호스트 (`red-...`) |
| `REDIS_PORT` | `6379` |
| `JWT_SECRET` | (비밀) 기존 운영 값 — 바꾸면 기존 로그인이 모두 풀린다 |
| `GEMINI_API_KEY` | (비밀) 없으면 RAG 검색만 꺼진다 |
| `S3_BUCKET_NAME` | Supabase 버킷 이름 |
| `S3_ENDPOINT` | `https://<ref>.storage.supabase.co/storage/v1/s3` |
| `S3_PUBLIC_BASE_URL` | `https://<ref>.supabase.co/storage/v1/object/public/<버킷>` |
| `AWS_REGION` | Supabase 리전 (예: `ap-southeast-1`) |
| `AWS_ACCESS_KEY_ID` · `AWS_SECRET_ACCESS_KEY` | (비밀) Supabase Storage S3 키 |
| `KAKAO_CLIENT_ID` · `KAKAO_CLIENT_SECRET` | (비밀) 카카오 콘솔 Redirect URI에 `https://<서비스>.onrender.com/api/v1/auth/oauth2/callback/kakao` 추가 |
| `SESSION_COOKIE_SECURE` | `true` |

## 무료 플랜 제약

- 15분 동안 요청이 없으면 잠들고, 첫 요청에 약 1분 걸린다
- Key Value는 메모리 전용이라 재시작하면 비워진다 → 로그인(리프레시 토큰)이 풀릴 수 있다
- Supabase 무료 프로젝트는 7일 동안 사용이 없으면 일시 정지된다 (대시보드에서 다시 켠다)
