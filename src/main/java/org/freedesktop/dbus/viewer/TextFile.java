/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

/** A Text file abstraction
 *
 *
 * @author pete
 * @since 10/02/2006
 */
class TextFile {
    final String fileName;
    final String contents;

    /** Create the TextFile
     *
     * @param fileName The file name
     * @param contents The contents
     */
    public TextFile(String fileName, String contents) {
        this.fileName = fileName;
        this.contents = contents;
    }

    /** Retrieve the fileName
     *
     * @return The fileName.
     */
    String getFileName() {
        return fileName;
    }

    /** Retrieve the contents
     *
     * @return The contents.
     */
    String getContents() {
        return contents;
    }
}
