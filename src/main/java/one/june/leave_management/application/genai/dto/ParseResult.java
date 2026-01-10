package one.june.leave_management.application.genai.dto;

import java.util.Map;

/**
 * Result object containing the parsed leave request along with metadata
 * about the parsing process, including confidence scores and raw extracted data.
 */
public class ParseResult {

    private ParsedLeaveRequest parsedRequest;
    private double confidenceScore;
    private boolean isSuccess;
    private String errorMessage;
    private Map<String, Object> rawData;
    private String modelUsed;
    private long processingTimeMs;

    public ParseResult() {
        // Default constructor
    }

    private ParseResult(Builder builder) {
        this.parsedRequest = builder.parsedRequest;
        this.confidenceScore = builder.confidenceScore;
        this.isSuccess = builder.isSuccess;
        this.errorMessage = builder.errorMessage;
        this.rawData = builder.rawData;
        this.modelUsed = builder.modelUsed;
        this.processingTimeMs = builder.processingTimeMs;
    }

    public ParsedLeaveRequest getParsedRequest() {
        return parsedRequest;
    }

    public void setParsedRequest(ParsedLeaveRequest parsedRequest) {
        this.parsedRequest = parsedRequest;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, Object> rawData) {
        this.rawData = rawData;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ParsedLeaveRequest parsedRequest;
        private double confidenceScore = 0.0;
        private boolean isSuccess = false;
        private String errorMessage;
        private Map<String, Object> rawData;
        private String modelUsed;
        private long processingTimeMs = 0;

        public Builder parsedRequest(ParsedLeaveRequest parsedRequest) {
            this.parsedRequest = parsedRequest;
            return this;
        }

        public Builder confidenceScore(double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder isSuccess(boolean isSuccess) {
            this.isSuccess = isSuccess;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder rawData(Map<String, Object> rawData) {
            this.rawData = rawData;
            return this;
        }

        public Builder modelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public ParseResult build() {
            return new ParseResult(this);
        }

        public Builder success() {
            this.isSuccess = true;
            return this;
        }

        public Builder failure(String errorMessage) {
            this.isSuccess = false;
            this.errorMessage = errorMessage;
            return this;
        }
    }
}
