package nonnull;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class NonNullRewrite {

    public static void main(String[] args) throws Exception {
        JavaClass jclass = new ClassParser(args[0]).parse();
        NonNullModifier mod = new NonNullModifier(jclass);
        
      //  mod.process();
        
        jclass.dump(args[1]);
    }
    
}
