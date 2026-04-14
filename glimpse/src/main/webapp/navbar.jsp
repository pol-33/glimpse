<%@page import="jakarta.servlet.http.HttpSession"%>
<%@page import="security.Csrf"%>
<%@page import="util.ViewUtils"%>
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@700;800&family=DM+Sans:wght@400;500;600&display=swap" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
<link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.css" rel="stylesheet">

<style>
    :root {
        --glimpse-primary:   #4338CA;
        --glimpse-accent:    #6366F1;
        --glimpse-light:     #EEF2FF;
        --glimpse-dark:      #1E1B4B;
        --glimpse-text:      #1E293B;
        --glimpse-muted:     #64748B;
        --glimpse-border:    #E2E8F0;
        --glimpse-danger:    #DC2626;
        --glimpse-success:   #16A34A;
    }

    * { box-sizing: border-box; }

    body {
        font-family: 'DM Sans', sans-serif;
        background: #F8F9FF;
        color: var(--glimpse-text);
        min-height: 100vh;
    }

    h1, h2, h3, h4, .brand {
        font-family: 'Syne', sans-serif;
    }

    /* Navbar */
    .glimpse-nav {
        background: var(--glimpse-dark);
        border-bottom: 3px solid var(--glimpse-accent);
        padding: 0.75rem 0;
    }

    .glimpse-nav .brand {
        font-size: 1.6rem;
        font-weight: 800;
        letter-spacing: -0.5px;
        color: #fff;
        text-decoration: none;
    }

    .glimpse-nav .brand span {
        color: var(--glimpse-accent);
    }

    .glimpse-nav .nav-username {
        color: #A5B4FC;
        font-size: 0.9rem;
        font-weight: 500;
    }

    .glimpse-nav .btn-nav {
        border: 1.5px solid #6366F1;
        color: #A5B4FC;
        font-size: 0.85rem;
        padding: 0.3rem 0.9rem;
        border-radius: 6px;
        text-decoration: none;
        transition: all 0.2s;
    }
    
    .glimpse-nav .btn-nav:hover {
        background: var(--glimpse-accent);
        color: #fff;
    }
    
    .glimpse-nav .btn-nav-primary {
        background: var(--glimpse-accent);
        color: #fff;
        border-color: var(--glimpse-accent);
    }
    
    .glimpse-nav .btn-nav-primary:hover {
        background: var(--glimpse-primary);
        border-color: var(--glimpse-primary);
        color: #fff;
    }

    /* Cards */
    .glimpse-card {
        background: #fff;
        border: 1.5px solid var(--glimpse-border);
        border-radius: 16px;
        box-shadow: 0 4px 24px rgba(67, 56, 202, 0.07);
        padding: 2.5rem;
    }
    
    .glimpse-card h2 {
        font-size: 1.8rem;
        font-weight: 800;
        color: var(--glimpse-dark);
        margin-bottom: 0.25rem;
    }
    
    .glimpse-card .subtitle {
        color: var(--glimpse-muted);
        font-size: 0.95rem;
        margin-bottom: 2rem;
    }

    /* Form controls */
    .form-label {
        font-weight: 600;
        font-size: 0.85rem;
        color: var(--glimpse-muted);
        text-transform: uppercase;
        letter-spacing: 0.5px;
        margin-bottom: 0.35rem;
    }
    
    .form-control {
        border: 1.5px solid var(--glimpse-border);
        border-radius: 8px;
        padding: 0.65rem 1rem;
        font-family: 'DM Sans', sans-serif;
        font-size: 0.95rem;
        transition: border-color 0.2s, box-shadow 0.2s;
    }
    
    .form-control:focus {
        border-color: var(--glimpse-accent);
        box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
        outline: none;
    }

    /* Buttons */
    .btn-glimpse {
        background: var(--glimpse-primary);
        color: #fff;
        border: none;
        border-radius: 8px;
        padding: 0.7rem 1.5rem;
        font-family: 'DM Sans', sans-serif;
        font-weight: 600;
        font-size: 0.95rem;
        cursor: pointer;
        transition: background 0.2s, transform 0.1s;
        text-decoration: none;
        display: inline-block;
    }
    
    .btn-glimpse:hover {
        background: var(--glimpse-accent);
        color: #fff;
        transform: translateY(-1px);
    }
    
    .btn-glimpse-outline {
        background: transparent;
        color: var(--glimpse-primary);
        border: 1.5px solid var(--glimpse-primary);
        border-radius: 8px;
        padding: 0.4rem 1rem;
        font-family: 'DM Sans', sans-serif;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.2s;
        text-decoration: none;
        display: inline-block;
    }

    .btn-glimpse-outline:hover {
        background: var(--glimpse-light);
    }

    .btn-glimpse-danger {
        background: transparent;
        color: var(--glimpse-danger);
        border: 1.5px solid var(--glimpse-danger);
        border-radius: 8px;
        padding: 0.4rem 1rem;
        font-family: 'DM Sans', sans-serif;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.2s;
    }

    .btn-glimpse-danger:hover {
        background: #FEE2E2;
    }

    .btn-glimpse-like {
        background: var(--glimpse-light);
        color: var(--glimpse-primary);
        border: 1.5px solid #C7D2FE;
        border-radius: 8px;
        padding: 0.4rem 1rem;
        font-family: 'DM Sans', sans-serif;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .btn-glimpse-like:hover, .btn-glimpse-like.active {
        background: var(--glimpse-accent);
        color: #fff;
        border-color: var(--glimpse-accent);
    }

    /* Play button ? green tint so it stands out from Like */
    .btn-glimpse-play {
        background: #F0FDF4;
        color: #15803D;
        border: 1.5px solid #BBF7D0;
        border-radius: 8px;
        padding: 0.4rem 1rem;
        font-family: 'DM Sans', sans-serif;
        font-weight: 600;
        font-size: 0.85rem;
        cursor: pointer;
        transition: all 0.2s;
    }
    
    .btn-glimpse-play:hover {
        background: #16A34A;
        color: #fff;
        border-color: #16A34A;
    }

    /* Alerts */
    .alert-glimpse-error {
        background: #FEF2F2;
        border: 1.5px solid #FECACA;
        color: var(--glimpse-danger);
        border-radius: 10px;
        padding: 0.85rem 1.25rem;
        font-size: 0.9rem;
        font-weight: 500;
    }
    
    .alert-glimpse-info {
        background: var(--glimpse-light);
        border: 1.5px solid #C7D2FE;
        color: var(--glimpse-primary);
        border-radius: 10px;
        padding: 0.85rem 1.25rem;
        font-size: 0.9rem;
        font-weight: 500;
    }

    /* Table */
    .glimpse-table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0;
        background: #fff;
        border-radius: 12px;
        overflow: hidden;
        border: 1.5px solid var(--glimpse-border);
        box-shadow: 0 4px 24px rgba(67, 56, 202, 0.07);
    }
    
    .glimpse-table thead tr {
        background: var(--glimpse-dark);
        color: #fff;
    }
    
    .glimpse-table thead th {
        padding: 1rem 1.25rem;
        font-size: 0.8rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        border: none;
        font-family: 'DM Sans', sans-serif;
    }
    
    .glimpse-table tbody tr {
        border-bottom: 1px solid var(--glimpse-border);
        transition: background 0.15s;
    }

    .glimpse-table tbody tr:last-child {
        border-bottom: none;
    }

    .glimpse-table tbody tr:hover {
        background: #F8F9FF;
    }

    .glimpse-table tbody td {
        padding: 0.9rem 1.25rem;
        font-size: 0.9rem;
        vertical-align: middle;
    }

    .like-count {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-weight: 600;
        color: var(--glimpse-primary);
    }

    .page-title {
        font-size: 2rem;
        font-weight: 800;
        color: var(--glimpse-dark);
        margin-bottom: 0.25rem;
    }
    
    .page-subtitle {
        color: var(--glimpse-muted);
        font-size: 0.95rem;
        margin-bottom: 2rem;
    }

    .actions-cell form {
        display: inline;
    }
