package dev.fikrat.urlshortener.exception;

public class CodeConflictException extends RuntimeException {
    public CodeConflictException(String code) {
        super("Custom code already in use: " + code);
    }
}
