@echo off
setlocal
REM set to true for debug builds
set debug=disable
REM set to empty for debug builds
set jcflags=-O -g:none -Xlint:all
REM set to the location you installed unix.jar, debug-*.jar and hexdump.jar
set jarpath=
REM set to the Java installation location
REM set JAVA_HOME=

%JAVA_HOME%\bin\javac -cp .;%jarpath%unix.jar;%jarpath%debug-%debug%.jar;%jarpath%hexdump.jar;%CLASSPATH% %jcflags% org\freedesktop\*.java org\freedesktop\dbus\*.java org\freedesktop\dbus\bin\*.java org\freedesktop\dbus\exceptions\*.java org\freedesktop\dbus\types\*.java org\freedesktop\dbus\viewer\*.java
%JAVA_HOME%\bin\jar -cf dbus.jar org\freedesktop\*.class org\freedesktop\dbus\*.class org\freedesktop\dbus\exceptions\*.class org\freedesktop\dbus\types\*.class
%JAVA_HOME%\bin\jar -cf dbus-bin.jar org\freedesktop\dbus\bin\*.class
%JAVA_HOME%\bin\jar -cf dbus-viewer.jar org\freedesktop\dbus\viewer\*.class

endlocal
