package ccs.classfile;
import java.io.*;

public class ExceptionInfo {
    private short startPc;
    private short endPc;
    private short handlerPc;
    private short typeIndex;

    public ExceptionInfo() {
    }

    public ExceptionInfo(short startPc, short endPc, short handlerPc,
            short typeIndex) {
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.typeIndex = typeIndex;
    }

    public void load(DataInput input) throws IOException {
        startPc = input.readShort();
        endPc = input.readShort();
        handlerPc = input.readShort();
        typeIndex = input.readShort();
    }


    public void store(DataOutput output) throws IOException {
        output.writeShort(startPc);
        output.writeShort(endPc);
        output.writeShort(handlerPc);
        output.writeShort(typeIndex);
    }

    public static void storeList(DataOutput output, ExceptionInfo list[])
            throws IOException {
        output.writeShort(list.length);
        for (int i = 0; i < list.length; ++i) {
            list[i].store(output);
        }
    }
}
