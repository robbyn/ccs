package ccs.classfile;

import java.io.*;
import java.util.*;

public class CodeBuilder extends ByteArrayOutputStream {
    private final ConstantPool cp;
    private short localTop;
    private short stackTop;
    private short localMax;
    private short stackMax;
    private final List<ExceptionInfo> exceptions = new ArrayList<>();
    private final List<AttributeInfo> attributes = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();

    public CodeBuilder(ConstantPool cp, int argsSize) {
        localTop = (short)argsSize;
        localMax = (short)argsSize;
        this.cp = cp;
    }

    public short getStackMax() {
        return stackMax;
    }

    public short getLocalMax() {
        return localMax;
    }

    public void load(DataInput input) throws IOException {
        stackMax = input.readShort();
        stackTop = 0;
        localMax = input.readShort();
        localTop = 0;
        buf = new byte[input.readInt()];
        input.readFully(buf);
        count = buf.length;
        int n = input.readShort() & 0xFFFF;
        for (int i = 0; i < n; ++i) {
            ExceptionInfo ei = new ExceptionInfo();
            ei.load(input);
            exceptions.add(ei);
        }
        attributes.addAll(Arrays.asList(AttributeInfo.loadList(input)));
    }

    public void store(DataOutput output) throws IOException {
        /* fixup label references */
        for (Label label: labels) {
            label.fixupRefs(this);
        }
        output.writeShort(stackMax);
        output.writeShort(localMax);
        output.writeInt(count);
        output.write(buf, 0, count);
        output.writeShort(exceptions.size());
        for (ExceptionInfo ei: exceptions) {
            ei.store(output);
        }
        AttributeInfo.storeList(output, attributes.toArray(
                new AttributeInfo[attributes.size()]));
    }

    public int getLength() {
        return count;
    }

    public int getLocation() {
        return count;
    }

    public void setLocation(int newValue) {
        count = newValue;
    }

    public void addException(ExceptionInfo ei) {
        exceptions.add(ei);
    }

    public void addException(short startPc, short endPc, short handlerPc,
            short typeIndex) {
        addException(new ExceptionInfo(startPc, endPc, handlerPc, typeIndex));
    }

    public void addException(short startPc, short endPc, short handlerPc,
            String typeName) {
        addException(startPc, endPc, handlerPc, cp.addClass(typeName));
    }

    public int getByte(int pos) {
        return buf[pos] & 0xff;
    }

    public short getShort(int pos) {
        return (short)(((buf[pos] & 0xff) <<  8) |
                       ((buf[pos+1] & 0xff)));
    }

    public int getInt(int pos) {
        return (((buf[pos] & 0xFF) << 24 ) |
                ((buf[pos+1] & 0xFF) << 16) |
                ((buf[pos+2] & 0xff) <<  8) |
                ((buf[pos+3] & 0xff)));
    }

    public void fixupByte(int pos, int value) {
        buf[pos] = (byte)value;
    }

    public void fixupShort(int pos, int value) {
        buf[pos] = (byte)(0xff & (value >>> 8));
        buf[pos+1] = (byte)(0xff & value);
    }

    public void fixupInt(int pos, int value) {
        buf[pos] = (byte)(0xff & (value >> 24));
        buf[pos+1] = (byte)(0xff & (value >> 16));
        buf[pos+2] = (byte)(0xff & (value >>  8));
        buf[pos+3] = (byte)(0xff & value);
    }

    public short newLocal() {
        short result = localTop;
        ++localTop;
        if (localTop > localMax) {
            localMax = localTop;
        }
        return result;
    }

    public short newLocal2() {
        short result = localTop;
        localTop += 2;
        if (localTop > localMax) {
            localMax = localTop;
        }
        return result;
    }

//------------------------------------------------------------------------------
//  constants
//------------------------------------------------------------------------------

