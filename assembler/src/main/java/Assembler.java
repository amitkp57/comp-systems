import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for the assembler. Takes input .asm file as command line argument and writes the machine language binary
 * output to a .hack output file in same directory as input file.
 */
public class Assembler {
    /**
     * Main method of the class. Reads input file name from command line arguments and writes the machine language binary
     * output to a .hack output file in same directory as input file.
     *
     * @param args args[0] is input file name
     */
    public static void main(String[] args) {
        String inputFileName = "/home/amit/Downloads/pradhanAmitProject4 (1)/pradhanAmitProject4/mult.asm";//args[0];
        File inputFIle = new File(inputFileName);
        File outputFile = new File(inputFileName.substring(0, inputFileName.length() - 3) + "hack");
        Parser parser = new Parser();
        Code code = new Code();
        SymbolTable symbolTable = new SymbolTable();

        parser.parse(inputFIle);
        // 1st pass to add pseudo-commands to symbol table
        int count = 0;
        while (parser.hasMoreCommands()) {
            String command = parser.nextCommand();
            if (parser.commandType(command).equals(Parser.L_COMMAND)) {
                symbolTable.addEntry(command.substring(1, command.length() - 1), count);
            } else {
                count++;
            }
        }

        parser.parse(inputFIle);
        // 2nd pass to add variable declared using @<var> to symbol table
        int ram = 16;
        while (parser.hasMoreCommands()) {
            String command = parser.nextCommand();
            if (parser.commandType(command).equals(Parser.A_COMMAND)) {
                String value = command.substring(1);
                if (!value.matches("[0-9]*") && !symbolTable.contains(value)) {
                    symbolTable.addEntry(value, ram++);
                }
            }
        }

        parser.parse(inputFIle);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            while (parser.hasMoreCommands()) {
                String command = parser.nextCommand();
                String commandType = parser.commandType(command);
                String output = "";
                if (commandType.equals(Parser.A_COMMAND)) {
                    String value = parser.getSymbol(command);
                    output = code.binaryValue(symbolTable.contains(value) ? symbolTable.getValue(value) :
                            Integer.valueOf(value));
                } else if (commandType.equals(Parser.C_COMMAND)) {
                    String comp = code.comp(parser.getComp(command));
                    String dest = code.dest(parser.getDest(command));
                    String jump = code.jump(parser.getJump(command));
                    output = String.format("111%s%s%s", comp, dest, jump);
                } else if (commandType.equals(Parser.L_COMMAND)) {
                    continue;
                }
                bw.write(output + "\n");
            }
            System.out.println("Successfully wrote to: " + outputFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}


/**
 * This class encapsulates logics to return binary code for different mnemonics of assembly language
 */
class Code {
    private static final Map<String, String> COMP_MNEMONIC_MAPPINGS = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("0", "101010");
                put("1", "111111");
                put("-1", "111010");
                put("D", "001100");
                put("A", "110000");
                put("!D", "001101");
                put("!A", "110001");
                put("-D", "001111");
                put("-A", "110011");
                put("D+1", "011111");
                put("A+1", "110111");
                put("D-1", "001110");
                put("A-1", "110010");
                put("D+A", "000010");
                put("D-A", "010011");
                put("A-D", "000111");
                put("D&A", "000000");
                put("D|A", "010101");
            }});

    private static final Map<String, String> DEST_MNEMONIC_MAPPINGS = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("null", "000");
                put("M", "001");
                put("D", "010");
                put("DM", "011");
                put("A", "100");
                put("AM", "101");
                put("AD", "110");
                put("ADM", "111");
            }});

    private static final Map<String, String> JUMP_MNEMONIC_MAPPINGS = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("null", "000");
                put("JGT", "001");
                put("JEQ", "010");
                put("JGE", "011");
                put("JLT", "100");
                put("JNE", "101");
                put("JLE", "110");
                put("JMP", "111");
            }});


    /**
     * Returns binary code for destination mnemonic
     *
     * @param mnemonic - dest mnemonic
     * @return binary code for dest mnemonic
     */
    public String dest(String mnemonic) {
        mnemonic = mnemonic == null ? "null" : mnemonic;

        // Permutations of A, D and M (e.g. AMD, ADM, DAM etc.) should all be same
        if (mnemonic != "null") {
            char[] chars = mnemonic.toCharArray();
            Arrays.sort(chars);
            mnemonic = new String(chars);
        }

        return DEST_MNEMONIC_MAPPINGS.get(mnemonic);
    }


    /**
     * Returns binary code for comp mnemonic
     *
     * @param mnemonic - comp mnemonic
     * @return binary code for comp mnemonic
     */
    public String comp(String mnemonic) {
        boolean memory = mnemonic.contains("M");
        mnemonic = mnemonic.replaceAll("M", "A");

        // A+D == D+A, A&D == D&A, A|D == D|A
        if (mnemonic.matches("[A|D][+&|][AD]]")) {
            mnemonic = "A" + mnemonic.charAt(1) + "D";
        }

        return String.format("%s%s", memory ? "1" : "0", COMP_MNEMONIC_MAPPINGS.get(mnemonic));
    }


    /**
     * Returns binary code for jump mnemonic
     *
     * @param mnemonic - jump mnemonic
     * @return binary code for jump mnemonic
     */
    public String jump(String mnemonic) {
        mnemonic = mnemonic == null ? "null" : mnemonic;
        return JUMP_MNEMONIC_MAPPINGS.get(mnemonic);
    }

    /**
     * Returns the 16 bit binary string where right most bit is always '0'
     *
     * @param value
     * @return
     */
    public String binaryValue(int value) {
        return String.format("0%15s", Integer.toBinaryString(value)).replace(' ', '0');
    }
}

