package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils;
import nl.bioinf.cawarmerdam.compound_evolver.util.UnknownProgressException;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.bioinf.cawarmerdam.compound_evolver.util.ServletUtils.getSessionId;

@WebServlet(name = "VisualizationFileServlet", urlPatterns = "/get.files")
public class VisualizationFileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        InputStream in = null;
        String filetype = request.getParameter("filetype");
        String pipelineTargetDirectory = System.getenv("PL_TARGET_DIR");
        String sessionId = null;
        try {
            sessionId = getSessionId(request);
        } catch (UnknownProgressException e) {
            e.printStackTrace();
        }

        // Set the directory to get the entire run
        Path directory = Paths.get(pipelineTargetDirectory, sessionId);

        // Try to get the compound id
        // When this fails the entire run will be returned
        try {
            int compoundId = ServletUtils.getIntegerParameterFromRequest(request, "compoundId");

            // Get the path of the folder to make a zip file from
            Path compound_directory = directory.resolve(String.valueOf(compoundId));
            System.out.println("compound_directory = " + compound_directory);
            System.out.println("directory = " + directory);
            if(filetype.equals("sdf")) {
                in = new FileInputStream(compound_directory.resolve("best-conformer.sdf").toFile());
            } else if (filetype.equals("pdb")) {
                File[] temp = directory.toFile().listFiles((dir, filename) -> filename.endsWith(".pdb"));
                if(temp != null) {
                    in = new FileInputStream(temp[0]);
                }
            }

        } catch (ServletUtils.FormFieldHandlingException ignored) {
        }

        // Sends the response back to the user / browser. The
        // content for zip file type is "text/html".
        if(in!=null) {
            ServletOutputStream sos = response.getOutputStream();
            response.setContentType("text/html;charset=UTF-8");
            IOUtils.copy(in, sos);
            sos.flush();
            in.close();
            sos.close();
        }
    }


}
