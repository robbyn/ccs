package ccs.conversion;

import ccs.arithmetics.Unary;
import ccs.expression.Expression;

public class IntToFloat extends Unary {
    public IntToFloat(Expression sub) {
        super(sub);
    }

    @Override
    protected Object apply(Object val) {
        return Double.valueOf((Integer)val);
    }
}