    public void pushInt(int value) {
        if (value >= -1 && value <= 5) {
            reserveStack(1);
            switch (value) {
            case -1:
                write(ByteCode.ICONST_M1);
                break;

            case 0:
                write(ByteCode.ICONST_0);
                break;

            case 1:
                write(ByteCode.ICONST_1);
                break;

            case 2:
                write(ByteCode.ICONST_2);
                break;

            case 3:
                write(ByteCode.ICONST_3);
                break;

            case 4:
                write(ByteCode.ICONST_4);
                break;

            case 5:
                write(ByteCode.ICONST_5);
                break;
            }
        } else if (value >= -128 && value <= 127) {
            reserveStack(1);
            write(ByteCode.BIPUSH);
            write(value);
        } else if (value >= -32768 && value <= 32767) {
            reserveStack(1);
            write(ByteCode.SIPUSH);
            writeShort(value);
        } else {
            pushConst(cp.addInteger(value));
        }
    }

    public void pushLong(long value) {
        if (value == 0) {
            reserveStack(2);
            write(ByteCode.LCONST_0);
        } else if (value == 1) {
            reserveStack(2);
            write(ByteCode.LCONST_1);
        } else if (value >= -128 && value <= 127) {
            pushInt((int)value);
            intToLong();
        } else {
            pushConst2(cp.addLong(value));
        }
    }

    public void pushFloat(float value) {
        if (value == 0.0) {
            reserveStack(1);
            write(ByteCode.FCONST_0);
        } else if (value == 1.0) {
            reserveStack(1);
            write(ByteCode.FCONST_1);
        } else if (value == 2.0) {
            reserveStack(1);
            write(ByteCode.FCONST_2);
        } else {
            pushConst(cp.addFloat(value));
        }
    }

    public void pushDouble(double value) {
        if (value == 0.0) {
            reserveStack(2);
            write(ByteCode.DCONST_0);
        } else if (value == 1.0) {
            reserveStack(2);
            write(ByteCode.DCONST_1);
        } else {
            pushConst2(cp.addDouble(value));
        }
    }

    public void pushNull() {
        reserveStack(1);
        write(ByteCode.ACONST_NULL);
    }

    public void pushString(String s) {
        pushConst(cp.addString(s));
    }

//------------------------------------------------------------------------------
//  local variables
//------------------------------------------------------------------------------

    public void loadInt(int index) {
        reserveStack(1);
        switch (index) {
        case 0:
            write(ByteCode.ILOAD_0);
            break;

        case 1:
            write(ByteCode.ILOAD_1);
            break;

        case 2:
            write(ByteCode.ILOAD_2);
            break;

        case 3:
            write(ByteCode.ILOAD_3);
            break;

        default:
            localOp(ByteCode.ILOAD, index);
        }
    }

    public void loadFloat(int index) {
        reserveStack(1);
        switch (index) {
        case 0:
            write(ByteCode.FLOAD_0);
            break;

        case 1:
            write(ByteCode.FLOAD_1);
            break;

        case 2:
            write(ByteCode.FLOAD_2);
            break;

        case 3:
            write(ByteCode.FLOAD_3);
            break;

        default:
            localOp(ByteCode.FLOAD, index);
        }
    }

    public void loadLong(int index) {
        reserveStack(2);
        switch (index) {
        case 0:
            write(ByteCode.LLOAD_0);
            break;

        case 1:
            write(ByteCode.LLOAD_1);
            break;

        case 2:
            write(ByteCode.LLOAD_2);
            break;

        case 3:
            write(ByteCode.LLOAD_3);
            break;

        default:
            localOp(ByteCode.LLOAD, index);
        }
    }

    public void loadDouble(int index) {
        reserveStack(2);
        switch (index) {
        case 0:
            write(ByteCode.DLOAD_0);
            break;

        case 1:
            write(ByteCode.DLOAD_1);
            break;

        case 2:
            write(ByteCode.DLOAD_2);
            break;

        case 3:
            write(ByteCode.DLOAD_3);
            break;

        default:
            localOp(ByteCode.DLOAD, index);
        }
    }

