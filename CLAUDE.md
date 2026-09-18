# HJO — 서버 고도화 스터디 baseline 실험대

## 배경
3인(우주, 유진, 창민) 서버 고도화 스터디(15주)의 공통 실험대다. 익명 게시판 API 하나 위에
Throughput → Volume → Concurrency 문제를 차례로 얹는다. 각자 개선 설계를 하고,
**같은 조건에서 측정해 baseline 대비 배율로 비교**한다.
이 프로젝트는 모두가 출발점으로 쓸 **나이브 baseline**이다. 좋은 설계가 아니라 **비교의 기준점**이어야 한다.

### 위키 (결정 사항의 단일 출처)
스터디 위키(Obsidian): `C:\Users\wjddn\OneDrive\바탕 화면\Obsidian\Server_specup`
- `wiki/decisions.md`: 공통 결정 사항. **작업 전에 반드시 읽어라.** 이 파일과 여기 적힌 내용이 다르면 decisions.md가 우선이다
- `wiki/topics/P0-실험대-세팅.md`: 세팅 단계 요약
- `wiki/sources/공통-테스트-환경-설계서-v3.md`: 설계 초안(참고용, decisions.md와 충돌하면 decisions.md 우선)
- 위키를 읽을 수 없으면 사용자에게 `.claude/settings.local.json`의 `additionalDirectories` 설정을 요청하라.

## 내 담당 범위 (우주)
코드, 테스트 코드, JVM 설정, 측정 하네스(앱 쪽).

**레포 구조 (2026-09-18 결정): 이 레포는 앱만 가진다.**
- 공통 인프라 레포(`bench-infra`)는 **유진**이 만든다. docker compose(SUT·k6·모니터링), cpuset·메모리 제한,
  postgres 설정(`postgresql.conf`), 모니터링 스택(Prometheus, Grafana, cAdvisor, postgres_exporter)과 대시보드,
  측정 실행·초기화 스크립트가 모두 거기에 들어간다.
- 시드 데이터 대량 생성(S/M/L)과 토큰 사전 발급은 **창민** 담당이다.
- → 이 레포에서는 위 것들을 **만들지 마라.** 대신 공통 인프라가 이 앱을 띄울 수 있도록 **앱 쪽 계약**을 지키고 README에 문서화한다
  (아래 "앱 계약").

## 확정된 제약 (✅, 바꾸지 말 것)
- JDK 25 (**Eclipse Temurin**으로 통일, 아래 "JDK 고정" 참고), Spring Boot 3.5.x (현재 3.5.16), Spring MVC (WebFlux 금지. 가상 스레드는 설정으로 on/off만 가능하게, 기본 off)
- PostgreSQL, JWT 인증, 로컬 Docker Desktop
- 리소스: SUT(앱+DB) 4코어 / k6 2코어 / 모니터링 1코어, 각각 cpuset 분리, 메모리 8GB
- DB 데이터는 named volume. bind mount는 설정 파일에만 쓴다
- 모니터링: Actuator + Micrometer Prometheus 레지스트리 (`/actuator/prometheus` 노출)
- 로그: root WARN, 앱 패키지(`com.example.HJO`) INFO, `org.hibernate.SQL` WARN. GC 로그(-Xlog:gc*) on
- 스키마는 Flyway 마이그레이션(.sql)으로만 관리한다 (`ddl-auto`는 `validate`만)
- 이미지 태그 `latest` 금지, 버전을 명시해 고정

## JDK 고정 (로컬 빌드와 컨테이너 실행의 JVM을 같게 한다)
baseline을 순수하게 유지하기 위해, 로컬 빌드·테스트와 컨테이너 실행 모두 **같은 벤더(Temurin)의 JDK 25**를 쓴다.
- `build.gradle` toolchain은 버전과 **벤더를 모두 고정**한다. 프로젝트를 다시 생성하면 반드시 다시 넣는다.
  ```gradle
  java {
      toolchain {
          languageVersion = JavaLanguageVersion.of(25)
          vendor = JvmVendorSpec.ADOPTIUM   // Temurin 고정. 다른 벤더의 25가 설치돼도 선택되지 않게
      }
  }
  ```
