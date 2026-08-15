package reservation.binancebackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import reservation.binancebackend.entity.Kline;

public interface KlineRepository extends JpaRepository<Kline, Long> {

    @Query("select max(k.openTime) from Kline k where k.symbol = :symbol")
    Optional<Instant> findLastOpenTime(@Param("symbol") String symbol);

    List<Kline> findBySymbolAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol, Instant from, Instant to);
}