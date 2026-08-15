package reservation.binancebackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TickerDto(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal priceChange,
        BigDecimal priceChangePercent,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Instant updatedAt) {
}