    public void loadRef(int index) {
        reserveStack(1);
        switch (index) {
        case 0:
            write(ByteCode.ALOAD_0);
            break;

        case 1:
            write(ByteCode.ALOAD_1);
            break;

        case 2:
            write(ByteCode.ALOAD_2);
            break;

        case 3:
            write(ByteCode.ALOAD_3);
            break;

        default:
            localOp(ByteCode.ALOAD, index);
        }
    }

    public void storeInt(int index) {
        releaseStack(1);
        switch (index) {
        case 0:
            write(ByteCode.ISTORE_0);
            break;

        case 1:
            write(ByteCode.ISTORE_1);
            break;

        case 2:
            write(ByteCode.ISTORE_2);
            break;

        case 3:
            write(ByteCode.ISTORE_3);
            break;

        default:
            localOp(ByteCode.ISTORE, index);
        }
    }

    public void storeFloat(int index) {
        releaseStack(1);
        switch (index) {
        case 0:
            write(ByteCode.FSTORE_0);
            break;

        case 1:
            write(ByteCode.FSTORE_1);
            break;

        case 2:
            write(ByteCode.FSTORE_2);
            break;

        case 3:
            write(ByteCode.FSTORE_3);
            break;

        default:
            localOp(ByteCode.FSTORE, index);
        }
    }

    public void storeLong(int index) {
        releaseStack(2);
        switch (index) {
        case 0:
            write(ByteCode.LSTORE_0);
            break;

        case 1:
            write(ByteCode.LSTORE_1);
            break;

        case 2:
            write(ByteCode.LSTORE_2);
            break;

        case 3:
            write(ByteCode.LSTORE_3);
            break;

        default:
            localOp(ByteCode.LSTORE, index);
        }
    }

    public void storeDouble(int index) {
        releaseStack(2);
        switch (index) {
        case 0:
            write(ByteCode.DSTORE_0);
            break;

        case 1:
            write(ByteCode.DSTORE_1);
            break;

        case 2:
            write(ByteCode.DSTORE_2);
            break;

        case 3:
            write(ByteCode.DSTORE_3);
            break;

        default:
            localOp(ByteCode.DSTORE, index);
        }
    }

    public void storeRef(int index) {
        releaseStack(1);
        switch (index) {
        case 0:
            write(ByteCode.ASTORE_0);
            break;

        case 1:
            write(ByteCode.ASTORE_1);
            break;

        case 2:
            write(ByteCode.ASTORE_2);
            break;

        case 3:
            write(ByteCode.ASTORE_3);
            break;

        default:
            localOp(ByteCode.ASTORE, index);
        }
    }

    public void incInt(int index, int increment) {
        if (index <= 255 && increment >= -128 && increment <= 127) {
            write(ByteCode.IINC);
            write(index);
            write(increment);
        } else {
            write(ByteCode.WIDE);
            write(ByteCode.IINC);
            writeShort(index);
            writeShort(increment);
        }
    }

//------------------------------------------------------------------------------
//  fields
//------------------------------------------------------------------------------

    public void getField(short refIndex) {
        releaseStack(1);
        String type = cp.getRefType(refIndex);
        if (type.equals("J") || type.equals("D")) {
            reserveStack(2);
        } else {
            reserveStack(1);
        }
        write(ByteCode.GETFIELD);
        writeShort(refIndex);
    }

    public void getField(short classIndex, short natIndex) {
        getField(cp.addFieldref(classIndex, natIndex));
    }

    public void getField(short classIndex, short nameIndex, short typeIndex) {
        getField(cp.addFieldref(classIndex, nameIndex, typeIndex));
    }

    public void getField(short classIndex, String name, String type) {
        getField(cp.addFieldref(classIndex, name, type));
    }