- 컨테이너 이미지도 Temurin 25(`eclipse-temurin:25-...`, 패치 버전까지 태그 고정)를 쓴다. 로컬 JDK(25.0.4.1)와 패치 버전이 다르면 README에 기록한다.
- 로컬 환경 (우주 PC, 2026-09-18 확인):
  - Temurin 25.0.4.1 LTS: `C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`
  - JAVA_HOME(사용자)도 위 경로다. Gradle 데몬 JVM과 toolchain이 모두 Temurin 25로 실행된다.
  - 다른 JDK(8, 15, 17, 19, 21, 22)도 설치돼 있다. **빌드가 25 이외의 JDK를 잡으면 버그로 보고 멈춰라.**
  - 확인 명령: `./gradlew --version`(Daemon JVM 줄), `./gradlew -q javaToolchains`
- 이미 열린 터미널이나 IDE는 옛 JAVA_HOME(21)을 들고 있을 수 있다. JVM이 21로 잡히면 사용자에게 새 터미널이나 IDE 재시작을 요청하라.

## baseline 동작 원칙 (🟡 잠정, 이대로 구현)
- **캐시, 배치, 비동기, 큐 없음.** 요청마다 동기로 DB에 쓴다
- 앱 + DB 두 컨테이너만 쓴다 (Kafka, Redis, Nginx 없음)
- 커넥션 풀과 스레드 풀은 기본값 (HikariCP 10)
- 조회수와 좋아요 카운터는 `UPDATE ... SET count = count + 1`로 갱신한다
  → **핫 로우 경합이 일부러 생기게 둔다.** 이게 P1의 개선 대상이다. 최적화하지 마라
- N+1은 만들지 않는다 (상세 조회 = 게시글 1쿼리 + 댓글과 작성자 join 1쿼리)
- 목록은 offset 페이지네이션. trending은 요청 시점에 이벤트 테이블에서 실시간 집계

## 인증
- OAuth2 Resource Server로 **검증만** 한다. HS256, 비밀키는 환경변수 `JWT_SECRET`
- 로그인이나 토큰 발급 API는 만들지 않는다. 토큰은 시드 단계에서 사전 발급한다(창민 담당)
  → 토큰 형식(`sub` = user id, `exp`)을 README에 문서화하고, 테스트용 토큰 생성 유틸(`NimbusJwtEncoder`)은 **test 코드에만** 둔다
- 인증 과정에서 DB 조회 금지 (`sub`에서 user id만 꺼내 쓴다)

## 스키마 (⬜ 팀 미확정, 잠정안으로 구현하되 TODO 표시)
2회차 시드 표 기준 테이블: boards, posts, comments, post_view_events, post_likes, post_stats(posts와 1:1).
v3 초안의 users 테이블도 포함한다 (JWT sub와 작성자 표시용).
- boards(id, name)
- users(id, nickname, created_at)
- posts(id bigint, board_id, author_id, title, content, created_at)
- post_stats(post_id PK/FK, view_count, like_count): 카운터는 여기서 갱신한다
- comments(id, post_id, user_id, content, created_at)
- post_likes(post_id, user_id, created_at, UNIQUE(post_id, user_id))
- post_view_events(id, event_id uuid UNIQUE, post_id, user_id, client_ts, created_at)
- PK는 bigint 시퀀스 (event_id만 UUID)
- 인덱스: **⬜ 미정 (PK만 vs 조회 조건 지원 인덱스).** 별도 마이그레이션 파일(`V2__baseline_indexes.sql`)로 분리해서
  뺄 수 있게 만들고, README에 TODO로 남긴다
- ERD 원본(`anonymous_board_erd.html`)이 아직 반영되지 않았다. 스키마 파일 상단에 "잠정" 주석을 단다

