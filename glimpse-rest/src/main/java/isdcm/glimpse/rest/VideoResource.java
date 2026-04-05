package isdcm.glimpse.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for videos.
 *
 * Base URL: http://localhost:8080/glimpse-rest/resources/videos
 *
 * ┌──────────┬─────────────────────────────┬──────────────────────────┬─────────────┐
 * │ Method   │ Path                        │ Input                    │ Output      │
 * ├──────────┼─────────────────────────────┼──────────────────────────┼─────────────┤
 * │ PUT      │ /videos/{id}/views          │ path param               │ JSON object │
 * │ GET      │ /videos/search              │ query params             │ JSON array  │
 * │ POST     │ /videos/search              │ form-encoded body        │ JSON array  │
 * └──────────┴─────────────────────────────┴──────────────────────────┴─────────────┘
 */
@Path("videos")
public class VideoResource {

    // ── PUT /videos/{id}/views ────────────────────────────────────────────────
    //
    // Called by glimpse every time a user starts playing an uploaded video.
    // Method : PUT  — partial update of an existing resource (the view counter).
    // Input  : {id} path parameter.
    // Output : JSON  {"id":<int>, "views":<int>}

    @PUT
    @Path("{id}/views")
    @Produces(MediaType.APPLICATION_JSON)
    public Response incrementViews(@PathParam("id") int id) {
        DBManager db = new DBManager();
        int newViews = db.incrementViews(id);

        if (newViews < 0) {
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Video not found\"}")
                .build();
        }

        return Response.ok("{\"id\":" + id + ",\"views\":" + newViews + "}").build();
    }

    // ── GET /videos/search ────────────────────────────────────────────────────
    //
    // Searches videos via URL query parameters. All parameters are optional.
    // Method : GET
    // Input  : query params — title, author, year, month, day  (all optional)
    // Output : JSON array of video objects
    //
    // Example: GET /resources/videos/search?author=alice&year=2026

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchGet(
            @QueryParam("title")  String title,
            @QueryParam("author") String author,
            @QueryParam("year")   String year,
            @QueryParam("month")  String month,
            @QueryParam("day")    String day) {

        return doSearch(title, author, year, month, day);
    }

    // ── POST /videos/search ───────────────────────────────────────────────────
    //
    // Same semantics as GET search. Offered as POST so the glimpse web app
    // can submit a standard HTML form without params in the URL.
    // Method  : POST
    // Input   : application/x-www-form-urlencoded (title, author, year, month, day)
    // Output  : JSON array of video objects

    @POST
    @Path("search")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchPost(
            @FormParam("title")  String title,
            @FormParam("author") String author,
            @FormParam("year")   String year,
            @FormParam("month")  String month,
            @FormParam("day")    String day) {

        return doSearch(title, author, year, month, day);
    }

    // ── Shared search logic ───────────────────────────────────────────────────

    private Response doSearch(String title, String author,
                              String year, String month, String day) {
        Integer y = toInt(year);
        Integer m = toInt(month);
        Integer d = toInt(day);

        DBManager db = new DBManager();
        List<DBManager.VideoInfo> videos = db.searchVideos(title, author, y, m, d);

        return Response.ok(toJson(videos)).build();
    }

    // ── Hand-rolled JSON serialisation (no extra dependencies) ───────────────

    private String toJson(List<DBManager.VideoInfo> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            DBManager.VideoInfo v = list.get(i);
            sb.append("{")
              .append("\"id\":").append(v.id).append(",")
              .append("\"title\":").append(qs(v.title)).append(",")
              .append("\"author\":").append(qs(v.author)).append(",")
              .append("\"creationDate\":").append(qs(v.creationDate)).append(",")
              .append("\"duration\":").append(qs(v.duration)).append(",")
              .append("\"views\":").append(v.views).append(",")
              .append("\"description\":").append(qs(v.description)).append(",")
              .append("\"format\":").append(qs(v.format)).append(",")
              .append("\"fileSource\":").append(qs(v.fileSource))
              .append("}");
        }
        return sb.append("]").toString();
    }

    /** Wraps a value as a JSON quoted string, handling nulls and escapes. */
    private String qs(String v) {
        if (v == null) return "null";
        return "\"" + v.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    private Integer toInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
