/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;
import nl.bioinf.cawarmerdam.compound_evolver.util.UnknownProgressException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "TerminateEvolutionServlet", urlPatterns = "./evolution.terminate")
public class TerminateEvolutionServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            handleTerminationRequest(request);

            // Write new generations
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (UnknownProgressException e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    private void handleTerminationRequest(HttpServletRequest request) throws UnknownProgressException {
        HttpSession session = request.getSession();
        SessionEvolutionProgressConnector progressConnector =
                (SessionEvolutionProgressConnector) session.getAttribute("progress_connector");
        if (progressConnector == null) {
            // Throw exception
            throw new UnknownProgressException("progress connector is null");
        }
        progressConnector.terminateEvolutionProgress();
    }
}
