package reservation.binancebackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record KlineDto(
        Instant openTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Integer tradeCount) {
}