    public void getField(String className, String name, String type) {
        getField(cp.addFieldref(className, name, type));
    }

    public void putField(short refIndex) {
        releaseStack(1);
        String type = cp.getRefType(refIndex);
        if (type.equals("J") || type.equals("D")) {
            releaseStack(2);
        } else {
            releaseStack(1);
        }
        write(ByteCode.PUTFIELD);
        writeShort(refIndex);
    }

    public void putField(short classIndex, short natIndex) {
        putField(cp.addFieldref(classIndex, natIndex));
    }

    public void putField(short classIndex, short nameIndex, short typeIndex) {
        putField(cp.addFieldref(classIndex, nameIndex, typeIndex));
    }

    public void putField(short classIndex, String name, String type) {
        putField(cp.addFieldref(classIndex, name, type));
    }

    public void putField(String className, String name, String type) {
        putField(cp.addFieldref(className, name, type));
    }

    public void getStatic(short refIndex) {
        String type = cp.getRefType(refIndex);
        if (type.equals("J") || type.equals("D")) {
            reserveStack(2);
        } else {
            reserveStack(1);
        }
        write(ByteCode.GETSTATIC);
        writeShort(refIndex);
    }

    public void getStatic(short classIndex, short natIndex) {
        getStatic(cp.addFieldref(classIndex, natIndex));
    }

    public void getStatic(short classIndex, short nameIndex, short typeIndex) {
        getStatic(cp.addFieldref(classIndex, nameIndex, typeIndex));
    }

    public void getStatic(short classIndex, String name, String type) {
        getStatic(cp.addFieldref(classIndex, name, type));
    }

    public void getStatic(String className, String name, String type) {
        getStatic(cp.addFieldref(className, name, type));
    }

    public void putStatic(short refIndex) {
        String type = cp.getRefType(refIndex);
        if (type.equals("J") || type.equals("D")) {
            releaseStack(2);
        } else {
            releaseStack(1);
        }
        write(ByteCode.PUTSTATIC);
        writeShort(refIndex);
    }

    public void putStatic(short classIndex, short natIndex) {
        putStatic(cp.addFieldref(classIndex, natIndex));
    }

    public void putStatic(short classIndex, short nameIndex, short typeIndex) {
        putStatic(cp.addFieldref(classIndex, nameIndex, typeIndex));
    }

    public void putStatic(short classIndex, String name, String type) {
        putStatic(cp.addFieldref(classIndex, name, type));
    }

    public void putStatic(String className, String name, String type) {
        putStatic(cp.addFieldref(className, name, type));
    }

//------------------------------------------------------------------------------
//  methods
//------------------------------------------------------------------------------

    public void invokeInterface(short refIndex) {
        releaseStack(1); /* for the 'this' reference */
        int asize = invokeStack(cp.getRefType(refIndex));
        write(ByteCode.INVOKEINTERFACE);
        writeShort(refIndex);
        write(asize+1);
        write(0);
    }

    public void invokeInterface(short classIndex, short natIndex) {
        invokeInterface(cp.addInterfaceMethodref(classIndex, natIndex));
    }

    public void invokeInterface(short classIndex,
            short nameIndex, short typeIndex) {
        invokeInterface(cp.addInterfaceMethodref(
                classIndex, nameIndex, typeIndex));
    }

    public void invokeInterface(short classIndex, String name, String type) {
        invokeInterface(cp.addInterfaceMethodref(classIndex, name, type));
    }

    public void invokeInterface(String className, String name, String type) {
        invokeInterface(cp.addInterfaceMethodref(className, name, type));
    }

    public void invokeSpecial(short refIndex) {
        releaseStack(1); /* for the 'this' reference */
        invokeStack(cp.getRefType(refIndex));
        write(ByteCode.INVOKESPECIAL);
        writeShort(refIndex);
    }

