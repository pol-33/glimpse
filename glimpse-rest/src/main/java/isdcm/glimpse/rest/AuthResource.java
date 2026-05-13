package isdcm.glimpse.rest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * REST resource for authentication.
 *
 * POST /auth/login  =  validates credentials and returns a signed JWT.
 */
@Path("auth")
@Tag(name = "Authentication")
public class AuthResource {

    // 64-byte secret — sufficient for HS512. To be changed before any real deployment.
    private static final String JWT_SECRET =
        "glimpse-isdcm-jwt-secret-key-do-not-use-in-production-2026-fib-upc";

    private static final long EXPIRY_MS = 15 * 60 * 1000; // 15 minutes

    /**
     * POST /auth/login
     *
     * Accepts form-encoded username + password, validates them against the DB,
     * and returns a signed HS512 JWT on success.
     *
     * @param username  the account username
     * @param password  the account password (plaintext, transmitted over HTTPS)
     * @return 200 {"JWT":"<token>"} on success, 401 on bad credentials, 400 on missing input
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Login and obtain a JWT",
        description = "Validates username/password and returns a signed JSON Web Token (HS512, 15-min expiry).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful — JWT returned"),
            @ApiResponse(responseCode = "400", description = "Missing username or password"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
        }
    )
    public Response login(
            @FormParam("username") String username,
            @FormParam("password") String password) {

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("{\"error\":\"Username and password are required\"}")
                .build();
        }

        DBManager db = new DBManager();
        if (!db.validateLogin(username.trim(), password)) {
            return Response
                .status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"Invalid credentials\"}")
                .build();
        }

        Date now    = new Date();
        Date expiry = new Date(now.getTime() + EXPIRY_MS);

        String token = Jwts.builder()
            .setSubject(username.trim())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(SignatureAlgorithm.HS512,
                      JWT_SECRET.getBytes(StandardCharsets.UTF_8))
            .compact();

        return Response.ok("{\"JWT\":\"" + token + "\"}").build();
    }
}
