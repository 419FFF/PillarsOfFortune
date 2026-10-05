package com.slop.pof.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes config.yml from the commented copy inside the jar, using the values already loaded.
 * An empty section such as {@code classic: {}} keeps the jar's keys for that section.
 */
public final class ConfigLayout {
    private ConfigLayout() {
    }

    public static int comments(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String line : text.replace("\r\n", "\n").split("\n", -1)) {
            if (line.trim().startsWith("#")) {
                count++;
            }
        }
        return count;
    }

    /** True when the file on disk has lost most of the notes shipped in the jar. */
    public static boolean needsCommentRestore(String disk, String template) {
        int shipped = comments(template);
        return shipped > 0 && comments(disk) * 2 < shipped;
    }

    public static String merge(String template, FileConfiguration user) {
        Document document = parse(template);
        Set<String> roots = new LinkedHashSet<>();
        for (Node node : document.roots) {
            roots.add(node.key);
            apply(node, node.key, user);
        }
        if (user != null) {
            boolean noted = false;
            for (String key : user.getKeys(false)) {
                if (roots.contains(key) || !ConfigUpdater.own(user, key)) {
                    continue;
                }
                Node extra = fromValue(key, user.get(key), 0);
                if (!noted) {
                    extra.prefix.add("# Kept from your config.");
                    noted = true;
                }
                document.roots.add(extra);
            }
        }
        return render(document);
    }

    private static void apply(Node node, String path, FileConfiguration user) {
        if (user == null || !ConfigUpdater.own(user, path)) {
            return;
        }
        if (node.kind == Kind.SCALAR) {
            Object live = user.get(path);
            if (live != null && !(live instanceof ConfigurationSection) && !(live instanceof List<?>)) {
                if (!sameScalar(live, node.scalar)) {
                    node.scalar = formatScalar(live);
                }
            }
            return;
        }
        if (node.kind == Kind.LIST) {
            if (user.isList(path)) {
                List<?> live = user.getList(path);
                if (!sameList(live, node.body)) {
                    node.body = formatList(live, node.indent + 2);
                }
            }
            return;
        }
        ConfigurationSection section = user.getConfigurationSection(path);
        if (section == null || section.getKeys(false).isEmpty()) {
            return;
        }
        Set<String> known = new LinkedHashSet<>();
        for (Node child : node.children) {
            known.add(child.key);
            apply(child, path + "." + child.key, user);
        }
        boolean noted = false;
        for (String key : section.getKeys(false)) {
            if (known.contains(key)) {
                continue;
            }
            Node extra = fromValue(key, section.get(key), node.indent + 2);
            if (!noted) {
                extra.prefix.add(" ".repeat(extra.indent) + "# Kept from your config.");
                noted = true;
            }
            node.children.add(extra);
        }
    }

    private static boolean sameList(List<?> live, List<String> body) {
        List<String> current = new ArrayList<>();
        if (live != null) {
            for (Object item : live) {
                current.add(String.valueOf(item));
            }
        }
        List<String> shipped = new ArrayList<>();
        for (String line : body) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("-")) {
                continue;
            }
            String value = trimmed.substring(1).trim();
            shipped.add(decode(value));
        }
        return current.equals(shipped);
    }

    private static List<String> formatList(List<?> live, int indent) {
        List<String> lines = new ArrayList<>();
        String pad = " ".repeat(Math.max(0, indent));
        if (live != null) {
            for (Object item : live) {
                lines.add(pad + "- " + formatScalar(item));
            }
        }
        return lines;
    }

    private static Node fromValue(String key, Object value, int indent) {
        Node node = new Node(key, indent);
        if (value instanceof ConfigurationSection section) {
            node.kind = Kind.SECTION;
            for (String child : section.getKeys(false)) {
                node.children.add(fromValue(child, section.get(child), indent + 2));
            }
            return node;
        }
        if (value instanceof List<?> list) {
            node.kind = Kind.LIST;
            node.body = formatList(list, indent + 2);
            return node;
        }
        node.kind = Kind.SCALAR;
        node.scalar = formatScalar(value);
        return node;
    }

    static String formatScalar(Object value) {
        if (value == null) {
            return "\"\"";
        }
        if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return String.valueOf(value);
        }
        if (value instanceof Float || value instanceof Double) {
            double number = ((Number) value).doubleValue();
            if (!Double.isInfinite(number) && number == Math.rint(number) && Math.abs(number) < 1.0e15) {
                return String.valueOf((long) number);
            }
            return String.valueOf(number);
        }
        String text = String.valueOf(value);
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n") + "\"";
    }

    static boolean sameScalar(Object live, String raw) {
        if (live == null || raw == null) {
            return false;
        }
        String decoded = decode(raw);
        if (live instanceof Number number) {
            try {
                return Math.abs(number.doubleValue() - Double.parseDouble(decoded)) < 1.0e-9;
            } catch (NumberFormatException ex) {
                return decoded.equals(String.valueOf(live));
            }
        }
        return decoded.equals(String.valueOf(live));
    }

    static String decode(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return unescape(trimmed.substring(1, trimmed.length() - 1));
        }
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '\'' && trimmed.charAt(trimmed.length() - 1) == '\'') {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }

    private static String unescape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 >= text.length()) {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case '\\' -> out.append('\\');
                case '"' -> out.append('"');
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private static Document parse(String template) {
        Document document = new Document();
        List<String> buffer = new ArrayList<>();
        List<Node> stack = new ArrayList<>();
        String[] lines = (template == null ? "" : template).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                buffer.add(line);
                continue;
            }
            if (trimmed.startsWith("-")) {
                Node owner = listOwner(stack, indentOf(line));
                if (owner == null) {
                    buffer.add(line);
                    continue;
                }
                owner.kind = Kind.LIST;
                owner.body.addAll(buffer);
                owner.body.add(line);
                buffer.clear();
                continue;
            }
            int colon = splitKey(line);
            if (colon < 0) {
                buffer.add(line);
                continue;
            }
            int indent = indentOf(line);
            String key = line.substring(indent, colon).trim();
            String rest = line.substring(colon + 1);
            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent >= indent) {
                stack.remove(stack.size() - 1);
            }
            Node node = new Node(key, indent);
            node.prefix.addAll(buffer);
            buffer.clear();
            if (isScalar(rest)) {
                node.kind = Kind.SCALAR;
                node.scalar = scalarText(rest);
                node.inline = inlineComment(rest);
            } else {
                node.kind = Kind.SECTION;
                String comment = rest.trim();
                if (comment.startsWith("#")) {
                    node.inline = rest;
                }
            }
            if (stack.isEmpty()) {
                document.roots.add(node);
            } else {
                Node parent = stack.get(stack.size() - 1);
                parent.kind = Kind.SECTION;
                parent.children.add(node);
            }
            if (node.kind != Kind.SCALAR) {
                stack.add(node);
            }
        }
        document.trailing.addAll(buffer);
        return document;
    }

    private static Node listOwner(List<Node> stack, int indent) {
        for (int i = stack.size() - 1; i >= 0; i--) {
            Node node = stack.get(i);
            if (node.indent < indent) {
                return node;
            }
        }
        return null;
    }

    private static boolean isScalar(String rest) {
        String trimmed = rest.trim();
        return !trimmed.isEmpty() && !trimmed.startsWith("#");
    }

    private static String scalarText(String rest) {
        String trimmed = rest.trim();
        if (trimmed.startsWith("\"")) {
            int end = endQuote(trimmed, '"');
            return end < 0 ? trimmed : trimmed.substring(0, end + 1);
        }
        if (trimmed.startsWith("'")) {
            int end = endQuote(trimmed, '\'');
            return end < 0 ? trimmed : trimmed.substring(0, end + 1);
        }
        int hash = trimmed.indexOf(" #");
        if (hash >= 0) {
            return trimmed.substring(0, hash).trim();
        }
        return trimmed;
    }

    private static String inlineComment(String rest) {
        String trimmed = rest.trim();
        String value = scalarText(rest);
        int at = rest.indexOf(value);
        if (at < 0) {
            return null;
        }
        String after = rest.substring(at + value.length());
        if (after.trim().startsWith("#")) {
            return after;
        }
        return null;
    }

    private static int endQuote(String text, char quote) {
        for (int i = 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && quote == '"' && i + 1 < text.length()) {
                i++;
                continue;
            }
            if (c == quote) {
                return i;
            }
        }
        return -1;
    }

    private static int splitKey(String line) {
        int indent = indentOf(line);
        if (indent >= line.length()) {
            return -1;
        }
        boolean quoted = line.charAt(indent) == '"' || line.charAt(indent) == '\'';
        if (quoted) {
            return -1;
        }
        for (int i = indent; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ':') {
                return i;
            }
            if (c == '#' || c == '[' || c == '{') {
                return -1;
            }
        }
        return -1;
    }

    private static int indentOf(String line) {
        int indent = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 2;
            } else {
                break;
            }
        }
        return indent;
    }

    private static String render(Document document) {
        StringBuilder out = new StringBuilder();
        for (Node node : document.roots) {
            write(out, node);
        }
        for (String line : document.trailing) {
            out.append(line).append('\n');
        }
        if (out.length() == 0 || out.charAt(out.length() - 1) != '\n') {
            out.append('\n');
        }
        return out.toString();
    }

    private static void write(StringBuilder out, Node node) {
        for (String line : node.prefix) {
            out.append(line).append('\n');
        }
        String pad = " ".repeat(Math.max(0, node.indent));
        if (node.kind == Kind.LIST) {
            out.append(pad).append(node.key).append(":");
            if (node.body.isEmpty()) {
                out.append(" []\n");
                return;
            }
            out.append('\n');
            for (String line : node.body) {
                out.append(line).append('\n');
            }
            return;
        }
        if (node.kind == Kind.SCALAR) {
            out.append(pad).append(node.key).append(": ").append(node.scalar == null ? "\"\"" : node.scalar);
            if (node.inline != null) {
                out.append(node.inline);
            }
            out.append('\n');
            return;
        }
        out.append(pad).append(node.key).append(":");
        if (node.inline != null) {
            out.append(node.inline.startsWith(" ") || node.inline.isEmpty() ? node.inline : " " + node.inline);
        }
        out.append('\n');
        for (Node child : node.children) {
            write(out, child);
        }
    }

    private enum Kind {
        SECTION, SCALAR, LIST
    }

    private static final class Document {
        private final List<Node> roots = new ArrayList<>();
        private final List<String> trailing = new ArrayList<>();
    }

    private static final class Node {
        private final String key;
        private final int indent;
        private final List<String> prefix = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();
        private List<String> body = new ArrayList<>();
        private Kind kind = Kind.SECTION;
        private String scalar;
        private String inline;

        private Node(String key, int indent) {
            this.key = key;
            this.indent = indent;
        }
    }

}
