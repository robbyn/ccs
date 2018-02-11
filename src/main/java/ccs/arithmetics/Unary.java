package ccs.arithmetics;

import ccs.expression.Expression;
import ccs.runtime.Context;

public abstract class Unary extends Expression {
    private final Expression sub;

    protected Unary(Expression sub) {
        this.sub = sub;
    }

    protected abstract Object apply(Object val);

    @Override
    public Object evaluate(Context context) {
        return apply(sub.evaluate(context));
    }
}
