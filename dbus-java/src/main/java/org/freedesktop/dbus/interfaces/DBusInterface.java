/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.interfaces;

/**
 * Denotes a class as exportable or a remote interface which can be called.
 * <p>
 * Any interface which should be exported or imported should extend this
 * interface. All public methods from that interface are exported/imported
 * with the given method signatures.
 * </p>
 * <p>
 * All method calls on exported objects are run in their own threads.
 * Application writers are responsible for any concurrency issues.
 * </p>
 */
public interface DBusInterface {

    /**
    * Returns true on remote objects.
    * Local objects implementing this interface MUST return false.
    *
    * @return boolean
    */
    boolean isRemote();

    /**
     * Returns the path of this object.
     *
     * @return string
     */
    String getObjectPath();

}
