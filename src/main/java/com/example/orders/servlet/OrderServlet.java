package com.example.orders.servlet;

import com.example.orders.exception.OrderNotFoundException;
import com.example.orders.model.Order;
import com.example.orders.repository.InMemoryOrderRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;


@WebServlet(name = "OrderServlet", urlPatterns = {"/orders"})
public class OrderServlet extends HttpServlet {

    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    private final OrderRepository repository;
    private final ObjectMapper objectMapper;


    public OrderServlet() {
        this(new InMemoryOrderRepository());
    }


    public OrderServlet(OrderRepository repository) {
        this.repository = repository;
        this.objectMapper = JsonUtil.getObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Order> parsed = readOrderFromBody(req, resp);
        if (parsed.isEmpty()) {
            return;
        }

        Order created = repository.create(parsed.get());
        writeJson(resp, HttpServletResponse.SC_CREATED, created);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> id = parseIdParam(req, resp);
        if (id.isEmpty()) {
            return;
        }

        Optional<Order> order = repository.findById(id.get());
        if (order.isPresent()) {
            writeJson(resp, HttpServletResponse.SC_OK, order.get());
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Order with id=" + id.get() + " was not found");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> id = parseIdParam(req, resp);
        if (id.isEmpty()) {
            return;
        }

        Optional<Order> parsed = readOrderFromBody(req, resp);
        if (parsed.isEmpty()) {
            return;
        }

        try {
            Order updated = repository.update(id.get(), parsed.get());
            writeJson(resp, HttpServletResponse.SC_OK, updated);
        } catch (OrderNotFoundException e) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> id = parseIdParam(req, resp);
        if (id.isEmpty()) {
            return;
        }

        boolean deleted = repository.delete(id.get());
        if (deleted) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Order with id=" + id.get() + " was not found");
        }
    }



    private Optional<Long> parseIdParam(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String idParam = req.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Query parameter 'id' is required");
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(idParam));
        } catch (NumberFormatException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Query parameter 'id' must be a number");
            return Optional.empty();
        }
    }

    private Optional<Order> readOrderFromBody(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        if (body == null || body.isBlank()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Request body must contain an Order JSON object");
            return Optional.empty();
        }
        try {
            Order order = objectMapper.readValue(body, Order.class);
            return Optional.of(order);
        } catch (JsonProcessingException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON: " + e.getOriginalMessage());
            return Optional.empty();
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void writeJson(HttpServletResponse resp, int status, Object payload) throws IOException {
        resp.setStatus(status);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(message)));
    }

    /** Small DTO for JSON error bodies. */
    private static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
