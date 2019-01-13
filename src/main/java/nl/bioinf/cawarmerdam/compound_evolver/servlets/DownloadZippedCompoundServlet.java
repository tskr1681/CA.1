/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "DownloadZippedCompoundServlet", urlPatterns = "/compound.download")
public class DownloadZippedCompoundServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        handleGet(request, response);
    }

    private void handleGet(HttpServletRequest request, HttpServletResponse response) {
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
                byte[] zip = zipFiles(directory, files);

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

    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String sessionID;
        if (session.isNew() || session.getAttribute("session_id") == null) {
            // No session id
        }
        sessionID = (String) session.getAttribute("session_id");
        return sessionID;
    }

    /**
     * Compress the given directory with all its files.
     */
    private byte[] zipFiles(Path directory, String[] files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        addDirToZipArchive(zos, directory.toFile(), null);
        zos.flush();
        baos.flush();
        zos.close();
        baos.close();

        return baos.toByteArray();
    }

    private static void addDirToZipArchive(ZipOutputStream zos, File fileToZip, String parrentDirectoryName) {
        if (fileToZip == null || !fileToZip.exists()) {
            return;
        }

        String zipEntryName = fileToZip.getName();
        if (parrentDirectoryName!=null && !parrentDirectoryName.isEmpty()) {
            zipEntryName = parrentDirectoryName + "/" + fileToZip.getName();
        }

        if (fileToZip.isDirectory()) {

            for (File file : Objects.requireNonNull(fileToZip.listFiles())) {
                addDirToZipArchive(zos, file, zipEntryName);
            }

        } else {

            try {
                byte[] buffer = new byte[1024];
                FileInputStream fis;
                fis = new FileInputStream(fileToZip);

                zos.putNextEntry(new ZipEntry(zipEntryName));
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
                fis.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
