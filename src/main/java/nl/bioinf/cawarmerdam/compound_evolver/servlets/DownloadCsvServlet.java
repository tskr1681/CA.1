package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet(name = "DownloadCsvServlet", urlPatterns = "./csv.download")
public class DownloadCsvServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sessionId = getSessionId(request);


        OutputStream out = response.getOutputStream();
        out.flush();
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
}
