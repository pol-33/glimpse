<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="model.Video"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Glimpse - Search Results</title>
    <%@include file="navbar.jsp"%>
</head>
<body>
    <%
        if (session.getAttribute("loggedUser") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        List<Video> videos    = (List<Video>) request.getAttribute("videos");
        String searchError    = (String) request.getAttribute("searchError");

        String qTitle  = (String) request.getAttribute("q_title");
        String qAuthor = (String) request.getAttribute("q_author");
        String qYear   = (String) request.getAttribute("q_year");
        String qMonth  = (String) request.getAttribute("q_month");
        String qDay    = (String) request.getAttribute("q_day");
        if (qTitle  == null) qTitle  = "";
        if (qAuthor == null) qAuthor = "";
        if (qYear   == null) qYear   = "";
        if (qMonth  == null) qMonth  = "";
        if (qDay    == null) qDay    = "";
    %>

    <div class="container py-5">

        <a href="search.jsp"
           style="color:var(--glimpse-accent); font-size:0.9rem; font-weight:600;
                  text-decoration:none; display:inline-flex; align-items:center;
                  gap:4px; margin-bottom:1.5rem;">
            <i class="bi bi-arrow-left"></i> New search
        </a>

        <h1 class="page-title">Search Results</h1>

        <%-- Show active filters as pills --%>
        <p class="page-subtitle" style="margin-bottom:1.5rem;">
            <% if (!qTitle.isEmpty())  { %><span style="background:var(--glimpse-light); color:var(--glimpse-primary); padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600; margin-right:6px;">Title: <%= qTitle %></span><% } %>
            <% if (!qAuthor.isEmpty()) { %><span style="background:var(--glimpse-light); color:var(--glimpse-primary); padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600; margin-right:6px;">Author: <%= qAuthor %></span><% } %>
            <% if (!qYear.isEmpty())   { %><span style="background:var(--glimpse-light); color:var(--glimpse-primary); padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600; margin-right:6px;">Year: <%= qYear %></span><% } %>
            <% if (!qMonth.isEmpty())  { %><span style="background:var(--glimpse-light); color:var(--glimpse-primary); padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600; margin-right:6px;">Month: <%= qMonth %></span><% } %>
            <% if (!qDay.isEmpty())    { %><span style="background:var(--glimpse-light); color:var(--glimpse-primary); padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600; margin-right:6px;">Day: <%= qDay %></span><% } %>
        </p>

        <%-- REST service error --%>
        <% if (searchError != null) { %>
            <div class="alert-glimpse-error mb-4">
                <i class="bi bi-wifi-off me-2"></i><%= searchError %>
            </div>
        <% } %>

        <%-- Results --%>
        <% if (videos != null && !videos.isEmpty()) { %>

            <p style="color:var(--glimpse-muted); font-size:0.9rem; margin-bottom:1rem;">
                <strong><%= videos.size() %></strong>
                video<%= videos.size() == 1 ? "" : "s" %> found.
            </p>

            <div style="overflow-x:auto;">
                <table class="glimpse-table">
                    <thead>
                        <tr>
                            <th>#</th>
                            <th>Title</th>
                            <th>Author</th>
                            <th>Date</th>
                            <th>Duration</th>
                            <th>Views</th>
                            <th>Format</th>
                            <th>Description</th>
                            <th>Play</th>
                        </tr>
                    </thead>
                    <tbody>
                    <%
                        for (Video v : videos) {
                            boolean canPlay = "upload".equals(v.getFileSource());
                    %>
                        <tr>
                            <td style="color:var(--glimpse-muted); font-size:0.8rem;">
                                <%= v.getId() %>
                            </td>
                            <td style="font-weight:600; color:var(--glimpse-dark);">
                                <%= v.getTitle() %>
                            </td>
                            <td>
                                <span style="background:var(--glimpse-light);
                                             color:var(--glimpse-primary);
                                             padding:2px 10px; border-radius:20px;
                                             font-size:0.82rem; font-weight:600;">
                                    <%= v.getAuthor() %>
                                </span>
                            </td>
                            <td style="color:var(--glimpse-muted); font-size:0.88rem;">
                                <%= v.getCreationDate() %>
                            </td>
                            <td style="font-size:0.88rem;"><%= v.getDuration() %></td>
                            <td style="font-size:0.88rem;"><%= v.getViews() %></td>
                            <td>
                                <span style="background:#F1F5F9; color:var(--glimpse-muted);
                                             padding:2px 8px; border-radius:4px;
                                             font-size:0.8rem; font-family:monospace;">
                                    <%= v.getFormat() %>
                                </span>
                            </td>
                            <td style="font-size:0.88rem; color:var(--glimpse-muted);
                                       max-width:180px;">
                                <%= v.getDescription() != null ? v.getDescription() : "" %>
                            </td>
                            <td>
                                <% if (canPlay) { %>
                                    <a href="PlayVideoServlet?id=<%= v.getId() %>"
                                       class="btn-glimpse-play"
                                       style="text-decoration:none; display:inline-block;">
                                        <i class="bi bi-play-circle me-1"></i>Play
                                    </a>
                                <% } else { %>
                                    <span style="color:var(--glimpse-muted); font-size:0.82rem;">
                                        <i class="bi bi-link-45deg me-1"></i>External URL
                                    </span>
                                <% } %>
                            </td>
                        </tr>
                    <%
                        }
                    %>
                    </tbody>
                </table>
            </div>

        <% } else if (searchError == null) { %>
            <div class="glimpse-card text-center py-5">
                <i class="bi bi-search" style="font-size:3rem; color:#C7D2FE;"></i>
                <p style="color:var(--glimpse-muted); margin-top:1rem; font-size:1.05rem;">
                    No videos matched your search.
                </p>
                <a href="search.jsp" class="btn-glimpse mt-2">
                    <i class="bi bi-arrow-left me-1"></i>Try a different search
                </a>
            </div>
        <% } %>

    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
