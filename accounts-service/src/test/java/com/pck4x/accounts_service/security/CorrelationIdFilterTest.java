package com.pck4x.accounts_service.security;

import com.pck4x.sharedcontracts.security.CorrelationContext;
import com.pck4x.sharedcontracts.security.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1.5: el correlation ID del gateway llega a MDC y a la respuesta. */
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void propagaElHeaderYLimpiaMDCAlTerminar() throws Exception {
        var request = new MockHttpServletRequest();
        var correlationId = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";
        request.addHeader(CorrelationIdFilter.HEADER, correlationId);
        var response = new MockHttpServletResponse();
        String[] seenInChain = new String[1];

        FilterChain chain = (req, res) -> seenInChain[0] = MDC.get(CorrelationIdFilter.MDC_KEY);
        filter.doFilter(request, response, chain);

        assertEquals(correlationId, seenInChain[0]);
        assertEquals(correlationId, response.getHeader(CorrelationIdFilter.HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void generaUnCorrelationIdCuandoElClienteNoLoEnvia() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertNotNull(response.getHeader(CorrelationIdFilter.HEADER));
        assertEquals(36, response.getHeader(CorrelationIdFilter.HEADER).length());
    }

    @Test
    void correlationContextCopiaElValorAlEvento() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        var event = new com.pck4x.sharedcontracts.event.AccountRejectedEvent();

        CorrelationContext.stamp(event);

        assertEquals("3f2504e0-4f89-11d3-9a0c-0305e82c3301", event.getCorrelationId().toString());
    }
}
