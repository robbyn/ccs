package ccs.conversion;

import ccs.arithmetics.Unary;
import ccs.expression.Expression;

public class CharToInt extends Unary {
    public CharToInt(Expression sub) {
        super(sub);
    }

    @Override
    protected Object apply(Object val) {
        return Integer.valueOf((Character)val);
    }
}
