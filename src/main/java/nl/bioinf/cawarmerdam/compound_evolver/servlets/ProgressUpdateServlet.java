/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.marvin.plugin.PluginException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;

import javax.servlet.ServletException;
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
@WebServlet(name = "ProgressUpdateServlet", urlPatterns = "/progress.update")
public class ProgressUpdateServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            // Set response type
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            SessionEvolutionProgressConnector progressConnector = handleProgressUpdateRequest(request);
            // Get object mapper
            SimpleModule module = new SimpleModule();
            module.addSerializer(Candidate.class, new CandidateSerializer());
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(module);

            // Write new generations
            mapper.writeValue(response.getOutputStream(), progressConnector);
        } catch (UnknownProgressException e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        } catch (Exception e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    private SessionEvolutionProgressConnector handleProgressUpdateRequest(HttpServletRequest request) throws UnknownProgressException {
        HttpSession session = request.getSession();
        // get sessions new generations
        return getProgressConnector(session);
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

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
class UnknownProgressException extends Exception {
    UnknownProgressException(String message) {
        super(message);
    }
}

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
class CandidateSerializer extends StdSerializer<Candidate> {

    CandidateSerializer() {
        this(null);
    }

    private CandidateSerializer(Class<Candidate> t) {
        super(t);
    }

    @Override
    public void serialize(
            Candidate candidate, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeNumberField("id", candidate.getIdentifier());
        String phenotypeName = null;
        try {
            phenotypeName = candidate.getPhenotypeName();
        } catch (PluginException e) {
            phenotypeName = "Anonymous compound";
        }
        String smilesString = null;
        try {
            smilesString = candidate.getPhenotypeSmiles();
        } catch (IOException e) {
            smilesString = "";
        }
        jgen.writeStringField("smiles", smilesString);
        jgen.writeStringField("iupacName", phenotypeName);
        jgen.writeNumberField("rawScore", candidate.getRawScore());
        jgen.writeNumberField("ligandEfficiency", candidate.getLigandEfficiency());
        jgen.writeNumberField("ligandLipophilicityEfficiency", candidate.getLigandLipophilicityEfficiency());
        jgen.writeNumberField("rmsd", candidate.getCommonSubstructureToAnchorRmsd());
        jgen.writeStringField("species", candidate.getSpecies().toString());
        jgen.writeEndObject();
    }
}
