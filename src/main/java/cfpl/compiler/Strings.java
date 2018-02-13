package cfpl.compiler;

public class Strings {
    public static String unescape(String s) {
        StringBuilder buf = new StringBuilder();
        int st = 0;
        for (char c: s.toCharArray()) {
            switch (st) {
                case 0:
                    switch (c) {
                        case '#':
                            buf.append('\n');
                            break;
                        case '[':
                            st = 1;
                            break;
                        default:
                            buf.append(c);
                            break;
                    }
                    break;
                case 1:
                    buf.append(c);
                    st = 2;
                    break;
                case 2:
                    if (c != ']') {
                        // error
                        buf.append(c);
                    }
                    st = 0;
                    break;
            }
        }
        return buf.substring(1, buf.length()-1); // remove enclosing quote marks
    }

    public static char unescapeChar(String s) {
        s = unescape(s);
        if (s.length() < 1) {
            return 0;
        } else {
            return s.charAt(0);
        }
    }
}
