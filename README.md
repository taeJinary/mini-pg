
미니 결제 서비스 예제입니다.
결제 생성 API(멱등성 + 동시성 복구)와 fake PG 연동(success/fail/timeout)을 구현했습니다.
(추가 예정: 웹훅 멱등 처리, 관리자 조회/재처리, 정산)

## Tech Stack

Java 17, Spring Boot 3

Spring Web, Validation, Spring Data JPA

MySQL

Gradle, JUnit

Features (현재 구현)

결제 생성 API: POST /api/payments

idempotencyKey 기반 멱등성 보장

동시성 상황에서 DataIntegrityViolationException 발생 시 idempotencyKey/orderId 재조회로 복구

Fake PG 승인 연동: pgMode=success|fail|timeout|random

success: pgTransactionId 저장

fail: 결제 상태 DECLINED + failureCode 기록

timeout: 결제 상태 REQUESTED 유지 + failureCode 기록

## Test

멱등성 테스트

동시성 테스트

타임아웃 시나리오 테스트

## How to Run

1) MySQL 준비

로컬 MySQL 또는 Docker MySQL 사용

2) 설정 파일

src/main/resources/application.yml은 로컬 전용(커밋 제외)

예시 파일: src/main/resources/application-example.yml을 복사해서 사용

3) 테스트/실행
   ./gradlew test
   ./gradlew bootRun

## Design Notes

멱등성: idempotencyKey 유니크 제약 + 충돌 시 재조회 반환

주문당 1결제: orderId 유니크 제약 + 충돌 시 재조회 반환

장애/운영 관점: PG 타임아웃은 실패로 확정하지 않고 REQUESTED 유지(추후 웹훅으로 확정)
 
# Implemented
1) 결제 생성 (Idempotency + Concurrency Recovery)
- `idempotencyKey` 기반 멱등 처리
- 동시성 경합으로 UNIQUE 충돌 발생 시 `idempotencyKey / orderId` 재조회로 복구

2) PG 연동
pgMode: success | fail | timeout | random

3) Webhook (eventId Idempotency)
eventId 기반 멱등 처리(중복 이벤트 no-op + 200 OK)

pgTransactionId로 결제 조회 후 상태 확정(REQUESTED → APPROVED/DECLINED)

결제 미존재(pgTransactionId 미매칭)도 200 OK 응답(운영 재시도 폭탄 방지) 

4) Admin 결제 조회 (Filter + Paging)
requestedAt 기준 정렬/기간 필터

merchantId/status/from/to 필터 + page/size 페이징

GET /api/admin/payments?status=REQUESTED&page=0&size=20