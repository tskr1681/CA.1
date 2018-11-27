package nl.bioinf.cawarmerdam.compound_evolver.util;

import javax.servlet.http.HttpServletRequest;

public class ServletUtils {
    public static String getIpFromRequest(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress.contains(",") ? ipAddress.split(",")[0] : ipAddress;
    }
}
