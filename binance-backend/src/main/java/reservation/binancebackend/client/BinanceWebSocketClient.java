package reservation.binancebackend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reservation.binancebackend.config.BinanceProperties;
import reservation.binancebackend.entity.Kline;
import reservation.binancebackend.entity.Ticker24hStat;
import reservation.binancebackend.repository.KlineUpsertDao;
import reservation.binancebackend.repository.TickerUpsertDao;
import reservation.binancebackend.service.KlineBackfillService;
import reservation.binancebackend.service.KlineMapper;

@Component
public class BinanceWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketClient.class);
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 30_000;

    private final BinanceProperties properties;
    private final KlineMapper klineMapper;
    private final KlineUpsertDao klineUpsertDao;
    private final TickerUpsertDao tickerUpsertDao;
    private final KlineBackfillService backfillService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger reconnectAttempt = new AtomicInteger(0);

    private volatile WebSocket webSocket;
    private volatile boolean shuttingDown = false;

    public BinanceWebSocketClient(BinanceProperties properties, KlineMapper klineMapper,
            KlineUpsertDao klineUpsertDao, TickerUpsertDao tickerUpsertDao,
            KlineBackfillService backfillService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.klineMapper = klineMapper;
        this.klineUpsertDao = klineUpsertDao;
        this.tickerUpsertDao = tickerUpsertDao;
        this.backfillService = backfillService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void connect() {
        URI uri = buildStreamUri();
        log.info("Connecting to Binance combined stream: {}", uri);

        httpClient.newWebSocketBuilder()
                .buildAsync(uri, new StreamListener())
                .whenComplete((ws, error) -> {
                    if (error != null) {
                        log.warn("WebSocket connect failed: {}", error.getMessage());
                        scheduleReconnect();
                    } else {
                        webSocket = ws;
                    }
                });
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        reconnectExecutor.shutdownNow();
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        }
    }

    private URI buildStreamUri() {
        List<String> symbols = properties.getSymbols();
        String streams = symbols.stream()
                .map(String::toLowerCase)
                .flatMap(s -> List.of(s + "@kline_1m", s + "@ticker").stream())
                .reduce((a, b) -> a + "/" + b)
                .orElseThrow();
        return URI.create(properties.getWsBaseUrl() + "/stream?streams=" + streams);
    }

    private void scheduleReconnect() {
        if (shuttingDown) {
            return;
        }
        int attempt = reconnectAttempt.incrementAndGet();
        long delay = Math.min(BASE_DELAY_MS * (1L << Math.min(attempt, 10)), MAX_DELAY_MS);
        delay += (long) (Math.random() * 500);

        log.info("Scheduling WebSocket reconnect attempt={} delayMs={}", attempt, delay);
        reconnectExecutor.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    }

    private void handleMessage(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            String stream = envelope.path("stream").asText();
            JsonNode data = envelope.path("data");

            if (stream.contains("@kline")) {
                handleKline(data);
            } else if (stream.contains("@ticker")) {
                handleTicker(data);
            }
        } catch (Exception e) {
            log.warn("Failed to handle WebSocket message: {}", e.getMessage());
        }
    }

    private void handleKline(JsonNode data) {
        JsonNode k = data.path("k");
        String symbol = data.path("s").asText();

        Kline kline = klineMapper.fromWsPayload(
                symbol,
                k.path("t").asLong(),
                k.path("T").asLong(),
                k.path("o").asText(),
                k.path("h").asText(),
                k.path("l").asText(),
                k.path("c").asText(),
                k.path("v").asText(),
                k.path("q").asText(),
                k.path("n").asInt());

        klineUpsertDao.upsert(kline);
    }

    private void handleTicker(JsonNode data) {
        String symbol = data.path("s").asText();

        Ticker24hStat ticker = Ticker24hStat.builder()
                .symbol(symbol)
                .lastPrice(new BigDecimal(data.path("c").asText()))
                .priceChange(new BigDecimal(data.path("p").asText()))
                .priceChangePercent(new BigDecimal(data.path("P").asText()))
                .highPrice(new BigDecimal(data.path("h").asText()))
                .lowPrice(new BigDecimal(data.path("l").asText()))
                .volume(new BigDecimal(data.path("v").asText()))
                .quoteVolume(new BigDecimal(data.path("q").asText()))
                .updatedAt(Instant.now())
                .build();

        tickerUpsertDao.upsert(ticker);
    }

    private class StreamListener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance WebSocket connected");
            reconnectAttempt.set(0);
            properties.getSymbols().forEach(backfillService::backfillSymbol);
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handleMessage(message);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("Binance WebSocket closed: status={} reason={}", statusCode, reason);
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("Binance WebSocket error: {}", error.getMessage());
            scheduleReconnect();
        }
    }
}