package com.example.orders.servlet;

import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.model.Order;
import com.example.orders.model.Product;
import com.example.orders.repository.OrderRepository;
import com.example.orders.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServletTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OrderServlet servlet;
    private final ObjectMapper mapper = JsonUtil.getObjectMapper();
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new OrderServlet(repository);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    private void mockRequestBody(String json) throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(request.getReader()).thenReturn(reader);
    }

    private Order sampleOrder(Long id) {
        Product product = new Product(1L, "Keyboard", new BigDecimal("799.99"));
        return new Order(id, LocalDate.of(2026, 1, 15), new BigDecimal("799.99"), List.of(product));
    }

    // ---------- POST (create) ----------

    @Test
    void doPost_validBody_createsOrderAndReturns201() throws Exception {
        Order requestOrder = sampleOrder(null);
        Order savedOrder = sampleOrder(1L);

        when(request.getMethod()).thenReturn("POST");
        mockRequestBody(mapper.writeValueAsString(requestOrder));
        when(repository.create(any(Order.class))).thenReturn(savedOrder);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        verify(repository).create(any(Order.class));

        Order returned = mapper.readValue(responseWriter.toString(), Order.class);
        assertEquals(1L, returned.getId());
    }

    @Test
    void doPost_blankBody_returns400AndDoesNotCallRepository() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        mockRequestBody("");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(repository, never()).create(any());
    }

    @Test
    void doPost_malformedJson_returns400() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        mockRequestBody("{not-valid-json");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(repository, never()).create(any());
    }

    // ---------- GET (read) ----------

    @Test
    void doGet_existingId_returnsOrderAnd200() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn("1");
        when(repository.findById(1L)).thenReturn(Optional.of(sampleOrder(1L)));

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        Order returned = mapper.readValue(responseWriter.toString(), Order.class);
        assertEquals(1L, returned.getId());
        assertEquals("Keyboard", returned.getProducts().get(0).getName());
    }

    @Test
    void doGet_missingId_returnsOrderNotFoundAnd404() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn("42");
        when(repository.findById(42L)).thenReturn(Optional.empty());

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doGet_noIdParam_returns400AndDoesNotCallRepository() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn(null);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(repository, never()).findById(any());
    }

    @Test
    void doGet_nonNumericId_returns400() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getParameter("id")).thenReturn("abc");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(repository, never()).findById(any());
    }

    // ---------- PUT (update) ----------

    @Test
    void doPut_existingId_updatesOrderAndReturns200() throws Exception {
        Order updateRequest = sampleOrder(null);
        Order updatedOrder = sampleOrder(1L);

        when(request.getMethod()).thenReturn("PUT");
        when(request.getParameter("id")).thenReturn("1");
        mockRequestBody(mapper.writeValueAsString(updateRequest));
        when(repository.update(eq(1L), any(Order.class))).thenReturn(updatedOrder);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(repository).update(eq(1L), any(Order.class));

        Order returned = mapper.readValue(responseWriter.toString(), Order.class);
        assertEquals(1L, returned.getId());
    }

    @Test
    void doPut_nonExistingId_returns404() throws Exception {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getParameter("id")).thenReturn("99");
        mockRequestBody(mapper.writeValueAsString(sampleOrder(null)));
        when(repository.update(eq(99L), any(Order.class))).thenThrow(new OrderNotFoundException(99L));

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    // ---------- DELETE ----------

    @Test
    void doDelete_existingId_returns204() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getParameter("id")).thenReturn("1");
        when(repository.delete(1L)).thenReturn(true);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
        verify(repository, times(1)).delete(1L);
    }

    @Test
    void doDelete_nonExistingId_returns404() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getParameter("id")).thenReturn("123");
        when(repository.delete(123L)).thenReturn(false);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void doDelete_noIdParam_returns400AndDoesNotCallRepository() throws Exception {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getParameter("id")).thenReturn(null);

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(repository, never()).delete(any());
    }
}