/**
 * Reads a input .asm file, translates the assembly language instructions to machine language instructions
 */
class Parser {
    public final static String A_COMMAND = "A-instruction";
    public final static String C_COMMAND = "C-instruction";
    public final static String L_COMMAND = "L Command"; // (symbol) e.g. (LOOP)
    private final static String WHITE_SPACE = "[\\s]*"; // white space characters
    private final static String EMPTY_LINE = "^[\\s]*$";
    private final static String SINGLE_LINE_COMMENT = "(//.*$)|(/\\*.*\\*/)";
    private final static String MULTI_LINE_COMMENT_START = "/\\*.*$";
    private final static String MULTI_LINE_COMMENT_END = "^.*\\*/";
    private final Queue<String> nextCommand = new LinkedList();
    private BufferedReader bufferedReader;
    private boolean multiLineComment = false;

    /**
     * Removes spaces, tabs, empty lines, single and multi line comments
     *
     * @param content a line from a text
     * @return
     */
    private static String trimContent(String content) {
        String[] patterns = {WHITE_SPACE, EMPTY_LINE, SINGLE_LINE_COMMENT};
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern);
            Matcher matcher = p.matcher(content);
            content = matcher.replaceAll("");
        }
        return content;
    }

    public void parse(File file) {
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Takes a instruction string and returns the type of instruction
     *
     * @param command
     * @return
     */
    public String commandType(String command) {
        if (command.startsWith("@")) {
            return A_COMMAND;
        } else if (command.startsWith("(") && command.endsWith(")")) {
            return L_COMMAND;
        } else {
            return C_COMMAND;
        }
    }

    /**
     * Takes an instruction of the form @<value> and returns <value>
     *
     * @param command
     * @return
     */
    public String getSymbol(String command) {
        return command.substring(1);
    }

    /**
     * Takes a C-instruction (dest=comp;jump) and return dest part
     *
     * @param command
     * @return
     */
    public String getDest(String command) {
        String[] splits = command.split("=");
        return splits.length == 1 ? null : splits[0];
    }

    /**
     * Takes a C-instruction (dest=comp;jump) and return comp part
     *
     * @param command
     * @return
     */
    public String getComp(String command) {
        String destComp = command.split(";")[0];
        String[] splits = destComp.split("=");
        return splits.length == 1 ? splits[0] : splits[1];
    }

    /**
     * Takes a C-instruction (dest=comp;jump) and return jump part
     *
     * @param command
     * @return
     */
    public String getJump(String command) {
        String[] splits = command.split(";");
        return splits.length == 1 ? null : splits[1];
    }

    /**
     * Returns true if there are any more commands to process else false
     *
     * @return
     */
    public boolean hasMoreCommands() {
        if (nextCommand.isEmpty())
            loadNextCommand();
        return !nextCommand.isEmpty();
    }

    /**
     * Returns the next command from the input file
     *
     * @return
     */
    public String nextCommand() {
        return nextCommand.poll();
    }

    /**
     * Reads the next command from the input file and loads it to the nextCommand queue
     *
     * @return
     */
    private void loadNextCommand() {
        while (true) {
            try {
                String line = bufferedReader.readLine();
                if (line == null) break;
                String trimmedLine = trimContent(line);
                Pattern multiLineStart = Pattern.compile(MULTI_LINE_COMMENT_START);
                Pattern multiLineEnd = Pattern.compile(MULTI_LINE_COMMENT_END);
                if (!trimmedLine.isEmpty()) {
                    Matcher multiLIneStartMatcher = multiLineStart.matcher(trimmedLine);
                    Matcher multiLIneEndMatcher = multiLineEnd.matcher(trimmedLine);
                    String command = "";
                    if (multiLIneStartMatcher.find()) {
                        multiLineComment = true;
                        command = multiLIneStartMatcher.replaceFirst(""); //return anything before comment starts
                    } else if (multiLIneEndMatcher.find()) {
                        multiLineComment = false;
                        command = multiLIneEndMatcher.replaceFirst("");//return anything after comment ends
                    } else if (!multiLineComment) {
                        command = trimmedLine;
                    }
                    if (!command.isEmpty()) {
                        nextCommand.add(command);
                        break;
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}

/**
 * Keeps a mapping between symbols and RAM/ROM address
 */
class SymbolTable {
    private final Map<String, Integer> symbolMapping = new HashMap<String, Integer>() {{
        put("SP", 0);
        put("LCL", 1);
        put("ARG", 2);
        put("THIS", 3);
        put("THAT", 4);
        put("R0", 0);
        put("R1", 1);
        put("R2", 2);
        put("R3", 3);
        put("R4", 4);
        put("R5", 5);
        put("R6", 6);
        put("R7", 7);
        put("R8", 8);
        put("R9", 9);
        put("R10", 10);
        put("R11", 11);
        put("R12", 12);
        put("R13", 13);
        put("R14", 14);
        put("R15", 15);
        put("SCREEN", 16384);
        put("KBD", 24576);
    }};

    /**
     * Adds a new entry
     *
     * @param key
     * @param val
     */
    public void addEntry(String key, int val) {
        symbolMapping.put(key, val);
    }

    /**
     * Returns true if symbol table contains the key else false
     *
     * @param key
     * @return
     */
    public boolean contains(String key) {
        return symbolMapping.containsKey(key);
    }

    /**
     * Return value associated with key in symbol table
     *
     * @param key
     * @return
     */
    public int getValue(String key) {
        return symbolMapping.get(key);
    }
}