    public void invokeSpecial(short classIndex, short natIndex) {
        invokeSpecial(cp.addMethodref(classIndex, natIndex));
    }

    public void invokeSpecial(short classIndex,
            short nameIndex, short typeIndex) {
        invokeSpecial(cp.addMethodref(classIndex, nameIndex, typeIndex));
    }

    public void invokeSpecial(short classIndex, String name, String type) {
        invokeSpecial(cp.addMethodref(classIndex, name, type));
    }

    public void invokeSpecial(String className, String name, String type) {
        invokeSpecial(cp.addMethodref(className, name, type));
    }

    public void invokeVirtual(short refIndex) {
        releaseStack(1); /* for the 'this' reference */
        invokeStack(cp.getRefType(refIndex));
        write(ByteCode.INVOKEVIRTUAL);
        writeShort(refIndex);
    }

    public void invokeVirtual(short classIndex, short natIndex) {
        invokeVirtual(cp.addMethodref(classIndex, natIndex));
    }

    public void invokeVirtual(short classIndex,
            short nameIndex, short typeIndex) {
        invokeVirtual(cp.addMethodref(classIndex, nameIndex, typeIndex));
    }

    public void invokeVirtual(short classIndex, String name, String type) {
        invokeVirtual(cp.addMethodref(classIndex, name, type));
    }

    public void invokeVirtual(String className, String name, String type) {
        invokeVirtual(cp.addMethodref(className, name, type));
    }

    public void invokeStatic(short refIndex) {
        invokeStack(cp.getRefType(refIndex));
        write(ByteCode.INVOKESTATIC);
        writeShort(refIndex);
    }

    public void invokeStatic(short classIndex, short natIndex) {
        invokeStatic(cp.addMethodref(classIndex, natIndex));
    }

    public void invokeStatic(short classIndex,
            short nameIndex, short typeIndex) {
        invokeStatic(cp.addMethodref(classIndex, nameIndex, typeIndex));
    }

    public void invokeStatic(short classIndex, String name, String type) {
        invokeStatic(cp.addMethodref(classIndex, name, type));
    }

    public void invokeStatic(String className, String name, String type) {
        invokeStatic(cp.addMethodref(className, name, type));
    }

    public void retrn() {
        write(ByteCode.RETURN);
    }

    public void returnInt() {
        releaseStack(1);
        write(ByteCode.IRETURN);
    }

    public void returnFloat() {
        releaseStack(1);
        write(ByteCode.FRETURN);
    }

    public void returnLong() {
        releaseStack(2);
        write(ByteCode.LRETURN);
    }

    public void returnDouble() {
        releaseStack(2);
        write(ByteCode.DRETURN);
    }

    public void returnRef() {
        releaseStack(1);
        write(ByteCode.ARETURN);
    }

//------------------------------------------------------------------------------
//  array operations
//------------------------------------------------------------------------------

    public void makeRefArray(short typeIndex) {
        write(ByteCode.ANEWARRAY);
        writeShort(typeIndex);
    }

    public void makeRefArray(String type) {
        makeRefArray(cp.addClass(type));
    }

    public void arrayLength() {
        write(ByteCode.ARRAYLENGTH);
    }

    public void loadArrayInt() {
        releaseStack(1);
        write(ByteCode.IALOAD);
    }

    public void loadArrayLong() {
        write(ByteCode.LALOAD);
    }

    public void loadArrayFloat() {
        releaseStack(1);
        write(ByteCode.FALOAD);
    }

    public void loadArrayDouble() {
        write(ByteCode.DALOAD);
    }

    public void loadArrayRef() {
        releaseStack(1);
        write(ByteCode.AALOAD);
    }

    public void storeArrayInt() {
        releaseStack(3);
        write(ByteCode.IASTORE);
    }

    public void storeArrayLong() {
        releaseStack(4);
        write(ByteCode.LASTORE);
    }

    public void storeArrayFloat() {
        releaseStack(3);
        write(ByteCode.FASTORE);
    }

