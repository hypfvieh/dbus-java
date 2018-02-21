package org.freedesktop.dbus.connections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.ExportedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FallbackContainer {

    /**
     * @param _abstractConnection
     */
    FallbackContainer() {
    }

    private final Logger                  logger    = LoggerFactory.getLogger(getClass());
    private Map<String[], ExportedObject> fallbacks = new HashMap<>();

    public synchronized void add(String path, ExportedObject eo) {
        logger.debug("Adding fallback on " + path + " of " + eo);
        fallbacks.put(path.split("/"), eo);
    }

    public synchronized void remove(String path) {
        logger.debug("Removing fallback on " + path);
        fallbacks.remove(path.split("/"));
    }

    public synchronized ExportedObject get(String path) {
        int best = 0;
        int i = 0;
        ExportedObject bestobject = null;
        String[] pathel = path.split("/");
        for (String[] fbpath : fallbacks.keySet()) {
            logger.trace("Trying fallback path " + Arrays.deepToString(fbpath) + " to match "
                    + Arrays.deepToString(pathel));
            for (i = 0; i < pathel.length && i < fbpath.length; i++) {
                if (!pathel[i].equals(fbpath[i])) {
                    break;
                }
            }
            if (i > 0 && i == fbpath.length && i > best) {
                bestobject = fallbacks.get(fbpath);
            }
            logger.trace("Matches " + i + " bestobject now " + bestobject);
        }

        logger.debug("Found fallback for " + path + " of " + bestobject);
        return bestobject;
    }
}