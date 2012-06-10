import java.io.File;
import java.io.IOException;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;


public class BCELChecker {

    private static final File TMP = new File("/tmp/T.class");

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        
        for (String arg : args) {
            try {
               // System.out.println(arg);
                readAndWrite(arg);
            } catch(Exception ex) {
                // e.g. generics
                ex.printStackTrace();
            }
            
            try {
                // and read again
                JavaClass jclass = new ClassParser(TMP.getAbsolutePath()).parse();
            } catch(Exception ex) {
                // now! that is a fault:
                System.err.println("FAILED on " + arg);
                throw ex;
            }
        }

    }

    private static void readAndWrite(String arg) throws ClassFormatException, IOException {
        File f = new File(arg);
        JavaClass jclass = new ClassParser(arg).parse();
        jclass.dump(TMP);
    }

}
