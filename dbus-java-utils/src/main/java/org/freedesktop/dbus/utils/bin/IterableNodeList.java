package org.freedesktop.dbus.utils.bin;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;

class IterableNodeList implements Iterable<Node> {
    private final NodeList nl;

    IterableNodeList(NodeList _nl) {
        this.nl = _nl;
    }

    @Override
    public Iterator<Node> iterator() {
        return new NodeListIterator(nl);
    }
}
