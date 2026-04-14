package security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

public final class Csrf {

    public static final String SESSION_KEY = "csrfToken";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {}

    public static String ensureToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String && !((String) existing).isEmpty()) {
            return (String) existing;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static boolean isValid(HttpServletRequest request, HttpSession session) {
        if (session == null) return false;

        String expected = (String) session.getAttribute(SESSION_KEY);
        if (expected == null || expected.isEmpty()) return false;

        String actual = request.getParameter("csrfToken");
        if (actual == null || actual.isEmpty()) {
            actual = request.getHeader("X-CSRF-Token");
        }
        return expected.equals(actual);
    }

    public static void rotateToken(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
            ensureToken(session);
        }
    }
}
