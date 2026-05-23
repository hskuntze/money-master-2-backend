package br.com.kuntzedevprojects.money_master_2.controllers;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.kuntzedevprojects.money_master_2.dtos.admin.AccessLogResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.FailureLogDetailResponse;
import br.com.kuntzedevprojects.money_master_2.dtos.admin.FailureLogResponse;
import br.com.kuntzedevprojects.money_master_2.services.AdminLogService;

@RestController
@RequestMapping("/admin/logs")
@PreAuthorize("hasAuthority('LOG_READ')")
public class AdminLogController {

    private final AdminLogService adminLogService;

    public AdminLogController(AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @GetMapping("/access")
    public ResponseEntity<Page<AccessLogResponse>> accessLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String principal,
            @PageableDefault(size = 25, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminLogService.accessLogs(from, to, path, statusCode, principal, pageable));
    }

    @GetMapping("/failures")
    public ResponseEntity<Page<FailureLogResponse>> failureLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) String principal,
            @PageableDefault(size = 25, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminLogService.failureLogs(from, to, path, statusCode, principal, pageable));
    }

    @GetMapping("/failures/{id}")
    public ResponseEntity<FailureLogDetailResponse> failureLog(@PathVariable Long id) {
        return ResponseEntity.ok(adminLogService.failureLog(id));
    }
}
