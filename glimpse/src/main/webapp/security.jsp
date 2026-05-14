<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.io.File"%>
<%@page import="security.Csrf"%>
<%@page import="util.ViewUtils"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Security</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
<%
    if (session.getAttribute("loggedUser") == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    // Consume flash messages from session
    String successMsg = (String) session.getAttribute("success");
    String errorMsg   = (String) session.getAttribute("error");
    if (successMsg != null) session.removeAttribute("success");
    if (errorMsg   != null) session.removeAttribute("error");

    String xmlDir = System.getProperty("user.home") + File.separator + "glimpse-xml";
    File plainFile     = new File(xmlDir, "didlFilm1.xml");
    File encryptedFile = new File(xmlDir, "didlFilm1_encrypted.xml");
    File decryptedFile = new File(xmlDir, "didlFilm1_decrypted.xml");

    boolean hasPlain     = plainFile.exists();
    boolean hasEncrypted = encryptedFile.exists();
    boolean hasDecrypted = decryptedFile.exists();

    String currentStatus;
    if (hasDecrypted) {
        currentStatus = "Decrypted output available";
    } else if (hasEncrypted) {
        currentStatus = "Encrypted";
    } else if (hasPlain) {
        currentStatus = "Plain";
    } else {
        currentStatus = "Uninitialised — encrypt to seed the demo file";
    }
%>

<div class="container py-5">

    <h1 class="page-title">Security</h1>
    <p class="page-subtitle">
        Encrypt and decrypt the demo Digital Item XML, or jump to the videos list
        to operate on uploaded media files.
    </p>

    <% if (successMsg != null) { %>
        <div class="alert-glimpse-success mb-4">
            <i class="bi bi-check-circle me-2"></i><%= ViewUtils.h(successMsg) %>
        </div>
    <% } %>
    <% if (errorMsg != null) { %>
        <div class="alert-glimpse-error mb-4">
            <i class="bi bi-exclamation-circle me-2"></i><%= ViewUtils.h(errorMsg) %>
        </div>
    <% } %>

    <%-- ======================== XML Digital Item ======================== --%>
    <div class="glimpse-card mb-4">
        <h2>
            <i class="bi bi-file-earmark-code me-2"></i>XML Digital Item
        </h2>
        <p class="subtitle">
            Operates on <code><%= ViewUtils.h(xmlDir) %></code>.
            The demo file is seeded from the webapp on first encrypt.
        </p>

        <div class="mb-4">
            <div class="form-label">Current status</div>
            <span class="alert-glimpse-info" style="display:inline-block;">
                <i class="bi bi-info-circle me-1"></i><%= ViewUtils.h(currentStatus) %>
            </span>
        </div>

        <div class="mb-4">
            <div class="form-label mb-2">Files on disk</div>
            <table class="glimpse-table">
                <thead>
                    <tr>
                        <th>File</th>
                        <th>State</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><code>didlFilm1.xml</code></td>
                        <td>
                            <% if (hasPlain) { %>
                                <span style="color:var(--glimpse-success); font-weight:600;">
                                    <i class="bi bi-check-circle-fill me-1"></i>Present
                                </span>
                            <% } else { %>
                                <span style="color:var(--glimpse-muted);">
                                    <i class="bi bi-dash-circle me-1"></i>Not yet seeded
                                </span>
                            <% } %>
                        </td>
                        <td>
                            <% if (hasPlain) { %>
                                <a href="DownloadXmlServlet?file=didlFilm1.xml"
                                   class="btn-glimpse-outline"
                                   style="text-decoration:none;">
                                    <i class="bi bi-download me-1"></i>Download
                                </a>
                            <% } %>
                        </td>
                    </tr>
                    <tr>
                        <td><code>didlFilm1_encrypted.xml</code></td>
                        <td>
                            <% if (hasEncrypted) { %>
                                <span style="color:var(--glimpse-success); font-weight:600;">
                                    <i class="bi bi-check-circle-fill me-1"></i>Present
                                </span>
                            <% } else { %>
                                <span style="color:var(--glimpse-muted);">
                                    <i class="bi bi-dash-circle me-1"></i>Run Encrypt to generate
                                </span>
                            <% } %>
                        </td>
                        <td>
                            <% if (hasEncrypted) { %>
                                <a href="DownloadXmlServlet?file=didlFilm1_encrypted.xml"
                                   class="btn-glimpse-outline"
                                   style="text-decoration:none;">
                                    <i class="bi bi-download me-1"></i>Download
                                </a>
                            <% } %>
                        </td>
                    </tr>
                    <tr>
                        <td><code>didlFilm1_decrypted.xml</code></td>
                        <td>
                            <% if (hasDecrypted) { %>
                                <span style="color:var(--glimpse-success); font-weight:600;">
                                    <i class="bi bi-check-circle-fill me-1"></i>Present
                                </span>
                            <% } else { %>
                                <span style="color:var(--glimpse-muted);">
                                    <i class="bi bi-dash-circle me-1"></i>Run Decrypt to generate
                                </span>
                            <% } %>
                        </td>
                        <td>
                            <% if (hasDecrypted) { %>
                                <a href="DownloadXmlServlet?file=didlFilm1_decrypted.xml"
                                   class="btn-glimpse-outline"
                                   style="text-decoration:none;">
                                    <i class="bi bi-download me-1"></i>Download
                                </a>
                            <% } %>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <div class="d-flex gap-2 flex-wrap">
            <form action="CryptoXmlServlet" method="POST" style="display:inline;">
                <input type="hidden" name="action"    value="encrypt">
                <input type="hidden" name="csrfToken" value="<%= ViewUtils.attr(Csrf.ensureToken(session)) %>">
                <button type="submit" class="btn-glimpse">
                    <i class="bi bi-lock me-1"></i>Encrypt
                </button>
            </form>
            <form action="CryptoXmlServlet" method="POST" style="display:inline;">
                <input type="hidden" name="action"    value="decrypt">
                <input type="hidden" name="csrfToken" value="<%= ViewUtils.attr(Csrf.ensureToken(session)) %>">
                <button type="submit" class="btn-glimpse-outline">
                    <i class="bi bi-unlock me-1"></i>Decrypt
                </button>
            </form>
        </div>
    </div>

    <%-- ======================== Video Files ======================== --%>
    <div class="glimpse-card">
        <h2>
            <i class="bi bi-camera-video me-2"></i>Video Files
        </h2>
        <p class="subtitle">
            Per-video encryption is handled in the videos list. The Encrypt / Decrypt
            buttons appear in the actions column for videos you own that were uploaded
            locally (URL-only videos cannot be encrypted).
        </p>
        <a href="ListVideosServlet" class="btn-glimpse">
            <i class="bi bi-arrow-right me-1"></i>Go to Videos
        </a>
    </div>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
