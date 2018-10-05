package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.bioinf.cawarmerdam.compound_evolver.control.CompoundEvolver;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
            CompoundEvolver evolver;

        } catch (IllegalArgumentException exception) {
            response.setStatus(400);
            mapper.writeValue(response.getOutputStream(), exception.getMessage());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendRedirect("./app");
    }
}
