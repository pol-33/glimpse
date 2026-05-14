<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="security.Csrf"%>
<%@page import="util.ViewUtils"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse HTTPS - Register</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <div class="container d-flex justify-content-center align-items-center py-5">
        <div style="width: 100%; max-width: 520px;">

            <div class="text-center mb-4">
                <h1 style="font-size:2.5rem; font-weight:800; color:var(--glimpse-dark);">
                    Join Glimpse.
                </h1>
                <p style="color:var(--glimpse-muted);">Create your account — securely over HTTPS.</p>
            </div>

            <div class="glimpse-card">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-glimpse-error mb-4">
                        <i class="bi bi-exclamation-circle me-2"></i><%= ViewUtils.h(request.getAttribute("error")) %>
                    </div>
                <% } %>
                <% if (request.getAttribute("info") != null) { %>
                    <div class="alert-glimpse-info mb-4">
                        <i class="bi bi-check-circle me-2"></i><%= ViewUtils.h(request.getAttribute("info")) %>
                    </div>
                <% } %>

                <form action="UserServlet" method="POST">
                    <input type="hidden" name="csrfToken"
                           value="<%= ViewUtils.attr(Csrf.ensureToken(request.getSession())) %>">

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Name</label>
                            <input type="text" name="name" class="form-control"
                                   placeholder="John" required>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Surname</label>
                            <input type="text" name="surname" class="form-control"
                                   placeholder="Doe" required>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Email</label>
                        <input type="email" name="email" class="form-control"
                               placeholder="john@example.com" required>
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" name="username" class="form-control"
                               placeholder="john_doe" required>
                    </div>

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label">Password</label>
                            <input type="password" name="password" class="form-control"
                                   placeholder="&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;" required>
                        </div>
                        <div class="col-md-6 mb-4">
                            <label class="form-label">Repeat Password</label>
                            <input type="password" name="confirmPassword" class="form-control"
                                   placeholder="&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;&#x2022;" required>
                        </div>
                    </div>

                    <button type="submit" class="btn-glimpse w-100">
                        Create account <i class="bi bi-arrow-right ms-1"></i>
                    </button>
                </form>
            </div>

        </div>
    </div>
</body>
</html>
