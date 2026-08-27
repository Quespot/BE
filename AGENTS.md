# Quespot Agent Guide

이 문서는 Quespot 서버 저장소에서 작업하는 에이전트가 따라야 하는 규칙입니다. 저장소의 일반 협업 규칙은 `CONTRIBUTING.md`를 함께 참고합니다.

## 1. 사용자와의 협업

- 코드나 설정 파일을 수정하기 전에 변경 목적, 대상 파일, 수정 범위를 먼저 알립니다.
- 사용자가 승인한 범위만 수정하고, 범위 밖 변경이 필요하면 다시 설명하고 확인받습니다.
- 기존 작업과 무관한 수정, 스테이징된 파일, untracked 파일은 사용자 작업으로 간주하고 보존합니다.
- 명시적인 요청 없이 `./gradlew build`, `./gradlew test`, `./gradlew bootRun` 등의 Gradle 명령을 실행하지 않습니다.
- 스테이징, 커밋, 푸시, PR 생성은 사용자가 요청한 경우에만 실행합니다.
- 작업 완료 시 변경한 파일, 검증한 항목, 사용자가 직접 확인할 항목을 구분해 안내합니다.

## 2. 프로젝트 환경

- Java 17
- Spring Boot 3.5.x
- Gradle Wrapper
- Spring Web, Validation
- Spring Data JPA, MySQL
- Spring Security, OAuth2 Client
- Spring Boot Actuator
- Lombok

JWT, Redis, S3, Docker, 배포 및 모니터링 설정은 도입 시 별도 이슈와 PR에서 관리합니다.

## 3. Java 및 Spring 규칙

- 클래스는 PascalCase, 메서드와 변수는 camelCase, 상수는 UPPER_SNAKE_CASE를 사용합니다.
- 의존성 주입은 생성자 주입을 우선합니다.
- Entity를 API 응답으로 직접 반환하지 않고 DTO와 Converter를 사용합니다.
- API 응답은 공통 응답 구조를 따릅니다.
- 예외 처리는 `GeneralErrorCode`와 `GeneralExceptionAdvice` 기반 전역 예외 처리 패턴을 따릅니다.
- 인증이 필요한 API와 공개 API를 추가할 때 Security 설정을 함께 확인합니다.
- 트랜잭션 변경 시 실패와 롤백 경로를 함께 고려합니다.
- 불필요한 전체 리팩터링이나 포맷 변경을 기능 수정과 섞지 않습니다.

## 4. 설정 및 보안

- 비밀번호, 토큰, API 키, SSH 키 등 실제 인증 정보를 저장소에 작성하지 않습니다.
- 실제 환경변수와 secret 설정은 Git에서 제외합니다.
- 새로운 환경변수를 추가하면 `.env.example`과 README도 함께 검토합니다.
- 운영 데이터 삭제, Docker volume 삭제, Git reset 등 복구가 어려운 작업은 사용자 승인 없이 실행하지 않습니다.

## 5. Git 규칙

- 기본 통합 브랜치는 `develop`입니다.
- 브랜치 이름은 `CONTRIBUTING.md`의 규칙을 따릅니다.
- 기능: `feature/#이슈번호-설명`
- 버그: `fix/#이슈번호-설명`
- 긴급 수정: `hotfix/#이슈번호-설명`
- 리팩터링: `refactor/#이슈번호-설명`
- 문서: `docs/#이슈번호-설명`
- 설정: `chore/#이슈번호-설명`
- 커밋 메시지 형식은 `[타입]#이슈번호 작업 내용`을 사용합니다.
- Git 상태와 diff는 확인할 수 있지만, 스테이징, 커밋, 푸시는 사용자 요청이 있을 때만 실행합니다.

## 6. 검증 및 인계

- 파일 수정 후 가능한 범위에서 문법, 설정 구조, `git diff --check`를 확인합니다.
- Gradle 빌드나 테스트가 필요하면 실행하지 않고 사용자에게 정확한 명령을 안내합니다.
- 실행하지 않은 검증을 성공했다고 표현하지 않습니다.
- 인프라 설정은 로컬 문법 검사와 실제 실행 검증을 구분해 보고합니다.
- 최종 안내에는 변경 파일, 예상 영향, 검증한 항목, 사용자가 직접 확인할 항목을 포함합니다.
- API, 환경변수, 패키지 구조, 협업 규칙이 바뀌면 관련 문서도 함께 확인합니다.
