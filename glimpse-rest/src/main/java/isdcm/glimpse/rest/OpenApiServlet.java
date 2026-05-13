package isdcm.glimpse.rest;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@WebServlet(name = "OpenApiServlet", urlPatterns = {"/openapi.json"})
public class OpenApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Set<Class<?>> applicationClasses = findApplicationClasses();
        String applicationPath = findApplicationPath(applicationClasses);
        Set<Class<?>> resourceClasses = findResourceClasses(applicationClasses);

        OpenAPI openApi = new OpenAPI()
            .info(new Info()
                .title("Glimpse REST API")
                .version("1.0.0")
                .description("Automatically generated from the deployed JAX-RS resources."))
            .addServersItem(new Server().url(serverUrl(request, applicationPath)));

        OpenAPI generatedOpenApi = new Reader(openApi).read(resourceClasses);
        Json.pretty().writeValue(response.getWriter(), generatedOpenApi);
    }

    private Set<Class<?>> findResourceClasses(Set<Class<?>> applicationClasses) {
        return applicationClasses.stream()
            .filter(candidate -> candidate.isAnnotationPresent(Path.class))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String findApplicationPath(Set<Class<?>> applicationClasses) {
        return applicationClasses.stream()
            .filter(Application.class::isAssignableFrom)
            .filter(candidate -> candidate.isAnnotationPresent(ApplicationPath.class))
            .sorted(Comparator.comparing(Class::getName))
            .map(candidate -> candidate.getAnnotation(ApplicationPath.class).value())
            .findFirst()
            .orElse("");
    }

    private Set<Class<?>> findApplicationClasses() {
        Set<String> classNames = new LinkedHashSet<>();
        scanServletClasses("/WEB-INF/classes/", classNames);

        if (classNames.isEmpty()) {
            scanClassLoaderRoot(classNames);
        }

        List<String> sortedClassNames = new ArrayList<>(classNames);
        Collections.sort(sortedClassNames);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        Set<Class<?>> classes = new LinkedHashSet<>();
        for (String className : sortedClassNames) {
            try {
                classes.add(Class.forName(className, false, classLoader));
            } catch (ClassNotFoundException | LinkageError ignored) {
                // Unloadable classes cannot contribute JAX-RS annotations.
            }
        }
        return classes;
    }

    private void scanServletClasses(String resourcePath, Set<String> classNames) {
        Set<String> resources = getServletContext().getResourcePaths(resourcePath);
        if (resources == null) {
            return;
        }

        for (String resource : resources) {
            if (resource.endsWith("/")) {
                scanServletClasses(resource, classNames);
            } else if (resource.endsWith(".class") && !resource.contains("$")) {
                classNames.add(resource.substring("/WEB-INF/classes/".length(), resource.length() - ".class".length())
                    .replace('/', '.'));
            }
        }
    }

    private void scanClassLoaderRoot(Set<String> classNames) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        URL root = classLoader.getResource("");
        if (root == null || !"file".equals(root.getProtocol())) {
            return;
        }

        try {
            java.nio.file.Path rootPath = Paths.get(root.toURI());
            try (Stream<java.nio.file.Path> files = Files.walk(rootPath)) {
                files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .map(path -> rootPath.relativize(path).toString())
                    .map(path -> path.substring(0, path.length() - ".class".length()))
                    .map(path -> path.replace(java.io.File.separatorChar, '.'))
                    .forEach(classNames::add);
            }
        } catch (IOException | URISyntaxException ignored) {
            // Servlet resource scanning is preferred; this is only a fallback.
        }
    }

    private String serverUrl(HttpServletRequest request, String applicationPath) {
        return request.getScheme() + "://" + request.getServerName()
            + ":" + request.getServerPort()
            + request.getContextPath()
            + normalizedPath(applicationPath);
    }

    private String normalizedPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? "" : "/" + normalized;
    }
}
