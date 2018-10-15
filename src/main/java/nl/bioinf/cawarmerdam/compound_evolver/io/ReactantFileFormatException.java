package nl.bioinf.cawarmerdam.compound_evolver.io;

public class ReactantFileFormatException extends Exception {
    private final int lineNumber;
    private final String fileName;

    public ReactantFileFormatException(String message, int lineNumber, String fileName) {
        super(message);
        this.lineNumber = lineNumber;
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getFileName() {
        return fileName;
    }
}
