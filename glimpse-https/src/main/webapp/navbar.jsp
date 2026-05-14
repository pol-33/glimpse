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
    }

    * { box-sizing: border-box; }

    body {
        font-family: 'DM Sans', sans-serif;
        background: #F8F9FF;
        color: var(--glimpse-text);
        min-height: 100vh;
    }

    h1, h2, h3, .brand {
        font-family: 'Syne', sans-serif;
    }

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

    .glimpse-nav .brand span { color: var(--glimpse-accent); }

    .glimpse-card {
        background: #fff;
        border: 1.5px solid var(--glimpse-border);
        border-radius: 16px;
        box-shadow: 0 4px 24px rgba(67, 56, 202, 0.07);
        padding: 2.5rem;
    }

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
        display: inline-block;
    }

    .btn-glimpse:hover {
        background: var(--glimpse-accent);
        color: #fff;
        transform: translateY(-1px);
    }

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
</style>

<%
    String csrfToken = Csrf.ensureToken(request.getSession());
%>

<nav class="glimpse-nav">
    <div class="container d-flex align-items-center">
        <a href="registerUser.jsp" class="brand">Glimpse<span>.</span></a>
        <span style="color:#A5B4FC; font-size:0.85rem; margin-left:1rem;">HTTPS Demo</span>
    </div>
</nav>
