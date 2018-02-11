package ccs.expression;

import ccs.runtime.Context;

public class VariableExpression extends Expression {
    private final int addr;

    public VariableExpression(int addr) {
        this.addr = addr;
    }

    @Override
    public Object evaluate(Context context) {
        return context.getVar(addr);
    }
}
