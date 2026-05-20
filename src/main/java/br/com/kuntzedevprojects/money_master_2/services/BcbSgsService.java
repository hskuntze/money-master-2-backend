package br.com.kuntzedevprojects.money_master_2.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import br.com.kuntzedevprojects.money_master_2.config.properties.BcbSgsProperties;
import br.com.kuntzedevprojects.money_master_2.entities.BcbDailyRate;
import br.com.kuntzedevprojects.money_master_2.exceptions.BusinessException;
import br.com.kuntzedevprojects.money_master_2.repositories.BcbDailyRateRepository;

@Service
public class BcbSgsService {

    private static final DateTimeFormatter BCB_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RestClient restClient;
    private final BcbSgsProperties properties;
    private final BcbDailyRateRepository rateRepository;

    public BcbSgsService(RestClient.Builder restClientBuilder, BcbSgsProperties properties, BcbDailyRateRepository rateRepository) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.rateRepository = rateRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DailyRate> getCdiRates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Data inicial e data final são obrigatórias para consultar CDI.");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("A data inicial do CDI não pode ser posterior à data final.");
        }

        try {
            fetchAndStoreRates(properties.getCdiSeriesCode(), from, to);
        } catch (Exception ex) {
            // Mantém o sistema utilizável com as taxas já cacheadas em banco quando a API do BCB estiver indisponível.
        }
        return rateRepository
                .findBySeriesCodeAndReferenceDateBetweenOrderByReferenceDateAsc(properties.getCdiSeriesCode(), from, to)
                .stream()
                .map(rate -> new DailyRate(rate.getReferenceDate(), rate.getValue()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DailyRate getLatestCdiRateUntil(LocalDate date) {
        LocalDate reference = date == null ? LocalDate.now() : date;
        LocalDate from = reference.minusDays(Math.max(properties.getLatestValuesLimit(), 1) * 2L);
        try {
            fetchAndStoreRates(properties.getCdiSeriesCode(), from, reference);
        } catch (Exception ex) {
            // Mantém o sistema utilizável com a última taxa cacheada em banco quando a API do BCB estiver indisponível.
        }

        return rateRepository
                .findTopBySeriesCodeAndReferenceDateLessThanEqualOrderByReferenceDateDesc(properties.getCdiSeriesCode(), reference)
                .map(rate -> new DailyRate(rate.getReferenceDate(), rate.getValue()))
                .orElseThrow(() -> new BusinessException("Não foi possível obter a taxa CDI mais recente no Banco Central."));
    }

    private void fetchAndStoreRates(Integer seriesCode, LocalDate from, LocalDate to) {
        String url = properties.getBaseUrl() + "." + seriesCode
                + "/dados?formato=json&dataInicial={dataInicial}&dataFinal={dataFinal}";

        BcbSgsValue[] values = restClient.get()
                .uri(url, from.format(BCB_DATE_FORMATTER), to.format(BCB_DATE_FORMATTER))
                .retrieve()
                .body(BcbSgsValue[].class);

        if (values == null || values.length == 0) {
            return;
        }

        Arrays.stream(values).forEach(value -> upsertRate(seriesCode, value));
    }

    private void upsertRate(Integer seriesCode, BcbSgsValue value) {
        LocalDate referenceDate = LocalDate.parse(value.data(), BCB_DATE_FORMATTER);
        BigDecimal rateValue = new BigDecimal(value.valor());

        BcbDailyRate rate = rateRepository
                .findBySeriesCodeAndReferenceDate(seriesCode, referenceDate)
                .orElseGet(BcbDailyRate::new);
        rate.setSeriesCode(seriesCode);
        rate.setReferenceDate(referenceDate);
        rate.setValue(rateValue);
        rateRepository.save(rate);
    }

    private record BcbSgsValue(String data, String valor) {
    }

    public record DailyRate(LocalDate referenceDate, BigDecimal value) {
    }
}
