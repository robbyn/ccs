package ccs.compiler;

import ccs.expression.Expression;

public class TypedExpression {
    public final Type type;
    public final Expression expression;

    public TypedExpression(Type type, Expression expression) {
        this.type = type;
        this.expression = expression;
    }
}
