package org.freedesktop.dbus.utils.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.Util;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassConstructor;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassType;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.MemberOrArgument;

/**
 * Helper to create a DBus struct class.
 * As Structs are regular classes (POJOs) in Java,
 * this helper also takes care about recursion (Struct in Struct/Map/List).
 *
 * @author hypfvieh
 * @since v3.0.1 - 2018-12-21
 */
public class StructTreeBuilder {

	/**
	 * Builds the struct(s) found in _dbusSig.
	 * If the struct is wrapped in a {@link Collection}, it will be unwrapped.
	 * <br><br>
	 * The resulting String will return the parent class name.
	 * This can be the Structs classname or e.g. List/Set class if the struct was wrapped in a {@link Collection}.
	 * <br><br>
	 * Structs which are inside of another struct will get the appendix 'Struct' for each iteration.
	 * This may lead to classes with names like FooStructStructStruct (FooStruct-&gt;(InnerStruct-&gt;InnerInnerStruct)).
	 *
	 * @param _dbusSig dbus Type string
	 * @param _structName name the struct should have
	 * @param _clzBldr class builder with the class where the struct was first seen
	 * @param _generatedClasses a list, this will contain additional struct classes created, if any. Should never be null!
	 *
	 * @return Struct class name or Collection type name
	 * @throws DBusException on DBus Error
	 */
    public String buildStructClasses(String _dbusSig, String _structName, ClassBuilderInfo _clzBldr, List<ClassBuilderInfo> _generatedClasses) throws DBusException {

        if (Util.isBlank(_dbusSig) || _generatedClasses == null) {
            return null;
        }

        List<StructTree> structTree = buildTree(_dbusSig);

        String parentType = null;
        if (!structTree.isEmpty() && Collection.class.isAssignableFrom(structTree.get(0).getDataType())) {
            parentType = structTree.get(0).getDataType().getName();
            structTree = structTree.get(0).getSubType();
        }

        int cnt = 0;
        for (StructTree treeItem : structTree) {
            ClassBuilderInfo info = new ClassBuilderInfo();
            info.setClassName(Util.upperCaseFirstChar(_structName));
            info.setPackageName(_clzBldr.getPackageName());
            info.setExtendClass(Struct.class.getName());
            info.setClassType(ClassType.CLASS);

            _clzBldr.getImports().add(info.getFqcn());

            _generatedClasses.add(info);

            if (cnt == 0 && parentType != null) {
                parentType += "<" + info.getClassName() + ">";
                cnt++;
            }

            if (!treeItem.getSubType().isEmpty()) {
                createNested(treeItem.getSubType(), info, _generatedClasses);
            }
            //_clzBldr.getImports().addAll(info.getImports());
        }

        return parentType == null ? _clzBldr.getPackageName() + "." + _structName : parentType;

    }

    /**
     * Create nested Struct class.
     *
     * @param _list List of struct tree elements
     * @param _info root class of this struct (maybe other struct)
     * @param _classes a list, this will contain additional struct classes created, if any. Should never be null!
     */
    private void createNested(List<StructTree> _list, ClassBuilderInfo _info, List<ClassBuilderInfo> _classes) {
        int position = 0;

        ClassBuilderInfo info = _info;

        ClassConstructor classConstructor = new ClassConstructor();

        for (StructTree inTree : _list) {
            ClassBuilderInfo.MemberOrArgument member = new ClassBuilderInfo.MemberOrArgument("member" + position, inTree.getDataType().getName(), true);
            member.getAnnotations().add("@Position(" + position + ")");

            String constructorArg = "member" + position;

            position++;

            if (Struct.class.isAssignableFrom(inTree.getDataType())) {
                info = new ClassBuilderInfo();
                info.setClassName(Util.upperCaseFirstChar(_info.getClassName()) + "Struct");
                info.setPackageName(_info.getClassName());
                info.setExtendClass(Struct.class.getName());
                info.setClassType(ClassType.CLASS);
                _classes.add(info);

                classConstructor.getArguments().add(new MemberOrArgument(constructorArg, inTree.getDataType().getName()));

            } else if (Collection.class.isAssignableFrom(inTree.getDataType()) || Map.class.isAssignableFrom(inTree.getDataType())) {
                ClassBuilderInfo temp = new ClassBuilderInfo();

                temp.setClassName(info.getClassName());
                temp.setPackageName(info.getPackageName());
                createNested(inTree.getSubType(), temp, _classes);
                info.getImports().addAll(temp.getImports());
                member.getGenerics().addAll(temp.getMembers().stream().map(l -> l.getType()).collect(Collectors.toList()));

                MemberOrArgument argument = new MemberOrArgument(constructorArg, inTree.getDataType().getName());
                argument.getGenerics().addAll(member.getGenerics());
                classConstructor.getArguments().add(argument);
            } else {
                classConstructor.getArguments().add(new MemberOrArgument(constructorArg, inTree.getDataType().getName()));
            }

            info.getImports().add(Position.class.getName()); // add position annotation as include

            info.getImports().add(inTree.getDataType().getName());
            info.getMembers().add(member);
        }

        info.getConstructors().add(classConstructor);

    }


