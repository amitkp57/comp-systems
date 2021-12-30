import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates token out of the jack file
 */
class Tokenizer {
    private final static Set<String> KEYWORDS = new HashSet<>(Arrays.asList("class", "constructor", "function",
            "method", "field", "static", "var", "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return"));
    private final static Set<String> SYMBOLS = new HashSet<>(Arrays.asList("{", "}", "(", ")", "[", "]", ".", ",", ";",
            "+", "-", "*", "/", "&", "|", ",", "<", ">", "=", "~"));

    // white space characters
    private final static String EMPTY_LINE = "^[\\s]*$";
    private final static String SINGLE_LINE_COMMENT = "(//.*$)|(/\\*.*\\*/)";
    private final static String MULTI_LINE_COMMENT_START = "/\\*.*$";
    private final static String MULTI_LINE_COMMENT_END = "^.*\\*/";
    private final static Pattern INT_CONSTANT = Pattern.compile("^[0-9]+");
    private final static Pattern STR_CONSTANT = Pattern.compile("^\"[^\"]+\"");
    private final static Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_]+[0-9A-Za-z_]*");
    private final BufferedReader bufferedReader;
    private final Queue<Token> tokens = new LinkedList();
    private boolean multiLineComment = false;

    Tokenizer(File file) {
        try {
            this.bufferedReader = new BufferedReader(new FileReader(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Removes empty lines, single line comments
     *
     * @param content a line from a text
     * @return
     */
    private static String trimContent(String content) {
        String[] patterns = {EMPTY_LINE, SINGLE_LINE_COMMENT};
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher matcher = p.matcher(content);
            content = matcher.replaceAll("");
        }
        return content.trim();
    }

    /**
     * Writes output to the output file
     *
     * @param outputFile
     */
    public void write(File outputFile) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile))) {
            bufferedWriter.write("<tokens>\n");
            while (hasMoreTokens()) {
                bufferedWriter.write(new TerminalNode(pollToken()).toString());
            }
            bufferedWriter.write("</tokens>\n");
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns true if there are any more tokens to process else false
     *
     * @return
     */
    boolean hasMoreTokens() {
        if (tokens.isEmpty())
            loadNextLine();
        return !tokens.isEmpty();
    }

    /**
     * Polls the next token from the input file
     *
     * @return
     */
    public Token pollToken() {
        if (hasMoreTokens())
            return tokens.poll();
        else
            throw new InvalidTokenException("No more token!");
    }

    /**
     * Peeks the next token from the input file
     *
     * @return
     */
    public Token peekToken() {
        if (hasMoreTokens())
            return tokens.peek();
        else
            throw new InvalidTokenException("No more token!");
    }

    /**
     * Reads the next line from the input file and loads it to the tokens queue
     *
     * @return
     */
    private void loadNextLine() {
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) break;
                line = trimContent(line);
                Pattern multiLineStart = Pattern.compile(MULTI_LINE_COMMENT_START);
                Pattern multiLineEnd = Pattern.compile(MULTI_LINE_COMMENT_END);
                if (!line.isEmpty()) {
                    Matcher multiLIneStartMatcher = multiLineStart.matcher(line);
                    Matcher multiLIneEndMatcher = multiLineEnd.matcher(line);
                    if (multiLIneStartMatcher.find()) {
                        multiLineComment = true;
                        line = multiLIneStartMatcher.replaceFirst("").trim(); //return anything before comment starts
                    } else if (multiLIneEndMatcher.find()) {
                        multiLineComment = false;
                        line = multiLIneEndMatcher.replaceFirst("").trim();//return anything after comment ends
                    } else if (multiLineComment) {
                        continue;
                    }
                    if (!line.isEmpty()) {
                        enqueTokens(line);
                        break;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Adds tokens from a line to the queue
     *
     * @param line
     */
    private void enqueTokens(String line) {
        while (!line.isEmpty()) {
            String token = "";
            Token.TokenType tokenType = null;

            for (String symbol : SYMBOLS) {
                if (line.startsWith(symbol)) {
                    token = symbol;
                    tokenType = Token.TokenType.SYMBOL;
                    break;
                }
            }
            Matcher intMatcher = INT_CONSTANT.matcher(line);
            Matcher strMatcher = STR_CONSTANT.matcher(line);
            Matcher identifierMatcher = IDENTIFIER.matcher(line);
            if (intMatcher.find()) {
                token = line.substring(intMatcher.start(), intMatcher.end());
                tokenType = Token.TokenType.INT_CONST;
            } else if (strMatcher.find()) {
                token = line.substring(strMatcher.start(), strMatcher.end());
                tokenType = Token.TokenType.STRING_CONST;
            } else if (token.isEmpty()) {
                if (!identifierMatcher.find()) {
                    throw new InvalidTokenException("Invalid line: " + line);
                }
                token = line.substring(identifierMatcher.start(), identifierMatcher.end());
                tokenType = Token.TokenType.IDENTIFIER;
                for (String keyword : KEYWORDS) {
                    if (token.equals(keyword)) {
                        tokenType = Token.TokenType.KEYWORD;
                        break;
                    }
                }
            }
            line = line.substring(token.length());
            line = line.trim();
            if (tokenType == Token.TokenType.STRING_CONST) {
                token = token.substring(1, token.length() - 1); // remove double quote chars from start and end
            }
            tokens.add(new Token(tokenType, token));
        }
    }
}
