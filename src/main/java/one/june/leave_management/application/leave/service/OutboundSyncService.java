package one.june.leave_management.application.leave.service;

import one.june.leave_management.domain.leave.model.Leave;
import one.june.leave_management.domain.leave.model.SourceType;

public interface OutboundSyncService {
    void sync(Leave leave, SourceType originatingSource);
}