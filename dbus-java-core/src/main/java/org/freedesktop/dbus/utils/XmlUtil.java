package org.freedesktop.dbus.utils;

import org.w3c.dom.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

/**
 * Assorted static XML utility methods.
 *
 * @author hypfvieh
 */
public final class XmlUtil {

    private XmlUtil() {
    }

    /**
     * Shortcut for checking if given node is of type {@link Element}.
     *
     * @param _node node
     * @return true if {@link Element}, false otherwise
     */
    public static boolean isElementType(Node _node) {
        return _node instanceof Element;
    }

    /**
     * Checks and converts given {@link Node} to {@link Element} if possible.
     * @param _node node
     * @return {@link Element} or null if given {@link Node} is not {@link Element} subtype
     */
    public static Element toElement(Node _node) {
        if (isElementType(_node)) {
            return (Element) _node;
        }
        return null;
    }

    /**
     * Applys a xpathExpression to a xml-Document and return a {@link NodeList} with the results.
     *
     * @param _xpathExpression xpath expression
     * @param _xmlDocumentOrNode document or node
     * @return {@link NodeList}
     * @throws IOException on error
     */
    public static NodeList applyXpathExpressionToDocument(String _xpathExpression, Node _xmlDocumentOrNode)
            throws IOException {

        XPathFactory xfactory = XPathFactory.newInstance();
        XPath xpath = xfactory.newXPath();
        XPathExpression expr = null;
        try {
            expr = xpath.compile(_xpathExpression);
        } catch (XPathExpressionException _ex) {
            throw new IOException(_ex);
        }

        Object result = null;
        try {
            result = expr.evaluate(_xmlDocumentOrNode, XPathConstants.NODESET);
        } catch (Exception _ex) {
            throw new IOException(_ex);
        }

        return (NodeList) result;
    }

    /**
     * Read the given string as XML document.
     *
     * @param _xmlStr xml string
     * @param _validating boolean
     * @param _namespaceAware boolean
     * @return {@link org.w3c.dom.Document}
     * @throws IOException on error
     */
    public static Document parseXmlString(String _xmlStr, boolean _validating, boolean _namespaceAware) throws IOException {

        DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
        dbFac.setNamespaceAware(_namespaceAware);
        dbFac.setValidating(_validating);

        try {
            dbFac.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return dbFac.newDocumentBuilder().parse(new ByteArrayInputStream(_xmlStr.getBytes(StandardCharsets.UTF_8)));

        } catch (IOException _ex) {
            throw _ex;
        } catch (Exception _ex) {
            throw new IOException("Failed to parse " + Util.abbreviate(_xmlStr, 500), _ex);
        }

    }

    /**
     * Convert a {@link NodeList} to a Java {@link List} of {@link Element}s.
     * @param _nodeList collection of nodes
     * @return list of elements
     */
    public static List<Element> convertToElementList(NodeList _nodeList) {
        List<Element> elemList = new ArrayList<>();
        for (int i = 0; i < _nodeList.getLength(); i++) {
            Element elem = (Element) _nodeList.item(i);
            elemList.add(elem);
        }
        return elemList;
    }

    /**
     * Converts {@link NamedNodeMap} to a {@link LinkedHashMap}&lt;String,String&gt;.
     * @param _nodeMap node map
     * @return {@link LinkedHashMap}, maybe empty but never null
     */
    public static Map<String, String> convertToAttributeMap(NamedNodeMap _nodeMap) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < _nodeMap.getLength(); i++) {
            Node node = _nodeMap.item(i);
            map.put(node.getNodeName(), node.getNodeValue());
        }
        return map;
    }

}
