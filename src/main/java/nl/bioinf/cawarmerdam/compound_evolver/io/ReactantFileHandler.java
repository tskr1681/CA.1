/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.apache.commons.io.IOUtils;

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
public final class ReactantFileHandler {
    /**
     * Method that loads molecules from set of filenames.
     *
     * @param filenames The files that should be loaded.
     * @return lists of molecules in a list.
     * @throws ReactantFileHandlingException If a reactant file could not be read.
     * @throws ReactantFileFormatException If a erroneous reactant format was encountered.
     */
    public static List<List<Molecule>> loadMolecules(String[] filenames) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (String fileName : filenames) {
            File initialFile = new File(fileName);
            try {
                // Read file by file.
                reactantLists.add(readSmileFile(new FileInputStream(initialFile), fileName));
            } catch (FileNotFoundException e) {
                throw new ReactantFileHandlingException(e.getMessage(), fileName);
            }
        }
        return reactantLists;
    }

    /**
     * Method responsible for loading reaction file parts as a list of reactant lists.
     *
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
     *
     * @param inputStream, starting with a SMILES string on each line.
     * @param fileName, name of the file that is read.
     * @return a list of reactants (molecules).
     * @throws ReactantFileHandlingException if an IO exception is encountered.
     * @throws ReactantFileFormatException if the molecules could not be read due to a formatting problem.
     */
    private static List<Molecule> readSmileFile(InputStream inputStream, String fileName) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<Molecule> moleculeMap = new ArrayList<>();

        /*
         * Create byte array with the content of the file
         */
        byte[] fileContent = getFileContent(inputStream, fileName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContent),
                getCorrectCharset(fileContent)))) {
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

    /**
     * Method that returns the file contents in a byte array.
     *
     * @param inputStream The input stream of the file.
     * @param fileName The filename to read in.
     * @return The byte array.
     * @throws ReactantFileHandlingException If the input stream cannot be copied to a byte array output stream.
     */
    private static byte[] getFileContent(InputStream inputStream, String fileName) throws ReactantFileHandlingException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new ReactantFileHandlingException(e.getMessage(), fileName);
        }
        return outputStream.toByteArray();
    }

    /**
     * Method that returns the charset that will be used for reading a file.
     *
     * @param fileContent The file contents in a byte array.
     * @return The name of the char set.
     */
    private static String getCorrectCharset(byte[] fileContent) {
        String charset = "UTF-8"; //Default char set

        CharsetDetector detector = new CharsetDetector();
        detector.setText(fileContent);

        CharsetMatch cm = detector.detect();

        if (cm != null) {
            int confidence = cm.getConfidence();
            // Only in case the confidence is above 99%, the detected charset is returned.
            if (confidence > 99) {
                charset = cm.getName();
            }
        }
        return charset;
    }
}

