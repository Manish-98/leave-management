package one.june.leave_management.adapter.inbound.web;

import jakarta.validation.Valid;
import one.june.leave_management.adapter.inbound.web.dto.LeaveFetchQuery;
import one.june.leave_management.adapter.inbound.web.dto.LeaveIngestionRequest;
import one.june.leave_management.application.leave.command.LeaveIngestionCommand;
import one.june.leave_management.application.leave.dto.LeaveDto;
import one.june.leave_management.application.leave.service.LeaveService;
import one.june.leave_management.common.annotation.Auditable;
import one.june.leave_management.common.mapper.LeaveMapper;
import one.june.leave_management.common.model.Quarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    private final LeaveService leaveService;
    private final LeaveMapper leaveMapper;

    public LeaveController(LeaveService leaveService,
                           LeaveMapper leaveMapper) {
        this.leaveService = leaveService;
        this.leaveMapper = leaveMapper;
    }

    @PostMapping("/ingest")
    @Auditable("Leave ingestion endpoint")
    public ResponseEntity<LeaveDto> ingestLeave(@Valid @RequestBody LeaveIngestionRequest request) {
        logger.info("Received leave ingestion request: {}", request);
        LeaveIngestionCommand command = leaveMapper.toCommand(request, request.getSourceType(), request.getSourceId());
        LeaveDto result = leaveService.ingest(command);
        logger.info("Successfully ingested leave with id: {}", result.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping
    @Auditable("Fetch leaves endpoint")
    public ResponseEntity<Page<LeaveDto>> fetchLeaves(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Quarter quarter,
            @PageableDefault(size = 20) Pageable pageable) {
        logger.info("Fetching leaves with filters - userId: {}, year: {}, quarter: {}, pageable: {}",
                    userId, year, quarter, pageable);

        // Validate that quarter is only used with year
        if (quarter != null && year == null) {
            throw new IllegalArgumentException("Quarter filter requires year parameter to be specified");
        }

        LeaveFetchQuery query = LeaveFetchQuery.builder()
                .userId(userId)
                .year(year)
                .quarter(quarter)
                .build();

        Page<LeaveDto> result = leaveService.fetchLeaves(query, pageable);

        logger.info("Successfully fetched {} leaves (page {} of {})",
                    result.getNumberOfElements(),
                    result.getNumber() + 1,
                    result.getTotalPages());

        return ResponseEntity.ok(result);
    }
}