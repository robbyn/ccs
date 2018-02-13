package cfpl.compiler;

import cfpl.compiler.Converter.Chain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.tastefuljava.classfile.CodeSegment;

public class Invoker {
    private static final Logger LOG = Logger.getLogger(Invoker.class.getName());

    private final Converter conv;
    private final Map<String,List<Function>> map = new HashMap<>();

    public static class Function {
        public final Type resultType;
        public final Type[] argTypes;
        public final Generator gen;

        public Function(Generator gen, Type resultType, Type... argTypes) {
            this.gen = gen;
            this.resultType = resultType;
            this.argTypes = argTypes;
        }
    }

    public class Call {
        public final Call link;
        private final CodeSegment code;
        private final List<Type> argTypes = new ArrayList<>();
        private final List<CodeSegment> argCode = new ArrayList<>();

        private Call(CodeSegment code, Call link) {
            this.code = code;
            this.link = link;
        }

        CodeSegment startArg() {
            if (argTypes.isEmpty()) {
                return code;
            }
            CodeSegment cs = code.newSegment();
            argCode.add(cs);
            return cs;
        }

        CodeSegment endArg(Type type) {
            argTypes.add(type);
            return code;
        }

        Type invoke(String name) {
            List<Function> list = map.get(name);
            if (list == null) {
                return null;
            }
            int argCount = argTypes.size();
            Chain[] chains = new Chain[argCount];
            floop:
            for (Function fct: list) {
                if (fct.argTypes.length == argCount) {
                    for (int i = 0; i < argCount; ++i) {
                        chains[i] = conv.getChain(argTypes.get(i), fct.argTypes[i]);
                        if (chains[i] == null) {
                            continue floop;
                        }
                    }
                    // found it
                    if (argCount > 0) {
                        // convert fist argument
                        chains[0].apply(code);
                        for (int i = 1; i < argCount; ++i) {
                            // append and convert next arguments
                            argCode.get(i-1).commit();
                            chains[i].apply(code);
                        }
                    }
                    fct.gen.generate(code);
                    return fct.resultType;
                }
            }
            return Type.INT;
        }
    }

    public Invoker(Converter conv) {
        this.conv = conv;
    }

    public void add(Generator gen, String name, Type resultType,
            Type... argTypes) {
        List<Function> list = map.get(name);
        if (list == null) {
            list = new ArrayList<>();
            map.put(name, list);
        }
        list.add(new Function(gen, resultType, argTypes));
    }

    public Call startCall(CodeSegment code, Call link) {
        return new Call(code, link);
    }
}
