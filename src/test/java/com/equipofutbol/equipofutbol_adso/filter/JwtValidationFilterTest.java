package com.equipofutbol.equipofutbol_adso.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtValidationFilterTest {

    private final TestableJwtValidationFilter filter = new TestableJwtValidationFilter();

    @Test
    void skipsCorsPreflightRequests() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/user/top5");

        assertThat(filter.shouldSkip(request)).isTrue();
    }

    @Test
    void skipsMonolithicFrontendStaticResources() {
        assertThat(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/v1/"))).isTrue();
        assertThat(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/v1/index.html"))).isTrue();
        assertThat(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/v1/styles.css"))).isTrue();
        assertThat(filter.shouldSkip(new MockHttpServletRequest("GET", "/api/v1/app.js"))).isTrue();
    }

    @Test
    void doesNotSkipProtectedNonAuthApiRoutesByDefault() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/private");

        assertThat(filter.shouldSkip(request)).isFalse();
    }

    private static class TestableJwtValidationFilter extends JwtValidationFilter {
        boolean shouldSkip(MockHttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
