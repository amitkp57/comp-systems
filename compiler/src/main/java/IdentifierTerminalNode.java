public class IdentifierTerminalNode implements Node {
    private final String name;
    private final String kind;
    private final String type;
    private final int index;
    private final boolean defined;

    public IdentifierTerminalNode(String name, String kind, String type, int index, boolean defined) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.index = index;
        this.defined = defined;
    }

    @Override
    public String toString() {
        String tagName = "identifier";
        String output = "";
        output += String.format("<%s>\n", tagName);
        output += String.format("<name>%s</name>\n", this.name);
        output += String.format("<kind>%s</kind>\n", this.kind);
        output += String.format("<type>%s</type>\n", this.type);
        output += String.format("<index>%d</index>\n", this.index);
        output += String.format("<defined>%s</defined>\n", this.defined);
        output += String.format("</%s>\n", tagName);
        return output;
    }

    @Override
    public String getName() {
        return "identifier";
    }

    @Override
    public String getValue() {
        return name;
    }
}
