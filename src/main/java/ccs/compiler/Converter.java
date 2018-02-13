package ccs.compiler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.tastefuljava.classfile.CodeSegment;

public class Converter {
    private static final Generator NULL_GENERATOR = (code)->{};

    private final Map<Type,Map<Type,Generator>> map = new EnumMap<>(Type.class);

    public static class Chain {
        private final Type type;
        private final Generator gen;
        private final Chain link;

        private Chain(Type type, Generator gen, Chain link) {
            this.type = type;
            this.gen = gen;
            this.link = link;
        }

        public void apply(CodeSegment code) {
            for (Chain p = this; p != null; p = p.link) {
                p.gen.generate(code);
            }
        }
    }

    public Converter() {
    }

    public Converter(Converter other) {
        for (Map.Entry<Type,Map<Type,Generator>> e: other.map.entrySet()) {
            Type to = e.getKey();
            for (Map.Entry<Type,Generator> e2: e.getValue().entrySet()) {
                Type from = e2.getKey();
                Generator gen = e2.getValue();
                add(from, to, gen);
            }
        }
    }

    public void add(Type from, Type to, Generator gen) {
        Map<Type,Generator> m = map.get(to);
        if (m == null) {
            m = new EnumMap<>(Type.class);
            map.put(to, m);
        }
        m.put(from, gen);
    }

    public Chain getChain(Type from, Type to) {
        List<Chain> list = new ArrayList<>();
        list.add(new Chain(to, NULL_GENERATOR, null));
        for (int i = 0; i < list.size(); ++i) {
            Chain c = list.get(i);
            if (c.type == from) {
                return c;
            }
            Map<Type,Generator> m = map.get(c.type);
            if (m != null) {
                for (Map.Entry<Type,Generator> e: m.entrySet()) {
                    Type f = e.getKey();
                    if (!containsType(list, f)) {
                        list.add(new Chain(f, e.getValue(), c));
                    }
                }
            }
        }
        return null;
    }

    private boolean containsType(Iterable<Chain> list, Type type) {
        for (Chain c: list) {
            if (c.type == type) {
                return true;
            }
        }
        return false;
    }
}
