package ccs.arithmetics;

import ccs.expression.Expression;

public class NegInt extends Unary {
    public NegInt(Expression sub) {
        super(sub);
    }

    @Override
    protected Object apply(Object val) {
        return -(Integer)val;
    }
}
