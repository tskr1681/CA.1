package nl.bioinf.cawarmerdam.compound_evolver.io;

public class ReactantFileHandlingException extends Exception {
    private String fileName;

    ReactantFileHandlingException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
