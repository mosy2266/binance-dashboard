package reservation.binancebackend.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reservation.binancebackend.dto.KlineDto;
import reservation.binancebackend.entity.Kline;
import reservation.binancebackend.repository.KlineRepository;

@Service
public class KlineQueryService {

    private static final String ROLLUP_SQL = """
            SELECT
              date_bin(?::interval, open_time, TIMESTAMPTZ '2000-01-01') AS bucket,
              (array_agg(open_price ORDER BY open_time ASC))[1] AS open_price,
              MAX(high_price) AS high_price,
              MIN(low_price) AS low_price,
              (array_agg(close_price ORDER BY open_time DESC))[1] AS close_price,
              SUM(volume) AS volume,
              SUM(quote_volume) AS quote_volume,
              SUM(trade_count) AS trade_count
            FROM kline_1m
            WHERE symbol = ? AND open_time >= ? AND open_time < ?
            GROUP BY bucket
            ORDER BY bucket
            """;

    private final KlineRepository klineRepository;
    private final IntervalParser intervalParser;
    private final JdbcTemplate jdbcTemplate;

    public KlineQueryService(KlineRepository klineRepository, IntervalParser intervalParser,
            JdbcTemplate jdbcTemplate) {
        this.klineRepository = klineRepository;
        this.intervalParser = intervalParser;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KlineDto> getKlines(String symbol, String interval, Instant from, Instant to) {
        intervalParser.parse(interval); // validates + rejects unsupported intervals

        if (intervalParser.isBaseInterval(interval)) {
            return klineRepository.findBySymbolAndOpenTimeBetweenOrderByOpenTimeAsc(symbol, from, to)
                    .stream()
                    .map(this::toDto)
                    .toList();
        }

        Duration duration = intervalParser.parse(interval);
        String intervalLiteral = duration.toSeconds() + " seconds";

        return jdbcTemplate.query(ROLLUP_SQL,
                (rs, rowNum) -> new KlineDto(
                        rs.getTimestamp("bucket").toInstant(),
                        rs.getBigDecimal("open_price"),
                        rs.getBigDecimal("high_price"),
                        rs.getBigDecimal("low_price"),
                        rs.getBigDecimal("close_price"),
                        rs.getBigDecimal("volume"),
                        rs.getBigDecimal("quote_volume"),
                        rs.getInt("trade_count")),
                intervalLiteral, symbol, Timestamp.from(from), Timestamp.from(to));
    }

    private KlineDto toDto(Kline k) {
        return new KlineDto(k.getOpenTime(), k.getOpenPrice(), k.getHighPrice(), k.getLowPrice(),
                k.getClosePrice(), k.getVolume(), k.getQuoteVolume(), k.getTradeCount());
    }
}