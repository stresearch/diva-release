package com.visym.collector.utils;

import java.util.regex.Pattern;

public class FieldValidator {

    public static String isValidName(String firstName) {
        String NAME_PATTERN = "[a-zA-Z][a-zA-Z ]*";

        if (!Pattern.matches(NAME_PATTERN, firstName)) {
            return "Not a valid input";
        }
        return null;
    }

    public static String isValidPassword(String newPassword) {
        String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[`~!@#$%^&*(),.?\":{}|<>;/\\\\])(?=.*[0-9])(?=.*[a-z]).{8,64}$";

        if (!Pattern.matches(PASSWORD_PATTERN, newPassword)){
            return "Password must contain atleast 8 characters, uppercase letters, lowercase letters, " +
                    "special characters and numbers";
        }
        return null;
    }

}
