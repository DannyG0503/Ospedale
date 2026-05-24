package packagee.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import packagee.model.Specialty;

public final class Validator {

    private static final Pattern EMAIL    = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.com$");
    private static final Pattern ID_12    = Pattern.compile("\\d{12}");
    private static final Pattern PHONE_10 = Pattern.compile("\\d{10}");
    private static final Pattern LICENSE  = Pattern.compile("L-\\d{10} MTL");
    private static final Pattern OFFICE   = Pattern.compile("O-\\d{3}");
    private static final Pattern TIME_HM  = Pattern.compile("(?:[01]\\d|2[0-3]):(?:00|15|30|45)");

    private Validator() {}

    public static boolean isValidEmail(String s) {
        return s != null && EMAIL.matcher(s).matches();
    }

    public static boolean isValidDate(String s) {
        if (s == null) return false;
        try {
            LocalDate.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidTime(String s) {
        if (s == null || !TIME_HM.matcher(s).matches()) return false;
        try {
            LocalTime.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidId12Digits(String s) {
        if (s == null || !ID_12.matcher(s).matches()) return false;
        try {
            return Long.parseLong(s) > 0L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPhone(String s) {
        return s != null && PHONE_10.matcher(s).matches();
    }

    public static boolean isValidLicense(String s) {
        return s != null && LICENSE.matcher(s).matches();
    }

    public static boolean isValidOffice(String s) {
        return s != null && OFFICE.matcher(s).matches();
    }

    public static boolean isValidSpecialty(String s) {
        if (s == null) return false;
        try {
            Specialty.valueOf(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
