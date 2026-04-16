package isdcm.glimpse.rest;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "OpenApiServlet", urlPatterns = {"/openapi.json"})
public class OpenApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String serverUrl = request.getScheme() + "://" + request.getServerName()
            + ":" + request.getServerPort() + request.getContextPath();

        String json = "{"
            + "\"openapi\":\"3.0.3\","
            + "\"info\":{"
                + "\"title\":\"Glimpse REST API\","
                + "\"version\":\"1.0.0\","
                + "\"description\":\"Search videos and update view counters for the Glimpse project.\""
            + "},"
            + "\"servers\":[{\"url\":\"" + escape(serverUrl) + "\"}],"
            + "\"paths\":{"
                + "\"/resources/videos/{id}/views\":{"
                    + "\"put\":{"
                        + "\"summary\":\"Increment the view counter of a video\","
                        + "\"parameters\":[{"
                            + "\"name\":\"id\",\"in\":\"path\",\"required\":true,"
                            + "\"schema\":{\"type\":\"integer\"}"
                        + "}],"
                        + "\"responses\":{"
                            + "\"200\":{\"description\":\"Updated view count\"},"
                            + "\"404\":{\"description\":\"Video not found\"}"
                        + "}"
                    + "}"
                + "},"
                + "\"/resources/videos/search\":{"
                    + "\"get\":{"
                        + "\"summary\":\"Search videos with pagination\","
                        + "\"parameters\":["
                            + param("title", "query", "string", false, "Substring match on title") + ","
                            + param("author", "query", "string", false, "Substring match on author") + ","
                            + param("year", "query", "integer", false, "Creation year") + ","
                            + param("month", "query", "integer", false, "Creation month") + ","
                            + param("day", "query", "integer", false, "Creation day") + ","
                            + param("page", "query", "integer", false, "Zero-based page number") + ","
                            + param("pageSize", "query", "integer", false, "Number of items per page") + ","
                            + param("sort", "query", "string", false, "Sort by: likes_desc, likes_asc, views_desc, views_asc, date_desc, date_asc")
                        + "],"
                        + "\"responses\":{\"200\":{\"description\":\"Paginated search results\"}}"
                    + "},"
                    + "\"post\":{"
                        + "\"summary\":\"Search videos with form parameters\","
                        + "\"requestBody\":{"
                            + "\"required\":false,"
                            + "\"content\":{\"application/x-www-form-urlencoded\":{"
                                + "\"schema\":{"
                                    + "\"type\":\"object\","
                                    + "\"properties\":{"
                                        + "\"title\":{\"type\":\"string\"},"
                                        + "\"author\":{\"type\":\"string\"},"
                                        + "\"year\":{\"type\":\"integer\"},"
                                        + "\"month\":{\"type\":\"integer\"},"
                                        + "\"day\":{\"type\":\"integer\"},"
                                        + "\"page\":{\"type\":\"integer\"},"
                                        + "\"pageSize\":{\"type\":\"integer\"},"
                                        + "\"sort\":{\"type\":\"string\"}"
                                    + "}"
                                + "}"
                            + "}}"
                        + "},"
                        + "\"responses\":{\"200\":{\"description\":\"Paginated search results\"}}"
                    + "}"
                + "}"
            + "}"
        + "}";

        response.getWriter().write(json);
    }

    private String param(String name, String in, String type, boolean required, String description) {
        return "{"
            + "\"name\":\"" + escape(name) + "\","
            + "\"in\":\"" + escape(in) + "\","
            + "\"required\":" + required + ","
            + "\"description\":\"" + escape(description) + "\","
            + "\"schema\":{\"type\":\"" + escape(type) + "\"}"
        + "}";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
