# Binance 실시간 데이터 수집 파이프라인 설계 문서

## 1. 배경 및 목표

BTCUSDT, ETHUSDT 두 종목의 실시간 시세(1분봉 캔들, 24시간 Ticker)를 Binance로부터 수집하여 PostgreSQL에 적재하고, 운영 대시보드가 조회할 수 있는 REST API로 제공한다.

핵심 요구사항:

- 서버 최초 기동 시 비어있는 DB를 채운다.
- 서버 재시작으로 인한 장기간(수주~수개월) 누락 데이터를 백필한다.
- 위 두 가지는 별개 기능이 아니라 동일한 gap 계산 로직으로 통합 처리한다.

## 2. 아키텍처 개요

```
Binance WebSocket (combined stream)          Binance REST (klines)
  btcusdt@kline_1m, ethusdt@kline_1m                  │
  btcusdt@ticker,  ethusdt@ticker                      │
        │                                              │
        ▼                                              ▼
BinanceWebSocketClient                    BinanceRestClient + KlineBackfillService
  (JDK HttpClient WebSocket)                 (RestClient, 페이지네이션, gap 계산)
        │                                              │
        └──────────────┬───────────────────────────────┘
                        ▼
         KlineUpsertDao / TickerUpsertDao
         (JdbcTemplate, INSERT ... ON CONFLICT)
                        ▼
              PostgreSQL (kline_1m, ticker_24h)
                        ▲
                        │
      GapScanScheduler (10분 주기 안전망) ──┘
                        │
        KlineQueryService (조회 시 SQL 집계 롤업)
                        │
              KlineController / TickerController
```

## 3. 데이터 모델

### `kline_1m` (1분봉만 저장)

`(symbol, open_time)` 에 UNIQUE 제약을 두어 조회 인덱스와 upsert 키를 겸한다. 5m/1h 등 상위 간격은 별도 저장하지 않고, 조회 시점에 `date_bin`으로 SQL 집계한다. 저장 공간과 쓰기 경로를 단순화하기 위해 해당 방식을 선택했다.

### `ticker_24h` (심볼당 최신 스냅샷 1행)

24시간 Ticker는 시계열이 아니라 현재 통계이므로 PK를 `symbol`로 하는 단일 행 upsert로 설계했다. 별도의 이력 테이블은 만들지 않는다.

### Upsert 전략

JPA의 `save()`/`merge()`는 백필처럼 심볼당 최대 수십만 행을 처리해야 하는 경우 매 행마다 SELECT 후 INSERT/UPDATE를 판단하는 오버헤드가 커서 비효율적이라고 판단했다. 따라서 쓰기 경로는 전부 `JdbcTemplate.batchUpdate` 기반 `INSERT ... ON CONFLICT (symbol, open_time) DO UPDATE`로 통일했다. 이 하나의 upsert 함수를 REST 백필(배치)과 WebSocket 실시간 갱신(단건) 양쪽에서 재사용한다.

이 설계는 **백필의 idempotency를 자연스럽게 보장**한다. 같은 시간 구간을 여러 번 재실행해도 결과가 같고, `MAX(open_time)`이 곧 진행 체크포인트 역할을 하므로 별도의 백필 진행상황 테이블이 불필요하다고 판단했다.

JPA(`KlineRepository`)는 읽기 전용(마지막 저장 시각 조회, 범위 조회)으로만 사용한다.

## 4. 백필 방법 비교 및 최종 선택

| 방법                                     | 설명                                                                         | 장점                                                              | 단점                                                                                                                      | 장기(수주~수개월) 백필 가능 여부                |
| ---------------------------------------- | ---------------------------------------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- |
| **A. REST `klines` 페이지네이션 (채택)** | `/api/v3/klines`를 `startTime`~`endTime`으로 최대 1000개씩 페이지네이션 호출 | 임의 과거 구간을 정확히 재구성 가능, Binance 공식 지원, 구현 단순 | 장기간일수록 요청 횟수 증가(rate limit 관리 필요)                                                                         | **가능**                                        |
| B. WebSocket 재연결 캐치업               | 연결이 끊겼다가 재연결되면 그 시점부터 다시 스트림 수신                      | 별도 구현 불필요, 실시간 파이프라인과 동일                        | Binance WebSocket은 재연결 시 과거 메시지를 **재전송하지 않음** → 다운타임 동안의 데이터는 영구 유실                      | **불가능** (순간적 유실만 대응 가능)            |
| C. 자체 이벤트 로그(Kafka 등) 재생       | 원본 메시지를 메시지 브로커에 적재 후 필요 시 재생                           | 완전한 원본 재현 가능                                             | 메시지 브로커 등 인프라가 추가로 필요하고,애초에 다운타임 동안은 로그 자체가 쌓이지 않으므로 결국 REST 백필과 병행해야 함 | 인프라 복잡도 대비 이점 없음(2심볼 규모에 과함) |

