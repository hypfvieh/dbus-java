package org.freedesktop.dbus.utils.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.utils.generator.ClassBuilderInfo.ClassType;

import com.github.hypfvieh.util.StringUtil;

public class StructTreeBuilder {

    public String buildStructClasses(String _dbusSig, String _structName, ClassBuilderInfo _clzBldr, List<ClassBuilderInfo> _generatedClasses) throws DBusException {

        if (StringUtil.isBlank(_dbusSig) || _generatedClasses == null) {
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
            info.setClassName(StringUtil.upperCaseFirstChar(_structName));
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
            _clzBldr.getImports().addAll(info.getImports());
        }

        return parentType == null ? _clzBldr.getPackageName() + "." + _structName : parentType;

    }

    private void createNested(List<StructTree> _list, ClassBuilderInfo _info, List<ClassBuilderInfo> _classes) {
        int position = 0;

        ClassBuilderInfo info = _info;

        for (StructTree inTree : _list) {
            ClassBuilderInfo.ClassMember member = new ClassBuilderInfo.ClassMember("member" + position, inTree.getDataType().getName(), true);
            member.getAnnotations().add("@Position(" + position + ")");
            position++;

            if (Struct.class.isAssignableFrom(inTree.getDataType())) {
                info = new ClassBuilderInfo();
                info.setClassName(StringUtil.upperCaseFirstChar(_info.getClassName()) + "Struct");
                info.setPackageName(_info.getClassName());
                info.setExtendClass(Struct.class.getName());
                info.setClassType(ClassType.CLASS);
                _classes.add(info);
            } else if (Collection.class.isAssignableFrom(inTree.getDataType())) {
                ClassBuilderInfo temp = new ClassBuilderInfo();
                temp.setClassName(info.getClassName());
                temp.setPackageName(info.getPackageName());
                createNested(inTree.getSubType(), temp, _classes);
                info.getImports().addAll(temp.getImports());
                member.getGenerics().addAll(temp.getMembers().stream().map(l -> l.getType()).collect(Collectors.toList()));
            }

            info.getImports().add(inTree.getDataType().getName());
            info.getMembers().add(member);

        }
    }


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


    private List<StructTree> buildTree(String _dbusTypeStr) throws DBusException {
        List<Type> dataType = new ArrayList<>();
        Marshalling.getJavaType(_dbusTypeStr, dataType, 1);

        List<StructTree> root = new ArrayList<>();

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

    private List<StructTree> buildTree(ParameterizedType _pType) throws DBusException {
        List<StructTree> trees = new ArrayList<>();

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
