package org.freedesktop.dbus.utils.bin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;

class NodeListIterator implements Iterator<Node> {
    // CHECKSTYLE:OFF
    NodeList nl;
    int      i;
    // CHECKSTYLE:ON

    NodeListIterator(NodeList _nl) {
        this.nl = _nl;
        i = 0;
    }

    @Override
    public boolean hasNext() {
        return i < nl.getLength();
    }

    @Override
    public Node next() {
        Node n = nl.item(i);
        i++;
        return n;
    }

    @Override
    public void remove() {
    }
}
