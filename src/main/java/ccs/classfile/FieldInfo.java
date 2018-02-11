package ccs.classfile;
import java.io.*;

public class FieldInfo {
    /** access flag */
    public static final short ACC_PUBLIC = 0x0001;
    /** access flag */
    public static final short ACC_PRIVATE = 0x0002;
    /** access flag */
    public static final short ACC_PROTECTED = 0x0004;
    /** access flag */
    public static final short ACC_STATIC = 0x0008;
    /** access flag */
    public static final short ACC_FINAL = 0x0010;
    /** access flag */
    public static final short ACC_SUPER = 0x0020;
    /** access flag */
    public static final short ACC_VOLATILE = 0x0040;
    /** access flag */
    public static final short ACC_TRANSIENT = 0x0080;

    private short accessFlags;
    private short nameIndex;
    private short typeIndex;
    private AttributeInfo attributes[] = new AttributeInfo[0];

    public FieldInfo(int accessFlags, short nameIndex, short typeIndex) {
        this.accessFlags = (short)accessFlags;
        this.nameIndex = nameIndex;
        this.typeIndex = typeIndex;
    }

    public FieldInfo() {
    }

    public short getAccessFlags() {
        return accessFlags;
    }

    public void setAccessFlags(short newValue) {
        accessFlags = newValue;
    }

    public short getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(short newValue) {
        nameIndex = newValue;
    }

    public short getTypeIndex() {
        return typeIndex;
    }

    public void setTypeIndex(short newValue) {
        typeIndex = newValue;
    }

    public AttributeInfo[] getAttributes() {
        return attributes;
    }

    public void addAttribute(AttributeInfo attr) {
        int len = attributes.length;
        AttributeInfo attributes2[] = new AttributeInfo[len+1];
        System.arraycopy(attributes, 0, attributes2, 0, len);
        attributes2[len] = attr;
        attributes = attributes2;
    }

    void load(DataInput input) throws IOException {
        accessFlags = input.readShort();
        nameIndex = input.readShort();
        typeIndex = input.readShort();
        attributes = AttributeInfo.loadList(input);
    }

    void store(DataOutput output) throws IOException {
        output.writeShort(accessFlags);
        output.writeShort(nameIndex);
        output.writeShort(typeIndex);
        AttributeInfo.storeList(output, attributes);
    }

    void print(ConstantPool cp, PrintStream out) throws IOException {
        out.println("access_flags = " + flagsToString(accessFlags));
        out.println("name_index = " + cp.getUtf8(nameIndex)
                + "(" + nameIndex + ")");
        out.println("descriptor_index = " + cp.getUtf8(typeIndex)
                + "(" + typeIndex + ")");
        AttributeInfo.printList(cp, out, attributes);
    }

    private static String flagsToString(int flags) {
        StringBuffer buff = new StringBuffer();
        if ((ACC_PUBLIC & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_PUBLIC");
        }
        if ((ACC_PRIVATE & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_PRIVATE");
        }
        if ((ACC_PROTECTED & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_PROTECTED");
        }
        if ((ACC_STATIC & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_STATIC");
        }
        if ((ACC_FINAL & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_FINAL");
        }
        if ((ACC_VOLATILE & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_VOLATILE");
        }
        if ((ACC_TRANSIENT & flags) != 0) {
            if (buff.length() > 0) {
                buff.append(',');
            }
            buff.append("ACC_TRANSIENT");
        }
        return buff.toString();
    }
}
