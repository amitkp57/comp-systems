import java.util.Collections;
import java.util.List;

/**
 * Node interface implemented by TerminalNode and NonTerminalNode
 */
interface Node {
    String getName(); // xml tag name

    default String getValue() { // xml text value
        throw new UnsupportedOperationException();
    }

    default Node getChild(String tagName) {
        return getChildren().stream().filter(node -> node.getName().equals(tagName)).findFirst().get();
    }

    default List<Node> getChildren() { // xml children
        return Collections.emptyList();
    }
}