## API (⬜ 팀 미확정, v3 초안 기준으로 구현)
| API | 동작 |
|---|---|
| `POST /posts/{id}/views` | body에 event_id, client_ts 필수. `INSERT ... ON CONFLICT (event_id) DO NOTHING`, 새로 들어갔을 때만 view_count +1 (한 트랜잭션). 같은 event_id는 1회만 반영 |
| `POST /posts/{id}/likes` | `INSERT ... ON CONFLICT DO NOTHING`, 새로 들어갔을 때만 like_count +1. 재요청이면 200 + 현재 상태(멱등). 취소 API 없음 |
| `GET /posts?sort=latest\|popular&cursor=&size=` | latest: created_at DESC, id DESC / popular: like_count DESC, id DESC. 불투명 cursor(내부는 offset), size 기본 20, 최대 50, 본문 미리보기 100자 |
| `GET /posts/{id}` | 게시글 + 최신 댓글 20개(작성자 nickname 포함) + 카운트 |
| `GET /posts/trending` | 최근 24시간 고유 조회 이벤트 수 DESC, id DESC, 상위 20개 |
| `POST /posts` | 게시글 작성 (배경 부하용) |
- 에러 규약: 검증 실패 400, 인증 실패 401, 없음 404. 5xx는 서버 에러로 집계된다
- 헬스체크: `GET /actuator/health`, 포트 8080
- SIGTERM이 오면 graceful shutdown

## 테스트 (2회차 규칙: 정합성과 동시성 문제는 테스트 코드로 검증)
Testcontainers(PostgreSQL, 운영과 같은 이미지 태그)로 다음을 검증한다.
- 같은 event_id를 여러 번(동시 포함) 보내면 1회만 반영된다
- 서로 다른 사용자 200명이 동시에 좋아요 → like_count 200
- 같은 사용자가 동시에 50회 좋아요 → 1
- latest 목록을 cursor로 끝까지 스크롤하면 누락과 중복이 없다
- 인증 없음이나 잘못된 토큰 → 401

## 앱 계약 (공통 인프라가 이 앱을 띄우기 위한 약속, README에 문서화)
이 레포가 제공하는 것은 **앱 이미지를 만드는 `Dockerfile`과 설정 계약**뿐이다.
- `Dockerfile`: eclipse-temurin 25 기반, 패치 버전까지 태그 고정. JVM 옵션은 환경변수 `JAVA_OPTS`로 주입
  (기본: `-XX:+UseG1GC -Xlog:gc*:file=/logs/gc.log:time,uptime`. `ActiveProcessorCount`와 힙은 인프라가 주입)
- 포트 8080, 헬스체크 `GET /actuator/health`, 지표 `GET /actuator/prometheus` (인증 없이 접근 가능)
- DB 접속: `DB_WRITE_URL`, `DB_READ_URL`, `DB_USERNAME`, `DB_PASSWORD` / 인증: `JWT_SECRET`
- 스키마: 앱 기동 시 Flyway가 적용한다. 인덱스 포함 여부는 `FLYWAY_LOCATIONS`로 전환한다
- SIGTERM이 오면 graceful shutdown
- 모든 설정값은 코드에 하드코딩하지 말고 `application.yml`과 환경변수로 뺀다. 실험 파라미터를 재빌드 없이 바꿀 수 있어야 한다
- README에 **환경변수 표(이름, 기본값, 의미)** 를 반드시 유지한다. 유진이 인프라 compose를 이 표 기준으로 작성한다
- 로컬 개발용 DB가 필요하면 Testcontainers를 쓰거나, 개발 전용 `compose.dev.yml`(app 제외, postgres만, **측정용 아님**을 명시)을 둘 수 있다

## 하지 말 것
- 캐시, 비동기, 배치, 큐, 인덱스 튜닝 같은 **개선을 미리 넣지 마라.** baseline은 일부러 순진해야 한다
- 미정(⬜) 항목을 임의로 확정하지 마라. 잠정안으로 구현하고 README의 "팀 확인 필요" 목록에 모은다
- DevTools, spring-boot-docker-compose 자동 기동 사용 금지 (측정 절차를 오염시킨다)

## 진행 방식
1. 먼저 위키의 `decisions.md`와 이 파일을 읽고, **구현 계획과 파일 구조를 제시한 뒤 사용자 확인을 받아라**
2. 순서: 스키마·마이그레이션(완료) → 인증 → API → 정합성·동시성 테스트 → Dockerfile·앱 계약 README
3. 단계마다 테스트를 통과시키고 커밋 단위로 끊어라. 커밋은 사용자 확인 후에 한다
4. 마지막에 README를 작성한다: 실행 방법, 환경변수 표, API 명세, 토큰 형식, "팀 확인 필요" 목록
5. 측정 결과나 새 결정이 생기면 위키에 기록할지 사용자에게 제안하라 (위키 규칙은 위키의 `CLAUDE.md`)
