/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.model.SessionEvolutionProgressConnector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class ServletUtils {
    /**
     * Gets the ip address from a http request
     *
     * @param request to get the ip address for
     * @return the ip address
     */
    public static String getIpFromRequest(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
    }

    /**
     * Parses the given fieldValue to an double
     *
     * @param request to get the field parameter from
     * @param name    of the field to process
     * @return the retrieved double value
     * @throws FormFieldHandlingException if the fieldValue is null, empty or not an integer
     */
    public static double getDoubleParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, null, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!NumberCheckUtilities.isDouble(parameter)) {
            throw new FormFieldHandlingException(
                    name, parameter, FormFieldHandlingException.Cause.BAD_DOUBLE);
        }
        return Double.parseDouble(parameter);
    }

    /**
     * Parses the given fieldValue to a boolean
     *
     * @param request to get the field parameter from
     * @param name    of the field to process
     * @return the retrieved boolean value
     */
    public static boolean getBooleanParameterFromRequest(HttpServletRequest request, String name) {
        String parameter = request.getParameter(name);
        return parameter != null;
    }

    /**
     * Parses the given fieldValue to an integer
     *
     * @param request to get the field parameter from
     * @param name    of the field to process
     * @return the retrieved integer value
     * @throws FormFieldHandlingException if the fieldValue is null, empty or not an integer
     */
    public static int getIntegerParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, null, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!NumberCheckUtilities.isInteger(parameter, 10)) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.BAD_INTEGER);
        }
        return Integer.parseInt(parameter);
    }

    /**
     * Exception that gets thrown whenever a form field value was illegal.
     */
    public static class FormFieldHandlingException extends Exception {
        private final String fieldName;
        private final String fieldValue;
        public final Cause cause;

        FormFieldHandlingException(String fieldName, String fieldValue, Cause cause) {
            super(String.format("Field '%s', ('%s') returned '%s'", fieldName, fieldValue, cause));
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.cause = cause;
        }

        public enum Cause {NULL, EMPTY, BAD_DOUBLE, BAD_INTEGER, BAD_BOOLEAN}
    }

    /**
     * Method that gets the progress connector from a http session.
     *
     * @param session The http session.
     * @return the progress connector instance.
     * @throws UnknownProgressException if the progress connector is null.
     */
    public static SessionEvolutionProgressConnector getProgressConnector(HttpSession session) throws UnknownProgressException {
        SessionEvolutionProgressConnector progressConnector =
                (SessionEvolutionProgressConnector) session.getAttribute("progress_connector");
        if (progressConnector == null) {
            // Throw exception
            throw new UnknownProgressException("progress connector is null");
        }
        return progressConnector;
    }

    /**
     * Method that gets the session id from from the request if it is present.
     *
     * @param request The HTTP request.
     * @return the session id
     * @throws UnknownProgressException if the session id is null or the session is new.
     */
    public static String getSessionId(HttpServletRequest request) throws UnknownProgressException {
        HttpSession session = request.getSession();
        String sessionID;
        if (session.isNew() || session.getAttribute("session_id") == null) {
            throw new UnknownProgressException("Session not found");
        }
        sessionID = (String) session.getAttribute("session_id");
        return sessionID;
    }
}
