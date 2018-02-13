package ccs.compiler;

import org.tastefuljava.classfile.CodeSegment;

public interface Generator {
    public void generate(CodeSegment code);
}
