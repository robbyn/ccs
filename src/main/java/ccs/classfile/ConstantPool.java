package ccs.classfile;
import java.io.*;
import java.util.*;

public class ConstantPool {
    /** constant tag */
    private static final int CONSTANT_Class = 7;
    /** constant tag */
    private static final int CONSTANT_Fieldref = 9;
    /** constant tag */
    private static final int CONSTANT_Methodref = 10;
    /** constant tag */
    private static final int CONSTANT_InterfaceMethodref = 11;
    /** constant tag */
    private static final int CONSTANT_String = 8;
    /** constant tag */
    private static final int CONSTANT_Integer = 3;
    /** constant tag */
    private static final int CONSTANT_Float = 4;
    /** constant tag */
    private static final int CONSTANT_Long = 5;
    /** constant tag */
    private static final int CONSTANT_Double = 6;
    /** constant tag */
    private static final int CONSTANT_NameAndType = 12;
    /** constant tag */
    private static final int CONSTANT_Utf8 = 1;

    /** list of the constant pool entries */
    private List<Entry> entries = new ArrayList<>();

    public ConstantPool() {
    }

    public int getCount() {
        return entries.size();
    }

    public String toString(short index) {
        Entry entry = (Entry)entries.get(index);
        return entry.toString(this);
    }

    public short addUtf8(String value) {
        return addEntry(new Utf8Entry(value));
    }

    public short addClass(String className) {
        return addEntry(new ClassEntry(addUtf8(className)));
    }

    public short addNameAndType(short nameIndex, short typeIndex) {
        return addEntry(new NameAndTypeEntry(nameIndex, typeIndex));
    }

    public short addNameAndType(String name, String descr) {
        return addNameAndType(addUtf8(name), addUtf8(descr));
    }

    public short addFieldref(short classIndex, short natIndex) {
        return addEntry(new FieldrefEntry(classIndex, natIndex));
    }

    public short addFieldref(short classIndex, short nameIndex,
            short typeIndex) {
        return addFieldref(classIndex, addNameAndType(nameIndex, typeIndex));
    }

    public short addFieldref(short classIndex, String field, String type) {
        return addFieldref(classIndex, addNameAndType(field, type));
    }

    public short addFieldref(String className, String field, String type) {
        return addFieldref(addClass(className), field, type);
    }

    public short addMethodref(short classIndex, short natIndex) {
        return addEntry(new MethodrefEntry(classIndex, natIndex));
    }

    public short addMethodref(short classIndex, short nameIndex,
            short typeIndex) {
        return addMethodref(classIndex, addNameAndType(nameIndex, typeIndex));
    }

    public short addMethodref(short classIndex, String field, String type) {
        return addMethodref(classIndex, addUtf8(field), addUtf8(type));
    }

    public short addMethodref(String className, String field, String type) {
        return addMethodref(addClass(className), field, type);
    }

    public short addInterfaceMethodref(short classIndex, short natIndex) {
        return addEntry(new InterfaceMethodrefEntry(classIndex, natIndex));
    }

    public short addInterfaceMethodref(short classIndex, short nameIndex,
            short typeIndex) {
        return addInterfaceMethodref(classIndex,
                addNameAndType(nameIndex, typeIndex));
    }

    public short addInterfaceMethodref(short classIndex, String field,
            String type) {
        return addInterfaceMethodref(classIndex, addUtf8(field), addUtf8(type));
    }

    public short addInterfaceMethodref(String className, String field,
                String type) {
        return addInterfaceMethodref(addClass(className), field, type);
    }

    public short addInteger(int value) {
        return addEntry(new IntegerEntry(value));
    }

    public short addFloat(float value) {
        return addEntry(new FloatEntry(value));
    }

    public short addLong(long value) {
        return addEntry(new LongEntry(value));
    }

    public short addDouble(double value) {
        return addEntry(new DoubleEntry(value));
    }

    public short addString(String value) {
        return addEntry(new StringEntry(addUtf8(value)));
    }

    public String getUtf8(short index) {
        Utf8Entry entry = (Utf8Entry)entries.get(index);
        return entry.value;
    }

    public String getString(short index) {
        StringEntry entry = (StringEntry)entries.get(index);
        return getUtf8(entry.stringIndex);
    }

    public String getClassName(short index) {
        ClassEntry entry = (ClassEntry)entries.get(index);
        return getUtf8(entry.nameIndex);
    }

    public short getNameIndex(short index) {
        NameAndTypeEntry entry = (NameAndTypeEntry)entries.get(index);
        return entry.nameIndex;
    }

    public short getTypeIndex(short index) {
        NameAndTypeEntry entry = (NameAndTypeEntry)entries.get(index);
        return entry.descrIndex;
    }

    public String getName(short index) {
        return getUtf8(getNameIndex(index));
    }

