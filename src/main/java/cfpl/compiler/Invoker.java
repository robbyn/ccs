package cfpl.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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

    public class FunctionCall {
        private final List<Type> argTypes = new ArrayList<>();
        private final List<CodeSegment> argCode = new ArrayList<>();

        private FunctionCall() {
        }

        void addArg(Type type, CodeSegment code) {
            argTypes.add(type);
            if (argTypes.size() > 1) {
                argCode.add(code);
            }
        }

        Type invoke(CodeSegment code, String name) {
            Type[] types = argTypes.toArray(new Type[argTypes.size()]);
            Function fct = find(name, types);
            if (fct == null) {
                LOG.log(Level.SEVERE, "Function {0} not found for arguments", name);
                return Type.INT;
            }
            /*TODO */
            return fct.resultType;
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

    public Function find(String name, Type... argTypes) {
        List<Function> list = map.get(name);
        if (list == null) {
            return null;
        }
        int argCount = argTypes.length;
        floop:
        for (Function fct: list) {
            if (fct.argTypes.length == argCount) {
                for (int i = 0; i < argCount; ++i) {
                    if (conv.getChain(argTypes[i], fct.argTypes[i]) == null) {
                        continue floop;
                    }
                }
                return fct;
            }
        }
        return null;
    }
}
