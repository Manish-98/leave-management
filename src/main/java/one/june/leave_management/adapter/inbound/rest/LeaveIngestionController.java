package one.june.leave_management.adapter.inbound.rest;

import jakarta.validation.Valid;
import one.june.leave_management.adapter.inbound.rest.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveIngestionService;
import one.june.leave_management.common.mapper.LeaveMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveIngestionController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveIngestionController.class);

    private final LeaveIngestionService leaveIngestionService;
    private final LeaveMapper leaveMapper;

    public LeaveIngestionController(LeaveIngestionService leaveIngestionService, LeaveMapper leaveMapper) {
        this.leaveIngestionService = leaveIngestionService;
        this.leaveMapper = leaveMapper;
    }

    @PostMapping("/ingest")
    public ResponseEntity<LeaveDto> ingestLeave(@Valid @RequestBody LeaveIngestionRequest request) {
        logger.info("Received leave ingestion request: {}", request);
        LeaveIngestionCommand command = leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId());
        LeaveDto result = leaveIngestionService.ingest(command);
        logger.info("Successfully ingested leave with id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}