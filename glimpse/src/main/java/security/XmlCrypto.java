package security;

import org.apache.xml.security.Init;
import org.apache.xml.security.encryption.XMLCipher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * XML encryption / decryption using the W3C "XML Encryption" standard.
 *
 * Unlike {@link ContentCrypto}, which encrypts an opaque byte stream, this class
 * preserves XML structure: the output is still a well-formed XML document, but
 * the protected element is replaced by an <EncryptedData> node that carries
 * algorithm metadata and the ciphertext (base64). That way an XML toolchain can
 * recognise that the document is encrypted and route it to a decryption step.
 *
 * Library: Apache Santuario (org.apache.xml.security, a.k.a. "xmlsec").
 * Spec:    https://www.w3.org/TR/xmlenc-core1/
 */
public final class XmlCrypto {

    /**
     * Standard namespace for XML Encryption elements (<EncryptedData>, <CipherValue>, ...).
     * Defined by the W3C XMLEnc spec and hardcoded by the spec, never changes.
     */
    private static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";

    /**
     * Santuario must be bootstrapped once per JVM via Init.init() before any
     * XMLCipher call — it registers the algorithm URIs (AES, RSA, ...) into
     * an internal table. Calling it twice is harmless but wasteful, so we
     * gate it with a flag.
     */
    private static boolean initialized = false;

    private XmlCrypto() {}

    private static synchronized void ensureInit() {
        if (!initialized) {
            Init.init();
            initialized = true;
        }
    }

    /**
     * Encrypts {@code doc} in place and serialises the result to {@code outputPath}.
     *
     * The input document is mutated: after this call, its root element is no
     * longer the original (e.g. <DIDL>) but an <EncryptedData> wrapper.
     *
     * @param doc        parsed XML document — caller is responsible for parsing
     *                   with a namespace-aware DocumentBuilder
     * @param outputPath where to write the resulting XML file
     * @param key        raw 16-byte key material (AES-128)
     */
    public static void encryptDocument(Document doc, String outputPath, byte[] key) throws Exception {
        ensureInit();

        // SecretKeySpec wraps raw bytes as a SecretKey. "AES" tells the JCE the
        // algorithm family; the key length (128 / 192 / 256) is inferred from
        // the byte count. 16 bytes → AES-128.
        SecretKey secretKey = new SecretKeySpec(key, "AES");

        // XMLCipher is the XMLEnc-aware equivalent of javax.crypto.Cipher.
        // The algorithm constant identifies the W3C URI for AES-128 in CBC mode.
        XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_128);
        xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

        // doFinal(doc, element, contentOnly):
        //   contentOnly = false → replace the element itself (whole tag + children)
        //   contentOnly = true  → keep the wrapper tag, encrypt only its contents
        // We want the entire document hidden, so we pass the document element
        // (root) with contentOnly = false. The original root is replaced by an
        // <EncryptedData> element which becomes the new root.
        xmlCipher.doFinal(doc, doc.getDocumentElement(), false);

        writeDocument(doc, outputPath);
    }

    /**
     * Reads an XML file containing an <EncryptedData> element, decrypts it,
     * and returns the resulting plain {@link Document}.
     *
     * Why asymmetric with {@link #encryptDocument} (which writes to a path)?
     * On decrypt the caller often wants to inspect the DOM (validate against
     * an XSD, transform, etc.) before persisting. Returning the Document keeps
     * that flexibility; the servlet then serialises it where it needs to.
     */
    public static Document decryptDocument(String inputPath, byte[] key) throws Exception {
        ensureInit();
        SecretKey secretKey = new SecretKeySpec(key, "AES");

        Document doc = parseDocument(inputPath);

        // The encrypted XML must contain an <EncryptedData> element somewhere.
        // Because XMLEnc elements live in their own namespace, we look them up
        // by namespace+localName rather than by tag name — that's the *only*
        // reliable way when a document may declare its own namespaces.
        NodeList nodes = doc.getElementsByTagNameNS(XMLENC_NS, "EncryptedData");
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("No EncryptedData element found in " + inputPath);
        }
        Element encryptedDataElement = (Element) nodes.item(0);

        // For DECRYPT_MODE we don't pass an algorithm constant: XMLCipher
        // reads the <EncryptionMethod Algorithm="..."/> attribute from the
        // EncryptedData node itself, so the file is self-describing.
        XMLCipher xmlCipher = XMLCipher.getInstance();
        xmlCipher.init(XMLCipher.DECRYPT_MODE, secretKey);

        // Replaces the EncryptedData element with the recovered plain element
        // (mutates the document in place). After this call, the document tree
        // is back to its original structure.
        xmlCipher.doFinal(doc, encryptedDataElement);

        return doc;
    }

    /**
     * Parse with namespace awareness ON. This is *critical* for XMLEnc to work:
     * if the parser ignores namespaces, getElementsByTagNameNS won't find the
     * EncryptedData element and Santuario will refuse to decrypt.
     */
    private static Document parseDocument(String path) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        return builder.parse(new File(path));
    }

    private static void writeDocument(Document doc, String path) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(doc), new StreamResult(new File(path)));
    }
}