    public void storeArrayDouble() {
        releaseStack(4);
        write(ByteCode.DASTORE);
    }

    public void storeArrayRef() {
        releaseStack(3);
        write(ByteCode.AASTORE);
    }

//------------------------------------------------------------------------------
//  conversions
//------------------------------------------------------------------------------

    public void intToByte() {
        write(ByteCode.I2B);
    }

    public void intToChar() {
        write(ByteCode.I2C);
    }

    public void intToShort() {
        write(ByteCode.I2F);
    }

    public void intToFloat() {
        write(ByteCode.I2F);
    }

    public void intToLong() {
        reserveStack(1);
        write(ByteCode.I2L);
    }

    public void intToDouble() {
        reserveStack(1);
        write(ByteCode.I2D);
    }

    public void floatToDouble() {
        reserveStack(1);
        write(ByteCode.F2D);
    }

    public void floatToLong() {
        reserveStack(1);
        write(ByteCode.F2L);
    }

    public void floatToInt() {
        write(ByteCode.F2I);
    }

    public void longToDouble() {
        write(ByteCode.L2D);
    }

    public void longToFloat() {
        releaseStack(1);
        write(ByteCode.L2F);
    }

    public void longToInt() {
        releaseStack(1);
        write(ByteCode.L2I);
    }

    public void doubleToLong() {
        write(ByteCode.D2L);
    }

    public void doubleToFloat() {
        releaseStack(1);
        write(ByteCode.D2F);
    }

    public void doubleToInt() {
        releaseStack(1);
        write(ByteCode.D2I);
    }

//------------------------------------------------------------------------------
//  stack operations
//------------------------------------------------------------------------------

    public void pop() {
        releaseStack(1);
        write(ByteCode.POP);
    }

    public void pop2() {
        releaseStack(2);
        write(ByteCode.POP2);
    }

    public void swap() {
        write(ByteCode.SWAP);
    }

    public void dup() {
        reserveStack(1);
        write(ByteCode.DUP);
    }

    public void dupX1() {
        reserveStack(1);
        write(ByteCode.DUP_X1);
    }

    public void dupX2() {
        reserveStack(1);
        write(ByteCode.DUP_X2);
    }

    public void dup2() {
        reserveStack(2);
        write(ByteCode.DUP2);
    }

    public void dup2X1() {
        reserveStack(2);
        write(ByteCode.DUP2_X1);
    }

    public void dup2X2() {
        reserveStack(2);
        write(ByteCode.DUP2_X2);
    }

//------------------------------------------------------------------------------
//  objects
//------------------------------------------------------------------------------

    public void checkCast(int classIndex) {
        write(ByteCode.CHECKCAST);
        writeShort(classIndex);
    }

    public void checkCast(String className) {
        checkCast(cp.addClass(className));
    }

    public void newObject(int classIndex) {
        reserveStack(1);
        write(ByteCode.NEW);
        writeShort(classIndex);
    }

    public void newObject(String className) {
        newObject(cp.addClass(className));
    }

//------------------------------------------------------------------------------
//  exceptions
//------------------------------------------------------------------------------

    public void throwRef() {
        releaseStack(1);
        write(ByteCode.ATHROW);
    }

//------------------------------------------------------------------------------
//  arithmetic operations
//------------------------------------------------------------------------------

    public void addInt() {
        releaseStack(1);
        write(ByteCode.IADD);
    }

    public void andInt() {
        releaseStack(1);
        write(ByteCode.IAND);
    }

    public void divInt() {
        releaseStack(1);
        write(ByteCode.IDIV);
    }

    public void mulInt() {
        releaseStack(1);
        write(ByteCode.IMUL);
    }

    public void negInt() {
        write(ByteCode.INEG);
    }

    public void orInt() {
        releaseStack(1);
        write(ByteCode.IOR);
    }

