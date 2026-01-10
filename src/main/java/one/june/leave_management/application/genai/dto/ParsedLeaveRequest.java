package one.june.leave_management.application.genai.dto;

import lombok.Data;
import lombok.ToString;
import one.june.leave_management.domain.leave.model.LeaveDurationType;
import one.june.leave_management.domain.leave.model.LeaveType;

import java.time.LocalDate;

/**
 * Data Transfer Object representing a leave request parsed from natural language.
 * Contains all necessary fields to create a leave request after AI-powered parsing.
 */
@ToString
public class ParsedLeaveRequest {

    private LocalDate startDate;
    private LocalDate endDate;
    private LeaveDurationType durationType;
    private LeaveType leaveType;
    private String reason;
    private boolean isOptionalHoliday;
    private String optionalHolidayName;
    private String slackUserId;

    public ParsedLeaveRequest() {
        // Default constructor
    }

    private ParsedLeaveRequest(Builder builder) {
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.durationType = builder.durationType;
        this.leaveType = builder.leaveType;
        this.reason = builder.reason;
        this.isOptionalHoliday = builder.isOptionalHoliday;
        this.optionalHolidayName = builder.optionalHolidayName;
        this.slackUserId = builder.slackUserId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LeaveDurationType getDurationType() {
        return durationType;
    }

    public void setDurationType(LeaveDurationType durationType) {
        this.durationType = durationType;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isOptionalHoliday() {
        return isOptionalHoliday;
    }

    public void setOptionalHoliday(boolean optionalHoliday) {
        isOptionalHoliday = optionalHoliday;
    }

    public String getOptionalHolidayName() {
        return optionalHolidayName;
    }

    public void setOptionalHolidayName(String optionalHolidayName) {
        this.optionalHolidayName = optionalHolidayName;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public void setSlackUserId(String slackUserId) {
        this.slackUserId = slackUserId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LocalDate startDate;
        private LocalDate endDate;
        private LeaveDurationType durationType = LeaveDurationType.FULL_DAY;
        private LeaveType leaveType = LeaveType.ANNUAL_LEAVE;
        private String reason;
        private boolean isOptionalHoliday = false;
        private String optionalHolidayName;
        private String slackUserId;

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder durationType(LeaveDurationType durationType) {
            this.durationType = durationType;
            return this;
        }

        public Builder leaveType(LeaveType leaveType) {
            this.leaveType = leaveType;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder isOptionalHoliday(boolean isOptionalHoliday) {
            this.isOptionalHoliday = isOptionalHoliday;
            return this;
        }

        public Builder optionalHolidayName(String optionalHolidayName) {
            this.optionalHolidayName = optionalHolidayName;
            return this;
        }

        public Builder slackUserId(String slackUserId) {
            this.slackUserId = slackUserId;
            return this;
        }

        public ParsedLeaveRequest build() {
            return new ParsedLeaveRequest(this);
        }
    }
}
