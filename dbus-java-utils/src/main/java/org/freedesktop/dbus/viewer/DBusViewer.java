/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Introspectable;
import org.freedesktop.dbus.types.UInt32;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A viewer for DBus
 *
 * goal is to replicate the functionality of kdbus with Java smarts
 *
 * @author pete
 * @since 29/01/2006
 */
public class DBusViewer {
    private static final Map<String, DBusBusType> CONNECTION_TYPES = new HashMap<>();
    private static final String DOC_TYPE = "<!DOCTYPE node PUBLIC \"-//freedesktop//DTD D-BUS Object Introspection 1.0//EN\"\n\"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd\">";

    static {
        CONNECTION_TYPES.put("System", DBusBusType.SYSTEM);
        CONNECTION_TYPES.put("Session", DBusBusType.SESSION);
    }

    private final List<DBusConnection> connections;

    /** Create the DBusViewer
     *
     * @param _connectionTypes The map of connection types
     */
    public DBusViewer(final Map<String, DBusBusType> _connectionTypes) {
        connections = new ArrayList<>(_connectionTypes.size());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                final JTabbedPane tabbedPane = new JTabbedPane();
                addTabs(tabbedPane, _connectionTypes);
                final JFrame frame = new JFrame("Dbus Viewer");
                frame.setContentPane(tabbedPane);
                frame.setSize(600, 400);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent _event) {
                        frame.dispose();
                        for (DBusConnection connection : connections) {
                            connection.disconnect();
                        }
                        System.exit(0);
                    }
                });
                frame.setVisible(true);
            }
        });
    }


    /**
     * @param _args unused
     */
    public static void main(String[] _args) {
        new DBusViewer(CONNECTION_TYPES);
    }

    /** Add tabs for each supplied connection type
     * @param _tabbedPane The tabbed pane
     * @param _connectionTypes The connection
     */
    private void addTabs(final JTabbedPane _tabbedPane, final Map<String, DBusBusType> _connectionTypes) {
        for (final String key : _connectionTypes.keySet()) {
            final JLabel label = new JLabel("Processing DBus for " + key);
            _tabbedPane.addTab(key, label);
        }
        Runnable loader = new Runnable() {
            @Override
            public void run() {
                boolean users = true;
                boolean owners = true;
                for (final String key : _connectionTypes.keySet()) {
                    try {
                        DBusConnection conn = DBusConnectionBuilder.forType(_connectionTypes.get(key)).build();
                        connections.add(conn);

                        final TableModel tableModel = listDBusConnection(users, owners, conn);

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                int index = _tabbedPane.indexOfTab(key);
                                final JTable table = new JTable(tableModel);

                                JScrollPane scrollPane = new JScrollPane(table);

                                JPanel tab = new JPanel(new BorderLayout());
                                tab.add(scrollPane, BorderLayout.CENTER);

                                JPanel southPanel = new JPanel();
                                final JButton button = new JButton(new IntrospectAction(table));
                                southPanel.add(button);

                                tab.add(southPanel, BorderLayout.SOUTH);

                                _tabbedPane.setComponentAt(index, tab);

                            }
                        });
                    } catch (DBusExecutionException | DBusException _ex) {
                        _ex.printStackTrace();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                int index = _tabbedPane.indexOfTab(key);
                                JLabel label = (JLabel) _tabbedPane.getComponentAt(index);
                                label.setText("Could not load Dbus information for " + key + ":" + _ex.getMessage());
                            }
                        });
                    }
                }
            }
        };
        final Thread thread = new Thread(loader);
        thread.setName("DBus Loader");
        thread.start();
    }

    /* based on code from org.freedesktop.dbus.ListDBus */
    private DBusTableModel listDBusConnection(boolean _users, boolean _owners, DBusConnection _conn) throws DBusException {
        DBusTableModel model = new DBusTableModel();

        DBus dbus = _conn.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
        String[] names = dbus.ListNames();

        ParsingContext p = new ParsingContext(_conn);

        for (String name : names) {
            List<DBusEntry> results = new ArrayList<>();
            try {
                // String objectpath = '/' + name.replace('.', '/');

                p.visitNode(name, "/");
            } catch (IOException | SAXException | DBusException | DBusExecutionException _ex) {
                _ex.printStackTrace();
            }
            results = p.getResult();
            p.reset();

            if (!results.isEmpty()) {
                if (_users) {
                    try {
                        final UInt32 user = dbus.GetConnectionUnixUser(name);
                        for (DBusEntry entry : results) {
                            entry.setUser(user);
                        }
                    } catch (DBusExecutionException _exDbe) {
                        LoggerFactory.getLogger(getClass()).warn("Could not get unix user", _exDbe);
                    }
                }
                if (!name.startsWith(":") && _owners) {
                    try {
                        final String owner = dbus.GetNameOwner(name);
                        for (DBusEntry entry : results) {
                            entry.setOwner(owner);
                        }
                    } catch (DBusExecutionException _exDbe) {
                        LoggerFactory.getLogger(getClass()).warn("Could not get owner", _exDbe);
                    }
                }
                for (DBusEntry entry : results) {
                    model.add(entry);
                }
            }
        }
        return model;
    }


    class ParsingContext {
        private final DBusConnection conn;
        private DocumentBuilder      builder;
        private List<DBusEntry>      result;

        ParsingContext(DBusConnection _conn) {
            this.conn = _conn;
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException _exPc) {
                throw new RuntimeException("Error during parser init: " + _exPc.getMessage(), _exPc);
            }
            reset();

        }

        DBusEntry addEntry(String _name, String _path) throws DBusException {
            DBusEntry entry = new DBusEntry();
            entry.setName(_name);
            entry.setPath(_path);
            Introspectable introspectable = conn.getRemoteObject(_name, _path, Introspectable.class);
            entry.setIntrospectable(introspectable);

            result.add(entry);

            return entry;
        }

        public void visitNode(String _name, String _path) throws DBusException, SAXException, IOException {
            System.out.println("visit " + _name + ":" + _path);
            if ("/org/freedesktop/DBus/Local".equals(_path)) {
                // this will disconnects us.
                return;
            }
            DBusEntry e = addEntry(_name, _path);
            String introspectData = e.getIntrospectable().Introspect();

            Document document = builder.parse(new InputSource(new StringReader(introspectData.replace(DOC_TYPE, ""))));
            Element root = document.getDocumentElement();

            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (Node.ELEMENT_NODE != node.getNodeType()) {
                    continue;
                }
                if ("node".equals(node.getNodeName())) {
                    Node nameNode = node.getAttributes().getNamedItem("name");
                    if (nameNode != null) {
                        try {
                            if (_path.endsWith("/")) {
                                visitNode(_name, _path + nameNode.getNodeValue());
                            } else {
                                visitNode(_name, _path + '/' + nameNode.getNodeValue());
                            }
                        } catch (DBusException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

            }

        }

        public List<DBusEntry> getResult() {
            return result;
        }

        void reset() {
            result = new ArrayList<>();
        }

    }

}
