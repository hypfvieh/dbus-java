package org.mpris;

import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.interfaces.DBusInterface;

import java.util.List;

/**
 * Auto-generated class.
 */
@DBusProperty(name = "Identity", type = String.class, access = Access.READ)
@DBusProperty(name = "DesktopEntry", type = String.class, access = Access.READ)
@DBusProperty(name = "SupportedMimeTypes", type = MediaPlayer2.PropertySupportedMimeTypesType.class, access = Access.READ)
@DBusProperty(name = "SupportedUriSchemes", type = MediaPlayer2.PropertySupportedUriSchemesType.class, access = Access.READ)
@DBusProperty(name = "HasTrackList", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanQuit", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "CanSetFullscreen", type = Boolean.class, access = Access.READ)
@DBusProperty(name = "Fullscreen", type = Boolean.class, access = Access.READ_WRITE)
@DBusProperty(name = "CanRaise", type = Boolean.class, access = Access.READ)
@SuppressWarnings({"checkstyle:methodname", "checkstyle:hideutilityclassconstructor", "checkstyle:visibilitymodifier"})
public interface MediaPlayer2 extends DBusInterface {

    void Quit();

    void Raise();

    interface PropertySupportedMimeTypesType extends TypeRef<List<String>> {

    }

    interface PropertySupportedUriSchemesType extends TypeRef<List<String>> {

    }
}
