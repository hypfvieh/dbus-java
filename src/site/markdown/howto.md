# HowTo


Here are some references to example code to demonstrate how to...

### Get Properties from DBus
 * [PulseAudioDbus](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/pulseaudio/PulseAudioDbus.java) 
 * [PrintUserSessions](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/systemd/PrintUserSessions.java)

### Get array of struct DBus Properties
 * [PrintUserSessions](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/systemd/PrintUserSessions.java)

### Use structs
 * [StructServer/StructClient](https://github.com/hypfvieh/dbus-java/tree/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/struct)

### Export properties using getters/setters (bound properties)
 * [ExportObjectWithProperties](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/properties/ExportObjectWithProperties.java)
 * See also the [Properties](./properties.html) guide
 
### Get a remote interface
 * [NetworkManagerExample](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/networkmanager/NetworkManagerExample.java)
 * [NetworkManagerExample2](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/networkmanager/NetworkManagerExample2.java)
 * [ControlVlcExample](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/mpris/ControlVlcExample.java)
 
### Dealing with signals
 * [PropertiesChangedSignalSample](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/signal/PropertiesChangedSignalSample.java)
 
### Export an nested object on DBus
 * [ExportNested](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/nested/ExportNested.java)
 
### Use EmbeddedDBusDaemon
 * [RunDaemon](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/daemon/RunDaemon.java)

### Run daemon and client in separate processes
 * [RunTwoPartDaemon](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/daemon/twopart/RunTwoPartDaemon.java)
 * [RunTwoPartClient](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/daemon/twopart/RunTwoPartClient.java)

### Call privileged methods (e.g. systemd)
 * [SystemdUnitsManagment](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/systemd/SystemdUnitsManagment.java)

### Using Variant<?> with proper type
 * [NetworkManagerExample3](https://github.com/hypfvieh/dbus-java/blob/master/dbus-java-examples/src/main/java/com/github/hypfvieh/dbus/examples/networkmanager/NetworkManagerExample3.java)
 * See also the [Variant](./variant-handling.html) guide (including `VariantBuilder`)
 * See also: [Issue 74](https://github.com/hypfvieh/dbus-java/issues/74#issuecomment-1280768515)

### Generate Java interfaces from introspection data
 * See the [Code Generation](./code-generation.html) guide