/**
 * Terminal node class
 */
class TerminalNode implements Node {
    private final Token token;

    TerminalNode(Token token) {
        this.token = token;
    }

    @Override
    public String getName() {
        String tagName = "";
        switch (token.getTokenType()) {
            case KEYWORD:
                tagName = "keyword";
                break;
            case SYMBOL:
                tagName = "symbol";
                break;
            case INT_CONST:
                tagName = "integerConstant";
                break;
            case STRING_CONST:
                tagName = "stringConstant";
                break;
            case IDENTIFIER:
                tagName = "identifier";
                break;
            default:
                throw new InvalidTokenException(token);
        }
        return tagName;
    }

    @Override
    public String getValue() {
        return this.token.getValue();
    }

    @Override
    public String toString() {
        String tagName = this.getName();
        String value = this.getValue();
        value = value.replace("&", "&amp;");
        value = value.replace("<", "&lt;");
        value = value.replace(">", "&gt;");
        value = value.replace("\"", "");
        return String.format("<%s>%s</%s>\n", tagName, value, tagName);

    }
}