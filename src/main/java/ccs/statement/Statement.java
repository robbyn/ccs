package ccs.statement;

import ccs.runtime.Context;

public abstract class Statement {
    abstract void execute(Context cxt);
}
