@echo off
setlocal
set debug=%DEBUG%
set version=%VERSION%
set jarpath=%WINJARPATH%
set javaunixjarpath=%WINUNIXJARPATH%

java -DVersion=%version% -cp %javaunixjarpath%debug-%debug%.jar;%javaunixjarpath%hexdump.jar;%jarpath%dbus.jar;%jarpath%dbus-bin.jar org.freedesktop.dbus.bin.ListDBus %*

endlocal
