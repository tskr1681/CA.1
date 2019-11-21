/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import chemaxon.marvin.plugin.PluginException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import nl.bioinf.cawarmerdam.compound_evolver.model.Candidate;
import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;
import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;
import nl.bioinf.cawarmerdam.compound_evolver.util.UnknownProgressException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet used for acquiring updates from the evolution.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "ProgressUpdateServlet", urlPatterns = "/progress.update")
public class ProgressUpdateServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        } catch (Exception e) {
            e.printStackTrace();
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    /**
     * Method that handles the progress update request by getting the progress connector from the session.
     *
     * @param request The http request.
     * @return the progress connector.
     * @throws UnknownProgressException if the session is new or null.
     */
    private SessionEvolutionProgressConnector handleProgressUpdateRequest(HttpServletRequest request) throws UnknownProgressException {
        HttpSession session = request.getSession();
        // get sessions new generations
        return ServletUtils.getProgressConnector(session);
    }
}

/**
 * Class that is used to slim down candidates for transfer with the client.
 * This is done by only serializing those fields that the client needs
 *
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
            throws IOException {

        jgen.writeStartObject();
        jgen.writeNumberField("id", candidate.getIdentifier());
        String phenotypeName;
        try {
            phenotypeName = candidate.getPhenotypeName();
        } catch (PluginException e) {
            phenotypeName = "Anonymous compound";
        }
        String smilesString;
        try {
            smilesString = candidate.getPhenotypeSmiles();
        } catch (IOException e) {
            smilesString = "";
        }
        // Write all fields that are necessary to the client.
        jgen.writeStringField("smiles", smilesString);
        jgen.writeStringField("iupacName", phenotypeName);
        jgen.writeNumberField("rawScore", candidate.getRawScore());
        jgen.writeNumberField("ligandEfficiency", candidate.getLigandEfficiency());
        jgen.writeNumberField("ligandLipophilicityEfficiency", candidate.getLigandLipophilicityEfficiency());
        jgen.writeStringField("species", candidate.getSpecies().toString());
        jgen.writeEndObject();
    }
}
