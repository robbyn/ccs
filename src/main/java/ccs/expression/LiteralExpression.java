package ccs.expression;

import ccs.runtime.Context;

public class LiteralExpression extends Expression {
    private final Object value;

    public LiteralExpression(Object value) {
        this.value = value;
    }

    @Override
    public Object evaluate(Context context) {
        return value;
    }
}
