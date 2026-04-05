package isdcm.glimpse.rest;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS bootstrap.
 * Sets the base path so all REST endpoints are reachable at:
 *   http://localhost:8080/glimpse-rest/resources/...
 *
 * NOTE: DELETE ApplicationConfig.java — having two @ApplicationPath
 * classes in the same project causes a deployment conflict.
 */
@ApplicationPath("resources")
public class JakartaRestConfiguration extends Application {
    // Empty body — JAX-RS auto-discovers resource classes in this package.
}
