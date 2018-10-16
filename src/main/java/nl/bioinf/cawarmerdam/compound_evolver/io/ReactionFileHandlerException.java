package nl.bioinf.cawarmerdam.compound_evolver.io;

public class ReactionFileHandlerException extends Exception {
    private final String fileName;

    ReactionFileHandlerException(String message, String fileName) {
        super(message);
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