    public String getType(short index) {
        return getUtf8(getTypeIndex(index));
    }

    public short getRefClassIndex(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return entry.classIndex;
    }

    public String getRefClassName(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return getClassName(entry.classIndex);
    }

    public short getRefNameIndex(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return getNameIndex(entry.nameAndTypeIndex);
    }

    public String getRefName(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return getName(entry.nameAndTypeIndex);
    }

    public short getRefTypeIndex(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return getTypeIndex(entry.nameAndTypeIndex);
    }

    public String getRefType(short index) {
        RefEntry entry = (RefEntry)entries.get(index);
        return getType(entry.nameAndTypeIndex);
    }

    public void load(DataInput input) throws IOException {
        short count = input.readShort();
        entries.clear();
        entries.add(NullEntry.instance);
        for (int i = 1; i < count; ++i) {
            int tag = input.readByte();
            Entry entry;
            switch (tag) {
            case CONSTANT_Class:
                entry = new ClassEntry();
                break;

            case CONSTANT_Fieldref:
                entry = new FieldrefEntry();
                break;

            case CONSTANT_Methodref:
                entry = new MethodrefEntry();
                break;

            case CONSTANT_InterfaceMethodref:
                entry = new InterfaceMethodrefEntry();
                break;

            case CONSTANT_String:
                entry = new StringEntry();
                break;

            case CONSTANT_Integer:
                entry = new IntegerEntry();
                break;

            case CONSTANT_Float:
                entry = new FloatEntry();
                break;

            case CONSTANT_Long:
                entry = new LongEntry();
                break;

            case CONSTANT_Double:
                entry = new DoubleEntry();
                break;

            case CONSTANT_NameAndType:
                entry = new NameAndTypeEntry();
                break;

            case CONSTANT_Utf8:
                entry = new Utf8Entry();
                break;

            default:
                throw new StreamCorruptedException(
                        "invalid constant pool tag: " + tag);
            }

            entry.load(input);
            entries.add(entry);
            if (tag == CONSTANT_Long || tag == CONSTANT_Double) {
                entries.add(NullEntry.instance);
                ++i;
            }
        }
    }

    public void store(DataOutput output) throws IOException {
        output.writeShort(entries.size());
        for (Entry entry: entries) {
            entry.store(output);
        }
    }

    public void print(PrintStream out) {
        out.println("number of entries: " + entries.size());
        for (int i = 1; i < getCount(); ++i) {
            out.println(Integer.toString(i) + " = " + toString((short)i));
        }
    }

    private short addEntry(Entry entry) {
        int count = entries.size();
        for (int i = 0; i < count; ++i) {
            if (entry.equals(entries.get(i))) {
                return (short)i;
            }
        }
        entries.add(entry);
        return (short)count;
    }

    private abstract static class Entry {
        abstract void load(DataInput input) throws IOException;
        abstract void store(DataOutput input) throws IOException;
        abstract String toString(ConstantPool cp);
    }

    private static class NullEntry extends Entry {
        static NullEntry instance = new NullEntry();

        public boolean equals(Object obj) {
            return false;
        }

        void load(DataInput input) {
        }

        void store(DataOutput output) {
        }

        String toString(ConstantPool cp) {
            return "";
        }
    }

    private static class ClassEntry extends Entry {
        short nameIndex;

        ClassEntry() {
        }

        ClassEntry(short nameIndex) {
            this.nameIndex = nameIndex;
        }

        public boolean equals(Object obj) {
            if (obj instanceof ClassEntry) {
                ClassEntry other = (ClassEntry)obj;
                return this.nameIndex == other.nameIndex;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            nameIndex = input.readShort();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Class);
            output.writeShort(nameIndex);
        }

        String toString(ConstantPool cp) {
            return "class " + cp.toString(nameIndex);
        }
    }

    private static abstract class RefEntry extends Entry {
        short classIndex;
        short nameAndTypeIndex;

        RefEntry() {
        }

        RefEntry(short classIndex, short nameAndTypeIndex) {
            this.classIndex = classIndex;
            this.nameAndTypeIndex = nameAndTypeIndex;
        }

        public boolean equals(Object obj) {
            if (obj.getClass() == this.getClass()) {
                RefEntry other = (RefEntry)obj;
                return this.classIndex == other.classIndex
                        && this.nameAndTypeIndex == other.nameAndTypeIndex;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            classIndex = input.readShort();
            nameAndTypeIndex = input.readShort();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(getTag());
            output.writeShort(classIndex);
            output.writeShort(nameAndTypeIndex);
        }

        abstract int getTag();

        String toString(ConstantPool cp) {
            return cp.getClassName(classIndex).replace('/','.')
                + cp.getName(nameAndTypeIndex) + " "
                + cp.getType(nameAndTypeIndex);
        }
    }