</style>

<%
    HttpSession navSession = request.getSession();
    boolean isLoggedIn = navSession != null && navSession.getAttribute("loggedUser") != null;
    String navUser = isLoggedIn ? (String) navSession.getAttribute("loggedUser") : "";
    String csrfToken = Csrf.ensureToken(navSession);
%>
<meta name="csrf-token" content="<%= ViewUtils.attr(csrfToken) %>">

<nav class="glimpse-nav">
    <div class="container d-flex align-items-center justify-content-between">
        <a href="<%= isLoggedIn ? "ListVideosServlet" : "login.jsp" %>" class="brand">
            Glimpse<span>.</span>
        </a>
        <% if (isLoggedIn) { %>
        <div class="d-flex align-items-center gap-3">
            <span class="nav-username">
                <i class="bi bi-person-circle me-1"></i><%= ViewUtils.h(navUser) %>
            </span>
            <%-- Search navigates to the search form page, not directly to the servlet --%>
            <a href="search.jsp" class="btn-nav">
                <i class="bi bi-search me-1"></i>Search
            </a>
            <a href="registerVideo.jsp" class="btn-nav btn-nav-primary">
                <i class="bi bi-plus-lg me-1"></i>New Video
            </a>
            <a href="logout.jsp" class="btn-nav">
                <i class="bi bi-box-arrow-right me-1"></i>Logout
            </a>
        </div>
        <% } %>
    </div>
</nav>
