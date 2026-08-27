# Quespot

전국 방방곡곡, 미션으로 떠나는 관광 서비스

## 기술 스택

- Java 17
- Spring Boot 3.5.x
- Gradle Wrapper
- Spring Web, Validation
- Spring Data JPA, MySQL
- Spring Security, OAuth2 Client
- Spring Boot Actuator
- Lombok

## 로컬 실행 준비

MySQL에 사용할 데이터베이스와 계정을 준비합니다.

```sql
CREATE DATABASE IF NOT EXISTS quespot
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

환경변수는 `.env.example`을 참고해 로컬 환경이나 IDE 실행 설정에 등록합니다. Spring Boot는 `.env` 파일을 자동으로 읽지 않으므로 터미널에서 실행할 때는 직접 export가 필요합니다.

```bash
export DB_URL="jdbc:mysql://localhost:3306/quespot?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true"
export DB_USERNAME="quespot"
export DB_PASSWORD="your-password"
```

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

## 협업 문서

- 브랜치, 커밋, PR 규칙: `CONTRIBUTING.md`
- 에이전트 작업 규칙: `AGENTS.md`
- PR 템플릿: `.github/PULL_REQUEST_TEMPLATE.md`
- Issue 템플릿: `.github/ISSUE_TEMPLATE/`

## 보안

- 실제 비밀번호, 토큰, API 키는 저장소에 커밋하지 않습니다.
- `.env` 파일은 Git에서 제외합니다.
- 공유가 필요한 값은 실제 값이 아닌 예시 형태로만 문서화합니다.
