package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        switch (method.toUpperCase()) {
            case "GET" -> handleGet(ex);
            case "POST" -> {
                if (path.equals("/movies")) {
                    handlePost(ex);
                } else {
                    sendError(ex, 404, "Incorrect request format");
                }
            }
            case "DELETE" -> handleDeleteRequest(ex, path);
            default -> sendError(ex, 405, "Method Not Allowed");
        }
    }

    private void handleGetRequest(HttpExchange ex, String path) throws IOException {

        if (path.equals("/movies")) {
            handleGet(ex);
            return;
        }

        if (path.startsWith("/movies/")) {
            handleGetById(ex, path);
            return;
        }

        sendError(ex, 404, "Incorrect request format");
    }

    private void handleGet(HttpExchange ex) throws IOException {
        List<Movie> movies = store.getAll();

        Gson gson = new Gson();
        String json = gson.toJson(movies);

        sendJson(ex, 200, json);
    }

    private void handleGetById(HttpExchange ex, String path) throws IOException {
        String idStr = path.substring("/movies/".length());

        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID");
            return;
        }

        Movie movie = store.getById(id);

        if (movie == null) {
            sendError(ex, 404, "Фильм не найден");
            return;
        }

        sendJson(ex, 200, movie.getTitle());
    }

    private void handlePost(HttpExchange ex) throws IOException {
        Gson gson = new Gson();

        if(!isJson(ex)) {
            sendError(ex, 415, "Unsupported Media Type");
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);


        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            sendError(ex, 400, "Incorrect JSON");
            return;
        }

        List<String> errors = validate(movie);

        if (!errors.isEmpty()) {
            sendError(ex, 422, "Unprocessable Entity", errors);
            return;
        }

        Movie created = store.add(movie);
        sendJson(ex, 201, gson.toJson(created));
    }

    private void handleDeleteRequest(HttpExchange ex, String path) throws IOException {
        String[] parts = path.split("/");

        // Ожидаем: ["", "movies", "{id}"]
        if (parts.length != 3) {
            sendError(ex, 404, "Incorrect request format");
            return;
        }

        long id;
        try {
            id = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "Некорректный ID");
            return;
        }

        boolean removed = store.delete(id);

        if (!removed) {
            sendError(ex, 404, "Фильм не найден");
            return;
        }

        sendNoContent(ex);
    }

    private List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("длина названия не должна превышать 100 символов");
        }

        int currentYear = Year.now().getValue();

        if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }

        return errors;
    }
}
