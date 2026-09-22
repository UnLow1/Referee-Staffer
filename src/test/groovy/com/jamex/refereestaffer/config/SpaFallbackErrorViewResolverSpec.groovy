package com.jamex.refereestaffer.config

import jakarta.servlet.RequestDispatcher
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit coverage for the SPA deep-link fallback (RS-98). The branching lives here because the
 * resolver only ever sees the error dispatch: the sibling integration spec proves the wiring end
 * to end, this one pins down which requests are SPA navigations and which are honest 404s.
 */
class SpaFallbackErrorViewResolverSpec extends Specification {

    def resolver = new SpaFallbackErrorViewResolver()

    @Unroll
    def "serves the Angular shell for client-side route #path"() {
        given:
        def request = errorRequest(path)

        when:
        def modelAndView = resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:])

        then:
        modelAndView.viewName == SpaFallbackErrorViewResolver.INDEX_VIEW_NAME
        // The shell is a successful response — BasicErrorController's 404 has to be overridden,
        // otherwise the browser renders the app under an error status.
        modelAndView.status == HttpStatus.OK

        where:
        path << ['/referees', '/matches/12', '/import', '/addVacation/3', '/some/unknown/angular/route']
    }

    def "keeps the requested URL by forwarding instead of redirecting"() {
        expect: 'the Angular router bootstraps from window.location, so the address bar must not change'
        SpaFallbackErrorViewResolver.INDEX_VIEW_NAME.startsWith('forward:')
    }

    @Unroll
    def "leaves #path to the error handling it belongs to"() {
        expect:
        resolver.resolveErrorView(errorRequest(path), HttpStatus.NOT_FOUND, [:]) == null

        where:
        path                          | _
        '/api/referees/999'           | _   // shaped like a real API call, which must never get the shell
        '/api'                        | _
        '/v3/api-docs/swagger-config' | _
        '/swagger-ui/index.html'      | _
        '/actuator/health'            | _
        '/error'                      | _
        '/main-K7QZ4T3I.js'           | _   // a missing bundle must not be answered with HTML
        '/styles-ABC123.css'          | _
        '/favicon.ico'                | _
        '/assets/logo.svg'            | _
    }

    def "does not mistake a route that merely starts with a server prefix for an API call"() {
        expect: '/apiary is an Angular route, /api/... is not'
        resolver.resolveErrorView(errorRequest('/apiary'), HttpStatus.NOT_FOUND, [:]) != null
    }

    @Unroll
    def "keeps a #method request to a server path off the shell"() {
        given: 'the container reports the error dispatch as GET, so only the path can rule this out'
        def request = errorRequest('/api/referees')
        request.method = method

        expect:
        resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) == null

        where:
        method << ['POST', 'PUT', 'DELETE', 'PATCH']
    }

    @Unroll
    def "ignores status #status"() {
        expect:
        resolver.resolveErrorView(errorRequest('/referees'), status, [:]) == null

        where:
        status << [HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_REQUEST, HttpStatus.METHOD_NOT_ALLOWED]
    }

    @Unroll
    def "resolves to #expectedShell for Accept header '#accept'"() {
        given:
        def request = errorRequest('/referees')
        request.removeHeader(HttpHeaders.ACCEPT)
        if (accept != null) {
            request.addHeader(HttpHeaders.ACCEPT, accept)
        }

        expect:
        (resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) != null) == expectedShell

        where:
        accept                                | expectedShell
        'text/html,application/xhtml+xml'     | true
        '*/*'                                 | true
        null                                  | true
        ''                                    | true
        'application/json'                    | false
        'application/problem+json'            | false
        'not-a-media-type'                    | false   // unparseable — fall back to the plain 404
    }

    def "reads the original path, not the /error the container dispatched to"() {
        given: 'the error dispatch rewrites the URI, so only the attribute carries the requested path'
        def request = errorRequest('/api/referees/999')
        request.requestURI = '/error'

        expect:
        resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) == null
    }

    def "falls back to the request URI when the error attribute is missing"() {
        given:
        def request = new MockHttpServletRequest('GET', '/referees')
        request.addHeader(HttpHeaders.ACCEPT, 'text/html')

        expect:
        resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) != null
    }

    def "strips the context path before classifying the route"() {
        given:
        def request = errorRequest('/staffer-app/api/referees/999')
        request.contextPath = '/staffer-app'

        expect: 'without stripping, the path would not match the /api prefix and would leak the shell'
        resolver.resolveErrorView(request, HttpStatus.NOT_FOUND, [:]) == null
    }

    private static MockHttpServletRequest errorRequest(String path) {
        def request = new MockHttpServletRequest('GET', '/error')
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path)
        request.addHeader(HttpHeaders.ACCEPT, 'text/html,application/xhtml+xml,*/*;q=0.8')
        return request
    }
}
