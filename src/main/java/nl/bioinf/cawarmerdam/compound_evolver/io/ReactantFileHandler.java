package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ReactantFileHandler {
    public static List<List<Molecule>> loadMolecules(String[] filenames) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (String fileName : filenames) {
            File initialFile = new File(fileName);
            try {
                reactantLists.add(readSmileFile(new FileInputStream(initialFile), fileName));
            } catch (FileNotFoundException e) {
                throw new ReactantFileHandlingException(e.getMessage(), fileName);
            }
        }
        return reactantLists;
    }

    /**
     * Method responsible for loading reaction file parts as a list of reactant lists.
     * @param fileParts, files as parts that starts with a SMILES string on each line.
     * @return a list of reactant lists.
     * @throws ReactantFileHandlingException if an IO exception is encountered.
     * @throws ReactantFileFormatException if a SMILES molecule could not be read.
     */
    public static List<List<Molecule>> loadMolecules(List<Part> fileParts) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (Part filePart : fileParts) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            try {
                reactantLists.add(readSmileFile(
                        filePart.getInputStream(),
                        fileName));
            } catch (IOException e) {
                throw new ReactantFileHandlingException(e.getMessage(), fileName);
            }
        }
        return reactantLists;
    }

    /**
     * Method responsible for reading a reactants file from an inputStream.
     * @param inputStream, starting with a SMILES string on each line.
     * @param fileName, name of the file that is read.
     * @return a list of reactants (molecules).
     * @throws ReactantFileHandlingException if an IO exception is encountered.
     * @throws ReactantFileFormatException if the molecules could not be read due to a formatting problem.
     */
    private static List<Molecule> readSmileFile(InputStream inputStream, String fileName) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<Molecule> moleculeMap = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
            int lineNumber = 0;
            for (String line; (line = reader.readLine()) != null; lineNumber++) {
                try {
                    moleculeMap.add(MolImporter.importMol(line));
                } catch (MolFormatException e) {
                    throw new ReactantFileFormatException(e.getMessage(), lineNumber, fileName);
                }
            }
        } catch (IOException e) {
            throw new ReactantFileHandlingException(e.getMessage(), fileName);
        }
        return moleculeMap;
    }
}

