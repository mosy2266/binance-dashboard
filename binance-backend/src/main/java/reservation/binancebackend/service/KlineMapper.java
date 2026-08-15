package reservation.binancebackend.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import reservation.binancebackend.entity.Kline;

@Component
public class KlineMapper {

    public Kline fromRestRow(String symbol, List<Object> row) {
        return Kline.builder()
                .symbol(symbol)
                .openTime(Instant.ofEpochMilli(toLong(row.get(0))))
                .openPrice(toDecimal(row.get(1)))
                .highPrice(toDecimal(row.get(2)))
                .lowPrice(toDecimal(row.get(3)))
                .closePrice(toDecimal(row.get(4)))
                .volume(toDecimal(row.get(5)))
                .closeTime(Instant.ofEpochMilli(toLong(row.get(6))))
                .quoteVolume(toDecimal(row.get(7)))
                .tradeCount(toInt(row.get(8)))
                .build();
    }

    public Kline fromWsPayload(String symbol, long openTimeMs, long closeTimeMs, String open,
            String high, String low, String close, String volume, String quoteVolume, int tradeCount) {
        return Kline.builder()
                .symbol(symbol)
                .openTime(Instant.ofEpochMilli(openTimeMs))
                .closeTime(Instant.ofEpochMilli(closeTimeMs))
                .openPrice(new BigDecimal(open))
                .highPrice(new BigDecimal(high))
                .lowPrice(new BigDecimal(low))
                .closePrice(new BigDecimal(close))
                .volume(new BigDecimal(volume))
                .quoteVolume(new BigDecimal(quoteVolume))
                .tradeCount(tradeCount)
                .build();
    }

    private long toLong(Object value) {
        return ((Number) value).longValue();
    }

    private int toInt(Object value) {
        return ((Number) value).intValue();
    }

    private BigDecimal toDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}