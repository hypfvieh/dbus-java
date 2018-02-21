# dbus-java
Improved version of [Java-DBus library provided by freedesktop.org](https://dbus.freedesktop.org/doc/dbus-java/) with compatibility to Java 7 and 8

##### Changes
  - Fixed lots of Java warnings (Generics, unclosed resources)
  - Removed broken 'Gettext' feature used for exception messages, english exception messages should be good enough
  - Renamed some classes/methods/variables to comply with Java naming scheme
  - Removed proprietary logger and replaced it with slf4j
  - Renamed/refactored some parts to be more 'Java' like (e.g. naming, shadowing)
  - Fixed problems with DbusConnection.getConnection(SESSION) when using display export (e.g. SSH X11 forward)
  
##### Changes by others:
   - Thanks to [thjomnx](https://github.com/thjomnx) for support of PropertiesChanged signal class
   - Thanks to [RafalSumislawski](https://github.com/RafalSumislawski) for support of org.freedesktop.DBus.ObjectManager
