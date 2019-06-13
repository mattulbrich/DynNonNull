package de.matul.nonnull;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class NonNullMethodVisitor extends MethodVisitor implements Opcodes {

    private static final String NON_NULL_CHECKER_CLASSNAME =
            NonNullChecker.class.getName().replace('.', '/');
    private static final String CHECK_FIELD_METHODNAME =
            "checkFieldNonNull";
    private static final String CHECK_METHOD_PARAMETER_METHODNAME =
            "checkMethodParameterNonNull";
    private static final String CHECK_SIGNATURE =
            "(Ljava/lang/Object;I)V";

    private boolean noChecks;
    private final String methodName;
    private final boolean isStatic;
    private final Type[] argTypes;
    private final String methDesc;
    private final String className;

    public NonNullMethodVisitor(MethodVisitor mv, boolean isStatic, String className, String name, String desc) {
        super(Opcodes.ASM7, mv);
        this.className = className;
        this.methodName = name;
        this.isStatic = isStatic;
        this.methDesc = desc;
        this.argTypes = Type.getArgumentTypes(desc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(desc.equals(NonNullClassVisitor.NOCHECKS_ANN)) {
            NonNullAgent.debug("no nullness checks for %s.%s", className, methodName);
            noChecks = true;
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if(noChecks) {
            return;
        }

        int j = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            int sort = argTypes[i].getSort();
            if ((sort == Type.OBJECT || sort == Type.ARRAY)) {
                mv.visitIntInsn(ALOAD, j);
                int id = NonNullChecker.registerArgumentCheck(className, methodName, methDesc, i);
                mv.visitLdcInsn(id);
                mv.visitMethodInsn(INVOKESTATIC, NON_NULL_CHECKER_CLASSNAME,
                        CHECK_METHOD_PARAMETER_METHODNAME, CHECK_SIGNATURE);
            }
            j += argTypes[i].getSize();
        }
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {

        if (noChecks) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        int sort = Type.getType(desc).getSort();
        int id;

        if(sort == Type.ARRAY || sort == Type.OBJECT) {
            switch (opcode) {
//            case GETSTATIC:
//            case GETFIELD:
//                mv.visitFieldInsn(opcode, owner, name, desc);
//                id = NonNullChecker.registerGetFieldCheck(owner, name);
//                mv.visitInsn(DUP); // Duplicate the value
//                mv.visitLdcInsn(id);
//                mv.visitMethodInsn(INVOKESTATIC, NON_NULL_CHECKER_CLASSNAME,
//                        CHECK_FIELD_METHODNAME, "(Ljava/lang/Object;I)V");
//                return;

            case PUTSTATIC:
            case PUTFIELD:
                mv.visitInsn(DUP); // Duplicate the value
                id = NonNullChecker.registerPutFieldCheck(owner, name);
                mv.visitLdcInsn(id);
                mv.visitMethodInsn(INVOKESTATIC, NON_NULL_CHECKER_CLASSNAME,
                        CHECK_FIELD_METHODNAME, CHECK_SIGNATURE);
                mv.visitFieldInsn(opcode, owner, name, desc);
                return;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitInsn(int opcode) {
        if(noChecks) {
            super.visitInsn(opcode);
            return;
        }

        switch(opcode) {
        case ARETURN:
            mv.visitInsn(DUP);
            int id = NonNullChecker.registerReturnCheck(className, methodName, methDesc);
            mv.visitLdcInsn(id);
            mv.visitMethodInsn(INVOKESTATIC, NON_NULL_CHECKER_CLASSNAME,
                    CHECK_METHOD_PARAMETER_METHODNAME, CHECK_SIGNATURE);
        }
        super.visitInsn(opcode);
    }

}
