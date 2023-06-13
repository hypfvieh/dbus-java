package org.mpris.mediaplayer2;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.TypeRef;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusProperty;
import org.freedesktop.dbus.annotations.DBusProperty.Access;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

/**
 * Auto-generated class.
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.TrackList")
@DBusProperty(name = "Tracks", type = TrackList.PropertyTracksType.class, access = Access.READ)
@DBusProperty(name = "CanEditTracks", type = Boolean.class, access = Access.READ)
@SuppressWarnings({"checkstyle:methodname", "checkstyle:hideutilityclassconstructor", "checkstyle:visibilitymodifier"})
public interface TrackList extends DBusInterface {

    List<Map<String, Variant<?>>> GetTracksMetadata(List<DBusPath> _arg0);

    void AddTrack(String _arg0, DBusPath _arg1, boolean _arg2);

    void RemoveTrack(DBusPath _arg0);

    void GoTo(DBusPath _arg0);

    interface PropertyTracksType extends TypeRef<List<DBusPath>> {

    }

    class TrackListReplaced extends DBusSignal {

        private final List<DBusPath> arg0;
        private final DBusPath arg1;

        public TrackListReplaced(String _path, List<DBusPath> _arg0, DBusPath _arg1) throws DBusException {
            super(_path, _arg0, _arg1);
            this.arg0 = _arg0;
            this.arg1 = _arg1;
        }

        public List<DBusPath> getArg0() {
            return arg0;
        }

        public DBusPath getArg1() {
            return arg1;
        }

    }

    class TrackAdded extends DBusSignal {

        private final Map<String, Variant<?>> arg0;
        private final DBusPath arg1;

        public TrackAdded(String _path, Map<String, Variant<?>> _arg0, DBusPath _arg1) throws DBusException {
            super(_path, _arg0, _arg1);
            this.arg0 = _arg0;
            this.arg1 = _arg1;
        }

        public Map<String, Variant<?>> getArg0() {
            return arg0;
        }

        public DBusPath getArg1() {
            return arg1;
        }

    }

    class TrackRemoved extends DBusSignal {

        private final DBusPath arg0;

        public TrackRemoved(String _path, DBusPath _arg0) throws DBusException {
            super(_path, _arg0);
            this.arg0 = _arg0;
        }

        public DBusPath getArg0() {
            return arg0;
        }

    }

    class TrackMetadataChanged extends DBusSignal {

        private final DBusPath arg0;
        private final Map<String, Variant<?>> arg1;

        public TrackMetadataChanged(String _path, DBusPath _arg0, Map<String, Variant<?>> _arg1) throws DBusException {
            super(_path, _arg0, _arg1);
            this.arg0 = _arg0;
            this.arg1 = _arg1;
        }

        public DBusPath getArg0() {
            return arg0;
        }

        public Map<String, Variant<?>> getArg1() {
            return arg1;
        }

    }
}
