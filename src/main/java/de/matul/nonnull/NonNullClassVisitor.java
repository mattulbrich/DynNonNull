package de.matul.nonnull;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class NonNullClassVisitor extends ClassVisitor implements Opcodes {

    public static final String NOCHECKS_ANN = "Lnonnull/NoNullnessChecks;";

    boolean noChecks;
    private String className;

    public NonNullClassVisitor(ClassWriter writer) {
        super(Opcodes.ASM7, writer);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(NOCHECKS_ANN)) {
            noChecks = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override 
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        NonNullAgent.debug("visitMethod for %s.%s", className, name);
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (noChecks) {
            return mv;
        }

        boolean isStatic = (access & ACC_STATIC) != 0;
        return new NonNullMethodVisitor(mv, isStatic, className, name, desc);
    }

}