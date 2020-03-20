# DBus-Java Quickstart

1. Add in dbus-java to your dependencies. [Documentation on adding to dependencies.](./dependency-info.html)
2. (optional) Add in a logging framework of your choice to see the log messages.
  DBus-Java uses SLF4J internally for logging.

        <!-- Example of using log4j2 as the logging implementation with Maven -->
        <dependencies>
            <!-- ... other dependencies here ... -->
            <dependency>
                <groupId>org.apache.logging.log4j</groupId>
                <artifactId>log4j-api</artifactId>
                <version>2.11.2</version>
            </dependency>
            <dependency>
                <groupId>org.apache.logging.log4j</groupId>
                <artifactId>log4j-slf4j-impl</artifactId>
                <version>2.11.2</version>
            </dependency>

        </dependencies>

3. Get a connection to the bus. This can be either the SESSION bus or the SYSTEM bus.

        DBusConnection conn = DBusConnection.getConnection( DBusConnection.DBusType.SESSION );

4. Request bus name if you want a well-known bus name, or begin calling methods on the bus!
  You only need to request a bus name if you are expecting other people to talk
  to you, so that they know how to talk to you.


## Session vs System bus

By default, there are two buses on the system for different purposes.  The
first bus, the session bus, is created on a per-user basis, generally when you
log in.  However, this bus may not be created unless you have a graphical
session.  This bus is used for session data such as notifications to the user,
property changes on the shell you are using, etc.

The second bus is the system bus.  There is only one system bus at a time, and
this bus contains information that is relevant to the system as a whole.  Some
of this data can be common to the session bus as well.  Data that may be on the
system bus includes things such as mount/unmount data, new devices found, etc.

Since there is only one system bus, it has more stringent security requirements.
You should always be able to listen to events on the system bus, but you may not
be able to request a bus name without updating some permissions.  The process
of updating permissions is outside the scope of this document; however, the XML
files that control the permissions can be found in `/etc/dbus-1/system.d`.