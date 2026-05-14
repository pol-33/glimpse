package controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import security.Csrf;
import security.XmlCrypto;

/**
 * Handles Encrypt / Decrypt button clicks from {@code security.jsp} for the
 * demo Digital Item XML file.
 *
 * Lifecycle of the file on disk (all in ~/glimpse-xml/):
 *
 *   didlFilm1.xml            ← seed copy from the webapp (read-only template)
 *   didlFilm1_encrypted.xml  ← written by encrypt; contains <EncryptedData>
 *   didlFilm1_decrypted.xml  ← written by decrypt; should match the seed
 *
 * Encrypt and decrypt write *different* files (rather than overwriting in place)
 * so the demo can show all three files side by side in the UI for educational
 * purposes — this differs from CryptoVideoServlet, which renames in place.
 */
@WebServlet(name = "CryptoXmlServlet", urlPatterns = {"/CryptoXmlServlet"})
public class CryptoXmlServlet extends HttpServlet {

    // Demo passphrase.
    // In any real system this would be loaded from a secret manager / env var.
    private static final String PASSPHRASE = "glimpse-xml-aes-key-2026-fib-upc";

    // Absolute, normalised path so every check is comparable (avoids the
    // surprise where a relative path passes a startsWith() check that a
    // resolved absolute path would fail, or vice versa).
    private static final Path XML_DIR =
        Paths.get(System.getProperty("user.home") + File.separator + "glimpse-xml")
             .toAbsolutePath().normalize();

    private static final String PLAIN_FILE     = "didlFilm1.xml";
    private static final String ENCRYPTED_FILE = "didlFilm1_encrypted.xml";
    private static final String DECRYPTED_FILE = "didlFilm1_decrypted.xml";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        // 1. Authn, only logged-in users may touch the file.
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        // 2. CSRF, the form on security.jsp posts a hidden token from the
        //    session; an attacker on another origin can't read that token, so
        //    a forged POST will be rejected here.
        if (!Csrf.isValid(request, session)) {
            session.setAttribute("error", "Security token missing or expired.");
            response.sendRedirect("security.jsp");
            return;
        }

        // 3. Validate the requested action (whitelist, not blacklist).
        String action = request.getParameter("action");
        if (!"encrypt".equals(action) && !"decrypt".equals(action)) {
            session.setAttribute("error", "Invalid action.");
            response.sendRedirect("security.jsp");
            return;
        }

        try {
            // 4. Make sure the seed file exists. First-run case: copy from the
            //    webapp (read-only template) into the user-home directory
            //    (read/write working area).
            ensurePlainFile();

            byte[] key = deriveKey(PASSPHRASE);
            Path plainPath     = XML_DIR.resolve(PLAIN_FILE);
            Path encryptedPath = XML_DIR.resolve(ENCRYPTED_FILE);
            Path decryptedPath = XML_DIR.resolve(DECRYPTED_FILE);

            if ("encrypt".equals(action)) {
                // Parse the plain XML → DOM, hand it to XmlCrypto which mutates
                // the DOM in place and writes the encrypted form to disk.
                Document doc = parseDocument(plainPath);
                XmlCrypto.encryptDocument(doc, encryptedPath.toString(), key);
                session.setAttribute("success",
                    "XML document encrypted to " + encryptedPath.getFileName());
            } else {
                // Decrypt only makes sense after an encrypt has happened,
                // otherwise there's no <EncryptedData> file to read.
                if (!Files.exists(encryptedPath)) {
                    session.setAttribute("error",
                        "No encrypted XML file found. Encrypt first.");
                    response.sendRedirect("security.jsp");
                    return;
                }

                // XmlCrypto.decryptDocument returns the recovered DOM; we then
                // serialise it to the decrypted output file. (Asymmetric API:
                // encrypt writes itself, decrypt returns the DOM — see XmlCrypto.)
                Document doc = XmlCrypto.decryptDocument(encryptedPath.toString(), key);
                writeDocument(doc, decryptedPath);
                session.setAttribute("success",
                    "XML document decrypted to " + decryptedPath.getFileName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            session.setAttribute("error", "XML crypto operation failed: " + e.getMessage());
        }

        // PRG (Post/Redirect/Get) — redirect after a POST so refreshing the
        // result page doesn't re-submit the form. Flash messages live on the
        // session and are consumed once by security.jsp.
        response.sendRedirect("security.jsp");
    }

    /**
     * Copies the seed XML out of the WAR into a stable filesystem location the
     * first time this servlet is invoked. After that, the file persists across
     * redeploys, restarts, and resets.
     *
     * The seed lives in {@code src/main/webapp/xml/didlFilm1.xml} and is read
     * through the {@link ServletContext} (which abstracts whether the WAR is
     * exploded or still zipped).
     */
    private void ensurePlainFile() throws IOException {
        Files.createDirectories(XML_DIR);
        Path dst = XML_DIR.resolve(PLAIN_FILE);
        if (!Files.exists(dst)) {
            ServletContext ctx = getServletContext();
            try (InputStream in = ctx.getResourceAsStream("/xml/" + PLAIN_FILE)) {
                if (in == null) {
                    throw new IOException("Could not find /xml/" + PLAIN_FILE + " in webapp.");
                }
                Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Stretches a passphrase into a 16-byte AES-128 key.
     *
     * SHA-256 produces 32 bytes; AES-128 only wants 16. We truncate, which is
     * standard practice for a demo. In production you'd use PBKDF2 / scrypt /
     * Argon2 with a salt and many iterations to slow down brute force.
     */
    private byte[] deriveKey(String passphrase) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(passphrase.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16];
        System.arraycopy(hash, 0, keyBytes, 0, 16);
        return keyBytes;
    }

    /**
     * Parse with namespace awareness — see {@link security.XmlCrypto} for why
     * this matters when XMLEnc is involved.
     */
    private Document parseDocument(Path path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(path.toFile());
    }

    private void writeDocument(Document doc, Path path) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(doc), new StreamResult(path.toFile()));
    }
}