**결론**: 장기간 백필 요구사항을 충족하는 것은 REST `klines` 페이지네이션(A)뿐이다. B는 참고용으로만 문서화하고(순간적 네트워크 유실 시 재연결 자체는 여전히 필요), C는 이 프로젝트 규모(심볼 2개, 저장 간격 1개)에 과설계라 채택하지 않았다.

**최초 기동과 재시작 후 누락을 통합하는 방법**: 둘 다 "심볼별 마지막 저장 `open_time` → 현재"라는 동일한 gap 계산 함수(`GapCalculator`)로 처리한다. DB가 비어있으면(`lastOpenTime`이 없으면) 설정된 기본 lookback 기간(기본 90일, `binance.backfill.default-lookback-days`)부터 시작하고, 데이터가 있으면 마지막 저장 시각 다음 분부터 시작한다 — 별도의 초기 백필과 재시작 백필 기능을 만들지 않고 시작점 계산 하나로 통합했다(`KlineBackfillService.backfillSymbol`).

이 동일한 메서드는 세 지점에서 재사용된다:

1. 앱 기동 시 (`ApplicationReadyEvent` 리스너)
2. WebSocket 재연결 성공 시 (다운타임 구간 메우기)
3. `GapScanScheduler`가 10분마다 최신 저장 시각이 2분 이상 뒤처진 것을 감지했을 때 (안전망)

## 5. Rate Limit 및 비동기 처리

- 백필은 단일 스레드 executor(`binanceTaskExecutor`)로 심볼을 순차 처리하고, 페이지 요청 사이에 고정 지연(`binance.backfill.request-delay-ms`, 기본 250ms)을 둔다. 2개 심볼 규모에서는 이 정도 완충만으로 Binance REST weight 한도 내에서 충분히 동작할 것이고, 별도 RateLimiter 라이브러리는 과설계로 판단해 포함하지 않았다.
- 백필은 `@Async`로 실행되어 앱 기동(HTTP 서버 바인딩)을 블로킹하지 않는다. 대시보드는 백필이 진행되는 동안에도 즉시 뜨고, 데이터는 점진적으로 채워진다. (약 1-2분 가량 소요)
- 페이지 요청 실패 시 최대 3회(`binance.backfill.max-retries`) 지수 백오프 재시도.

## 6. WebSocket 클라이언트

Spring Boot 기본 의존성에 `spring-boot-starter-websocket`(Tyrus, STOMP 지향)이 있지만, 서버 사이드 STOMP 브로커 기능이 필요 없고 재연결 로직을 직접 제어하기 쉬운 JDK 21 내장 `java.net.http.HttpClient` WebSocket API를 사용했다.

- Combined stream(`btcusdt@kline_1m/ethusdt@kline_1m/btcusdt@ticker/ethusdt@ticker`)을 하나의 연결로 구독.
- 연결 종료/에러 시 지수 백오프(1s → 최대 30s, jitter 포함)로 재연결.
- 재연결 성공(`onOpen`) 시 각 심볼에 대해 `KlineBackfillService.backfillSymbol()`을 호출해 다운타임 구간을 메운다.
- 매 kline 메시지(진행 중인 캔들 포함)마다 upsert하므로 대시보드에 마감 전 캔들도 실시간 반영되고, 마감 시 최종값으로 자연스럽게 덮어써진다.

## 7. 조회 API 및 상위 간격 집계

`GET /api/klines?symbol=BTCUSDT&interval=5m&from=...&to=...`

- `interval=1m`이면 `kline_1m`을 그대로 범위 조회.
- 그 외(5m/15m/1h/4h/1d)는 PostgreSQL `date_bin`으로 집계:
  - open = 버킷 내 최초 값, close = 버킷 내 최종 값, high = MAX, low = MIN, volume/quote_volume = SUM.

`GET /api/tickers`

- 심볼별 최신 24시간 통계 스냅샷 목록을 반환한다.
