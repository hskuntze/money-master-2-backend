package br.com.kuntzedevprojects.money_master_2.config.bootstrap;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.kuntzedevprojects.money_master_2.config.properties.SavingsJarYieldProperties;
import br.com.kuntzedevprojects.money_master_2.services.SavingsJarYieldService;

@Component
public class SavingsJarYieldStartupRunner {

    private static final Logger logger = LoggerFactory.getLogger(SavingsJarYieldStartupRunner.class);

    private final SavingsJarYieldService savingsJarYieldService;
    private final SavingsJarYieldProperties properties;

    public SavingsJarYieldStartupRunner(SavingsJarYieldService savingsJarYieldService, SavingsJarYieldProperties properties) {
        this.savingsJarYieldService = savingsJarYieldService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applyPendingSavingsJarYieldsOnStartup() {
        if (!properties.isApplyOnStartup()) {
            return;
        }

        try {
            LocalDate today = LocalDate.now(ZoneId.of(properties.getZoneId()));
            var results = savingsJarYieldService.applyPendingYieldsForAll(today);
            int totalMovements = results.stream().mapToInt(result -> result.createdYieldMovements() == null ? 0 : result.createdYieldMovements()).sum();
            logger.info("Savings jar startup yield calculation finished. jars={}, movements={}", results.size(), totalMovements);
        } catch (Exception ex) {
            logger.warn("Could not apply pending savings jar yields on startup: {}", ex.getMessage(), ex);
            if (properties.isFailStartupOnError()) {
                throw ex;
            }
        }
    }
}
