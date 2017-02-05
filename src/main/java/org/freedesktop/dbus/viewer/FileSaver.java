/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

import static org.freedesktop.dbus.Gettext.t;

import java.awt.Component;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.JOptionPane;

final class FileSaver implements Runnable {
    private static final String      CANCEL        = "Cancel";

    private static final String      SKIP_ALL      = "Skip All";

    private static final String      SKIP          = "Skip";

    private static final String      OVERWRITE     = "Overwrite";

    private static final String      OVERWRITE_ALL = "Overwrite All";

    private final File               parentDirectory;

    private final Component          parentComponent;

    private final Iterable<TextFile> textFiles;

    FileSaver(Component parentComponent, File parentDirectory, Iterable<TextFile> files) {
        this.parentComponent = parentComponent;
        this.parentDirectory = parentDirectory;
        this.textFiles = files;
    }

    /** {@inheritDoc} */
    @Override
    public void run() {
        saveFiles();
    }

    private void saveFiles() {
        String overwritePolicy = null;
        final Iterator<TextFile> iterator = textFiles.iterator();
        while (iterator.hasNext()) {
            final TextFile textFile = iterator.next();
            String fileName = textFile.getFileName();
            File fileToSave = new File(parentDirectory, fileName);
            File parentFile = fileToSave.getParentFile();
            if (parentFile.exists() || parentFile.mkdirs()) {
                boolean doSave = !fileToSave.exists() || OVERWRITE_ALL.equals(overwritePolicy);
                if (!doSave && !SKIP_ALL.equals(overwritePolicy)) {
                    String[] selectionValues;
                    if (iterator.hasNext()) {
                        selectionValues = new String[] {
                                OVERWRITE, OVERWRITE_ALL, SKIP, SKIP_ALL, CANCEL
                        };
                    } else {
                        selectionValues = new String[] {
                                OVERWRITE, CANCEL
                        };
                    }
                    int option = JOptionPane.showOptionDialog(parentComponent, "File exists: " + fileName, "Save", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, selectionValues, null);
                    if (option == -1) {
                        break;
                    }
                    overwritePolicy = selectionValues[option];
                    if (CANCEL.equals(overwritePolicy)) {
                        break;
                    }

                    doSave = OVERWRITE.equals(overwritePolicy) || OVERWRITE_ALL.equals(overwritePolicy);
                }
                if (doSave) {
                    try {
                        String contents = textFile.getContents();
                        writeFile(fileToSave, contents);
                    } catch (final IOException ex) {
                        /* Can't access parent directory for saving */
                        final String errorMessage = "Could not save " + fileName + ": " + ex.getLocalizedMessage();
                        if (iterator.hasNext()) {

                            int confirm = JOptionPane.showConfirmDialog(parentComponent, errorMessage + ".\n" + t("Try saving other files?"), t("Save Failed"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                            if (confirm != JOptionPane.OK_OPTION) {
                                break;
                            }
                        } else {
                            JOptionPane.showMessageDialog(parentComponent, errorMessage + ".", t("Save Failed"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            } else {

                final String errorMessage = t("Could not access parent directory for ") + fileName;
                if (iterator.hasNext()) {

                    int confirm = JOptionPane.showConfirmDialog(parentComponent, errorMessage + ".\n" + t("Try saving other files?"), t("Save Failed"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                    if (confirm != JOptionPane.OK_OPTION) {
                        break;
                    }
                } else {
                    JOptionPane.showMessageDialog(parentComponent, errorMessage + ".", t("Save Failed"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * @param fileToSave
     * @param contents
     * @throws IOException
     */
    private void writeFile(File fileToSave, String contents) throws IOException {
        FileWriter fileWriter = null;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
            writer.append(contents);
            writer.flush();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e1) {
                }
            }
        }
    }
}
