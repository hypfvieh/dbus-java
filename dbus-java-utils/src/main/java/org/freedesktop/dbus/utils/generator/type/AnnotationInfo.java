package org.freedesktop.dbus.utils.generator.type;

import org.freedesktop.dbus.utils.generator.type.AnnotationInfo.AnnotArgs.AnnotClass;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains information about annotation to place on classes, members or methods.
 *
 * @author hypfvieh
 * @since v3.2.1 - 2019-11-13
 */
public class AnnotationInfo {

    /** Annotation class. */
    private final Class<? extends Annotation> annotationClass;
    /** Map of parameters for the annotation (should be ordered). */
    private final Map<String, Object> annotationParams = new LinkedHashMap<>();

    private final Set<Class<?>> additionalImports = new LinkedHashSet<>();

    public AnnotationInfo(Class<? extends Annotation> _annotationClass, AnnotArgs _annotationParams) {
        annotationClass = _annotationClass;
        if (_annotationParams != null) {
            _annotationParams.args.forEach(e -> {
                annotationParams.put(e.key(), e.value());
                if (e.value() != null && !e.value().getClass().getPackage().getName().startsWith("java.lang")) {
                    additionalImports.add(e.value().getClass());
                }
            });
        }
    }

    public Class<? extends Annotation> getAnnotationClass() {
        return annotationClass;
    }

    public Map<String, Object> getAnnotationParams() {
        return annotationParams;
    }

    public Set<Class<?>> getAdditionalImports() {
        return additionalImports;
    }

    public String getAnnotationString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getAnnotationClass().getSimpleName());

        if (!getAnnotationParams().isEmpty()) {
            sb.append("(");

            if (getAnnotationParams().size() == 1 && "value".equals(getAnnotationParams().keySet().iterator().next())) {
                sb.append(handleArg(getAnnotationParams().values().iterator().next()));
            } else {
                sb.append(getAnnotationParams().entrySet().stream()
                    .map(e -> "%s = %s".formatted(e.getKey(), handleArg(e.getValue())))
                    .collect(Collectors.joining(", ")));
            }

            sb.append(")");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + " [annotationClass=" + annotationClass
            + ", annotationParams=" + annotationParams
            + ", additionalImports=" + additionalImports + "]";
    }

    private String handleArg(Object _value) {
        if (_value instanceof AnnotClass ct) {
            return ct.fqcn() + ".class";
        }
        if (_value instanceof String s && !s.endsWith(".class")) {
            return "\"" + s + "\"";
        } else {
            return String.valueOf(_value);
        }
    }

    public static final class AnnotArgs {
        private final Set<AnnotArg> args = new LinkedHashSet<>();

        private AnnotArgs() {

        }

        public AnnotArgs add(String _key, Object _val) {
            Objects.requireNonNull(_key);
            Objects.requireNonNull(_val);

            args.add(new AnnotArg(_key, _val));
            return this;
        }

        public AnnotArgs add(String _key, AnnotClass _val) {
            Objects.requireNonNull(_key);
            Objects.requireNonNull(_val);

            args.add(new AnnotArg(_key, _val));
            return this;
        }

        /**
         * Shortcut for {@code #add("value", _val)}.
         * @param _val value to set for "value" option
         * @return this
         */
        public AnnotArgs add(Object _val) {
            return this.add("value", _val);
        }

        public static AnnotArgs create() {
            return new AnnotArgs();
        }

        public record AnnotClass(String fqcn) {
            public static AnnotClass of(String _fqcn) {
                return new AnnotClass(_fqcn);
            }
        }

        record AnnotArg(String key, Object value) {

            @Override
            public int hashCode() {
                return Objects.hash(key);
            }

            @Override
            public boolean equals(Object _obj) {
                if (this == _obj) {
                    return true;
                }
                if (_obj == null) {
                    return false;
                }
                if (getClass() != _obj.getClass()) {
                    return false;
                }
                AnnotArg other = (AnnotArg) _obj;
                return Objects.equals(key, other.key);
            }
        }
    }
}
