package nonnull;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class NonNullAgent {

    public static void premain(final String arg, Instrumentation instr) {
        instr.addTransformer(new NonNullTransformer());
    }
}

class NonNullTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> cl, ProtectionDomain pd, byte[] data) {
        try
        {
            System.err.println("Working on: " + className);
            ClassParser parser = new ClassParser(
                    new ByteArrayInputStream(data), className + ".class");
            JavaClass jclass = parser.parse();
            NonNullModifier modifier = new NonNullModifier(jclass);
            jclass = modifier.process();
            byte[] byteArray = jclass.getBytes();
            return byteArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

