/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.control;

import chemaxon.formats.MolExporter;
import chemaxon.formats.MolImporter;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.RandomCompoundReactor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class CompoundEvolver {
    private List<Molecule> reactionProducts;

    private CompoundEvolver(List<List<Molecule>> reactantLists, Reactor reactor, int maxProducts) {
        reactionProducts = new RandomCompoundReactor(reactor, maxProducts).execute(reactantLists);
    }

    /**
     * Evolve compounds
     */
    private void evolve() {
        try {
            for (int i = 0; i < reactionProducts.size(); i++) {
                Molecule reactionProduct = reactionProducts.get(i);
                String smilesMolecule = MolExporter.exportToFormat(reactionProduct, "smiles");
//                generateThreeDimensionalMolecule(smilesMolecule, i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * main for cli
     * @param args An array of strings being the command line input
     */
    public static void main(String[] args) throws Exception {
        // Load reactor from argument
        Reactor reactor = CompoundEvolver.loadReaction(args[0]);
        // Get maximum amount of product
        int maxSamples = Integer.parseInt(args[args.length - 1]);
        // Load molecules
        String[] reactantFiles = Arrays.copyOfRange(args, 1, args.length - 1);
        List<List<Molecule>> reactantLists = CompoundEvolver.loadMolecules(reactantFiles);
        // Create new CompoundEvolver
        CompoundEvolver compoundEvolver = new CompoundEvolver(reactantLists, reactor, maxSamples);
    }

    private static Reactor loadReaction(String filename) throws Exception {
        Reactor reactor = new Reactor();
        try {
            MolImporter importer = new MolImporter(filename);
            reactor.setReaction(importer.read());
            importer.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return reactor;
    }

    private static List<List<Molecule>> loadMolecules(String[] filenames) throws Exception {
        List<List<Molecule>> reactantLists = new ArrayList<>();
        for (String i : filenames) {
            List<Molecule> moleculeMap = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(i));
                for (String line; (line = br.readLine()) != null; ) {
                    try {
                        moleculeMap.add(MolImporter.importMol(line));
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
                reactantLists.add(moleculeMap);
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
        return reactantLists;
    }
}
