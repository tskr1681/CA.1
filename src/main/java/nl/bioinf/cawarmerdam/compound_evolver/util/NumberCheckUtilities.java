/*
 * Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
 * All rights reserved.
 */
package nl.bioinf.cawarmerdam.compound_evolver.util;

/**
 * Class that has utilities for checking if a string can be parsed to an integer.
 *
 * @author C.A. (Robert) Warmerdam
 * @author c.a.warmerdam@st.hanze.nl
 * @version 0.0.1
 */
public class NumberCheckUtilities {

    /**
     * Method that checks if a string is an integer.
     *
     * @param string, The string that is checked.
     * @param radix, The number of unique digits used to represent numbers in a positional numeral system.
     * @return true if the string is an integer.
     */
    public static boolean isInteger(String string, int radix) {
        try {
            Integer.parseInt(string, radix);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method that checks if a string is a double.
     *
     * @param parameter, the string to check.
     * @return true if the string can be a double, false if not.
     */
    public static Boolean isDouble(String parameter) {
        try {
            Double.parseDouble(parameter);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
