/*
   D-Bus Java Viewer
   Copyright (c) 2006 Peter Cox

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the COPYING file with this program.
*/
package org.freedesktop.dbus.viewer;

/**
 * A Text file abstraction
 *
 * @param fileName The file name
 * @param contents The contents
 *
 * @author pete
 * @since 10/02/2006
 */
record TextFile(String fileName, String contents) {
}