    public void remInt() {
        releaseStack(1);
        write(ByteCode.IREM);
    }

    public void shlInt() {
        releaseStack(1);
        write(ByteCode.ISHL);
    }

    public void shrInt() {
        releaseStack(1);
        write(ByteCode.ISHR);
    }

    public void subInt() {
        releaseStack(1);
        write(ByteCode.ISUB);
    }

    public void ushrInt() {
        releaseStack(1);
        write(ByteCode.IUSHR);
    }

    public void xorInt() {
        releaseStack(1);
        write(ByteCode.IXOR);
    }

    public void addLong() {
        releaseStack(2);
        write(ByteCode.LADD);
    }

    public void andLong() {
        releaseStack(2);
        write(ByteCode.LAND);
    }

    public void divLong() {
        releaseStack(2);
        write(ByteCode.LDIV);
    }

    public void mulLong() {
        releaseStack(2);
        write(ByteCode.LMUL);
    }

    public void negLong() {
        write(ByteCode.LNEG);
    }

    public void orLong() {
        releaseStack(2);
        write(ByteCode.LOR);
    }

    public void remLong() {
        releaseStack(2);
        write(ByteCode.LREM);
    }

    public void shlLong() {
        releaseStack(1);
        write(ByteCode.LSHL);
    }

    public void shrLong() {
        releaseStack(1);
        write(ByteCode.LSHR);
    }

    public void subLong() {
        releaseStack(2);
        write(ByteCode.LSUB);
    }

    public void ushrLong() {
        releaseStack(1);
        write(ByteCode.LUSHR);
    }

    public void xorLong() {
        releaseStack(2);
        write(ByteCode.LXOR);
    }

    public void addFloat() {
        releaseStack(1);
        write(ByteCode.FADD);
    }

    public void cmpgFloat() {
        releaseStack(1);
        write(ByteCode.FCMPG);
    }

    public void cmplFloat() {
        releaseStack(1);
        write(ByteCode.FCMPL);
    }

    public void divFloat() {
        releaseStack(1);
        write(ByteCode.FDIV);
    }

    public void mulFloat() {
        releaseStack(1);
        write(ByteCode.FMUL);
    }

    public void negFloat() {
        write(ByteCode.FNEG);
    }

    public void remFloat() {
        releaseStack(1);
        write(ByteCode.FREM);
    }

    public void subFloat() {
        releaseStack(1);
        write(ByteCode.ISUB);
    }

    public void addDouble() {
        releaseStack(2);
        write(ByteCode.DADD);
    }

    public void cmpgDouble() {
        releaseStack(2);
        write(ByteCode.DCMPG);
    }

    public void cmplDouble() {
        releaseStack(2);
        write(ByteCode.DCMPL);
    }

    public void divDouble() {
        releaseStack(2);
        write(ByteCode.DDIV);
    }

    public void mulDouble() {
        releaseStack(2);
        write(ByteCode.DMUL);
    }

    public void negDouble() {
        write(ByteCode.DNEG);
    }

    public void remDouble() {
        releaseStack(2);
        write(ByteCode.DREM);
    }

    public void subDouble() {
        releaseStack(2);
        write(ByteCode.DSUB);
    }

//------------------------------------------------------------------------------
//  labels and jumps
//------------------------------------------------------------------------------

    public void define(Label label) {
        label.define(getLocation());
        labels.add(label);
    }

    public void jump(Label label) {
        jump(ByteCode.GOTO, label);
    }

