package reservation.binancebackend.repository;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import reservation.binancebackend.entity.Kline;

@Repository
public class KlineUpsertDao {

    private static final String UPSERT_SQL = """
            INSERT INTO kline_1m
                (symbol, open_time, close_time, open_price, high_price,
                 low_price, close_price, volume, quote_volume, trade_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (symbol, open_time) DO UPDATE SET
                close_time = EXCLUDED.close_time,
                open_price = EXCLUDED.open_price,
                high_price = EXCLUDED.high_price,
                low_price = EXCLUDED.low_price,
                close_price = EXCLUDED.close_price,
                volume = EXCLUDED.volume,
                quote_volume = EXCLUDED.quote_volume,
                trade_count = EXCLUDED.trade_count
            """;

    private final JdbcTemplate jdbcTemplate;

    public KlineUpsertDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertBatch(List<Kline> klines) {
        if (klines.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(UPSERT_SQL, klines, klines.size(), (ps, k) -> {
            ps.setString(1, k.getSymbol());
            ps.setTimestamp(2, Timestamp.from(k.getOpenTime()));
            ps.setTimestamp(3, Timestamp.from(k.getCloseTime()));
            ps.setBigDecimal(4, k.getOpenPrice());
            ps.setBigDecimal(5, k.getHighPrice());
            ps.setBigDecimal(6, k.getLowPrice());
            ps.setBigDecimal(7, k.getClosePrice());
            ps.setBigDecimal(8, k.getVolume());
            ps.setBigDecimal(9, k.getQuoteVolume());
            ps.setInt(10, k.getTradeCount());
        });
    }

    public void upsert(Kline kline) {
        upsertBatch(List.of(kline));
    }
}