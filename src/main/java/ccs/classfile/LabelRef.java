package ccs.classfile;

public abstract class LabelRef {
    public abstract void fixup(CodeBuilder cb, int location);
}
