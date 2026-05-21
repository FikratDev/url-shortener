package dev.fikrat.urlshortener.exception;

public class CodeNotFoundException extends RuntimeException {
    public CodeNotFoundException(String code) {
        super("Short URL not found: " + code);
    }
}
