package ccs.compiler;

import ccs.arithmetics.NegFloat;
import ccs.arithmetics.NegInt;
import ccs.conversion.CharToInt;

public class Arithmetics {
    private static TypedExpression neg(TypedExpression e) {
        switch (e.type) {
            case CHAR:
                e = new TypedExpression(Type.INT, new CharToInt(e.expression));
            case INT:
                return new TypedExpression(Type.INT, new NegInt(e.expression));
            case FLOAT:
                return new TypedExpression(Type.FLOAT,
                        new NegFloat(e.expression));
            default:
                // Error
                return e;
        }
    }
}
