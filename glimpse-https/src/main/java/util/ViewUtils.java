package util;

public final class ViewUtils {

    private ViewUtils() {}

    public static String h(Object value) {
        if (value == null) return "";
        String s = String.valueOf(value);
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String attr(Object value) {
        return h(value);
    }
}
