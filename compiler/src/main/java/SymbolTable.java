import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    final Map<String, Map<String, String>> classSymbols;
    final Map<String, Map<String, String>> functionSymbols;
    final Map<Kind, Integer> indexMapping;

    public SymbolTable() {
        this.classSymbols = new HashMap<>();
        this.functionSymbols = new HashMap<>();
        this.indexMapping = new HashMap<Kind, Integer>() {
            {
                put(Kind.FIELD, 0);
                put(Kind.STATIC, 0);
                put(Kind.VAR, 0);
                put(Kind.ARG, 0);
            }
        };
    }

    boolean contains(String symbol) {
        return functionSymbols.containsKey(symbol) || classSymbols.containsKey(symbol);
    }

    void add(String name, Kind kind, String type) {
        if (kind == Kind.FIELD || kind == Kind.STATIC) {
            classSymbols.put(name, new HashMap<String, String>() {{
                put("kind", kind.name());
                put("type", type);
                put("index", indexMapping.get(kind).toString());
            }});
        } else {
            functionSymbols.put(name, new HashMap<String, String>() {{
                put("kind", kind.name());
                put("type", type);
                put("index", indexMapping.get(kind).toString());
            }});
        }
        indexMapping.put(kind, indexMapping.get(kind) + 1);
    }

    int getIndex(String symbol) {
        if (functionSymbols.containsKey(symbol)) {
            return Integer.valueOf(functionSymbols.get(symbol).get("index"));
        }
        return Integer.valueOf(classSymbols.get(symbol).get("index"));
    }

    String getType(String symbol) {
        if (functionSymbols.containsKey(symbol)) {
            return functionSymbols.get(symbol).get("type");
        }
        return classSymbols.get(symbol).get("type");
    }

    String getKind(String symbol) {
        if (functionSymbols.containsKey(symbol)) {
            return functionSymbols.get(symbol).get("kind");
        }
        return classSymbols.get(symbol).get("kind");
    }

    String getVmKind(String symbol) {
        Kind kind = Kind.valueOf(getKind(symbol));
        switch (kind) {
            case ARG:
                return "argument";
            case VAR:
                return "local";
            case STATIC:
                return "static";
            case FIELD:
                return "this";
            default:
                throw new InvalidTokenException(kind.name());
        }
    }

    void resetClassSymbols() {
        classSymbols.clear();
        indexMapping.put(Kind.FIELD, 0);
        indexMapping.put(Kind.STATIC, 0);
    }

    void resetFunctionSymbols() {
        functionSymbols.clear();
        indexMapping.put(Kind.VAR, 0);
        indexMapping.put(Kind.ARG, 0);
    }

    enum Kind {
        FIELD, STATIC, VAR, ARG;

        public static Kind getEnum(String val) {
            return Arrays.asList(values()).stream().filter(kind -> val.toUpperCase().equals(kind.name())).findFirst().get();
        }
    }
}