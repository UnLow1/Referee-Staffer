package com.jamex.refereestaffer.integration

import com.jamex.refereestaffer.config.SpaFallbackErrorViewResolver
import jakarta.servlet.RequestDispatcher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import spock.lang.Specification
import spock.lang.Unroll

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * End-to-end net for the SPA deep-link fallback (RS-98).
 *
 * MockMvc cannot cover this: the fallback hangs off the servlet container's error dispatch to
 * {@code /error}, which MockMvc does not perform. Hence a real Tomcat on a random port driven with
 * the JDK HTTP client — the same path a browser takes when someone opens a deep link or presses F5.
 *
 * <p>The shell served here is the stub in {@code src/test/resources/spa-stub}: the backend CI job
 * builds with {@code -DskipFrontend=true}, so the real Angular output is not on the classpath. The
 * static locations are repointed for this context only — a stub under {@code static/} would come
 * first on the classpath and shadow the real shell for every other spec.
 *
 * <p>The assertions are deliberately data-free (status, content type, shell marker). This spec
 * shares the in-memory H2 with the other integration specs, which wipe and seed domain tables in
 * their setup; asserting on a payload here would make the build go red at random.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = 'spring.web.resources.static-locations=classpath:/spa-stub/')
class SpaFallbackIntegrationSpec extends Specification {

    private static final String BROWSER_ACCEPT = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'

    @LocalServerPort
    int port

    @Autowired RequestMappingHandlerMapping handlerMapping
    @Autowired SpaFallbackErrorViewResolver resolver

    // NO_PROXY: the JDK client would otherwise consult the system proxy selector for a loopback call.
    def httpClient = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build()

    @Unroll
    def "GET #path typed into the address bar serves the Angular shell"() {
        when:
        def response = browserGet(path)

        then: 'status 200, not the 404 the error dispatch started with'
        response.statusCode() == 200
        contentType(response).startsWith('text/html')
        response.body().contains('<app-root>')

        where:
        path << ['/referees', '/matches/12', '/import', '/standings', '/addTeam/7', '/route/angular/does/not/know']
    }

    def "the welcome page still resolves"() {
        when:
        def response = browserGet('/')

        then:
        response.statusCode() == 200
        response.body().contains('<app-root>')
    }

    def "an unknown API endpoint stays a 404 and never becomes the shell"() {
        when:
        def response = browserGet('/api/definitely-not-an-endpoint')

        then:
        response.statusCode() == 404
        !response.body().contains('<app-root>')
    }

    def "an existing API endpoint still answers with JSON"() {
        when:
        def response = browserGet('/api/referees')

        then: 'the fallback must not shadow the API the SPA itself calls'
        response.statusCode() == 200
        contentType(response).startsWith('application/json')
    }

    def "a missing asset stays a 404 instead of being answered with HTML"() {
        when: 'a stale bundle reference, as a cached index.html would produce'
        def response = browserGet('/main-K7QZ4T3I.js')

        then:
        response.statusCode() == 404
        !response.body().contains('<app-root>')
    }

    def "a JSON client asking for an unknown route gets an error, not the shell"() {
        when:
        def response = send(HttpRequest.newBuilder(uri('/referees')).header('Accept', 'application/json').GET())

        then:
        response.statusCode() == 404
        !response.body().contains('<app-root>')
    }

    def "no path the application maps would ever be answered with the shell"() {
        given: 'the exclusion list is hard-coded, so it is checked against the real server surface'
        def patterns = handlerMapping.handlerMethods.keySet()
                .collectMany { it.pathPatternsCondition?.patternValues ?: [] as Set }
                .collect { it.replaceAll(/\{[^}]+}/, '1') } as Set

        when: 'each of them is put through the resolver as a failed browser navigation'
        def leaked = patterns.findAll { String path ->
            def request = new MockHttpServletRequest('GET', '/error')
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path)
            request.addHeader('Accept', BROWSER_ACCEPT)
            resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) != null
        }

        then: 'a mapping added outside the excluded namespaces would answer a typo with 200 HTML'
        !patterns.isEmpty()
        leaked.isEmpty()
    }

    def "a POST to an unknown API endpoint stays an error"() {
        when: 'the container reports the error dispatch as GET, so the path is what has to hold the line'
        def response = send(HttpRequest.newBuilder(uri('/api/definitely-not-an-endpoint'))
                .header('Accept', BROWSER_ACCEPT)
                .POST(HttpRequest.BodyPublishers.noBody()))

        then:
        response.statusCode() >= 400
        !response.body().contains('<app-root>')
    }

    def "a POST to an unknown non-API path lands on the shell — the documented blind spot"() {
        when: 'Tomcat presents the error dispatch as a GET, so the method cannot be told apart'
        def response = send(HttpRequest.newBuilder(uri('/referees'))
                .header('Accept', BROWSER_ACCEPT)
                .POST(HttpRequest.BodyPublishers.noBody()))

        then: 'pinned rather than desired: change this assertion if the method ever becomes visible'
        response.statusCode() == 200
        response.body().contains('<app-root>')
    }

    private HttpResponse<String> browserGet(String path) {
        send(HttpRequest.newBuilder(uri(path)).header('Accept', BROWSER_ACCEPT).GET())
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    private URI uri(String path) {
        URI.create("http://localhost:$port$path")
    }

    private static String contentType(HttpResponse<String> response) {
        response.headers().firstValue('Content-Type').orElse('')
    }
}
