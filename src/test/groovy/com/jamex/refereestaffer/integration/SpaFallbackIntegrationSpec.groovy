package com.jamex.refereestaffer.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
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
 * The shell served here is the stub in {@code src/test/resources/static}: the backend CI job builds
 * with {@code -DskipFrontend=true}, so the real Angular output is not on the classpath.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpaFallbackIntegrationSpec extends Specification {

    private static final String BROWSER_ACCEPT = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'

    @Value('${local.server.port}')
    int port

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

    def "a POST to an unknown API endpoint stays an error"() {
        when: 'the container reports the error dispatch as GET, so the path is what has to hold the line'
        def response = send(HttpRequest.newBuilder(uri('/api/definitely-not-an-endpoint'))
                .header('Accept', BROWSER_ACCEPT)
                .POST(HttpRequest.BodyPublishers.noBody()))

        then:
        response.statusCode() >= 400
        !response.body().contains('<app-root>')
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
