package ccs.arithmetics;

import ccs.expression.Expression;

public class NegFloat extends Unary {
    public NegFloat(Expression sub) {
        super(sub);
    }

    @Override
    protected Object apply(Object val) {
        return -(Double)val;
    }
}
