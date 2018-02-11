package ccs.classfile;

public class TryBlock {
    private final CodeBuilder cb;
    private final ConstantPool cp;
    private short mark;
    private short beginPos;
    private short endPos;
    private final Label endLabel;

    public TryBlock(ConstantPool cp, CodeBuilder cb) {
        this.cp = cp;
        this.cb = cb;
        endLabel = new Label();
    }

    public void beginMain() {
        beginPos = (short)cb.getLocation();
        mark = cb.beginBlock();
    }

    public void endMain() {
        cb.endBlock(mark);
        endPos = (short)cb.getLocation();
    }

    public void beginCatch(short classIndex) {
        cb.jump(ByteCode.GOTO, endLabel);
        cb.addException(beginPos, endPos, (short)cb.getLocation(), classIndex);
        mark = cb.beginBlock();
        cb.reserveStack(1);
    }

    public void beginCatch(String className) {
        beginCatch(cp.addClass(className));
    }

    public void endCatch() {
        cb.endBlock(mark);
    }

    public void close() {
        cb.define(endLabel);
    }
}
