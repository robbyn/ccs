package ccs.compiler;

import ccs.expression.VariableExpression;

public class Variable {
    public final Type type;
    public final int addr;

    Variable(Type type, int addr) {
        this.type = type;
        this.addr = addr;
    }

    public TypedExpression asExpression() {
        return new TypedExpression(type, new VariableExpression(addr));
    }
}
