package org.freedesktop.dbus.spi.filedescriptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class ReflectionFileDescriptorHelper implements IFileDescriptorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionFileDescriptorHelper.class);
    private static volatile IFileDescriptorHelper instance;

    private final Field fdField;
    private final Constructor<FileDescriptor> constructor;

    public ReflectionFileDescriptorHelper() throws ReflectiveOperationException {
        fdField = FileDescriptor.class.getDeclaredField("fd");
        fdField.setAccessible(true);
        constructor = FileDescriptor.class.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
    }

    public static Optional<? extends IFileDescriptorHelper> getInstance()  {
        if (instance == null) {
            synchronized (ReflectionFileDescriptorHelper.class) {
                if (instance == null) {
                    try {
                        instance = new ReflectionFileDescriptorHelper();
                    } catch (ReflectiveOperationException _ex) {
                        LOGGER.error("Unable to hook up java.io.FileDescriptor by using reflection.");
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.ofNullable(instance);
    }

    @Override
    public Optional<Integer> getFileDescriptorValue(FileDescriptor _fd) {
        try {
            return Optional.of(fdField.getInt(_fd));
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException _ex) {
            LOGGER.error("Could not get file descriptor by reflection.", _ex);
            return Optional.empty();
        }
    }

    @Override
    public Optional<FileDescriptor> createFileDescriptor(int _fd) {
        try {
            return Optional.of(constructor.newInstance(_fd));
        } catch (SecurityException | InstantiationException | IllegalAccessException
                 | IllegalArgumentException | InvocationTargetException _ex) {
            LOGGER.error("Could not create new FileDescriptor instance by reflection.", _ex);
            return Optional.empty();
        }
    }
}
