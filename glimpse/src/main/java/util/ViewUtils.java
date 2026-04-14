package util;

import java.net.URI;
import java.net.URISyntaxException;

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

    public static String safeExternalUrl(String url) {
        if (url == null) return "#";
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return "#";
            String lower = scheme.toLowerCase();
            if (!"http".equals(lower) && !"https".equals(lower)) {
                return "#";
            }
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            return "#";
        }
    }
}
