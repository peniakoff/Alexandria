package pl.tomaszmiller.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Manages internationalization and locale switching.
 * Default language is English; supports switching to Polish.
 */
public final class I18n {

    private static final String BUNDLE_BASE = "pl.tomaszmiller.i18n.messages";
    private static Locale currentLocale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);

    private I18n() {
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, locale);
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String get(String key, Object... args) {
        try {
            return java.text.MessageFormat.format(bundle.getString(key), args);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public static String getEnum(Enum<?> value) {
        if (value == null) {
            return "";
        }
        String key = "enum."
                + value.getClass().getSimpleName().toLowerCase(Locale.ROOT)
                + "."
                + value.name().toLowerCase(Locale.ROOT);
        try {
            return bundle.getString(key);
        } catch (MissingResourceException exception) {
            return value.name();
        }
    }

    public static void switchToEnglish() {
        setLocale(Locale.ENGLISH);
    }

    public static void switchToPolish() {
        setLocale(Locale.of("pl", "PL"));
    }
}
