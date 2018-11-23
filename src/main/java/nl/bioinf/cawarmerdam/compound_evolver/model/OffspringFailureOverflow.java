package nl.bioinf.cawarmerdam.compound_evolver.model;

import java.util.List;

public class OffspringFailureOverflow extends Exception {
    private List<String> offspringRejectionMessages;

    public OffspringFailureOverflow(String message, List<String> offspringRejectionMessages) {
        super(message);
        this.offspringRejectionMessages = offspringRejectionMessages;
    }

    public List<String> getOffspringRejectionMessages() {
        return offspringRejectionMessages;
    }
}
