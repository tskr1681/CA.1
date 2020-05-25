/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import nl.bioinf.cawarmerdam.compound_evolver.util.ReactantFilter;
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
     * @throws ReactantFileFormatException   If a erroneous reactant format was encountered.
     */
    public static List<List<String>> loadMolecules(String[] filenames, double weight) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<String>> reactantLists = new ArrayList<>();
        for (String fileName : filenames) {
            File initialFile = new File(fileName);
            try {
                if (weight == 0) {
                    // Read file by file.
                    reactantLists.add(readSmileFile(new FileInputStream(initialFile), fileName));
                } else {
                    List<String> filtered = ReactantFilter.filterByWeight(readSmileFile(new FileInputStream(initialFile), fileName), weight);
                    if (filtered.size() < 3) {
                        throw new ReactantFileHandlingException("Filtering removed too many items, algorithm can not run.", fileName);
                    }
                    reactantLists.add(filtered);
                }
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
     * @throws ReactantFileFormatException   if a SMILES molecule could not be read.
     */
    public static List<List<String>> loadMolecules(List<Part> fileParts, double weight, String[] smarts_filters) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<String>> reactantLists = new ArrayList<>();
        for (Part filePart : fileParts) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            try {
                if (weight == 0) {
                    List<String> filtered = ReactantFilter.filterBySmarts(readSmileFile(
                            filePart.getInputStream(),
                            fileName), smarts_filters);
                    if (filtered.size() < 2) {
                        throw new ReactantFileHandlingException("Filtering removed too many items, algorithm can not run.", fileName);
                    }
                    reactantLists.add(filtered);
                } else {
                    List<String> filtered = ReactantFilter.filterByWeight(readSmileFile(filePart.getInputStream(), fileName), weight);
                    filtered = ReactantFilter.filterBySmarts(filtered, smarts_filters);
                    if (filtered.size() < 2) {
                        throw new ReactantFileHandlingException("Filtering removed too many items, algorithm can not run.", fileName);
                    }
                    reactantLists.add(filtered);
                }
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
     * @param fileName,    name of the file that is read.
     * @return a list of reactants (molecules).
     * @throws ReactantFileHandlingException if an IO exception is encountered.
     * @throws ReactantFileFormatException   if the molecules could not be read due to a formatting problem.
     */
    private static List<String> readSmileFile(InputStream inputStream, String fileName) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<String> moleculeList = new ArrayList<>();

        /*
         * Create byte array with the content of the file
         */
        byte[] fileContent = getFileContent(inputStream, fileName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(fileContent),
                getCorrectCharset(fileContent)))) {
            int lineNumber = 0;
            for (String line; (line = reader.readLine()) != null; lineNumber++) {
                // Don't bother with lines with multiple molecules
                if (line.contains("."))
                    continue;
                try {
                    moleculeList.add(MolImporter.importMol(line).toFormat("smiles"));
                } catch (MolFormatException e) {
                    throw new ReactantFileFormatException(e.getMessage(), lineNumber, fileName);
                }
            }
        } catch (IOException e) {
            throw new ReactantFileHandlingException(e.getMessage(), fileName);
        }
        return moleculeList;
    }

    /**
     * Method that returns the file contents in a byte array.
     *
     * @param inputStream The input stream of the file.
     * @param fileName    The filename to read in.
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

