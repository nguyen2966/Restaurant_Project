package softarch.restaurant.shared.exception;

import org.springframework.http.HttpStatus;

public class RestaurantException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public RestaurantException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    // ── Convenience factories ──────────────────────────────────────────────────

    public static RestaurantException notFound(String resource, Object id) {
        return new RestaurantException(
            "NOT_FOUND",
            resource + " not found with id: " + id,
            HttpStatus.NOT_FOUND
        );
    }

    public static RestaurantException conflict(String message) {
        return new RestaurantException("CONFLICT", message, HttpStatus.CONFLICT);
    }

    public static RestaurantException badRequest(String message) {
        return new RestaurantException("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
    }

    public static RestaurantException unprocessable(String message) {
        return new RestaurantException("UNPROCESSABLE", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public String getErrorCode()    { return errorCode; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}