package reservation.binancebackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.binancebackend.entity.Kline;

class KlineMapperTest {

    private final KlineMapper mapper = new KlineMapper();

    @Test
    void fromRestRow_mapsBinanceOfficialExampleRow() {
        List<Object> row = List.of(
                1499040000000L,
                "0.01634790",
                "0.80000000",
                "0.01575800",
                "0.01577100",
                "148976.11427815",
                1499644799999L,
                "2434.19055334",
                308
        );

        Kline kline = mapper.fromRestRow("BTCUSDT", row);

        assertThat(kline.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(kline.getOpenTime()).isEqualTo(Instant.ofEpochMilli(1499040000000L));
        assertThat(kline.getCloseTime()).isEqualTo(Instant.ofEpochMilli(1499644799999L));
        assertThat(kline.getOpenPrice()).isEqualByComparingTo(new BigDecimal("0.01634790"));
        assertThat(kline.getHighPrice()).isEqualByComparingTo(new BigDecimal("0.80000000"));
        assertThat(kline.getLowPrice()).isEqualByComparingTo(new BigDecimal("0.01575800"));
        assertThat(kline.getClosePrice()).isEqualByComparingTo(new BigDecimal("0.01577100"));
        assertThat(kline.getVolume()).isEqualByComparingTo(new BigDecimal("148976.11427815"));
        assertThat(kline.getQuoteVolume()).isEqualByComparingTo(new BigDecimal("2434.19055334"));
        assertThat(kline.getTradeCount()).isEqualTo(308);
    }

    @Test
    void fromWsPayload_mapsStreamFieldsDirectly() {
        Kline kline = mapper.fromWsPayload(
                "ETHUSDT", 1499040000000L, 1499040059999L,
                "0.1", "0.2", "0.05", "0.15", "10.5", "1.05", 5);

        assertThat(kline.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(kline.getOpenTime()).isEqualTo(Instant.ofEpochMilli(1499040000000L));
        assertThat(kline.getCloseTime()).isEqualTo(Instant.ofEpochMilli(1499040059999L));
        assertThat(kline.getTradeCount()).isEqualTo(5);
    }
}