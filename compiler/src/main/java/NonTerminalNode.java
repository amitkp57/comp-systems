import java.util.List;

/**
 * NonTerminal node class
 */
class NonTerminalNode implements Node {
    private final List<Node> children;
    private final String name;

    NonTerminalNode(String name, List<Node> children) {
        this.children = children;
        this.name = name;
    }

    @Override
    public String toString() {
        String output = "";
        for (Node child : this.children) {
            output += child.toString();
        }
        return String.format("<%s>\n%s</%s>\n", name, output, name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return "";
    }

    @Override
    public List<Node> getChildren() {
        return children;
    }
}
