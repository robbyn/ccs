package ccs.classfile;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClassFile {
    /** class file magic number */
    public static final int MAGIC = 0xCAFEBABE;
    /** access flag */
    public static final short ACC_PUBLIC = 0x0001;
    /** access flag */
    public static final short ACC_FINAL = 0x0010;
    /** access flag */
    public static final short ACC_SUPER = 0x0020;
    /** access flag */
    public static final short ACC_INTERFACE = 0x0200;
    /** access flag */
    public static final short ACC_ABSTRACT = 0x0400;

    /** minor version number */
    private short minorVersion;
    /** major version number */
    private short majorVersion;
    /** constant pool */
    private final ConstantPool cp = new ConstantPool();
    /** access flags */
    private short accessFlags;
    /** index in the constant pool of this class */
    private short thisClass;
    /** index in the constant pool of the superclass */
    private short superClass;
    /** list of implemented interfaces */
    final List<Short> interfaces = new ArrayList<>();
    /** list of the fields */
    final List<FieldInfo> fields = new ArrayList<>();
    /** list of the methods */
    List<MethodInfo> methods = new ArrayList<>();
    /** list of the attributes */
    List<AttributeInfo> attributes = new ArrayList<>();

    public ClassFile() {
    }

    public short getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(short newValue) {
        minorVersion = newValue;
    }

    public short getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(short newValue) {
        majorVersion = newValue;
    }

    public ConstantPool getConstantPool() {
        return cp;
    }

    public short getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(short newValue) {
        accessFlags = newValue;
    }

    public String getClassName() {
        return cp.getClassName(thisClass);
    }

    public short getThisClass() {
        return thisClass;
    }

    public void setThisClass(short newValue) {
        thisClass = newValue;
    }

    public short getSuperClass() {
        return superClass;
    }

    public void setSuperClass(short newValue) {
        superClass = newValue;
    }

    public short[] getInterfaces() {
        short[] result = new short[interfaces.size()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = interfaces.get(i);
        }
        return result;
    }

    public void addInterface(short index) {
        interfaces.add(index);
    }

    public void addInterface(String name) {
        addInterface(cp.addClass(name));
    }

    public boolean hasInterface(short index) {
        return interfaces.indexOf(index) >= 0;
    }

    public boolean hasInterface(String className) {
        return hasInterface(cp.addClass(className));
    }

    public FieldInfo[] getFields() {
        return fields.toArray(new FieldInfo[fields.size()]);
    }

    public void addField(FieldInfo field) {
        fields.add(field);
    }

    public FieldInfo addField(int flags, String name, String type) {
        FieldInfo field = new FieldInfo(
                flags, cp.addUtf8(name), cp.addUtf8(type));
        addField(field);
        return field;
    }

    public MethodInfo[] getMethods() {
        return methods.toArray(new MethodInfo[methods.size()]);
    }

    public void addMethod(MethodInfo method) {
        methods.add(method);
    }

    public MethodInfo addMethod(int flags, String name, String descr) {
        MethodInfo method = new MethodInfo(
                flags, cp.addUtf8(name), cp.addUtf8(descr));
        addMethod(method);
        return method;
    }

    public MethodInfo findMethod(short flags,
            short nameIndex, short descrIndex) {
        for (MethodInfo method: methods) {
            if ((method.getAccessFlags() & flags) == flags
                    && method.getNameIndex() == nameIndex
                    && method.getDescrIndex() == descrIndex) {
                return method;
            }
        }
        return null;
    }

    public MethodInfo findMethod(short flags, String name, String descr) {
        return findMethod(flags, cp.addUtf8(name), cp.addUtf8(descr));
    }

    public AttributeInfo[] getAttributes() {
        return attributes.toArray(new AttributeInfo[attributes.size()]);
    }

    public void addAttribute(AttributeInfo attr) {
        attributes.add(attr);
    }

    public void load(String fileName) throws IOException {
        load(new File(fileName));
    }

    public void load(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            load(stream);
        }
    }

    public void load(InputStream stream) throws IOException {
        DataInput input = new DataInputStream(stream);
        if (input.readInt() != MAGIC) {
            throw new StreamCorruptedException("no magic number found");
        }
        minorVersion = input.readShort();
        majorVersion = input.readShort();
        cp.load(input);
        accessFlags = input.readShort();
        thisClass = input.readShort();
        superClass = input.readShort();
        loadInterfaces(input);
        loadFields(input);
        loadMethods(input);
        loadAttributes(input);
    }

    public void store(String fileName) throws IOException {
        store(new File(fileName));
    }

    public void store(File file) throws IOException {
        file.getParentFile().mkdirs();
        try (OutputStream stream = new FileOutputStream(file)) {
            store(stream);
        }
    }

    public void store(OutputStream stream) throws IOException {
        DataOutput output = new DataOutputStream(stream);
        output.writeInt(MAGIC);
        output.writeShort(minorVersion);
        output.writeShort(majorVersion);
        cp.store(output);
        output.writeShort(accessFlags);
        output.writeShort(thisClass);
        output.writeShort(superClass);
        storeInterfaces(output);
        storeFields(output);
        storeMethods(output);
        storeAttributes(output);
    }

    public void print(PrintStream out) throws IOException {
        out.println("minor_version = " + minorVersion);
        out.println("major_version = " + minorVersion);
        out.print("access_flags =" + flagsToString(accessFlags));
        out.println();
        out.println("this_class = " + cp.getClassName(thisClass)
                + "(" + thisClass + ")");
        out.println("super_class = " + cp.getClassName(superClass)
                + "(" + superClass + ")");
        out.println();
        out.println("Constant pool:");
        cp.print(out);
        out.println();
        printInterfaces(out);
        printFields(out);
        printMethods(out);
        printAttributes(out);
    }

    private void loadInterfaces(DataInput input) throws IOException {
        interfaces.clear();
        int count = input.readShort();
        for (int i = 0; i < count; ++i) {
            interfaces.add(input.readShort());
        }
    }

    private void storeInterfaces(DataOutput output) throws IOException {
        output.writeShort(interfaces.size());
        for (Short s: interfaces) {
            output.writeShort(s);
        }
    }

    private void printInterfaces(PrintStream out) throws IOException {
        out.println("Interfaces: " + interfaces.size());
        for (Short s: interfaces) {
            out.println("  " + cp.toString(s) + "(" + s + ")");
        }
        out.println();
    }

    private void loadFields(DataInput input) throws IOException {
        short count = input.readShort();
        fields.clear();
        for (int i = 0; i < count; ++i) {
            FieldInfo fi = new FieldInfo();
            fi.load(input);
            fields.add(fi);
        }
    }

    private void storeFields(DataOutput output) throws IOException {
        output.writeShort(fields.size());
        for (FieldInfo fi: fields) {
            fi.store(output);
        }
    }

    private void printFields(PrintStream out) throws IOException {
        out.println("Fields: " + fields.size());
        for (FieldInfo fi: fields) {
            fi.print(cp, out);
        }
        out.println();
    }

    private void loadMethods(DataInput input) throws IOException {
        int count = input.readShort();
        for (int i = 0; i < count; ++i) {
            MethodInfo mi = new MethodInfo();
            mi.load(input);
        }
    }

    private void storeMethods(DataOutput output) throws IOException {
        output.writeShort(methods.size());
        for (MethodInfo mi: methods) {
            mi.store(output);
        }
    }

    private void printMethods(PrintStream out) throws IOException {
        out.println("Methods: " + methods.size());
        for (MethodInfo mi: methods) {
            mi.print(cp, out);
        }
        out.println();
    }

    private void loadAttributes(DataInput input) throws IOException {
        attributes.clear();
        attributes.addAll(Arrays.asList(AttributeInfo.loadList(input)));
    }

    private void storeAttributes(DataOutput output) throws IOException {
        AttributeInfo.storeList(output, getAttributes());
    }

    private void printAttributes(PrintStream out) throws IOException {
        AttributeInfo.printList(cp, out, getAttributes());
    }

    public static String flagsToString(int flags) {
        StringBuilder buf = new StringBuilder();
        if ((ACC_PUBLIC & flags) != 0) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append("ACC_PUBLIC");
        }
        if ((ACC_FINAL & flags) != 0) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append("ACC_FINAL");
        }
        if ((ACC_SUPER & flags) != 0) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append("ACC_SUPER");
        }
        if ((ACC_INTERFACE & flags) != 0) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append("ACC_INTERFACE");
        }
        if ((ACC_ABSTRACT & flags) != 0) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            buf.append("ACC_ABSTRACT");
        }
        return buf.toString();
    }
}
