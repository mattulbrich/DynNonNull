package de.matul.nonnull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class NonNullAgent {
    public static final boolean VERBOSE = Boolean.getBoolean("de.matul.nonnull.debug");

    public static String DEBUG_OUTPUT_DIR = "/tmp/nonnullDebug";

    public static void premain(String arg, Instrumentation instr) {
        if(arg == null) {
            throw new IllegalArgumentException("You need to provide a class prefix");
        }
        instr.addTransformer(new NonNullTransformer(arg));
    }

    static void debug(String msg) {
        if(VERBOSE) {
            System.err.println("[NN] " + msg);
        }
    }

    static void debug(String msg, Object p1) {
        if(VERBOSE) {
            System.err.printf("[NN] " + msg + "%n", p1);
        }
    }

    static void debug(String msg, Object p1, Object p2) {
        if(VERBOSE) {
            System.err.printf("[NN] " + msg + "%n", p1, p2);
        }
    }

}

class NonNullTransformer implements ClassFileTransformer {

    private final String prefix;

    public NonNullTransformer(String arg) {
        this.prefix = arg.replace('.', '/');
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> cl, ProtectionDomain pd, byte[] data) {
        try {
            if(!className.startsWith(prefix)) {
                return null;
            }

            NonNullAgent.debug("Instrumenting class: %s", className);
            NonNullAgent.debug("Existing class: %s", cl);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            NonNullClassVisitor inspector = new NonNullClassVisitor(writer);
            ClassReader reader = new ClassReader(data);
            reader.accept(inspector, 0);
            byte[] result = writer.toByteArray();

            if(NonNullAgent.VERBOSE) {
                File file = new File(NonNullAgent.DEBUG_OUTPUT_DIR, className + ".class");
                file.getParentFile().mkdirs();
                FileOutputStream fw = new FileOutputStream(
                        file);
                fw.write(result);
                fw.close();
                NonNullAgent.debug("Successfully finished instrumenting " + className);
            }

            if(inspector.noChecks) {
                NonNullAgent.debug("Leaving data untouched for " + className);
                return null;
            }

            return result;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        NonNullTransformer tr = new NonNullTransformer("");
        String className = args[0];
        Class<?> clazz = Class.forName(className);
        InputStream is = NonNullTransformer.class.
                getResourceAsStream("/" + className.replace('.', '/') + ".class");
        byte[] code = Util.drainStream(is);
        tr.transform(NonNullTransformer.class.getClassLoader(), className,
                clazz, null, code);
    }
}
