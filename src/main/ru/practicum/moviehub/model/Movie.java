package ru.practicum.moviehub.model;

public class Movie {
    private String title;
    private int year;
    private long movieId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getId() {
        return movieId;
    }

    public void setId(long movieId) {
        this.movieId = movieId;
    }
}