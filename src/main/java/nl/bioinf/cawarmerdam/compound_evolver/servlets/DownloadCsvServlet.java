/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.Generation;
import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;
import nl.bioinf.cawarmerdam.compound_evolver.util.GenerateCsv;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "DownloadCsvServlet", urlPatterns = "./csv.download")
public class DownloadCsvServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        // get sessions new generations
        try {

            SessionEvolutionProgressConnector progressConnector = getProgressConnector(session);
            List<List<Double>> scores = extractScoresFromGenerations(progressConnector);
            OutputStream outputStream = response.getOutputStream();
            String csvFile = GenerateCsv.generateCsvFile(scores, System.lineSeparator());

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"scores.csv\"");

            outputStream.write(csvFile.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (UnknownProgressException e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    private List<List<Double>> extractScoresFromGenerations(SessionEvolutionProgressConnector progressConnector) {
        List<Generation> generations = progressConnector.getGenerations();

        List<List<Double>> scores = new ArrayList<>();
        // Collect scores
        for (Generation generation : generations) {
            List<Double> fitnesses = generation.getCandidateList().stream()
                    .map(Candidate::getFitness)
                    .collect(Collectors.toList());
            // Add scores for the archive
            scores.add(fitnesses);
        }
        return scores;
    }

    private SessionEvolutionProgressConnector getProgressConnector(HttpSession session) throws UnknownProgressException {
        @SuppressWarnings("unchecked")
        SessionEvolutionProgressConnector progressConnector =
                (SessionEvolutionProgressConnector) session.getAttribute("progress_connector");
        if (progressConnector == null) {
            // Throw exception
            throw new UnknownProgressException("progress connector is null");
        }
        return progressConnector;
    }
}
