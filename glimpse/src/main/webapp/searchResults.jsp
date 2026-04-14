<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="model.Video"%>
<%@page import="util.ViewUtils"%>
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

        // Same variable names as listVideos.jsp so videoTable.jsp works identically
        List<Video> videos  = (List<Video>) request.getAttribute("videos");
        String loggedUser   = (String) session.getAttribute("loggedUser");
        int currentPage     = 0; // no pagination for search results

        String searchError  = (String) request.getAttribute("searchError");

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

        <%-- Active filter pills --%>
        <p class="page-subtitle" style="margin-bottom:1.5rem; display:flex; flex-wrap:wrap; gap:6px;">
            <% if (!qTitle.isEmpty())  { %>
                <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                             padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600;">
                    Title: <%= ViewUtils.h(qTitle) %></span>
            <% } %>
            <% if (!qAuthor.isEmpty()) { %>
                <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                             padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600;">
                    Author: <%= ViewUtils.h(qAuthor) %></span>
            <% } %>
            <% if (!qYear.isEmpty())   { %>
                <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                             padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600;">
                    Year: <%= ViewUtils.h(qYear) %></span>
            <% } %>
            <% if (!qMonth.isEmpty())  { %>
                <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                             padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600;">
                    Month: <%= ViewUtils.h(qMonth) %></span>
            <% } %>
            <% if (!qDay.isEmpty())    { %>
                <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                             padding:2px 10px; border-radius:20px; font-size:0.82rem; font-weight:600;">
                    Day: <%= ViewUtils.h(qDay) %></span>
            <% } %>
        </p>

        <%-- REST offline error --%>
        <% if (searchError != null) { %>
            <div class="alert-glimpse-error mb-4">
                <i class="bi bi-wifi-off me-2"></i><%= ViewUtils.h(searchError) %>
            </div>
        <% } %>

        <%-- Results table --%>
        <% if (videos != null && !videos.isEmpty()) { %>

            <p style="color:var(--glimpse-muted); font-size:0.9rem; margin-bottom:1rem;">
                <strong><%= videos.size() %></strong>
                video<%= videos.size() == 1 ? "" : "s" %> found.
            </p>

            <%-- Shared table fragment --%>
            <%@include file="_videoTable.jsp"%>

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
