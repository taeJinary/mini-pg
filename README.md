# mini-pg 💳✨

미니 결제(PG) 프로젝트입니다.  
**결제 생성(멱등/동시성)** → **PG 웹훅(eventId 멱등)** → **PG 조회(verify)로 불확실 상태 확정** → **정산 집계(merchant/day)** 흐름을 구현했습니다 🧩

---

## Tech Stack 🛠️
- Java 17 / Spring Boot 3
- Spring Web, Validation, Spring Data JPA
- MySQL
- JUnit / Gradle

---

## What’s implemented ✅

### 1) 결제 생성 (Idempotency + Concurrency Recovery) 🔁
- `idempotencyKey` 기반 멱등 처리
- 동시성 경합으로 UNIQUE 충돌 발생 시 `idempotencyKey` 및 `orderId` 기반 재조회 복구

**POST** `/api/payments`

```json
{
  "orderId": "ORDER_ID",
  "merchantId": "MERCHANT_ID",
  "amount": 15000,
  "method": "CARD",
  "idempotencyKey": "IDEMPOTENCY_KEY",
  "pgMode": "success"
}
```

---

### 2) PG Webhook (eventId Idempotency) 🪝
- `eventId` 기반 멱등 처리(중복 이벤트 no-op + 200 OK)
- `pgTransactionId`로 결제를 찾아 상태 확정(REQUESTED → APPROVED/DECLINED)
- 결제 미존재(`pgTransactionId` 미매칭)도 200 OK 응답(재시도 폭탄 방지)

**POST** `/api/pg/webhooks`

```json
{
  "eventId": "EVENT_ID",
  "pgTransactionId": "PG_TX_ID",
  "status": "APPROVED"
}
```

---

### 3) Admin 결제 조회 (Filter + Paging) 🔎
- `requestedAt` 기준 정렬/기간 필터
- `merchantId/status/from/to` 필터 + `page/size` 페이징

**GET** `/api/admin/payments?status=REQUESTED&page=0&size=20`

---

### 4) Admin Verify (PG Inquiry로 Timeout/불확실 상태 확정) 🧾
- timeout/error 같은 불확실 상황에서 즉시 실패 확정하지 않고, PG 조회로 상태를 확정
- `NOT_FOUND`는 즉시 실패 확정하지 않음
    - `requestedAt + 2분` 이전: REQUESTED 유지
    - `requestedAt + 2분` 이후: DECLINED 확정 (`PG_NOT_FOUND_TIMEOUT`)

**POST** `/api/admin/payments/{paymentId}/verify`

---

### 5) 정산 집계 (Settlement Aggregation) 🧮
- `merchant + settlementDate` 기준 정산 스냅샷 생성(멱등)
- APPROVED 결제만 `requestedAt` 기준으로 집계하여 저장
- `grossAmount / feeAmount / netAmount` 저장

**POST** `/api/admin/settlements`

```json
{
  "merchantId": "MERCHANT_ID",
  "settlementDate": "2026-02-06"
}
```

**GET** `/api/admin/settlements?merchantId=MERCHANT_ID&from=2026-02-01&to=2026-02-10&page=0&size=20`

---

## Demo scenario 🧪
1. `POST /api/payments`로 결제 생성(REQUESTED)
2. (선택) `POST /api/pg/webhooks`로 승인/거절 웹훅 수신
3. timeout/불확실 케이스는 `POST /api/admin/payments/{id}/verify`로 상태 확정
4. `POST /api/admin/settlements`로 특정 일자 정산 생성
5. `GET /api/admin/settlements`로 정산 조회

---

## Notes 📝
- 이 프로젝트는 결제 시스템에서 자주 발생하는 문제(멱등/동시성/웹훅/불확실 상태/정산)를 작은 스케일로 재현하고 해결하는 데 집중했습니다 😊

## update
Redis 캐시 추가 (PG inquiry 최적화)

- Redis를 PG inquiry 결과 캐시로 사용
- 키: pg:inquiry:{orderId}
- TTL: APPROVED/DECLINED 60초, PENDING 10초, NOT_FOUND 2초
- 캐시 미스 시 PG inquiry 호출 후 캐시 저장
- 상태 확정(웹훅/verify) 시 캐시 eviction 적용