    private static class FieldrefEntry extends RefEntry {
        FieldrefEntry() {
            super();
        }

        FieldrefEntry(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }

        String toString(ConstantPool cp) {
            return "fieldref " + super.toString(cp);
        }

        int getTag() {
            return CONSTANT_Fieldref;
        }
    }

    private static class MethodrefEntry extends RefEntry {
        MethodrefEntry() {
            super();
        }

        MethodrefEntry(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }

        String toString(ConstantPool cp) {
            return "methodref " + super.toString(cp);
        }

        int getTag() {
            return CONSTANT_Methodref;
        }
    }

    private static class InterfaceMethodrefEntry extends RefEntry {
        InterfaceMethodrefEntry() {
            super();
        }

        InterfaceMethodrefEntry(short classIndex, short nameAndTypeIndex) {
            super(classIndex, nameAndTypeIndex);
        }

        String toString(ConstantPool cp) {
            return "interfacemethodref " + super.toString(cp);
        }

        int getTag() {
            return CONSTANT_InterfaceMethodref;
        }
    }

    private static class StringEntry extends Entry {
        short stringIndex;

        StringEntry() {
        }

        StringEntry(short stringIndex) {
            this.stringIndex = stringIndex;
        }

        public boolean equals(Object obj) {
            if (obj instanceof StringEntry) {
                StringEntry other = (StringEntry)obj;
                return this.stringIndex == other.stringIndex;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            stringIndex = input.readShort();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_String);
            output.writeShort(stringIndex);
        }

        String toString(ConstantPool cp) {
            return '"' + cp.toString(stringIndex) + '"';
        }
    }

    private static class IntegerEntry extends Entry {
        int value;

        IntegerEntry() {
        }

        IntegerEntry(int value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj instanceof IntegerEntry) {
                IntegerEntry other = (IntegerEntry)obj;
                return this.value == other.value;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            value = input.readInt();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Integer);
            output.writeInt(value);
        }

        String toString(ConstantPool cp) {
            return "int " + Integer.toString(value);
        }
    }

    private static class FloatEntry extends Entry {
        float value;

        FloatEntry() {
        }

        FloatEntry(float value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj instanceof FloatEntry) {
                FloatEntry other = (FloatEntry)obj;
                return this.value == other.value;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            value = input.readFloat();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Float);
            output.writeFloat(value);
        }

        String toString(ConstantPool cp) {
            return "float " + Float.toString(value);
        }
    }

    private static class LongEntry extends Entry {
        long value;

        LongEntry() {
        }

        LongEntry(long value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj instanceof LongEntry) {
                LongEntry other = (LongEntry)obj;
                return this.value == other.value;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            value = input.readLong();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Long);
            output.writeLong(value);
        }

        String toString(ConstantPool cp) {
            return "long " + Long.toString(value);
        }
    }

    private static class DoubleEntry extends Entry {
        double value;

        DoubleEntry() {
        }

        DoubleEntry(double value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj instanceof DoubleEntry) {
                DoubleEntry other = (DoubleEntry)obj;
                return this.value == other.value;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            value = input.readDouble();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Double);
            output.writeDouble(value);
        }

        String toString(ConstantPool cp) {
            return "double " + Double.toString(value);
        }
    }

    private static class NameAndTypeEntry extends Entry {
        short nameIndex;
        short descrIndex;

        NameAndTypeEntry() {
        }

        NameAndTypeEntry(short nameIndex, short descrIndex) {
            this.nameIndex = nameIndex;
            this.descrIndex = descrIndex;
        }

        public boolean equals(Object obj) {
            if (obj instanceof NameAndTypeEntry) {
                NameAndTypeEntry other = (NameAndTypeEntry)obj;
                return this.nameIndex == other.nameIndex
                        && this.descrIndex == other.descrIndex;
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            nameIndex = input.readShort();
            descrIndex = input.readShort();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_NameAndType);
            output.writeShort(nameIndex);
            output.writeShort(descrIndex);
        }

        String toString(ConstantPool cp) {
            return cp.getUtf8(nameIndex) + " " + cp.getUtf8(descrIndex);
        }
    }

    private static class Utf8Entry extends Entry {
        String value;

        Utf8Entry() {
        }

        Utf8Entry(String value) {
            this.value = value;
        }

        public boolean equals(Object obj) {
            if (obj instanceof Utf8Entry) {
                Utf8Entry other = (Utf8Entry)obj;
                return this.value.equals(other.value);
            } else {
                return false;
            }
        }

        void load(DataInput input) throws IOException {
            value = input.readUTF();
        }

        void store(DataOutput output) throws IOException {
            output.writeByte(CONSTANT_Utf8);
            output.writeUTF(value);
        }

        String toString(ConstantPool cp) {
            return value;
        }
    }
}
