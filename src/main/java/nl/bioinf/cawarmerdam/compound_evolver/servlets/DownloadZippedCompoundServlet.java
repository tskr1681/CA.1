package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@WebServlet(name = "DownloadZippedCompoundServlet", urlPatterns = "/compound.download")
public class DownloadZippedCompoundServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        handlePost(request, response);
    }

    private void handlePost(HttpServletRequest request, HttpServletResponse response) {
        String pipelineTargetDirectory = System.getenv("PL_TARGET_DIR");
        String sessionId = getSessionId(request);
        String compoundId = request.getParameter("compoundId");

        System.out.println("compoundId = " + compoundId);

        // Get the path of the folder to make a zip file from
        Path directory = Paths.get(pipelineTargetDirectory, sessionId, compoundId);

        String filename = String.format("%s_compound-%s.zip", sessionId, compoundId);

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
        byte bytes[] = new byte[2048];

        for (String fileName : files) {
            FileInputStream fis = new FileInputStream(directory.resolve(fileName).toFile());
            BufferedInputStream bis = new BufferedInputStream(fis);

            zos.putNextEntry(new ZipEntry(fileName));

            int bytesRead;
            while ((bytesRead = bis.read(bytes)) != -1) {
                zos.write(bytes, 0, bytesRead);
            }
            zos.closeEntry();
            bis.close();
            fis.close();
        }
        zos.flush();
        baos.flush();
        zos.close();
        baos.close();

        return baos.toByteArray();
    }
}
