package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.practicum.moviehub.http.BaseHttpHandler.CT_JSON;
import static ru.practicum.moviehub.http.BaseHttpHandler.GSON;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();

    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals(CT_JSON, contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenHasMovies_returnsList() throws Exception {

        createMovie("Матрица", 1999);
        createMovie("Аватар", 2009);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());

        List<Movie> movies = GSON.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(2, movies.size());
    }

    @Test
    void getMovieById_whenExists_returns200() throws Exception {


        Movie movie = createMovie("Игра", 1997);

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(get, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("Игра"));
    }

    @Test
    void postMovie_whenValid_returns201() throws Exception {

        Movie movie = createMovie("Матрица", 1999);

        assertEquals("Матрица", movie.getTitle());
        assertEquals(1999, movie.getYear());
    }

    @Test
    void postMovie_whenEmptyTitle_returns422() throws Exception {

        Movie movie = new Movie();
        movie.setTitle("");
        movie.setYear(1999);

        String json = GSON.toJson(movie);

        HttpResponse<String> resp = sendPost(json);

        assertEquals(422, resp.statusCode());
    }

    @Test
    void postMovie_whenWrongContentType_returns415() throws Exception {

        Movie movie = new Movie();
        movie.setTitle("Матрица");
        movie.setYear(1999);

        String json = GSON.toJson(movie);

        HttpResponse<String> resp = sendPost(json, "text/plain");

        assertEquals(415, resp.statusCode());
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        // 1. Большой тест с комментариями для облегчения

        // Создаем фильм
        Movie movie = createMovie("Аватар", 2009);

        // 3. Удаляем по реальному id
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .DELETE()
                .build();

        HttpResponse<String> deleteResponse =
                client.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, deleteResponse.statusCode());

        // 4. Проверяем, что фильм реально удалён
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movie.getId()))
                .GET()
                .build();

        HttpResponse<String> getResponse =
                client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, getResponse.statusCode());
    }

    private HttpResponse<String> sendPost(String json, String type) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", type)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String json) throws Exception {
        return sendPost(json, CT_JSON);
    }

    private Movie createMovie(String title, int year) throws Exception {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setYear(year);

        String json = GSON.toJson(movie);

        HttpResponse<String> resp = sendPost(json, CT_JSON);

        assertEquals(201, resp.statusCode());

        return GSON.fromJson(resp.body(), Movie.class);
    }
}