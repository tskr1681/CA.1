package nl.bioinf.cawarmerdam.compound_evolver.io;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RequestParameterStorage {
    public static void storeParameters(File out, HttpServletRequest request) {
        try {
            if (out.createNewFile()) {
                FileWriter writer = new FileWriter(out);
                BufferedWriter buffer = new BufferedWriter(writer);
                buffer.write("Building Blocks And Reaction Used (file names only):");
                buffer.newLine();
                writeBuildingBlocks(buffer, request);
                buffer.newLine();
                buffer.write("Multiple reaction settings: ");
                buffer.newLine();
                writeMultipleReactionSettings(buffer, request);
                buffer.newLine();
                buffer.write("Genetic operators: ");
                buffer.newLine();
                writeGeneticOperators(buffer, request);
                buffer.newLine();
                buffer.write("Scoring options: ");
                buffer.newLine();
                writeScoringOptions(buffer, request);
                buffer.newLine();
                buffer.write("Filters: ");
                buffer.newLine();
                writeFilters(buffer, request);
                buffer.flush();
                buffer.close();
                System.out.println("Wrote to file " + out);
            }
        } catch (IOException | ServletException e) {
            e.printStackTrace();
        }

    }

    private static void writeBuildingBlocks(BufferedWriter buffer, HttpServletRequest request) throws IOException, ServletException {
        buffer.write("Name: " + request.getParameter("name"));
        buffer.newLine();
        buffer.write("Randomization seed: " + request.getParameter("baseSeed"));
        buffer.newLine();
        buffer.write("Progress ID: " + request.getParameter("progressID"));
        buffer.newLine();
        buffer.write("Reaction Filenames: ");
        buffer.newLine();
        writeList(getFileNamesFromRequest(request, "reactionFiles"), buffer);
        buffer.newLine();
        buffer.write("Reactant Filenames: ");
        buffer.newLine();
        writeList(getFileNamesFromRequest(request, "reactantFiles"), buffer);
        buffer.newLine();
        buffer.write("Maximum Reactant Weight: " + request.getParameter("maxReactantWeight"));
        buffer.newLine();
        buffer.write("Smarts filters: ");
        buffer.newLine();
        writeList(Arrays.asList(request.getParameter("smartsFiltering").split("\\R")), buffer);
        buffer.newLine();
        buffer.write("Receptor Filenames: ");
        buffer.newLine();
        writeList(getFileNamesFromRequest(request, "receptorFile"), buffer);
        buffer.newLine();
        buffer.write("Prepare receptor: " + (request.getParameter("setPrepareReceptor") != null));
        buffer.newLine();
        buffer.write("Anchor Filenames: ");
        buffer.newLine();
        writeList(getFileNamesFromRequest(request, "anchorFragmentFile"), buffer);
        buffer.newLine();
        buffer.write("Selectivity: " + (request.getParameter("selective") != null));
        buffer.newLine();
    }

    private static void writeMultipleReactionSettings(BufferedWriter buffer, HttpServletRequest request) throws IOException {
        buffer.write("Reaction determination method: " + request.getParameter("speciesDeterminationMethod"));
        buffer.newLine();
        buffer.write("Interspecies crossover method: " + request.getParameter("interspeciesCrossoverMethod"));
        buffer.newLine();
    }

    private static void writeGeneticOperators(BufferedWriter buffer, HttpServletRequest request) throws IOException {
        buffer.write("Population size: " + request.getParameter("generationSize"));
        buffer.newLine();
        buffer.write("Fill generations completely: " + (request.getParameter("setFillGen") != null));
        buffer.newLine();
        buffer.write("Maximum number of generations: " + request.getParameter("numberOfGenerations"));
        buffer.newLine();
        buffer.write("Selection size: " + request.getParameter("selectionSize"));
        buffer.newLine();
        buffer.write("Adaptive genetic algorithm: " + (request.getParameter("setAdaptive") != null));
        buffer.newLine();
        buffer.write("Adaptive mutation: " + (request.getParameter("setAdaptiveMutation") != null));
        buffer.newLine();
        if (request.getParameter("setAdaptive") == null) {
            buffer.write("Crossover rate: " + request.getParameter("crossoverRate"));
            buffer.newLine();
            buffer.write("Mutation rate: " + request.getParameter("mutationRate"));
            buffer.newLine();
        }
        buffer.write("Elitism rate: " + request.getParameter("elitismRate"));
        buffer.newLine();
        buffer.write("Random immigrant rate: " + request.getParameter("randomImmigrantRate"));
        buffer.newLine();
        buffer.write("Selection method: " + request.getParameter("selectionMethod"));
        buffer.newLine();
        buffer.write("Mutation method: " + request.getParameter("mutationMethod"));
        buffer.newLine();
        buffer.write("Termination condition: " + request.getParameter("terminationCondition"));
        buffer.newLine();
        if (request.getParameter("terminationCondition").equals("convergence")) {
            buffer.write("Non-improving generation amount: " + request.getParameter("nonImprovingGenerationQuantity"));
            buffer.newLine();
        }
    }

    private static void writeScoringOptions(BufferedWriter buffer, HttpServletRequest request) throws IOException {
        buffer.write("Maximum number of conformers: " + request.getParameter("conformerCount"));
        buffer.newLine();
        buffer.write("Use fast alignment: " + (request.getParameter("alignFast") != null));
        buffer.newLine();
        buffer.write("Conformer generation option: " + request.getParameter("conformerOption"));
        buffer.newLine();
        buffer.write("Force field: " + request.getParameter("forceField"));
        buffer.newLine();
        buffer.write("Scoring option: " + request.getParameter("scoringOption"));
        buffer.newLine();
        buffer.write("Fitness measure: " + request.getParameter("fitnessMeasure"));
        buffer.newLine();
    }

    private static void writeFilters(BufferedWriter buffer, HttpServletRequest request) throws IOException {
        buffer.write("Maximum RMSD: " + request.getParameter("maxAnchorMinimizedRmsd"));
        buffer.newLine();
        buffer.write("Receptor exclusion shape tolerance: " + request.getParameter("exclusionShapeTolerance"));
        buffer.newLine();
        buffer.write("Allow duplicates: " + (request.getParameter("allowDuplicates") != null));
        buffer.newLine();
        buffer.write("Use Lipinski's Rule of Five: " + (request.getParameter("useLipinski") != null));
        buffer.newLine();
        if (request.getParameter("useLipinski") != null) {
            buffer.write("Maximum molecular mass: " + request.getParameter("maxMolecularMass"));
            buffer.newLine();
            buffer.write("Maximum hydrogen bond donors: " + request.getParameter("maxHydrogenBondDonors"));
            buffer.newLine();
            buffer.write("Maximum hydrogen bond acceptors: " + request.getParameter("maxHydrogenBondAcceptors"));
            buffer.newLine();
            buffer.write("Maximum logP: " + request.getParameter("maxPartitionCoefficient"));
            buffer.newLine();
        }
        buffer.write("Minimum QED: " + request.getParameter("minQED"));
        buffer.newLine();
        buffer.write("Minimum BBB: " + request.getParameter("minBBB"));
        buffer.newLine();
    }

    /**
     * Gets the files from the field with the given name.
     *
     * @param request       the http request.
     * @param fileFieldName the file field to get the files from.
     * @return a list of file parts that where uploaded.
     * @throws IOException      If the parts could not be obtained due to an IO exception.
     * @throws ServletException If the parts could not be obtained due to a servlet exception.
     */
    private static List<String> getFileNamesFromRequest(HttpServletRequest request, String fileFieldName) throws IOException, ServletException {
        return request.getParts().stream()
                .filter(part -> fileFieldName
                        .equals(part.getName()))
                .map(Part::getSubmittedFileName)
                .collect(Collectors.toList());
    }

    private static void writeList(List<String> list, BufferedWriter buffer) throws IOException {
        for (String s : list) {
            buffer.write("\t" + s);
            buffer.newLine();
        }
    }
}
