# dbus-java
Improved version of Java-DBus library provided by freedesktop.org (https://dbus.freedesktop.org/doc/dbus-java/).

Java 1.7 compatible!

##### Changes
  - Fixed lot's of Java warnings (Generics, unclosed resources)
  - Fixed broken 'Gettext' feature used for exception messages
    - Instead of throwing NullPointerExceptions the method will use the english message text if no translation was available
    - Renamed the method "\_" to "t" as "\_" is a reserved word since Java 8
  - Renamed some classes/methods/variables to comply with Java naming scheme.
  - Removed usage of proprietary logger and replaced it with slf4j.
  - Renamed/refactored some parts to be more 'Java' like (e.g. naming, shadowing)
  - Fixed problems with DbusConnection.getConnection(SESSION) when using display export (e.g. SSH X11 forward)
  
##### Changes by others:
   - Thanks to [thjomnx](https://github.com/thjomnx) for support of PropertiesChanged signal class
   - Thanks to [RafalSumislawski](https://github.com/RafalSumislawski) for support of org.freedesktop.DBus.ObjectManager
