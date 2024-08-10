package com.github.hypfvieh.dbus.graal;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.AbstractFilter;

import java.io.*;
import java.util.*;

public final class CreateGraalConfig {

    private CreateGraalConfig() {
    }

    public static void main(String[] _args) {
        List<String> candidates = new ArrayList<>();
        try {
            InputStream resourceAsStream = CreateGraalConfig.class.getResourceAsStream("/required_interfaces.txt");
            if (resourceAsStream != null) {
                try (var br = new BufferedReader(new InputStreamReader(resourceAsStream))) {
                    br.lines()
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .filter(s -> !s.trim().startsWith("#"))
                        .forEach(candidates::add);
                }
            }
        } catch (IOException _ex) {
            _ex.printStackTrace();
        }

        SpoonAPI spoon = new Launcher();
        spoon.addInputResource("/home/maniac/git/github/dbus-java/dbus-java-examples/src/main/java/");
        spoon.getEnvironment().setComplianceLevel(17);
        spoon.buildModel();

        Set<String> found = new LinkedHashSet<>();

        spoon.getModel().getElements(new AbstractFilter<CtType<?>>() {
            @Override
            public boolean matches(CtType<?> _element) {
                if (_element.getSuperInterfaces().stream()
                    .anyMatch(e -> candidates.contains(e.getQualifiedName()))) {
                    found.add(_element.getQualifiedName());
                    return true;
                }
                return false;
            };
        });

        System.out.println(found);
    }

}
