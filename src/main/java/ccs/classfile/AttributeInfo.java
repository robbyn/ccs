package ccs.classfile;
import java.io.*;

public class AttributeInfo {
    private short nameIndex;
    private byte data[];
    private static byte info;

    private AttributeInfo() {
    }

    public AttributeInfo(short nameIndex, byte data[]) {
        this.nameIndex = nameIndex;
        this.data = data;
    }

    public short getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(short newValue) {
        nameIndex = newValue;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte newValue[]) {
        data = newValue;
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(data);
    }

    public DataInputStream getDataInput() {
        return new DataInputStream(getInputStream());
    }

    public OutputStream getOutputStream() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                data = toByteArray();
            }
        };
    }

    public DataOutputStream getDataOutput() {
        return new DataOutputStream(getOutputStream());
    }

    public void load(DataInput input) throws IOException {
        nameIndex = input.readShort();
        data = new byte[input.readInt()];
        input.readFully(data);
    }

    public static AttributeInfo[] loadList(DataInput input) throws IOException {
        AttributeInfo result[] = new AttributeInfo[input.readShort()];
        for (int i = 0; i < result.length; ++i) {
            result[i] = new AttributeInfo();
            result[i].load(input);
        }
        return result;
    }

    public void store(DataOutput output) throws IOException {
        output.writeShort(nameIndex);
        output.writeInt(data.length);
        output.write(data);
    }

    public static void storeList(DataOutput output, AttributeInfo array[])
            throws IOException {
        output.writeShort(array.length);
        for (int i = 0; i < array.length; ++i) {
            array[i].store(output);
        }
    }

    public void print(ConstantPool cp, PrintStream out) throws IOException {
        out.println("attribute " + cp.getUtf8(nameIndex)
                + " length: " + data.length);
    }

    public static void printList(ConstantPool cp, PrintStream out,
            AttributeInfo array[]) throws IOException {
        out.println("Attributes: " + array.length);
        for (int i = 0; i < array.length; ++i) {
            array[i].print(cp, out);
        }
        out.println();
    }
}
