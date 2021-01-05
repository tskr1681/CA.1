/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.servlets;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * The servlet that serves the applications graphical user interface.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
@WebServlet(name = "ApplicationServlet", urlPatterns = "/app")
public class ApplicationServlet extends javax.servlet.http.HttpServlet {

    protected void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws javax.servlet.ServletException, IOException {
        dispatchApp(request, response);
    }

    /**
     * Method that forwards the application jsp.
     *
     * @param request  The request from which a request dispatcher can be obtained.
     * @param response The response that can be used in forwarding the request.
     * @throws ServletException if a servlet related exception occurs.
     * @throws IOException      if an IO related exception occurs.
     */
    private void dispatchApp(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("application.jsp");
        dispatcher.forward(request, response);
    }
}
