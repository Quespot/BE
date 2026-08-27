# Quespot Contributing Guide

## 1. 브랜치 전략

Quespot은 간소화된 Git Flow를 사용합니다.

- `main`: 최종 제출 및 배포용 브랜치입니다.
- `develop`: 기능 통합 및 테스트용 브랜치입니다.
- `feature/*`: 기능 개발 브랜치입니다.
- 기본 흐름은 `feature/*` -> PR -> `develop` -> 데모/제출 전 PR -> `main`입니다.

## 2. 브랜치 네이밍

브랜치는 이슈 번호를 포함해서 생성합니다.

- 기능 개발: `feature/#이슈번호-기능명`
- 버그 수정: `fix/#이슈번호-버그명`
- 긴급 수정: `hotfix/#이슈번호-설명`
- 리팩토링: `refactor/#이슈번호-리팩토링명`
- 문서: `docs/#이슈번호-문서명`
- 설정: `chore/#이슈번호-설정명`

예시:

```text
feature/#12-sign-up
fix/#18-login-exception
refactor/#21-user-entity
docs/#5-package-structure
chore/#7-gradle-dependency
```

작업을 시작할 때는 최신 `develop` 기준으로 기능 브랜치를 만듭니다.

```bash
git switch develop
git pull origin develop
git switch -c feature/#이슈번호-기능명
```

## 3. 커밋 메시지

커밋 메시지는 아래 형식을 사용합니다.

```text
[타입]#이슈번호 작업 내용
```

사용 가능한 타입은 다음과 같습니다.

- `[Feat]`: 기능 추가
- `[Fix]`: 버그 수정
- `[Refactor]`: 리팩토링
- `[Docs]`: 문서
- `[Test]`: 테스트
- `[Chore]`: 기타 설정

예시:

```text
[Feat]#12 회원가입 API 추가
[Fix]#18 로그인 실패 시 예외 처리 수정
[Refactor]#21 유저 엔티티 필드명 정리
[Docs]#5 README 패키지 구조 문서화
[Test]#30 회원가입 서비스 테스트 추가
[Chore]#7 Gradle 의존성 추가
```

## 4. PR 규칙

- PR은 이슈 단위로 생성합니다.
- PR 대상 브랜치는 기본적으로 `develop`입니다.
- 데모 또는 제출 전 안정화가 끝난 변경만 `main`으로 PR을 생성합니다.
- 충돌 해결 책임자는 PR 작성자입니다.
- PR에는 작업 내용, 변경 사항, 테스트 결과, 리뷰 포인트, 관련 이슈를 포함합니다.
- API가 변경되면 Notion API 명세와 Swagger/OpenAPI 문서를 함께 업데이트합니다.
- CodeRabbitAI 검토 후 자체 수정과 확인을 거치고, 필요하면 리뷰어를 지정합니다.

PR을 올리기 전에는 다음 항목을 확인합니다.

- 변경 범위가 한 이슈 안에서 설명 가능한 크기인지 확인합니다.
- API 경로, 요청, 응답, 에러 코드 변경 여부를 PR에 적습니다.
- DB 스키마, 환경변수, 외부 API 연동 변경 여부를 PR에 적습니다.
- 테스트 방법과 결과를 PR에 적습니다.
- 불필요한 로그, 주석, 민감 정보가 포함되지 않았는지 확인합니다.

## 5. Issue 규칙

- Issue 제목은 `[Feat]`, `[Bug]`, `[Refactor]`, `[Docs]`, `[Chore]` 형식을 사용합니다.
- Issue는 기능, 버그, 리팩토링, 문서, 설정 작업 단위로 생성합니다.
- 브랜치 이름에 이슈 번호를 포함해서 Issue, Branch, PR을 연결합니다.
- GitHub Issue Template을 사용합니다.

## 6. Java 코드 컨벤션

- 클래스 이름은 PascalCase를 사용합니다.
- 변수와 메서드는 camelCase를 사용합니다.
- 상수는 UPPER_SNAKE_CASE를 사용합니다.
- 의존성 주입은 생성자 주입을 우선합니다.
- Entity를 API 응답으로 직접 반환하지 않고 DTO를 사용합니다.
- API 응답 변환 로직이 반복되면 Converter를 둡니다.
- 불필요한 전체 포맷 변경과 기능 변경을 한 PR에 섞지 않습니다.
- Request DTO는 `*Request`, Response DTO는 `*Response` 형태로 이름을 맞춥니다.
- 입력값 검증은 Bean Validation을 우선 사용합니다.
- 비즈니스 로직은 Controller가 아니라 Service에 둡니다.
- Repository는 데이터 접근 책임만 갖도록 유지합니다.
- 로그에는 비밀번호, 토큰, 인증 코드, 개인 식별 정보를 남기지 않습니다.

## 7. API 응답 형식

API 응답은 공통 응답 형태로 통일합니다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "요청에 성공했습니다.",
  "result": {}
}
```

실패 응답은 `errorDetail`을 포함할 수 있습니다.

```json
{
  "isSuccess": false,
  "code": "USER404",
  "message": "사용자를 찾을 수 없습니다.",
  "result": null,
  "errorDetail": {}
}
```

## 8. 예외 처리

- 공통 예외 처리는 `BaseException`, `ErrorCode`, `GlobalExceptionHandler`, 도메인별 CustomException 구조로 통일합니다.
- 컨트롤러에서 예외 응답을 직접 만들지 않고 전역 예외 처리로 위임합니다.
- 인증, 권한, 유효성 검증, 외부 API 오류는 구분 가능한 에러 코드로 관리합니다.

## 9. 설정 및 보안

- 비밀번호, 토큰, API 키, SSH 키 등 실제 인증 정보는 저장소에 작성하지 않습니다.
- DB 접속 정보와 외부 API 키는 환경변수로 주입합니다.
- 예시가 필요하면 실제 값이 아닌 샘플 값만 문서화합니다.
- 운영 데이터 삭제, Docker volume 삭제, Git reset 등 복구가 어려운 작업은 사전 합의 후 진행합니다.

## 10. 테스트 및 검증

작업 성격에 맞는 검증을 수행하고 PR에 결과를 남깁니다.

- 단위 테스트: Service, Converter, Validator처럼 순수 로직이 있는 코드
- 통합 테스트: Repository, Security, Controller처럼 Spring Context가 필요한 코드
- 직접 실행 확인: 애플리케이션 기동, 주요 API 호출, Swagger 확인

## 11. 문서 업데이트 기준

아래 변경이 있으면 관련 문서를 함께 수정합니다.

- API가 추가, 수정, 삭제되면 Notion API 명세와 Swagger/OpenAPI를 확인합니다.
- 환경변수가 추가되면 `.env.example`과 README를 확인합니다.
- 패키지 구조가 확정되면 README, CONTRIBUTING, AGENTS를 확인합니다.
- 협업 규칙이 바뀌면 README, CONTRIBUTING, AGENTS를 확인합니다.
- DB 구조가 바뀌면 PR에 변경 의도와 주요 변경 내용을 적습니다.
