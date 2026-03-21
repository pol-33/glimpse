<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Login</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <div class="container d-flex justify-content-center align-items-center" style="min-height: 88vh;">
        <div style="width: 100%; max-width: 440px;">

            <div class="text-center mb-4">
                <h1 style="font-size:2.5rem; font-weight:800; color:var(--glimpse-dark);">
                    Welcome back.
                </h1>
                <p style="color:var(--glimpse-muted);">Sign in to your Glimpse account.</p>
            </div>

            <div class="glimpse-card">
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert-glimpse-error mb-4">
                        <i class="bi bi-exclamation-circle me-2"></i><%= request.getAttribute("error") %>
                    </div>
                <% } %>
                <% if (request.getAttribute("info") != null) { %>
                    <div class="alert-glimpse-info mb-4">
                        <i class="bi bi-check-circle me-2"></i><%= request.getAttribute("info") %>
                    </div>
                <% } %>

                <form action="UserServlet" method="POST">
                    <input type="hidden" name="action" value="login">

                    <div class="mb-3">
                        <label class="form-label">Username</label>
                        <input type="text" name="username" class="form-control"
                               placeholder="your_username" required>
                    </div>

                    <div class="mb-4">
                        <label class="form-label">Password</label>
                        <input type="password" name="password" class="form-control"
                               placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn-glimpse w-100">
                        Sign in <i class="bi bi-arrow-right ms-1"></i>
                    </button>
                </form>
            </div>

            <p class="text-center mt-4" style="color:var(--glimpse-muted); font-size:0.9rem;">
                Don't have an account?
                <a href="registerUser.jsp" style="color:var(--glimpse-accent); font-weight:600;">
                    Register here
                </a>
            </p>
        </div>
    </div>
</body>
</html>