    public void jump(int opcode, Label label) {
        /* adjust stack */
        switch (opcode) {
        case ByteCode.IF_ICMPEQ:
        case ByteCode.IF_ICMPNE:
        case ByteCode.IF_ICMPLT:
        case ByteCode.IF_ICMPGE:
        case ByteCode.IF_ICMPGT:
        case ByteCode.IF_ICMPLE:
        case ByteCode.IF_ACMPEQ:
        case ByteCode.IF_ACMPNE:
            releaseStack(2);
            break;

        case ByteCode.IFNULL:
        case ByteCode.IFNONNULL:
        case ByteCode.IFEQ:
        case ByteCode.IFNE:
        case ByteCode.IFLT:
        case ByteCode.IFGE:
        case ByteCode.IFGT:
        case ByteCode.IFLE:
            releaseStack(1);
            break;

        case ByteCode.GOTO:
            break;

        default:
            throw new InvalidInstructionException(opcode);
        }

        label.addRef(new JumpRef(getLocation()));
        write(opcode);
        writeShort(0);
    }

    public void tableSwitch(int min, int max, Label def, Label table[]) {
        if (max-min+1 != table.length) {
            throw new TableSwitchException();
        }
        int opLocation = getLocation();
        write(ByteCode.TABLESWITCH);
        while ((getLocation() % 4) != 0) {
            write(0);
        }
        def.addRef(new TableSwitchRef(opLocation, getLocation()));
        writeInt(0);
        writeInt(min);
        writeInt(max);
        for (int i = 0; i < table.length; ++i) {
            table[i].addRef(new TableSwitchRef(opLocation, getLocation()));
            writeInt(0);
        }
    }

    public short beginBlock() {
        return localTop;
    }

    public void endBlock(short b) {
        localTop = b;
    }

//------------------------------------------------------------------------------
//  private stuff
//------------------------------------------------------------------------------

    void writeShort(int value) {
        write((byte)(value >>> 8));
        write((byte)value);
    }

    void writeInt(int value) {
        write((byte)(value >>> 24));
        write((byte)(value >>> 16));
        write((byte)(value >>> 8));
        write((byte)value);
    }

    void reserveStack(int count) {
        stackTop += count;
        if (stackTop > stackMax) {
            stackMax = stackTop;
        }
    }

    void releaseStack(int count) {
        stackTop -= count;
    }

    private void pushConst(int index) {
        reserveStack(1);
        if (index <= 255) {
            write(ByteCode.LDC);
            write(index);
        } else {
            write(ByteCode.LDC_W);
            writeShort(index);
        }
    }

    private void pushConst2(int index) {
        reserveStack(2);
        write(ByteCode.LDC2_W);
        writeShort(index);
    }

    private void localOp(int opcode, int index) {
        if (index <= 255) {
            write(opcode);
            write((byte)index);
        } else {
            write(ByteCode.WIDE);
            write(opcode);
            writeShort(index);
        }
    }

    /**
     * updates the stack pointer for a method invocation
     * @param type descriptor of the function
     * @return number of slots required by the arguments
     */
    private int invokeStack(String type) {
        try {
            Reader reader = new StringReader(type);
            int argsSize = 0;
            int c = reader.read();
            if (c != '(') {
                throw new RuntimeException(
                        "Invalid method descriptor: " + type);
            }
            c = reader.read();
            while (c >= 0 && c != ')') {
                switch (c) {
                case 'D':
                case 'J':
                    argsSize += 2;
                    break;

                default:
                    ++argsSize;
                }
                while (c == '[') {
                    c = reader.read();
                }
                if (c == 'L') {
                    do {
                        c = reader.read();
                    } while (c >= 0 && c != ';');
                }
                if (c < 0) {
                    break;
                }
                c = reader.read();
            }
            if (c != ')') {
                throw new RuntimeException(
                        "Invalid method descriptor: " + type);
            }
            releaseStack(argsSize);
            c = reader.read();
            if (c < 0) {
                throw new RuntimeException(
                        "Invalid method descriptor: " + type);
            }
            switch (c) {
            case 'V':
                break;

            case 'D':
            case 'J':
                reserveStack(2);
                break;

            default:
                reserveStack(1);
            }
            return argsSize;
        } catch (IOException e) {
            /* should never happend */
            throw new Error(e.toString());
        }
    }
}