    /**
     * Helper to print a StructTree to STDOUT (for debugging purposes).
     *
     * @param _buildTree tree to print
     * @param _indent indention level (usually 0)
     */
    static void printTree(List<StructTree> _buildTree, int _indent) {
        for (StructTree tree : _buildTree) {
            for (int i = 0; i < _indent; i++) {
                System.out.print(" ");
            }

            System.out.println("DataType = " + tree.dataType);
            if (!tree.subType.isEmpty()) {
                for (int i = 0; i < _indent; i++) {
                    System.out.print(" ");
                }

                System.out.println("SubElements = " + tree.subType.size());
                printTree(tree.subType, ++_indent);
            }
        }
    }

    /**
     * Builds a tree of types based on the given DBus type definition string.
     *
     * @param _dbusTypeStr DBus type string
     * @return List with tree structure, maybe empty - never null
     * @throws DBusException on Error
     */
    private List<StructTree> buildTree(String _dbusTypeStr) throws DBusException {
        List<StructTree> root = new ArrayList<>();

        if (Util.isBlank(_dbusTypeStr)) {
        	return root;
        }

    	List<Type> dataType = new ArrayList<>();
        Marshalling.getJavaType(_dbusTypeStr, dataType, 1);

        for (Type type : dataType) {
            StructTree subTree;
            if (type instanceof ParameterizedType) {
                subTree = new StructTree(((ParameterizedType) type).getRawType().getTypeName());
                subTree.subType.addAll(buildTree((ParameterizedType) type));
            } else {
                subTree = new StructTree(type.getClass().getName());
            }
            root.add(subTree);
        }

        return root;
    }

    /**
     * Create tree from {@link ParameterizedType}.
     *
     * @param _pType {@link ParameterizedType} object
     * @return List of tree elements, maybe empty, never null
     * @throws DBusException on error
     */
    private List<StructTree> buildTree(ParameterizedType _pType) throws DBusException {
        List<StructTree> trees = new ArrayList<>();
        if (_pType == null) {
        	return trees;
        }

        for (Type type : _pType.getActualTypeArguments()) {
            if (type instanceof ParameterizedType) {
                 StructTree tree = new StructTree(((ParameterizedType) type).getRawType().getTypeName());
                 tree.subType.addAll(buildTree((ParameterizedType) type));
                 trees.add(tree);
            } else {
                StructTree tree = new StructTree(type.getTypeName());
                trees.add(tree);
            }
        }
        return trees;
    }

    /**
     * Class to represent a tree structure.
     *
     * @author hypfvieh
     * @since v3.0.1 - 2018-12-22
     */
    static class StructTree {
        private final Class<?> dataType;
        private final List<StructTree> subType = new ArrayList<>();

        public StructTree(String _dataType) {
            try {
                dataType = Class.forName(_dataType);
            } catch (ClassNotFoundException _ex) {
                throw new RuntimeException(_ex);
            }
        }

        public Class<?> getDataType() {
            return dataType;
        }

        public List<StructTree> getSubType() {
            return subType;
        }

    }
}
