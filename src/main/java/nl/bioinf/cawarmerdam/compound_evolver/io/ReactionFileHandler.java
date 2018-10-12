package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Paths;

public final class ReactionFileHandler {
    public static Reactor loadReaction(String filePath) throws FileNotFoundException {
        File initialFile = new File(filePath);
        return loadReactionFromInputStream(new FileInputStream(initialFile), filePath);
    }

    public static Reactor loadReaction(Part filePart) throws IOException {
        return loadReactionFromInputStream(filePart.getInputStream(),
                Paths.get(filePart.getSubmittedFileName()).getFileName().toString());
    }

    private static Reactor loadReactionFromInputStream(InputStream inputStream, String fileName) {
        Reactor reactor = new Reactor();
        try {
            MolImporter importer = new MolImporter(inputStream);
            reactor.setReaction(importer.read());
            importer.close();
        } catch (IOException | ReactionException exception) {
            throw new IllegalArgumentException(
                    String.format("Could not read file '%s': %s", fileName, exception.toString()));
        }
        return reactor;
    }
}
