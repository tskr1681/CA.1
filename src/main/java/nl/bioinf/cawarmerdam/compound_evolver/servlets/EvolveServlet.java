package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.io.*;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@WebServlet(name = "EvolveServlet", urlPatterns = "/evolve.do")
@MultipartConfig
public class EvolveServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Set response type to JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            // Create new compoundEvolver
            CompoundEvolver compoundEvolver = constructCompoundEvolver(request);
            compoundEvolver.evolve();
            mapper.writeValue(response.getOutputStream(), compoundEvolver.getPopulationFitness());
        } catch (FormFieldHandlingException exception) {
            response.setStatus(400);
            response.setContentType("application/json");
            mapper.writeValue(response.getOutputStream(), exception.toJSON());
        } catch (IllegalArgumentException | IOException | ReactantFileHandlingException | ReactantFileFormatException e) {
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        } catch (ReactionFileHandlerException e) {
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    private CompoundEvolver constructCompoundEvolver(HttpServletRequest request) throws IOException, ServletException, ReactantFileHandlingException, ReactantFileFormatException, ReactionFileHandlerException, FormFieldHandlingException {
        // Get generation size
        int generationSize = getIntegerParameterFromRequest(request, "generationSize");

        // Get reaction
        Reactor reaction = ReactionFileHandler.loadReaction(getFileFromRequest(request, "reactionFile"));
        System.out.println(reaction.getReactantCount());

        // Get reactants
        List<List<Molecule>> reactantLists = getReactants();

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
        double elitistRate = getDoubleParameterFromRequest(request, "elitistRate");
        initialPopulation.setElitistRate(elitistRate);

        // Get random immigrant rate
        double randomImmigrantRate = getDoubleParameterFromRequest(request, "randomImmigrantRate");
        initialPopulation.setRandomImmigrantRate(randomImmigrantRate);

        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondAcceptors = getDoubleParameterFromRequest(request, "maxHydrogenBondAcceptors");
            Candidate.setMaxHydrogenBondAcceptors(maxHydrogenBondAcceptors);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum hydrogen bond acceptors
        try {
            double maxHydrogenBondDonors = getDoubleParameterFromRequest(request, "maxHydrogenBondDonors");
            Candidate.setMaxHydrogenBondDonors(maxHydrogenBondDonors);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum molecular mass
        try {
            double maxMolecularMass = getDoubleParameterFromRequest(request, "maxMolecularMass");
            Candidate.setMaxMolecularMass(maxMolecularMass);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

        // Get maximum partition coefficient
        try {
            double maxPartitionCoefficient = getDoubleParameterFromRequest(request, "maxPartitionCoefficient");
            Candidate.setMaxPartitionCoefficient(maxPartitionCoefficient);
        } catch (FormFieldHandlingException e) {
            if (e.cause != FormFieldHandlingException.Cause.EMPTY) throw e;
            // Continue if it is not present
        }

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

        initialPopulation.setSelectionMethod(selectionMethod);
        CompoundEvolver evolver = new CompoundEvolver(
                initialPopulation,
                Paths.get("X:\\Internship\\reference_fragment\\anchor.sdf"));

        evolver.setupPipeline(Paths.get("X:\\uploads", generateRandomToken()));

        // Get number of numberOfGenerations
        int numberOfGenerations = getIntegerParameterFromRequest(request, "numberOfGenerations");
        evolver.setMaxNumberOfGenerations(numberOfGenerations);

        // Get termination condition
        CompoundEvolver.TerminationCondition terminationCondition = CompoundEvolver.TerminationCondition.valueOf(
                request.getParameter("terminationCondition"));
        evolver.setTerminationCondition(terminationCondition);

        return evolver;
    }

    private List<List<Molecule>> getReactants() throws ReactantFileHandlingException, ReactantFileFormatException {
        List<List<Molecule>> reactantLists = null;
        reactantLists = ReactantFileHandler.loadMolecules(new String[]{
                "X:\\Internship\\reactants\\aldehyde_small.smiles",
                "X:\\Internship\\reactants\\amine_tryptamine.smiles",
                "X:\\Internship\\reactants\\acids_small.smiles",
                "X:\\Internship\\reactants\\isocyanide_small.smiles"});
        return reactantLists;
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
            // Throw parameter
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!isInteger(parameter, 10)) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.BAD_INTEGER);
        }
        return Integer.parseInt(parameter);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("./app");
    }

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
        final String fpRegex =
                ("[\\x00-\\x20]*" +  // Optional leading "whitespace"
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
        byte bytes[] = new byte[128];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    private static class FormFieldHandlingException extends Exception {
        private String fieldName;
        private String fieldValue;
        private Cause cause;

        enum Cause {NULL, EMPTY, BAD_FLOAT, BAD_INTEGER}

        FormFieldHandlingException(String fieldName, String fieldValue, Cause cause) {
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
    }
}
