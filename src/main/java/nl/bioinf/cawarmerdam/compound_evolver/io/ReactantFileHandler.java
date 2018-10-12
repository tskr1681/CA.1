package nl.bioinf.cawarmerdam.compound_evolver.io;

import chemaxon.formats.MolFormatException;
import chemaxon.formats.MolImporter;
import chemaxon.struc.Molecule;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public final class ReactantFileHandler {
    public static List<List<Molecule>> loadMolecules(String[] filenames) throws FileNotFoundException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (String filename : filenames) {
            File initialFile = new File(filename);
            reactantLists.add(readSmileFile(new FileInputStream(initialFile)));
        }
        return reactantLists;
    }

    public static List<List<Molecule>> loadMolecules(Part[] fileParts) throws IOException {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (Part filePart : fileParts) {
            reactantLists.add(readSmileFile(filePart.getInputStream()));
        }
        return reactantLists;
    }

    private static List<Molecule> readSmileFile(InputStream inputStream) {
        List<Molecule> moleculeMap = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charset.defaultCharset()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                try {
                    moleculeMap.add(MolImporter.importMol(line));
                } catch (MolFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return moleculeMap;
    }
}
