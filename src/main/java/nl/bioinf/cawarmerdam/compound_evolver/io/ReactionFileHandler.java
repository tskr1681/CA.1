/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolImporter;
import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public final class ReactionFileHandler {
    /**
     * Method that loads a reaction in a given file.
     *
     * @param filePath The path to the file that holds the reaction.
     * @return A Reactor reaction object.
     * @throws FileNotFoundException if the file could not be found.
     * @throws ReactionFileHandlerException if an exception is encountered while handling the reaction file.
     */
    public static Reactor loadReaction(String filePath) throws FileNotFoundException, ReactionFileHandlerException {
        File initialFile = new File(filePath);
        return loadReactionFromInputStream(new FileInputStream(initialFile), filePath);
    }

    /**
     * Method that loads a reaction from a servlet file part.
     *
     * @param filePart The part that holds the reaction.
     * @return the Reactor reaction.
     * @throws IOException if an IO related exception occurred.
     * @throws ReactionFileHandlerException if an exception occurred while handling the reaction file.
     */
    private static Reactor loadReaction(Part filePart) throws ReactionFileHandlerException, IOException {
        return loadReactionFromInputStream(filePart.getInputStream(),
                Paths.get(filePart.getSubmittedFileName()).getFileName().toString());
    }

    /**
     * Method that loads a reaction from an input stream.
     *
     * @param inputStream The input stream that holds the reaction.
     * @param fileName The name of the file corresponding to the input stream.
     * @return the Reactor reaction.
     * @throws ReactionFileHandlerException if an exception was encountered while handling the files.
     */
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

    /**
     * Method that loads multiple reactions from servlet file parts.
     *
     * @param reactionFileParts The parts that hold the reactions.
     * @return a list of Reactor reactions.
     * @throws IOException if an IO related exception occurred.
     * @throws ReactionFileHandlerException if an exception occurred while handling a reaction file.
     */
    public static List<Reactor> loadReactions(List<Part> reactionFileParts)
            throws IOException, ReactionFileHandlerException {
        List<Reactor> reactantLists = new ArrayList<>();
        for (Part filePart : reactionFileParts) {
            reactantLists.add(loadReaction(filePart));
        }
        return reactantLists;
    }
}
