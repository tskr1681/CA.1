/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;
import nl.bioinf.cawarmerdam.compound_evolver.util.UnknownProgressException;
import nl.bioinf.cawarmerdam.compound_evolver.util.ZipHelper;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils.getSessionId;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "DownloadZippedCompoundServlet", urlPatterns = "/compound.download")
public class DownloadZippedCompoundServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            handleGet(request, response);
        } catch (UnknownProgressException e) {
            ObjectMapper mapper = new ObjectMapper();
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), e.getMessage());
        }
    }

    /**
     * Method that handles a get request for a zip archive.
     *
     * @param request  The HTTP request.
     * @param response The HTTP response.
     */
    private void handleGet(HttpServletRequest request, HttpServletResponse response) throws UnknownProgressException {
        String pipelineTargetDirectory = System.getenv("PL_TARGET_DIR");
        String sessionId = getSessionId(request);

        // Set the directory to get the entire run
        System.out.println(pipelineTargetDirectory + "\\" + sessionId);
        Path directory = Paths.get(pipelineTargetDirectory, sessionId);

        // Set the format non compound specific
        String filename = String.format("%s_compounds.zip", sessionId);

        // Try to get the compound id
        // When this fails the entire run will be returned
        try {
            int compoundId = ServletUtils.getIntegerParameterFromRequest(request, "compoundId");

            System.out.println("Requested compound " + compoundId);

            // Get the path of the folder to make a zip file from
            directory = directory.resolve(String.valueOf(compoundId));

            filename = String.format("%s_compound-%s.zip", sessionId, compoundId);

        } catch (ServletUtils.FormFieldHandlingException ignored) {
        }

        // Get zipped archive
        String[] files = directory.toFile().list();

        // Checks to see if the directory contains some files.
        try {
            System.out.println("files = " + Arrays.toString(files));
            if (files != null && files.length > 0) {

                // Call the zipFiles method for creating a zip stream.
                byte[] zip = ZipHelper.zipDir(directory);

                // Sends the response back to the user / browser. The
                // content for zip file type is "application/zip". We
                // also set the content disposition as attachment for
                // the browser to show a dialog that will let user
                // choose what action will he do to the sent content.
                ServletOutputStream sos = response.getOutputStream();

                response.setContentType("application/zip");
                response.setHeader("Content-Disposition", "attachment; filename=" + filename);

                sos.write(zip);
                sos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
