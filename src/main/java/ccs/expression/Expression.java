package ccs.expression;

import ccs.runtime.Context;

public abstract class Expression {
    public abstract Object evaluate(Context context);
}
