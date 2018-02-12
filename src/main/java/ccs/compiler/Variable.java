package ccs.compiler;

public class Variable {
    public final Type type;
    public final int addr;

    Variable(Type type, int addr) {
        this.type = type;
        this.addr = addr;
    }
}
