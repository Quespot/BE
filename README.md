# ✈️ Quespot

> 전국 방방곡곡, 미션으로 떠나는 관광 서비스

## ✨ 프로젝트 소개

Quespot은 관광지를 직접 방문해 미션을 수행하며 지역을 탐험하는 참여형 관광 서비스입니다.
관광 데이터와 지역 자원을 미션 콘텐츠로 재구성해 여행의 재미를 높이고 지역 방문과 소비로 연결합니다.

- **기간** : 2026.05.06 ~ 2026.09.21
- **인원** : Backend 2명
- **목표** : 관광 데이터와 위치 기반 미션, 보상 시스템을 결합해 사용자가 관광지를 능동적으로 경험하고 여행 기록을 지속적으로 남길 수 있도록 하는 것
- **핵심 흐름** : 관광 데이터 수집 → 미션 탐색·추천 → 현장 수행·인증 → 포인트·배지·스탬프 획득 → 여행 아카이브·캐릭터 꾸미기

---

## 🧑🏻‍💻 팀원 및 역할

|               [허건우](https://github.com/woo6629058)                |                [이창훈](https://github.com/Chhun-Lee)                 |
|:-----------------------------------------------------------------:|:-----------------------------------------------------------------:|
| <img src="https://avatars.githubusercontent.com/u/156512340?v=4" width="200" alt="허건우 프로필"> | <img src="https://avatars.githubusercontent.com/u/220421602?v=4" width="200" alt="이창훈 프로필"> |
|                    인증·회원·프로필·미션 탐색·스팟·인프라·모니터링                    |                     관광 데이터·미션 수행·코스·보상·아이템·알림                     |

---

## 🚀 주요 기능

| 기능 | 설명 |
| --- | --- |
| 인증·회원 | 이메일 인증, JWT 로그인, Google·Kakao·Naver OAuth2, 프로필 및 로그인 수단 관리 |
| 관광 데이터 | 한국관광공사 TourAPI 데이터 수집·정제, 쿼터·체크포인트 관리, 미션 후보 생성·검수 |
| 미션·코스 | 미션 탐색·추천, GPS 도착 인증, 사진·회고 등록, 코스 생성 및 진행 |
| 여행 기록 | 미션 수행 사진과 자유 업로드 사진을 통합한 목록·지도형 아카이브 |
| 보상·꾸미기 | 포인트, 배지, 스탬프, 아이템 구매·장착, Questy 구성 조회 |
| 알림 | 인앱 알림, FCM 푸시, 주변 추천 미션 스케줄링 |
| 파일 | S3 Presigned URL 기반 이미지 직접 업로드 |

---

## 🧩 핵심 기술 및 설계

> 작성 예정

---

## 🛠 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 17, Spring Boot 3.5.15, Gradle |
| Data | Spring Data JPA, MySQL 8.x, Redis 8 |
| Security | Spring Security, JWT, OAuth2 Client |
| External | TourAPI, Gmail SMTP, Firebase Cloud Messaging, Amazon S3 |
| API Docs | Swagger UI, springdoc-openapi |
| Test | JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers |
| Infra | AWS EC2, RDS, ECR, S3, Systems Manager, Docker Compose, Nginx, Certbot |
| Monitoring | Actuator, Micrometer, Prometheus, Grafana Alloy, Grafana Cloud, Discord |

---

## 🏗 시스템 아키텍처
<img width="1671" height="941" alt="인프라 아키텍처" src="https://github.com/user-attachments/assets/8a3a30a5-5a49-4451-a05d-a25078f1d15c" />

---

## 🗃 ERD
<img width="4000" height="3332" alt="Quespot" src="https://github.com/user-attachments/assets/07b075aa-b671-4e53-8b0a-e464532c3f5a" />

---

## 📡 API 구성

| 구분 | 주요 경로 | 기능 |
| --- | --- | --- |
| 인증·회원 | `/api/auth/**`, `/api/users/me/**` | 가입, 로그인, OAuth2, 프로필, 로그인 수단 |
| 미션 | `/api/missions/**`, `/api/mission-attempts/**` | 탐색, 추천, 수행, 도착 인증, 사진·회고 |
| 코스 | `/api/mission-courses/**`, `/api/course-attempts/**` | 코스 생성, 조회, 진행, 포기 |
| 지도·아카이브 | `/api/mission-spots/**`, `/api/users/me/archives/**` | 주변 장소와 여행 기록 조회 |
| 보상·아이템 | `/api/users/me/points/**`, `/api/users/me/badges/**`, `/api/shop/items/**` | 포인트, 배지·스탬프, 상점, 아이템 장착 |
| 알림 | `/api/notifications/**` | FCM 토큰, 인앱 알림, 읽음 처리 |
| 관리자 | `/api/admin/**` | 관광 데이터 수집과 미션 후보 검수·공개 |
| 파일 | `/api/files/**` | S3 Presigned URL 발급 |

- Swagger UI (운영): [https://api.quespot.site/swagger-ui/index.html#/](https://api.quespot.site/swagger-ui/index.html#/)
- Swagger UI (로컬): [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 에러 코드: [`docs/error-codes.md`](docs/error-codes.md)

---

## 📁 프로젝트 구성

```text
src/main/java/com/quespot
├── domain
│   ├── user          # 인증, 회원, 프로필, OAuth2
│   ├── tour          # TourAPI 데이터 수집
│   ├── spot          # 관광 장소 정제
│   ├── mission       # 미션, 코스, 수행, 아카이브
│   ├── reward        # 포인트, 배지, 스탬프
│   ├── item          # 상점, 보유·장착 아이템
│   ├── like          # 미션·코스 좋아요
│   └── notification  # FCM, 인앱 알림
└── global
    ├── apiPayload    # 공통 응답과 예외 처리
    ├── config        # Security, OAuth2, Swagger, Firebase
    ├── security      # JWT 인증
    ├── file          # 파일 업로드
    ├── s3            # S3 연동
    └── monitoring    # 오류 메트릭
```

---

## 💻 로컬 실행 방법

### 요구 사항

- JDK 17
- MySQL 8.x
- Redis 8.x

### 데이터베이스

```sql
CREATE DATABASE IF NOT EXISTS quespot
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 환경변수

```bash
cp .env.example .env
```

`.env`에 최소한 다음 값을 설정합니다.

```dotenv
DB_URL=jdbc:mysql://localhost:3306/quespot?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
DB_USERNAME=your_local_user
DB_PASSWORD=your_local_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=base64_encoded_secret_at_least_32_bytes
MAIL_VERIFICATION_CODE_SECRET=local_verification_secret
```

Spring Boot는 `.env`를 자동으로 읽지 않으므로 IDE 실행 설정에 등록하거나 터미널에서 불러옵니다.

```bash
set -a
source .env
set +a
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

OAuth2, Gmail, S3, Firebase, TourAPI 사용 시 필요한 값은 [`.env.example`](.env.example)을 참고합니다.

---

## 🔄 CI/CD

- `develop` 대상 Pull Request: MySQL·Redis 환경에서 Gradle 테스트
- `develop` 반영: AWS OIDC 인증 → Docker 이미지 빌드 → ECR Push
- AWS Systems Manager로 EC2에 배포
- Health Check 실패 시 이전 이미지로 자동 롤백

---

## 📊 모니터링

- Actuator·Micrometer 기반 애플리케이션 및 JVM 메트릭 수집
- Grafana Alloy가 Prometheus 메트릭을 수집해 Grafana Cloud로 Remote Write
- API 오류를 에러 코드·HTTP 상태·메서드·경로별로 집계
- Grafana Cloud Alert를 Discord로 전달
- 외부에서는 `/actuator/prometheus` 접근 차단

---

## 🤝 협업 규칙

- 기본 흐름: `작업 브랜치 → Pull Request → develop → main`
- 브랜치: `feature/#이슈번호-설명`, `fix/#이슈번호-설명`, `docs/#이슈번호-설명`
- 커밋: `[타입]#이슈번호 작업 내용`
- PR에 작업 내용, API·DB·환경변수 변경, 테스트 결과와 리뷰 포인트 작성
- 민감 정보는 환경변수로 관리하고 저장소에 커밋하지 않음

자세한 규칙은 [`CONTRIBUTING.md`](CONTRIBUTING.md)를 참고합니다.
