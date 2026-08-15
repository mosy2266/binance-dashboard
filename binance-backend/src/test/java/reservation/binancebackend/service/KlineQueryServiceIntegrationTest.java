package reservation.binancebackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reservation.binancebackend.dto.KlineDto;
import reservation.binancebackend.entity.Kline;
import reservation.binancebackend.repository.KlineUpsertDao;

@Testcontainers
@SpringBootTest
class KlineQueryServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private KlineUpsertDao klineUpsertDao;

    @Autowired
    private KlineQueryService klineQueryService;

    @Test
    void getKlines_rollsUpFifteenOneMinuteCandlesIntoThreeFiveMinuteBuckets() {
        String symbol = "BTCUSDT";
        Instant base = Instant.parse("2026-08-15T00:00:00Z");

        List<Kline> candles = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Instant openTime = base.plus(i, ChronoUnit.MINUTES);
            candles.add(Kline.builder()
                    .symbol(symbol)
                    .openTime(openTime)
                    .closeTime(openTime.plusSeconds(59))
                    .openPrice(BigDecimal.valueOf(100 + i))
                    .highPrice(BigDecimal.valueOf(200 + i))
                    .lowPrice(BigDecimal.valueOf(50 + i))
                    .closePrice(BigDecimal.valueOf(150 + i))
                    .volume(BigDecimal.ONE)
                    .quoteVolume(BigDecimal.TEN)
                    .tradeCount(1)
                    .build());
        }
        klineUpsertDao.upsertBatch(candles);

        List<KlineDto> result = klineQueryService.getKlines(
                symbol, "5m", base, base.plus(15, ChronoUnit.MINUTES));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).openPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.get(0).closePrice()).isEqualByComparingTo(BigDecimal.valueOf(154));
        assertThat(result.get(0).highPrice()).isEqualByComparingTo(BigDecimal.valueOf(204));
        assertThat(result.get(0).lowPrice()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(result.get(0).volume()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }
}