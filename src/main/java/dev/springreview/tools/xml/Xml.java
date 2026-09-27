package dev.springreview.tools.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Xml {

    private Xml() {
    }

    public static Document parse(Path file) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(file.toFile());
    }

    public static List<Element> children(Node parent, String tag) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(tag)) {
                out.add((Element) n);
            }
        }
        return out;
    }

    public static String attr(Element e, String name) {
        String v = e.getAttribute(name);
        return v == null ? "" : v;
    }

    public static int attrInt(Element e, String name) {
        try {
            String v = e.getAttribute(name);
            return (v == null || v.isBlank()) ? 0 : Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
