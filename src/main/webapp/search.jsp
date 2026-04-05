<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Search</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }
    %>

    <div class="container py-5">
        <div style="max-width: 640px; margin: 0 auto;">

            <a href="ListVideosServlet"
               style="color:var(--glimpse-accent); font-size:0.9rem; font-weight:600;
                      text-decoration:none; display:inline-flex; align-items:center;
                      gap:4px; margin-bottom:1.5rem;">
                <i class="bi bi-arrow-left"></i> Back to all videos
            </a>

            <h1 class="page-title">Search Videos</h1>
            <p class="page-subtitle">
                Find videos by title, author, or upload date. All fields are optional.
            </p>

            <div class="glimpse-card">
                <form action="SearchServlet" method="GET">

                    <div class="mb-3">
                        <label class="form-label">Title</label>
                        <input type="text" name="title" class="form-control"
                               placeholder="Any word in the title">
                    </div>

                    <div class="mb-3">
                        <label class="form-label">Author (username)</label>
                        <input type="text" name="author" class="form-control"
                               placeholder="e.g. john_doe">
                    </div>

                    <div class="mb-4">
                        <label class="form-label">Upload date</label>
                        <div class="row g-2">
                            <div class="col-4">
                                <input type="number" name="year" class="form-control"
                                       placeholder="Year" min="2000" max="2099">
                            </div>
                            <div class="col-4">
                                <input type="number" name="month" class="form-control"
                                       placeholder="Month" min="1" max="12">
                            </div>
                            <div class="col-4">
                                <input type="number" name="day" class="form-control"
                                       placeholder="Day" min="1" max="31">
                            </div>
                        </div>
                    </div>

                    <button type="submit" class="btn-glimpse w-100">
                        <i class="bi bi-search me-2"></i>Search
                    </button>

                </form>
            </div>
        </div>
    </div>
</body>
</html>
