/**
 * Token class
 */
class Token {
    private final TokenType tokenType;
    private final String value;

    Token(TokenType tokenType, String value) {
        this.tokenType = tokenType;
        this.value = value;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Token{tokenType='%s', value='%s'}", tokenType, value);
    }

    enum TokenType {
        KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST
    }
}
