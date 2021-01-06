/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.model.pipeline;

import chemaxon.struc.Molecule;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.util.ConformerHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A pipeline step that handles scored candidates.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ScoredCandidateHandlingStep implements PipelineStep<Candidate, Void> {
    /**
     * Executes the scored candidate handling step.
     *
     * @param candidate The candidate that is scored.
     * @return void
     * @throws PipelineException if the candidates conformers could not be handled.
     */
    @Override
    public Void execute(Candidate candidate) throws PipelineException {
        if (candidate == null || candidate.getConformerScores() == null) {
            throw new PipelineException("Scored candidate handling step got null, validation failed?");
        }
        List<Double> conformerScores = candidate.getConformerScores();
        Path outputFilePath = candidate.getScoredConformersFile();
        // Declare default score
        if (conformerScores.size() > 0) {
            List<Molecule> conformers = new ArrayList<>();
            for (int i = 0; i < conformerScores.size(); i++) {
                Molecule conformer = ConformerHelper.getConformer(outputFilePath, i);
                conformers.add(conformer);
            }
            // Get best conformer.
            double score = Collections.min(conformerScores);
            Molecule bestConformer = conformers.get(conformerScores.indexOf(score));
            if (bestConformer == null) {
                throw new PipelineException("Scored candidate handling could not get best conformer!");
            }
            List<Molecule> conformerAsList = new ArrayList<>();
            conformerAsList.add(bestConformer);
            ConformerHelper.exportConformers(outputFilePath.resolveSibling("best-conformer.sdf"), conformerAsList);
            if (candidate.getColor() != null) {
                switch (candidate.getColor()) {
                    case RED:
                        throw new PipelineException("Candidate color was red, not using candidate.");
                    case YELLOw:
                        score /= 0.35;
                        break;
                    case GREEN:
                        score /= 0.75;
                }
            }
            candidate.setRawScore(score);
        }
        return null;
    }
}
