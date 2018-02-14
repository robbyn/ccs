package cfpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import cfpl.parser.ParseException;
import cfpl.parser.Parser;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        File file = new File(args[0]);
        try (InputStream in = new FileInputStream(file)) {
            Parser parser = new Parser(in, "UTF-8");
            parser.parse();
            parser.writeTo(changeExtension(file, ".cfpl", ".class"));
        } catch (IOException | ParseException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static File changeExtension(File file, String from, String to) {
        File dir = file.getParentFile();
        String name = file.getName();
        if (name.endsWith(from)) {
            name = name.substring(0, name.length()-from.length());
        }
        return new File(dir, name + to);
    }
}
