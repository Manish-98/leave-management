package one.june.leave_management.adapter.inbound.web.csv;

public class CsvValidationException extends RuntimeException {

    private final int lineNumber;

    public CsvValidationException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }

    public CsvValidationException(String message, int lineNumber, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
