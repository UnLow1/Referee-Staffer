package com.jamex.refereestaffer.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;

/**
 * Serves the Angular shell for client-side routes so that deep links and page refreshes work
 * when the jar hosts the SPA (RS-98).
 *
 * <p>Spring only knows {@code static/index.html} as the welcome page for {@code /}. Every other
 * Angular route ({@code /referees}, {@code /matches/12}, …) matches no controller and no static
 * resource, so opening it directly — or pressing F5 on it — used to end in a whitelabel 404. The
 * Vite dev server has no such problem, which is why this only ever showed up on the packaged jar.
 *
 * <p>The fallback hooks into error handling rather than mapping a catch-all route: a route list
 * would have to be kept in sync with {@code app.routes.ts} forever, while a 404 hook automatically
 * covers every route Angular gains later. {@code BasicErrorController} consults this resolver only
 * for requests that negotiate to HTML, and the checks below narrow that further — the fallback must
 * never swallow a genuine API 404 and turn it into an HTML page with status 200.
 *
 * <p>Defining an {@link ErrorViewResolver} bean switches off Spring Boot's convention-based
 * {@code DefaultErrorViewResolver} ({@code @ConditionalOnMissingBean}), which looks for
 * {@code error/404.html} / {@code error/4xx.html} templates. The project ships none, so that
 * resolver returns null on every request today and nothing is lost; add such a template and this
 * class has to delegate to it.
 */
@Component
public class SpaFallbackErrorViewResolver implements ErrorViewResolver {

    /**
     * Forward (not redirect) so the browser keeps the requested URL — that URL is what the Angular
     * router reads on bootstrap to render the right screen.
     */
    static final String INDEX_VIEW_NAME = "forward:/index.html";

    /**
     * Path namespaces that are server-side, never Angular routes. A 404 below one of them is a real
     * 404 and must stay one: {@code /api/nope} has to answer with a ProblemDetail, not the SPA.
     * {@code /actuator} is listed ahead of the health endpoint planned in RS-64.
     */
    private static final List<String> SERVER_PREFIXES = List.of("/api", "/swagger-ui", "/v3/api-docs", "/actuator", "/error");

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        if (!isSpaNavigation(request, status)) {
            return null;
        }
        // BasicErrorController has already put 404 on the response; the SPA shell is a successful
        // response, so the status is overridden here (DispatcherServlet applies ModelAndView#getStatus).
        return new ModelAndView(INDEX_VIEW_NAME, HttpStatus.OK);
    }

    /**
     * Note what cannot be checked here: the HTTP method. Tomcat presents the error dispatch as a
     * GET regardless of the original request, so {@code POST /referees} answers 200 with the shell
     * where it used to answer 404. Harmless — no browser posts to an Angular route, and every
     * server namespace is excluded by path below, whatever the method — and pinned by a spec so the
     * behaviour is on record rather than merely described.
     */
    private static boolean isSpaNavigation(HttpServletRequest request, HttpStatus status) {
        if (status != HttpStatus.NOT_FOUND) {
            return false;
        }
        var path = requestPath(request);
        return !isServerPath(path) && !looksLikeStaticAsset(path) && acceptsHtml(request);
    }

    /**
     * The original path, not {@code /error} — the container dispatches the error to the error page
     * and parks the requested URI in a request attribute.
     */
    private static String requestPath(HttpServletRequest request) {
        var uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String errorUri
                ? errorUri
                : request.getRequestURI();
        var contextPath = request.getContextPath();
        // Segment boundary, same as isServerPath: a bare startsWith would maul /application/x
        // into lication/x under a /app context path.
        if (StringUtils.hasText(contextPath) && (uri.equals(contextPath) || uri.startsWith(contextPath + "/"))) {
            uri = uri.substring(contextPath.length());
        }
        return StringUtils.hasText(uri) ? uri : "/";
    }

    private static boolean isServerPath(String path) {
        return SERVER_PREFIXES.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    /**
     * A missing asset ({@code /main-K7QZ4T3I.js}, {@code /favicon.ico}) must stay a 404: answering it
     * with HTML would feed the page a script that fails to parse instead of an honest error. Angular
     * routes carry no file extension, so a dot in the last segment is a good enough discriminator.
     *
     * <p>It cuts the other way too, and that is the constraint to remember: an Angular route whose
     * last segment contains a dot ({@code /referees/jan.kowalski}) would be read as an asset and
     * 404. Every route in {@code app.routes.ts} takes numeric ids today. Matching a list of known
     * extensions instead would fail the other way — an unrecognised extension would be answered
     * with the shell — so the dot stays, and the constraint is written down here and in CLAUDE.md.
     */
    private static boolean looksLikeStaticAsset(String path) {
        return path.substring(path.lastIndexOf('/') + 1).indexOf('.') >= 0;
    }

    /**
     * Belt and braces next to {@code BasicErrorController}'s own content negotiation: a client that
     * explicitly asks for JSON gets the ProblemDetail 404 even on an SPA-looking path. A request
     * with no Accept header, or a wildcard one, is treated as a browser navigation.
     */
    private static boolean acceptsHtml(HttpServletRequest request) {
        var accept = request.getHeader(HttpHeaders.ACCEPT);
        if (!StringUtils.hasText(accept)) {
            return true;
        }
        try {
            return MediaType.parseMediaTypes(accept).stream().anyMatch(type -> type.includes(MediaType.TEXT_HTML));
        } catch (InvalidMediaTypeException ex) {
            return false;
        }
    }
}
