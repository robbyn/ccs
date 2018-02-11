package ccs.arithmetics;

import ccs.expression.Expression;
import ccs.runtime.Context;

public abstract class Binary extends Expression {
    private final Expression sub1;
    private final Expression sub2;

    protected Binary(Expression sub1, Expression sub2) {
        this.sub1 = sub1;
        this.sub2 = sub2;
    }

    protected abstract Object apply(Object x, Object y);

    @Override
    public Object evaluate(Context context) {
        return apply(sub1.evaluate(context), sub2.evaluate(context));
    }
}
