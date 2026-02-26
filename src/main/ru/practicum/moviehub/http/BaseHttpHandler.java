package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // !!! Укажите содержимое заголовка Content-Type

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }

    }

    protected boolean isJson(HttpExchange ex) {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.equalsIgnoreCase(CT_JSON);
    }

    protected void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendError(ex, status, message, List.of());
    }

    protected void sendError(HttpExchange ex, int status, String message, List<String> details) throws IOException {
        Gson gson = new Gson();
        ErrorResponse error = new ErrorResponse(message, details);
        sendJson(ex, status, gson.toJson(error));
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
    }
}