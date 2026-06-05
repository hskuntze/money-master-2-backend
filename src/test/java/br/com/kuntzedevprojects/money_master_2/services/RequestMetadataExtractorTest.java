package br.com.kuntzedevprojects.money_master_2.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import br.com.kuntzedevprojects.money_master_2.config.properties.SecurityHardeningProperties;

class RequestMetadataExtractorTest {

    @Test
    void shouldIgnoreForwardedHeadersFromUntrustedRemoteAddress() {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        RequestMetadataExtractor extractor = new RequestMetadataExtractor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("CF-Connecting-IP", "198.51.100.20");
        request.addHeader("X-Forwarded-For", "198.51.100.21");

        assertEquals("203.0.113.10", extractor.clientIp(request));
    }

    @Test
    void shouldUseCloudflareHeaderOnlyFromTrustedProxy() {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        RequestMetadataExtractor extractor = new RequestMetadataExtractor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("CF-Connecting-IP", "198.51.100.20");

        assertEquals("198.51.100.20", extractor.clientIp(request));
    }

    @Test
    void shouldRedactSensitiveQueryStringValues() {
        SecurityHardeningProperties properties = new SecurityHardeningProperties();
        RequestMetadataExtractor extractor = new RequestMetadataExtractor(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setQueryString("token=abc&name=hassan&refresh_token=xyz");

        assertEquals("token=[REDACTED]&name=hassan&refresh_token=[REDACTED]", extractor.safeQueryString(request));
    }
}
