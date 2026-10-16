package br.edu.unijorge.niteconnect.shared.api;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus status() { return status; }
    public static ApiException forbidden() { return new ApiException(HttpStatus.FORBIDDEN, "Acesso não autorizado"); }
    public static ApiException notFound() { return new ApiException(HttpStatus.NOT_FOUND, "Recurso não encontrado"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    public static ApiException invalid(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
}
