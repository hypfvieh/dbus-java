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
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.freedesktop.dbus.interfaces.Introspectable;

@SuppressWarnings("serial")
final class IntrospectAction extends AbstractAction implements ListSelectionListener {
    private final JTable table;

    IntrospectAction(JTable _table) {
        super("Introspect");
        setEnabled(false);
        this.table = _table;

        ListSelectionModel selectionModel = _table.getSelectionModel();
        selectionModel.addListSelectionListener(this);
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /** {@inheritDoc} */
    @Override
    public void valueChanged(ListSelectionEvent _event) {
        if (!_event.getValueIsAdjusting()) {
            DBusTableModel model = (DBusTableModel) table.getModel();
            int selection = table.getSelectedRow();
            if (selection > -1 && selection < model.getRowCount()) {
                DBusEntry entry = model.getEntry(selection);
                final Introspectable introspectable = entry.getIntrospectable();
                setEnabled(introspectable != null);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent _event) {

        int row = table.getSelectedRow();
        DBusTableModel model = (DBusTableModel) table.getModel();
        if (row > -1 && row < model.getRowCount()) {
            DBusEntry entry = model.getEntry(row);
            final String xmlFile = entry.getName() + ".xml";
            final Introspectable introspectable = entry.getIntrospectable();
            new Thread(new Runnable() {
                @Override
                public void run() {

                    StringStreamFactory factory = new StringStreamFactory();
                    try {
                        String xml = introspectable.Introspect();

                        final JTabbedPane tabbedPane = new JTabbedPane();

                        tabbedPane.addTab(xmlFile, createSourceTab(xml));

                        for (String file : factory.streamMap.keySet()) {
                            final String source = factory.streamMap.get(file).toString();

                            tabbedPane.addTab(file, createSourceTab(source));
                        }
                        tabbedPane.setPreferredSize(new Dimension(600, 400));

                        final JPanel introspectionPanel = new JPanel(new BorderLayout());
                        introspectionPanel.add(tabbedPane, BorderLayout.CENTER);

                        JPanel southPanel = new JPanel();
                        southPanel.add(new JButton(new SaveFileAction(tabbedPane)));
                        southPanel.add(new JButton(new SaveAllAction(tabbedPane)));
                        introspectionPanel.add(southPanel, BorderLayout.SOUTH);

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(table, introspectionPanel, "Introspection", JOptionPane.PLAIN_MESSAGE);
                            }
                        });

                    } catch (final Exception e) {
                        e.printStackTrace();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(table, e.getMessage(), "Introspection Failed", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                }

                private JScrollPane createSourceTab(final String _source) {
                    JTextArea area = new JTextArea(_source);
                    area.setLineWrap(true);
                    area.setWrapStyleWord(true);
                    return new JScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                }
            }).start();

        }
    }
}
