<%-- ???????????????????????????????????????????????????????????????????????????
     videoTable.jsp  ?  SHARED TABLE FRAGMENT
     ???????????????????????????????????????????????????????????????????????????
     Static include (<%@include file="videoTable.jsp"%>) used by:
       ? listVideos.jsp
       ? searchResults.jsp

     Expects the following variables already declared in the parent page:
       List<Video> videos       ? the list to render
       String      loggedUser   ? current user (from session)
       int         currentPage  ? used in delete form's hidden "page" field

     Likes are handled via AJAX (LikeAjaxServlet) so toggling a like never
     triggers a page reload. Delete still uses a standard form POST.
 ??????????????????????????????????????????????????????????????????????????? --%>

<div style="overflow-x: auto;">
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
                <th>File</th>
                <th>Likes</th>
                <th>Actions</th>
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
                    <span style="background:var(--glimpse-light); color:var(--glimpse-primary);
                                 padding:2px 10px; border-radius:20px;
                                 font-size:0.82rem; font-weight:600;">
                        <%= v.getAuthor() %>
                    </span>
                </td>
                <td style="color:var(--glimpse-muted); font-size:0.88rem;">
                    <%= v.getCreationDate() %>
                </td>
                <td style="font-size:0.88rem;"><%= v.getDuration() %></td>
                <td style="font-size:0.88rem;" id="views-<%= v.getId() %>">
                    <%= v.getViews() %>
                </td>
                <td>
                    <span style="background:#F1F5F9; color:var(--glimpse-muted);
                                 padding:2px 8px; border-radius:4px;
                                 font-size:0.8rem; font-family:monospace;">
                        <%= v.getFormat() %>
                    </span>
                </td>
                <td style="font-size:0.88rem; color:var(--glimpse-muted); max-width:180px;">
                    <%= v.getDescription() != null ? v.getDescription() : "" %>
                </td>
                <td>
                    <% if ("url".equals(v.getFileSource())) { %>
                        <a href="<%= v.getFilePath() %>" target="_blank"
                           style="color:var(--glimpse-accent); font-size:0.88rem; font-weight:600;">
                            <i class="bi bi-box-arrow-up-right me-1"></i>Open link
                        </a>
                    <% } else { %>
                        <span style="color:var(--glimpse-muted); font-size:0.85rem;">
                            <i class="bi bi-file-earmark-play me-1"></i>
                            <%= v.getOriginalFilename() %>
                        </span>
                    <% } %>
                </td>
                <td>
                    <%-- Like count badge ? updated live by JS --%>
                    <span class="like-count" id="like-count-<%= v.getId() %>">
                        <i class="bi bi-heart-fill" style="font-size:0.85rem;"></i>
                        <span class="like-num"><%= v.getLikeCount() %></span>
                    </span>
                </td>
                <td class="actions-cell">
                    <div class="d-flex gap-2 flex-wrap">

                        <%-- Play: only for uploaded files --%>
                        <% if (canPlay) { %>
                            <a href="PlayVideoServlet?id=<%= v.getId() %>"
                               class="btn-glimpse-play"
                               style="text-decoration:none; display:inline-block;">
                                <i class="bi bi-play-circle me-1"></i>Play
                            </a>
                        <% } %>

                        <%-- Like / Unlike ? AJAX, no page reload --%>
                        <button
                            class="btn-glimpse-like<%= v.isUserLiked() ? " active" : "" %>"
                            id="like-btn-<%= v.getId() %>"
                            data-id="<%= v.getId() %>"
                            data-liked="<%= v.isUserLiked() ? "true" : "false" %>"
                            onclick="toggleLike(this)">
                            <i class="bi bi-heart<%= v.isUserLiked() ? "-fill" : "" %> me-1"
                               id="like-icon-<%= v.getId() %>"></i>
                            <span id="like-label-<%= v.getId() %>">
                                <%= v.isUserLiked() ? "Unlike" : "Like" %>
                            </span>
                        </button>

                        <%-- Delete (own videos only) --%>
                        <% if (loggedUser.equals(v.getAuthor())) { %>
                            <form action="DeleteVideoServlet" method="POST"
                                  onsubmit="return confirm('Delete this video?');">
                                <input type="hidden" name="id"   value="<%= v.getId() %>">
                                <input type="hidden" name="page" value="<%= currentPage %>">
                                <button type="submit" class="btn-glimpse-danger">
                                    <i class="bi bi-trash me-1"></i>Delete
                                </button>
                            </form>
                        <% } %>

                    </div>
                </td>
            </tr>
        <%
            }
        %>
        </tbody>
    </table>
</div>

<script>
/**
 * AJAX like toggle ? shared by listVideos.jsp and searchResults.jsp.
 * Calls LikeAjaxServlet, animates the button and count on success.
 * On error, falls back gracefully (no change shown).
 */
function toggleLike(btn) {
    const videoId = btn.dataset.id;
    const isLiked = btn.dataset.liked === "true";

    // Disable immediately to prevent double-clicks
    btn.disabled = true;

    fetch("LikeAjaxServlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "id=" + encodeURIComponent(videoId)
    })
    .then(r => {
        if (!r.ok) throw new Error("HTTP " + r.status);
        return r.json();
    })
    .then(data => {
        const nowLiked   = data.liked;
        const newCount   = data.likeCount;

        // Update button state
        btn.dataset.liked = nowLiked ? "true" : "false";

        if (nowLiked) {
            btn.classList.add("active");
        } else {
            btn.classList.remove("active");
        }

        // Update icon
        const icon = document.getElementById("like-icon-" + videoId);
        if (icon) {
            icon.className = nowLiked
                ? "bi bi-heart-fill me-1"
                : "bi bi-heart me-1";
        }

        // Update label
        const label = document.getElementById("like-label-" + videoId);
        if (label) label.textContent = nowLiked ? "Unlike" : "Like";

        // Update the like count badge with a pop animation
        const countBadge = document.getElementById("like-count-" + videoId);
        if (countBadge) {
            const numSpan = countBadge.querySelector(".like-num");
            if (numSpan) numSpan.textContent = newCount;
            countBadge.classList.remove("like-pop");
            // Trigger reflow to restart animation
            void countBadge.offsetWidth;
            countBadge.classList.add("like-pop");
        }
    })
    .catch(() => {
        // Silent fail ? just re-enable the button
    })
    .finally(() => {
        btn.disabled = false;
    });
}
</script>
