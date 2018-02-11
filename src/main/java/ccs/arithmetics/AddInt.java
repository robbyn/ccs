package ccs.arithmetics;

import ccs.expression.Expression;

public class AddInt extends Binary {

    public AddInt(Expression sub1, Expression sub2) {
        super(sub1, sub2);
    }

    @Override
    protected Object apply(Object x, Object y) {
        return (Integer)x + (Integer)y;
    }    
}
