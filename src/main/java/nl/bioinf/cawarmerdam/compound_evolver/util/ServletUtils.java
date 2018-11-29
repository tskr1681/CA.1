package nl.bioinf.cawarmerdam.compound_evolver.util;

import nl.bioinf.cawarmerdam.compound_evolver.servlets.EvolveServlet;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {
    public static String getIpFromRequest(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
    }

    public static Double getDoubleParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!NumberCheckUtilities.isDouble(parameter)) {
            throw new FormFieldHandlingException(
                    name, parameter, FormFieldHandlingException.Cause.BAD_FLOAT);
        }
        return Double.valueOf(parameter); // Will not throw NumberFormatException
    }

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
     * @throws IllegalArgumentException if the fieldValue is null or not an integer
     */
    public static int getIntegerParameterFromRequest(HttpServletRequest request, String name) throws FormFieldHandlingException {
        String parameter = request.getParameter(name);
        if (parameter == null) {
            // Throw exception
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.NULL);
        } else if (parameter.length() == 0) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.EMPTY);
        } else if (!NumberCheckUtilities.isInteger(parameter, 10)) {
            throw new FormFieldHandlingException(name, parameter, FormFieldHandlingException.Cause.BAD_INTEGER);
        }
        return Integer.parseInt(parameter);
    }

    public static class FormFieldHandlingException extends Exception {
        private String fieldName;
        private String fieldValue;
        public Cause cause;

        public FormFieldHandlingException(String fieldName, String fieldValue, Cause cause) {
            super(String.format("Field '%s', ('%s') returned '%s'", fieldName, fieldValue, cause));
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
            this.cause = cause;
        }

        String toJSON() {
            String jsonFormat =
                    "{" +
                            "\"fieldName\": \"%s\"," +
                            "\"fieldValue\": \"%s\"," +
                            "\"cause\": \"%s\"," +
                            "\"message\": \"%s\"}";
            return String.format(jsonFormat, fieldName, fieldValue, cause, getMessage());
        }

        public enum Cause {NULL, EMPTY, BAD_FLOAT, BAD_INTEGER, BAD_BOOLEAN}
    }
}
