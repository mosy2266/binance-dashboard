package reservation.binancebackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import reservation.binancebackend.entity.Ticker24hStat;

public interface TickerRepository extends JpaRepository<Ticker24hStat, String> {
}