/*
 * NonNull Runtime Checking for Methods
 *
 * 2009 by Mattias Ulbrich
 *
 * published under GPL.
 */
package de.matul.nonnull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

// TODO: Auto-generated Javadoc
/**
 * The agent which is the entry point for the java-agent system.
 */
public class NonNullAgent {

    /**
     * The system can be set to verbose if the system property
     * {@code de.matul.nonnull.debug} is set to "true" for instance on the
     * invocation of java.
     */
    public static final boolean VERBOSE = Boolean.getBoolean("de.matul.nonnull.debug");

    /**
     * The debug output directory. If transformed class files are to be saved,
     * it will be done to this direcory. Can be set using the system property
     * {@code de.matul.nonnull.debugdir}.
     */
    public static String DEBUG_OUTPUT_DIR =
            System.getProperty("de.matul.nonnull.debugdir", "/tmp/nonnullDebug");

    /**
     * This is the entry point for the instrumentation. It adds an transformer
     * to the instrumentation.
     *
     * @param arg
     *            the argument passed to the agent on the commandline
     * @param instr
     *            the instrumentation to operate on.
     */
    public static void premain(String arg, Instrumentation instr) {
        if(arg == null) {
             throw new IllegalArgumentException("You need to provide a class prefix");
        }
        instr.addTransformer(new NonNullTransformer(arg));
    }

    /**
     * Print a debug message, if verbose mode is set.
     *
     * @param msg
     *            the message to print
     */
    static void debug(String msg) {
        if(VERBOSE) {
            System.err.println("[NN] " + msg);
        }
    }

    /**
     * Print a debug message with arguments.
     *
     * @param msg
     *            the message to print, formatted in
     *            {@link String#format(String, Object...)} style.
     * @param args
     *            arguments to the message
     */
    static void debug(String msg, Object... args) {
        if(VERBOSE) {
            System.err.printf("[NN] " + msg + "%n", args);
        }
    }
}

/**
 * The transformer which actually triggers the transformation.
 */
class NonNullTransformer implements ClassFileTransformer {

    /**
     * The package prefix for which classes are to be modified.
     */
    private final String prefix;

    /**
     * Instantiates a new transformer.
     *
     * @param arg
     *            the package/class prefix to be considered
     */
    public NonNullTransformer(String arg) {
        this.prefix = arg.replace('.', '/');
    }

    /*
     * Transform the class:
     * - instrument it with non-null check instructions
     * - write the class file to temp directory if debug is on
     */
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> cl, ProtectionDomain pd, byte[] data) {
        try {
            // anonymously created classes have no class name, bail out
            if(className == null) {
                return null;
            }

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
            System.err.println("Error while transforming " + className);
            e.printStackTrace();
            return null;
        }
    }

    /* For test purposes */
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
