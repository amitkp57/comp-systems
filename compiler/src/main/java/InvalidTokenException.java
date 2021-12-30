class InvalidTokenException extends RuntimeException {
    InvalidTokenException(Token token) {
        super(String.format("Invalid token '%s'!", token));
    }

    InvalidTokenException(String message) {
        super(message);
    }
}
