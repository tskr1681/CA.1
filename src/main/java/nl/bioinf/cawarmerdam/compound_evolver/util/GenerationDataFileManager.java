package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Class for outputting a file with generation data (candidate ids and positions per generation) to a txt file
 */
public class GenerationDataFileManager {
    private FileWriter writer;

    public GenerationDataFileManager(File path) throws IOException {
        this.writer = new FileWriter(path);
    }

    private String getCandidateId(Candidate c) {
        return c.getConformersFile().getParent().getFileName().toString();
    }

    public void writeGeneration(Population population) throws IOException {
        this.writer.write("Generation: " + population.getGenerationNumber() + "\n");
        List<Candidate> sortedList = MultiReceptorHelper.getCandidatesWithFitness(population.getCandidateList(), population.isSelective());
        sortedList.sort(Candidate::compareTo);
        for (int i = 0; i < sortedList.size(); i++) {
            this.writer.write(i + ": " + getCandidateId(sortedList.get(i)) + "; Fitness: " + sortedList.get(i).getFitness() + "\n");
        }
        this.writer.flush();
    }

    public void close() {
        try {
            this.writer.close();
        }catch(Exception ignored) {

        }
    }
}
