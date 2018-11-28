package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.reaction.ReactionException;
import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.io.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.*;
import nl.bioinf.cawarmerdam.compound_evolver.util.GenerateCsv;
import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@MultipartConfig(maxFileSize = 10000000)    // upload file's size up to 10MB
@WebServlet(name = "EvolveServlet", urlPatterns = "/evolve.do")
public class EvolveServlet extends HttpServlet {
    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) return false;
                else continue;
            }
            if (Character.digit(s.charAt(i), radix) < 0) return false;
        }
        return true;
    }

    private static Double getDoubleParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!isDouble(parameter)) {
            throw new FormFieldHandlingException(
                    name, parameter, FormFieldHandlingException.Cause.BAD_FLOAT);
        }
        return Double.valueOf(parameter); // Will not throw NumberFormatException
    }

    private static Boolean isDouble(String parameter) {
        final String Digits = "(\\p{Digit}+)";
        final String HexDigits = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp = "[eE][+-]?" + Digits;
        final String fpRegex = (
                        "[\\x00-\\x20]*" +  // Optional leading "whitespace"
                        "[+-]?(" + // Optional sign character
                        "NaN|" +           // "NaN" string
                        "Infinity|" +      // "Infinity" string

                        // A decimal floating-point string representing a finite positive
                        // number without a leading sign has at most five basic pieces:
                        // Digits . Digits ExponentPart FloatTypeSuffix
                        //
                        // Since this method allows integer-only strings as input
                        // in addition to strings of floating-point literals, the
                        // two sub-patterns below are simplifications of the grammar
                        // productions from section 3.10.2 of
                        // The Java Language Specification.

                        // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
                        "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|" +

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        "(\\.(" + Digits + ")(" + Exp + ")?)|" +

                        // Hexadecimal strings
                        "((" +
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "(\\.)?)|" +

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                        ")[pP][+-]?" + Digits + "))" +
                        "[fFdD]?))" +
                        "[\\x00-\\x20]*");// Optional trailing "whitespace"

        return Pattern.matches(fpRegex, parameter);
    }

    /**
     * Generates a random token that will be used as a session id
     *
     * @return a random token
     */
    private static String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[32];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.printf("Received request from %s%n", ServletUtils.getIpFromRequest(request));
        ObjectMapper mapper = new ObjectMapper();
        // Set response type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            // Create new compoundEvolver
            CompoundEvolver compoundEvolver = constructCompoundEvolver(request);
            compoundEvolver.evolve();
            mapper.writeValue(response.getOutputStream(), GenerateCsv.generateCsvFile(compoundEvolver.getFitness(), "\n"));
        } catch (Exception e) {
            response.setStatus(400);
            response.setContentType("application/json");
            mapper.writeValue(response.getOutputStream(), e);
        }
    }

    private CompoundEvolver constructCompoundEvolver(HttpServletRequest request) throws IOException, ServletException, ReactantFileHandlingException, ReactantFileFormatException, ReactionFileHandlerException, FormFieldHandlingException, MisMatchedReactantCount, ReactionException, PipelineException {
        // Get generation size
        int generationSize = getIntegerParameterFromRequest(request, "generationSize");

        // Get reaction
        Reactor reaction = ReactionFileHandler.loadReaction(getFileFromRequest(request, "reactionFile"));

        // Get reactants
        List<List<Molecule>> reactantLists = ReactantFileHandler.loadMolecules(getReactantFilesFromRequest(request));
        List<Integer> reactantsFileOrder = getFileOrderParameterFromRequest(request);
        reactantLists = reorderReactantsLists(reactantLists, reactantsFileOrder);

        // Initialize population instance
        Population initialPopulation = new Population(reactantLists, reaction, generationSize);

        // Get crossover rate
        double crossoverRate = getDoubleParameterFromRequest(request, "crossoverRate");
        initialPopulation.setCrossoverRate(crossoverRate);

        // Get selection size
        double selectionSize = getDoubleParameterFromRequest(request, "selectionSize");
        initialPopulation.setSelectionFraction(selectionSize);

        // Get mutation rate
        double mutationRate = getDoubleParameterFromRequest(request, "mutationRate");
        initialPopulation.setMutationRate(mutationRate);

        // Get elitist rate
        double elitismRate = getDoubleParameterFromRequest(request, "elitismRate");
        initialPopulation.setElitismRate(elitismRate);

        // Get random immigrant rate
        double randomImmigrantRate = getDoubleParameterFromRequest(request, "randomImmigrantRate");
        initialPopulation.setRandomImmigrantRate(randomImmigrantRate);

        // Get the maximum allowed rmsd from the anchor
        double maxAnchorMinimizedRmsd = getDoubleParameterFromRequest(request, "maxAnchorMinimizedRmsd");
        initialPopulation.setMaxAnchorMinimizedRmsd(maxAnchorMinimizedRmsd);

        // If use lipinski is set to true, handle the filter options.
        boolean useLipinski = getBooleanParameterFromRequest(request, "useLipinski");
        if (useLipinski) handleFilterOptions(request, initialPopulation);

        // Get mutation method
        Population.MutationMethod mutationMethod = Population.MutationMethod.fromString(
                request.getParameter("mutationMethod"));
        initialPopulation.setMutationMethod(mutationMethod);

        // Compute allele similarity values if mutation method relies on these
        if (mutationMethod == Population.MutationMethod.DISTANCE_DEPENDENT)
            initialPopulation.initializeAlleleSimilaritiesMatrix();

        // Get selection method
        Population.SelectionMethod selectionMethod = Population.SelectionMethod.fromString(
                request.getParameter("selectionMethod"));

        SessionEvolutionProgressConnector progressConnector = new SessionEvolutionProgressConnector();

        initialPopulation.setSelectionMethod(selectionMethod);
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
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.NULL && e.cause != FormFieldHandlingException.Cause.EMPTY)
                throw e;
        }

        // Get termination condition
        CompoundEvolver.TerminationCondition terminationCondition = CompoundEvolver.TerminationCondition.fromString(
                request.getParameter("terminationCondition"));
        evolver.setTerminationCondition(terminationCondition);

        // Get and set fitness measure that should be used
        evolver.setFitnessMeasure(CompoundEvolver.FitnessMeasure.fromString(
                request.getParameter("fitnessMeasure")));

        System.out.printf("Evolution setup complete with session-id %s", sessionID);
        return evolver;
    }

    private void setPipelineParameters(HttpServletRequest request, CompoundEvolver evolver, Path outputFileLocation)
            throws IOException, ServletException, PipelineException, FormFieldHandlingException {

        // Check if the location already exists
        if (!outputFileLocation.toFile().exists()) {
            boolean mkdir = outputFileLocation.toFile().mkdir();
            System.out.println("mkdir = " + mkdir);
        }

        // Get and set force field that should be used
        evolver.setForceField(CompoundEvolver.ForceField.fromString(
                request.getParameter("forceField")));

        // Get conformer count
        int conformerCount = this.getIntegerParameterFromRequest(request, "conformerCount");

        // Copy files from request to the pipeline target directory
        Path receptorLocation = outputFileLocation.resolve("rec.pdb");
        copyFilePart(getFileFromRequest(request, "receptorFile"), receptorLocation);
        Path anchorLocation = outputFileLocation.resolve("anchor.sdf");
        copyFilePart(getFileFromRequest(request, "anchorFragmentFile"), anchorLocation);

        // Setup the pipeline using the gathered locations paths
        evolver.setupPipeline(outputFileLocation, receptorLocation, anchorLocation, conformerCount);

        System.out.println("evolver.getPipelineOutputFileLocation() = " + evolver.getPipelineOutputFileLocation());
    }

    private String getSessionId(HttpSession session, Path pipelineTargetDirectory) {
        String sessionID;
        // Create new session id
        do {
            sessionID = generateRandomToken();
        } while (Files.exists(pipelineTargetDirectory.resolve(sessionID)));
        session.setAttribute("session_id", sessionID);

        return sessionID;
    }

    private List<List<Molecule>> reorderReactantsLists(List<List<Molecule>> reactantLists, List<Integer> reactantsFileOrder) {
        return reactantsFileOrder.stream().map(reactantLists::get).collect(Collectors.toList());
    }

    private List<Integer> getFileOrderParameterFromRequest(HttpServletRequest request) throws IOException {
        // Get parameter as string
        String fileOrder = request.getParameter("fileOrder");
        // this parses the json
        ObjectMapper mapper = new ObjectMapper();
        return Arrays.asList(mapper.readValue(fileOrder, Integer[].class));
    }

    private void handleFilterOptions(HttpServletRequest request, Population initialPopulation) throws FormFieldHandlingException {
        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondAcceptors = getDoubleParameterFromRequest(request, "maxHydrogenBondAcceptors");
            initialPopulation.setMaxHydrogenBondAcceptors(maxHydrogenBondAcceptors);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondDonors = getDoubleParameterFromRequest(request, "maxHydrogenBondDonors");
            initialPopulation.setMaxHydrogenBondDonors(maxHydrogenBondDonors);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum molecular mass
        try {
            double maxMolecularMass = getDoubleParameterFromRequest(request, "maxMolecularMass");
            initialPopulation.setMaxMolecularMass(maxMolecularMass);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum partition coefficient
        try {
            double maxPartitionCoefficient = getDoubleParameterFromRequest(request, "maxPartitionCoefficient");
            initialPopulation.setMaxPartitionCoefficient(maxPartitionCoefficient);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }
    }

    private boolean getBooleanParameterFromRequest(HttpServletRequest request, String name) {
        String parameter = request.getParameter(name);
        return parameter != null;
    }

    private Part getFileFromRequest(HttpServletRequest request, String fileFieldName) throws IOException, ServletException {
        Part filePart = request.getPart(fileFieldName);
        if (filePart != null) {
            return filePart;
        } else {
            throw new IllegalArgumentException(
                    String.format("File in field '%s' was not specified", fileFieldName));
        }
    }

    private void copyFilePart(Part filePart, Path path) throws IOException {
        try (OutputStream out = new FileOutputStream(path.toFile()); InputStream filecontent = filePart.getInputStream()) {

            int read = 0;
            final byte[] bytes = new byte[1024];

            while ((read = filecontent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

        } catch (FileNotFoundException fne) {
            // Throw exception

        }
    }

    private List<Part> getReactantFilesFromRequest(HttpServletRequest request) throws IOException, ServletException {
        return request.getParts().stream()
                .filter(part -> "reactantFiles"
                        .equals(part.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Parses the given fieldValue to an integer
     *
     * @param request to get the field parameter from
     * @param name    of the field to process
     * @return the retrieved integer value
     * @throws IllegalArgumentException if the fieldValue is null or not an integer
     */
    private int getIntegerParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!isInteger(parameter, 10)) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.BAD_INTEGER);
        }
        return Integer.parseInt(parameter);
    }

    private static class FormFieldHandlingException extends Exception {
        private String fieldName;
        private String fieldValue;
        private Cause cause;

        FormFieldHandlingException(String fieldName, String fieldValue, Cause cause) {
            super(String.format("Field '%s', ('%s') returned '%s'", fieldName, fieldValue, cause));
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.cause = cause;
        }

        String toJSON() {
            String jsonFormat =
                    "{" +
                            "\"fieldName\": \"%s\"," +
                            "\"fieldValue\": \"%s\"," +
                            "\"cause\": \"%s\"," +
                            "\"message\": \"%s\"}";
            return String.format(jsonFormat, fieldName, fieldValue, cause, getMessage());
        }

        enum Cause {NULL, EMPTY, BAD_FLOAT, BAD_INTEGER, BAD_BOOLEAN}
    }
}
