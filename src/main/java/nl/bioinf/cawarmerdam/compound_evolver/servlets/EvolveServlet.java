/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.formats.MolExporter;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.io.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.pipeline.PipelineException;
import nl.bioinf.cawarmerdam.compound_evolver.util.GenerateCsv;
import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils.*;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@MultipartConfig(maxFileSize = 10000000)    // upload file's size up to 10MB
@WebServlet(name = "EvolveServlet", urlPatterns = "/evolve.do")
public class EvolveServlet extends HttpServlet {

    /**
     * Generates a random token that will be used as a session id
     *
     * @return a random token
     */
    private static String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.printf("Received request from %s%n", ServletUtils.getIpFromRequest(request));
        ObjectMapper mapper = new ObjectMapper();
        // Set response type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            // Create new compoundEvolver
            CompoundEvolver compoundEvolver = handleRequest(request);
            compoundEvolver.evolve();
            mapper.writeValue(response.getOutputStream(), GenerateCsv.generateCsvFile(compoundEvolver.getFitness(), "\n"));
        } catch (Exception e) {
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e);
        }
    }

    /**
     * Handles the request of evolution.
     *
     * @param request The http request to handle
     * @return a compound evolver instance set according to the http request.
     * @throws IOException If an IO related exception was thrown.
     * @throws ServletException If a servlet exception was thrown.
     * @throws ReactantFileHandlingException If a reactant file could not be imported.
     * @throws ReactantFileFormatException If the reactant file could not be imported due to illegal formatting.
     * @throws ReactionFileHandlerException If the reaction file could not be imported.
     * @throws ServletUtils.FormFieldHandlingException If a form field could not be handled.
     * @throws MisMatchedReactantCount If the amount of reactants supplied was not according to the reaction.
     * @throws PipelineException If the pipeline could not be initialized.
     */
    private CompoundEvolver handleRequest(HttpServletRequest request)
            throws IOException,
            ServletException,
            ReactantFileHandlingException,
            ReactantFileFormatException,
            ReactionFileHandlerException,
            ServletUtils.FormFieldHandlingException,
            MisMatchedReactantCount,
            PipelineException {

        // Get generation size
        int generationSize = getIntegerParameterFromRequest(request, "generationSize");

        // Get reactionList
        List<Reactor> reactionList = ReactionFileHandler.loadReactions(getFilesFromRequest(request, "reactionFiles"));

        double maxWeight = getDoubleParameterFromRequest(request, "maxReactantWeight");
        System.out.println("maxWeight = " + maxWeight);
        // Get reactants
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(getFilesFromRequest(request, "reactantFiles"), maxWeight);
        List<List<Integer>> reactantsFileOrder = getFileOrderParameterFromRequest(request);
        List<Species> species = Species.constructSpecies(reactionList, reactantsFileOrder);

        // Get species determination method
        Population.SpeciesDeterminationMethod speciesDeterminationMethod = Population.SpeciesDeterminationMethod.
                fromString(request.getParameter("speciesDeterminationMethod"));

        // Initialize population instance
        Population initialPopulation = new Population(reactantLists, species, speciesDeterminationMethod, generationSize);
        MolExporter molExporter = new MolExporter(Paths.get(System.getenv("PL_TARGET_DIR")).resolve("pop.smiles").toString(), "smiles");
        for (Candidate candidate :
                initialPopulation) {
            molExporter.write(candidate.getPhenotype());
        }
        molExporter.close();

        // Get interspecies crossover method
        Population.InterspeciesCrossoverMethod interspeciesCrossoverMethod = Population.InterspeciesCrossoverMethod.
                fromString(request.getParameter("interspeciesCrossoverMethod"));
        initialPopulation.setInterspeciesCrossoverMethod(interspeciesCrossoverMethod);

        // Get crossover rate and mutation rate, but only if not running an adaptive GA
        boolean adaptive = getBooleanParameterFromRequest(request,"setAdaptive");
        initialPopulation.setAdaptive(adaptive);
        if (adaptive) {
            initialPopulation.setCrossoverRate(1);
            initialPopulation.setMutationRate(0.5);
        } else {
            double crossoverRate = getDoubleParameterFromRequest(request, "crossoverRate");
            double mutationRate = getDoubleParameterFromRequest(request, "mutationRate");
            initialPopulation.setCrossoverRate(crossoverRate);
            initialPopulation.setMutationRate(mutationRate);
        }

        // Get selection size
        double selectionSize = getDoubleParameterFromRequest(request, "selectionSize");
        initialPopulation.setSelectionFraction(selectionSize);

        // Get elitist rate
        double elitismRate = getDoubleParameterFromRequest(request, "elitismRate");
        initialPopulation.setElitismRate(elitismRate);

        // Get random immigrant rate
        double randomImmigrantRate = getDoubleParameterFromRequest(request, "randomImmigrantRate");
        initialPopulation.setRandomImmigrantRate(randomImmigrantRate);

        // If use lipinski is set to true, handle the filter options.
        boolean useLipinski = getBooleanParameterFromRequest(request, "useLipinski");
        if (useLipinski) handleFilterOptions(request, initialPopulation);

        boolean allowDuplicates = getBooleanParameterFromRequest(request, "allowDuplicates");
        initialPopulation.setDuplicatesAllowed(allowDuplicates);

        double minQED = getDoubleParameterFromRequest(request, "minQED");
        initialPopulation.setMinQED(minQED);

        // Get mutation method
        Population.MutationMethod mutationMethod = Population.MutationMethod.fromString(
                request.getParameter("mutationMethod"));
        initialPopulation.setMutationMethod(mutationMethod);

//        // Compute allele similarity values if mutation method relies on these
//        if (mutationMethod == Population.MutationMethod.DISTANCE_DEPENDENT)
//            initialPopulation.initializeAlleleSimilaritiesMatrix();

        // Get selection method
        Population.SelectionMethod selectionMethod = Population.SelectionMethod.fromString(
                request.getParameter("selectionMethod"));
        initialPopulation.setSelectionMethod(selectionMethod);

        SessionEvolutionProgressConnector progressConnector = new SessionEvolutionProgressConnector();

        CompoundEvolver evolver = new CompoundEvolver(
                initialPopulation, progressConnector);

        // Get the environment variable containing the pipeline target directory.
        Path pipelineTargetDirectory = Paths.get(System.getenv("PL_TARGET_DIR"));

        HttpSession session = request.getSession();
        session.setAttribute("progress_connector", progressConnector);

        String sessionID = getSessionId(session, pipelineTargetDirectory);
        evolver.setDummyFitness(getInitParameter("dummy.fitness").equals("1"));
        if (!evolver.isDummyFitness()) {
            Path outputFileLocation = pipelineTargetDirectory.resolve(sessionID);
            setPipelineParameters(request, evolver, outputFileLocation);
        }

        // Get number of numberOfGenerations
        int numberOfGenerations = getIntegerParameterFromRequest(request, "numberOfGenerations");
        evolver.setMaxNumberOfGenerations(numberOfGenerations);

        // Get non improving generation quantity
        try {
            double nonImprovingGenerationQuantity = getDoubleParameterFromRequest(request, "nonImprovingGenerationQuantity");
            evolver.setNonImprovingGenerationAmountFactor(nonImprovingGenerationQuantity);
        } catch (ServletUtils.FormFieldHandlingException e) {
            if (e.cause != ServletUtils.FormFieldHandlingException.Cause.NULL && e.cause != ServletUtils.FormFieldHandlingException.Cause.EMPTY)
                throw e;
        }

        // Get and set termination condition
        CompoundEvolver.TerminationCondition terminationCondition = CompoundEvolver.TerminationCondition.fromString(
                request.getParameter("terminationCondition"));
        evolver.setTerminationCondition(terminationCondition);

        // Get and set fitness measure that should be used
        evolver.setFitnessMeasure(CompoundEvolver.FitnessMeasure.fromString(
                request.getParameter("fitnessMeasure")));

        System.out.printf("Evolution setup complete with session-id %s", sessionID);
        return evolver;
    }

    /**
     * Sets the pipeline parameters.
     *
     * @param request The http request.
     * @param evolver The compound evolution instance.
     * @param outputFileLocation The location of the pipeline files.
     * @throws IOException If an IO related exception was thrown.
     * @throws ServletException If a servlet exception was thrown.
     * @throws PipelineException If the pipeline could not be initialized.
     * @throws ServletUtils.FormFieldHandlingException If a form field could not be handled.
     */
    private void setPipelineParameters(HttpServletRequest request, CompoundEvolver evolver, Path outputFileLocation)
            throws IOException, ServletException, PipelineException, ServletUtils.FormFieldHandlingException {

        // Check if the location already exists
        if (!outputFileLocation.toFile().exists()) {
            boolean mkdir = outputFileLocation.toFile().mkdir();
            System.out.println("mkdir = " + mkdir);
        }

        // Get and set force field that should be used
        evolver.setForceField(CompoundEvolver.ForceField.fromString(
                request.getParameter("forceField")));

        evolver.setScoringOption(CompoundEvolver.ScoringOption.fromString(
                request.getParameter("scoringOption")));

        // Get conformer count
        int conformerCount = getIntegerParameterFromRequest(request, "conformerCount");

        // Copy files from request to the pipeline target directory
        Path receptorLocation = outputFileLocation.resolve("rec.pdb");
        copyFilePart(getFileFromRequest(request, "receptorFile"), receptorLocation);
        Path anchorLocation = outputFileLocation.resolve("anchor.sdf");
        copyFilePart(getFileFromRequest(request, "anchorFragmentFile"), anchorLocation);

        // Get the exclusion shape tolerance
        double exclusionShapeTolerance = getDoubleParameterFromRequest(request, "exclusionShapeTolerance");

        // Get the maximum allowed rmsd from the anchor
        double maxAnchorMinimizedRmsd = getDoubleParameterFromRequest(request, "maxAnchorMinimizedRmsd");

        // Setup the pipeline using the gathered locations paths
        evolver.setupPipeline(
                outputFileLocation,
                receptorLocation,
                anchorLocation,
                conformerCount,
                exclusionShapeTolerance,
                maxAnchorMinimizedRmsd);

        System.out.println("evolver.getPipelineOutputFilePath() = " + evolver.getPipelineOutputFilePath());
    }

    /**
     * Generates a session id that is not yet used.
     *
     * @param session The http session instance.
     * @param pipelineTargetDirectory The directory where the sessions are stored.
     * @return the session id.
     */
    private String getSessionId(HttpSession session, Path pipelineTargetDirectory) {
        String sessionID;
        // Create new session id
        do {
            sessionID = generateRandomToken();
        } while (Files.exists(pipelineTargetDirectory.resolve(sessionID)));
        session.setAttribute("session_id", sessionID);

        return sessionID;
    }

    /**
     * Gets the file orders from the request instance.
     *
     * @param request The request instance to get the parameters from.
     * @return the file order per reaction.
     * @throws IOException if the file order could not be read.
     */
    private List<List<Integer>> getFileOrderParameterFromRequest(HttpServletRequest request) throws IOException {
        // Get parameter as string
        String fileOrder = request.getParameter("fileOrder");
        // this parses the json
        ObjectMapper mapper = new ObjectMapper();
        Integer[][] integers = mapper.readValue(fileOrder, Integer[][].class);
        return Arrays.stream(integers)
                .map(Arrays::asList)
                .collect(Collectors.toList());
    }

    /**
     * Method that handles the values for filters by setting them in the population if they are present.
     *
     * @param request The http request that is being handled.
     * @param initialPopulation The population that the values are set to.
     * @throws ServletUtils.FormFieldHandlingException if the field was either null, or badly formatted.
     */
    private void handleFilterOptions(HttpServletRequest request, Population initialPopulation) throws ServletUtils.FormFieldHandlingException {
        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondAcceptors = getDoubleParameterFromRequest(request, "maxHydrogenBondAcceptors");
            initialPopulation.setMaxHydrogenBondAcceptors(maxHydrogenBondAcceptors);
        } catch (ServletUtils.FormFieldHandlingException e) {
            if (e.cause != ServletUtils.FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondDonors = getDoubleParameterFromRequest(request, "maxHydrogenBondDonors");
            initialPopulation.setMaxHydrogenBondDonors(maxHydrogenBondDonors);
        } catch (ServletUtils.FormFieldHandlingException e) {
            if (e.cause != ServletUtils.FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum molecular mass
        try {
            double maxMolecularMass = getDoubleParameterFromRequest(request, "maxMolecularMass");
            initialPopulation.setMaxMolecularMass(maxMolecularMass);
        } catch (ServletUtils.FormFieldHandlingException e) {
            if (e.cause != ServletUtils.FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum partition coefficient
        try {
            double maxPartitionCoefficient = getDoubleParameterFromRequest(request, "maxPartitionCoefficient");
            initialPopulation.setMaxPartitionCoefficient(maxPartitionCoefficient);
        } catch (ServletUtils.FormFieldHandlingException e) {
            if (e.cause != ServletUtils.FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }
    }

    /**
     * Gets a file from a http request.
     *
     * @param request The http request to get the file from.
     * @param fileFieldName The name of the field to which the file was uploaded.
     * @return the file part that was found in the specified field.
     * @throws IOException if the file could not be obtained from the request.
     * @throws ServletException if the file could not be obtained from the request.
     */
    private Part getFileFromRequest(HttpServletRequest request, String fileFieldName) throws IOException, ServletException {
        Part filePart = request.getPart(fileFieldName);
        // Check if the file part is null.
        if (filePart != null) {
            return filePart;
        } else {
            // Throw exception if it is null.
            throw new IllegalArgumentException(
                    String.format("File in field '%s' was not specified", fileFieldName));
        }
    }

    /**
     * Writes a file part to a path on the system.
     *
     * @param filePart The file part to write.
     * @param path The path to write the file part to.
     */
    private void copyFilePart(Part filePart, Path path) {
        try (OutputStream out = new FileOutputStream(path.toFile()); InputStream filecontent = filePart.getInputStream()) {

            int read;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the files from the field with the given name.
     *
     * @param request the http request.
     * @param fileFieldName the file field to get the files from.
     * @return a list of file parts that where uploaded.
     * @throws IOException If the parts could not be obtained due to an IO exception.
     * @throws ServletException If the parts could not be obtained due to a servlet exception.
     */
    private List<Part> getFilesFromRequest(HttpServletRequest request, String fileFieldName) throws IOException, ServletException {
        return request.getParts().stream()
                .filter(part -> fileFieldName
                        .equals(part.getName()))
                .collect(Collectors.toList());
    }
}
