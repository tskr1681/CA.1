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

    public static List<List<Molecule>> loadMolecules(List<Part> fileParts) throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (Part filePart : fileParts) {
            String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
            System.out.println("fileName = " + fileName);
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

