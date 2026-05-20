package br.com.kuntzedevprojects.money_master_2.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.kuntzedevprojects.money_master_2.entities.BcbDailyRate;

public interface BcbDailyRateRepository extends JpaRepository<BcbDailyRate, Long> {

    Optional<BcbDailyRate> findBySeriesCodeAndReferenceDate(Integer seriesCode, LocalDate referenceDate);

    List<BcbDailyRate> findBySeriesCodeAndReferenceDateBetweenOrderByReferenceDateAsc(Integer seriesCode, LocalDate from, LocalDate to);

    Optional<BcbDailyRate> findTopBySeriesCodeAndReferenceDateLessThanEqualOrderByReferenceDateDesc(Integer seriesCode, LocalDate referenceDate);
}
