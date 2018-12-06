package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ReactionFileHandler {
    public static Reactor loadReaction(String filePath) throws FileNotFoundException, ReactionFileHandlerException {
        File initialFile = new File(filePath);
        return loadReactionFromInputStream(new FileInputStream(initialFile), filePath);
    }

    public static Reactor loadReaction(Part filePart) throws IOException, ReactionFileHandlerException {
        return loadReactionFromInputStream(filePart.getInputStream(),
                Paths.get(filePart.getSubmittedFileName()).getFileName().toString());
    }

    private static Reactor loadReactionFromInputStream(InputStream inputStream, String fileName) throws ReactionFileHandlerException {
        Reactor reactor = new Reactor();
        try {
            MolImporter importer = new MolImporter(inputStream);
            reactor.setReaction(importer.read());
            importer.close();
        } catch (IOException | ReactionException exception) {
            throw new ReactionFileHandlerException(exception.getMessage(), fileName);
        }
        return reactor;
    }

    public static List<Reactor> loadReactions(List<Part> reactionFileParts)
            throws IOException, ReactionFileHandlerException {
        List<Reactor> reactantLists = new ArrayList<>();
        for (Part filePart : reactionFileParts) {
            reactantLists.add(loadReaction(filePart));
        }
        return reactantLists;
    }
}
