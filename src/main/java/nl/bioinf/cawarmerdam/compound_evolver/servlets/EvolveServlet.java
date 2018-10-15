package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.reaction.Reactor;
import chemaxon.struc.Molecule;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileFormatException;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactantFileHandlingException;
import nl.bioinf.cawarmerdam.compound_evolver.io.ReactionFileHandler;
import nl.bioinf.cawarmerdam.compound_evolver.model.Population;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
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
            mapper.writeValue(response.getOutputStream(), "Success");
        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), exception.getMessage());
        }
    }

    private CompoundEvolver constructCompoundEvolver(HttpServletRequest request) throws IOException, ServletException {
        int generationSize = getIntegerParameter(request.getParameter("generationSize"));
        double crossoverRate = getDoubleParameter(request.getParameter("crossoverRate"));
        double mutationRate = getDoubleParameter(request.getParameter("mutationRate"));
        double elitistRate = getDoubleParameter(request.getParameter("elitistRate"));
        double randomImmigrantRate = getDoubleParameter(request.getParameter("randomImmigrantRate"));
        Reactor reaction = ReactionFileHandler.loadReaction(getFileFromRequest(request, "reactionFile"));
        List<List<Molecule>> reactantLists = null;
        try {
            reactantLists = ReactantFileHandler.loadMolecules(new String[] {
                    "X:\\Internship\\reactants\\aldehyde_small.smiles",
                    "X:\\Internship\\reactants\\amine_tryptamine.smiles",
                    "X:\\Internship\\reactants\\acids_small.smiles",
                    "X:\\Internship\\reactants\\isocyanide_small.smiles"});
        } catch (ReactantFileHandlingException e) {
            e.printStackTrace();
        } catch (ReactantFileFormatException e) {
            e.printStackTrace();
        }
        Population initialPopulation = new Population(reactantLists, reaction, generationSize);
        initialPopulation.setCrossoverRate(crossoverRate);
        initialPopulation.setMutationRate(mutationRate);
        return null;
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
     * Parses the given parameter to an integer
     * @param parameter that is parsed
     * @return the retrieved integer value
     * @throws IllegalArgumentException if the parameter is null or not an integer
     */
    private int getIntegerParameter(String parameter) throws IllegalArgumentException {
        if (parameter != null && isInteger(parameter, 10)) {
            return Integer.parseInt(parameter);
        }
        throw new IllegalArgumentException(
                String.format("Parameter '%s' was not a valid integer value", parameter));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("./app");
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    private static Double getDoubleParameter(String parameter) {
        if (parameter == null) {

        }
        final String Digits     = "(\\p{Digit}+)";
        final String HexDigits  = "(\\p{XDigit}+)";
        // an exponent is 'e' or 'E' followed by an optionally
        // signed decimal integer.
        final String Exp        = "[eE][+-]?"+Digits;
        final String fpRegex    =
                ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
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
                        "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

                        // . Digits ExponentPart_opt FloatTypeSuffix_opt
                        "(\\.("+Digits+")("+Exp+")?)|"+

                        // Hexadecimal strings
                        "((" +
                        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "(\\.)?)|" +

                        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
                        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

                        ")[pP][+-]?" + Digits + "))" +
                        "[fFdD]?))" +
                        "[\\x00-\\x20]*");// Optional trailing "whitespace"

        if (parameter != null && Pattern.matches(fpRegex, parameter))
            return Double.valueOf(parameter); // Will not throw NumberFormatException
        else {
            throw new IllegalArgumentException(
                    String.format("Parameter '%s' was not a valid integer value", parameter));
        }
    }
}
