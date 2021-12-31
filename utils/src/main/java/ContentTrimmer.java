import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Functionality provided by the class (taken from the question)
 * 1) Remove white space:
 * Strip out all white space from <filename>.in. White space in this case means spaces,
 * tabs, and blank lines, but not line returns.
 * 2) Remove comments:
 * Remove all comments in addition to the whitespace. Comments come in two forms:
 * 1. comments begin with the sequence "//" and end at the line return
 * 2. comments begin with the sequence "/{@literal *}" and end at the sequence "{@literal *}/"
 */
public class ContentTrimmer {
    private final static String WHITE_SPACE = "[ \t\n]*"; //space and tab
    private final static String EMPTY_LINE = "^[ \t\n]*$";
    private final static String SINGLE_LINE_COMMENT = "(//.*$)|(/\\*.*\\*/)";
    private final static String MULTI_LINE_COMMENT_START = "/\\*.*$";
    private final static String MULTI_LINE_COMMENT_END = "^.*\\*/";

    /**
     * Reads input file name command line arguments. Reads content of the file line by line and writes trimmed line
     * to the output file in same directory as input file.
     *
     * @param args args[0] is input file name
     */
    public static void main(String[] args) {
        String inputFileName = args[0];
        File inputFIle = new File(inputFileName);
        File outputFile = new File(inputFileName.substring(0, inputFileName.length() - 2) + "out");
        List<String> lines = new ArrayList<>();
        boolean multiLineComment = false;
        try (BufferedReader br = new BufferedReader(new FileReader(inputFIle));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            for (String line; (line = br.readLine()) != null; ) {
                String trimmedLine = trimContent(line);
                Pattern multiLineStart = Pattern.compile(MULTI_LINE_COMMENT_START);
                Pattern multiLineEnd = Pattern.compile(MULTI_LINE_COMMENT_END);
                if (!trimmedLine.isEmpty()) {
                    Matcher multiLIneStartMatcher = multiLineStart.matcher(trimmedLine);
                    Matcher multiLIneEndMatcher = multiLineEnd.matcher(trimmedLine);
                    if (multiLIneStartMatcher.find()) {
                        appendLIne(lines, multiLIneStartMatcher.replaceFirst("")); //add everything before comment starts
                        multiLineComment = true;
                    } else if (multiLIneEndMatcher.find()) {
                        appendLIne(lines, multiLIneEndMatcher.replaceFirst(""));//add everything after comment ends
                        multiLineComment = false;
                    } else if (!multiLineComment) {
                        appendLIne(lines, trimmedLine);
                    }
                }
            }

            bw.write(lines.stream().collect(Collectors.joining("\n")));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Removes spaces, tabs, empty lines, single and multi line comments
     *
     * @param content a line from a text
     * @return
     */
    private static String trimContent(String content) {
        String[] patterns = {WHITE_SPACE, EMPTY_LINE, SINGLE_LINE_COMMENT};
        for (String pattern : patterns) {
//            System.out.println(pattern);
            Pattern p = Pattern.compile(pattern);
            Matcher matcher = p.matcher(content);
            content = matcher.replaceAll("");
        }
        return content;
    }

    /**
     * if @line is non-empty, append it to string buffer
     *
     * @param lines
     * @param line
     */
    private static void appendLIne(List<String> lines, String line) {
        if (!line.isEmpty()) {
            lines.add(line);
        }
    }
}
