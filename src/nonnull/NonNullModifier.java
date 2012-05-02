/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package nonnull;

import java.util.Collections;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.LocalVariableTypeTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.ParameterAnnotations;
import org.apache.bcel.classfile.StackMapTable;
import org.apache.bcel.classfile.Unknown;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.AnnotationEntryGen;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * The Class NonNullModifier does the actual patching.
 * 
 * It uses BCEL methods to:
 * <ol>
 * <li>analyse the annotations on methods and method parameters
 * <li>insert check code on method entries and return
 * </ol>
 * 
 */
public class NonNullModifier {

    /**
     * The type string describing the classname of the nonnull annotation
     */
    private static final String NON_NULL_TYPEREF = "Lnonnull/NonNull;";

    /**
     * The type string describing the classname of the nullable annotation
     */
    private static final String NULLABLE_TYPEREF = "Lnonnull/Nullable;";
    
    /**
     * The type string describing the classname of the deep nonnull annotation
     */
    private static final String DEEP_NON_NULL_TYPEREF = "Lnonnull/DeepNonNull;";
    
    /**
     * The type string describing the classname of the patched annotation
     */
    private static final ObjectType PATCHED_CLASS = new ObjectType("nonnull/NonNullPatched");
    
    /**
     * The type string describing the classname of the patched annotation
     */
    private static final String PATCHED_TYPEREF = "Lnonnull/NonNullPatched;";

    /**
     * The object type of the error that is to be created
     */
    private static final ObjectType EXCEPTION_CLASS = new ObjectType(
            "nonnull/NonNullError");

    /**
     * The parameter signature of the error constructor used for parameter
     * failures.
     */
    private static final Type[] PARAMETER_CHECKNONNULL_SIG = { Type.OBJECT,
            Type.STRING, Type.BOOLEAN };

    /**
     * The parameter signature of the error constructor used for return
     * failures.
     */
    private static final Type[] RETURN_CHECKNONNULL_SIG = { Type.OBJECT, Type.BOOLEAN };

    /**
     * When looking for an annotation there are 3 possible states:
     * <ul>
     * <li>NONNULL: NonNull annotation found
     * <li>NULLABLE: Nullable annotation found
     * <li>DEEP_NONNULL: DeepNonNull annotation found, implies NonNull
     * <li>NONE: no annotation (of this kind) found
     * </ul>
     */
    private static enum AnnotationKind {
        NONNULL, NULLABLE, DEEP_NONNULL, NONE;
    }

    /**
     * The jclass stores the BCEL information of the original class
     */
    private JavaClass jclass;

    /**
     * The class generator that is used to manipulate jclass
     */
    private ClassGen classGen;

    /**
     * is there at least one method that has been altered?
     */
    private boolean hasBeenAltered = false;

    /**
     * the annotation attached to the class
     */
    private AnnotationKind classAnnotation;

    /**
     * Instantiates a new non null modifier.
     * 
     * @param jclass
     *                the class to be patched
     */
    public NonNullModifier(@NonNull JavaClass jclass) {
        this.jclass = jclass;
    }

    /**
     * Patch the class.
     * 
     * The result is a copy of {@link #jclass} with the desired runtime checks
     * included.
     * 
     * @return a newly created JavaClass
     */
    public @NonNull JavaClass process() {
        
        if(jclass.isInterface())
            return jclass;

        classGen = new ClassGen(jclass);

        classAnnotation = getNonNullAnnotation(jclass.getAnnotationEntries());
        
        Method methods[] = jclass.getMethods();

        for (Method method : methods) {
            try {
                doMethod(method);
            } catch (RuntimeException e) {
                System.err.println("Cannot work on method " + method
                        + " (possibly generics): ");
                e.printStackTrace();
            }
        }
        
        if(hasBeenAltered()) {
            makePatchAnnotation();
        }

        return classGen.getJavaClass();
    }

    /**
     * if the class file has been patched, annotate this with an annotation to
     * the class file.
     */
    private void makePatchAnnotation() {
        AnnotationEntryGen ag = new AnnotationEntryGen(PATCHED_CLASS, 
                Collections.EMPTY_LIST, false, classGen.getConstantPool());
        classGen.addAnnotationEntry(ag);
    }

    /**
     * check the annotations of the class to find whether the class file has
     * alredy been patched. The marker annotation for this is
     * {@link #PATCHED_TYPEREF}.
     * 
     * @return true iff this class has already been patched.
     */
    public boolean alreadyPatched() {
        
        AnnotationEntry[] annotations = jclass.getAnnotationEntries();
        for (AnnotationEntry annotationEntry : annotations) {
            if(PATCHED_TYPEREF.equals(annotationEntry.getAnnotationType()))
                return true;
        }
        return false;
    }

    /*
     * For test reasons only
     */
    public static void main(String[] args) throws ClassFormatException,
            Exception {
        
        String arg = args.length == 0 ? "bin/nonnull/NonNullPatch.class" : args[0];
        
        JavaClass jclass = new ClassParser(arg).parse();

        NonNullModifier main = new NonNullModifier(jclass);

        System.out.println(System.getProperties());
        
        if(main.alreadyPatched())
            System.out.println("already patched");

        JavaClass jclass2 = main.process();
        System.out.println(jclass2);

        for (Method m : jclass2.getMethods()) {
            System.out.println(m);
            System.out.println(m.getCode());
            for (Attribute a : m.getCode().getAttributes()) {
                System.out.println(a.getClass());
            }
        }
    }

    /**
     * Process one method.
     * 
     * <ol>
     * <li>check the annotations on the method (store in nonNullReturn)
     * <li>check the annotations on the parameters (store in nonNullParams)
     * <li>if there is some annotation create a {@link MethodGen}
     * <li>add the desired checks
     * </ol>
     * 
     * @param method
     *                the method to patch
     */
    private void doMethod(@NonNull Method method) {

        int arity = method.getArgumentTypes().length;

        if (method.isAbstract())
            return;

        // annotations to the class.
        boolean deepCheckByClass = requiresElementsCheck(classAnnotation);
        boolean checkByClass = requiresCheck(classAnnotation);

        // Initialise from class annotations
        boolean returnCheck = checkByClass;
        boolean returnDeepCheck = deepCheckByClass;
        boolean paramCheck[] = fillArray(arity, checkByClass);
        boolean paramDeepCheck[] = fillArray(arity, deepCheckByClass);

//        // Initialise the arrays from class annotations
//        if(classAnnotation == AnnotationKind.NONNULL || classAnnotation == AnnotationKind.DEEP_NONNULL) {
//            returnCheck = true;
//            for (int i = 0; i < paramCheck.length; i++) {
//                paramCheck[i] = true;
//            }
//        }

        //
        // check for NonNull annotations
        for (Attribute attr : method.getAttributes()) {
            if (attr instanceof Annotations) {
                Annotations annotations = (Annotations) attr;
                AnnotationKind nonNullReturn = getNonNullAnnotation(annotations
                        .getAnnotationEntries());
                if(nonNullReturn != AnnotationKind.NONE) {
                    // override class values only if something is set!
                    returnCheck = requiresCheck(nonNullReturn);
                    returnDeepCheck = requiresElementsCheck(nonNullReturn);
                }
            }

            if (attr instanceof ParameterAnnotations) {
                ParameterAnnotations annotations = (ParameterAnnotations) attr;
                ParameterAnnotationEntry[] entries = annotations
                        .getParameterAnnotationEntries();

                for (int i = 0; i < entries.length; i++) {
                    AnnotationKind nonNullann = getNonNullAnnotation(entries[i]
                            .getAnnotationEntries());
                    if(nonNullann != AnnotationKind.NONE) {
                        paramCheck[i] = requiresCheck(nonNullann);
                        paramDeepCheck[i] = requiresElementsCheck(nonNullann);
                    }
                }
            }
        }

        boolean someCheck = returnCheck;
        for (int i = 0; i < paramCheck.length && !someCheck; i++) {
            someCheck |= paramCheck[i];
        }

        if (someCheck) {

            removeLocalVariableTypeTable(method);

            MethodGen mg = new MethodGen(method, jclass.getClassName(),
                    classGen.getConstantPool());
            InstructionFactory instFact = new InstructionFactory(classGen);

            if (returnCheck) {
                addReturnCheck(mg, instFact, returnDeepCheck);
            }

            int offset = mg.isStatic() ? 0 : 1;
            for(int i = 0; i < paramCheck.length; i++) {
                if(paramCheck[i]) {
                    addParameterCheck(mg, instFact, i, offset, paramDeepCheck[i]);
                }

                // advance the pointer by the length of the argument.
                // (long and double are 64 bit wide)
                Type argTy = mg.getArgumentType(i);
                if(argTy == Type.LONG || argTy == Type.DOUBLE) {
                    offset += 2;
                } else {
                    offset ++;
                }
            }

            NonNullPatch.out(" ... changing " + method);
            mg.setMaxLocals();
            mg.setMaxStack();
            removeStackMap(mg);
            classGen.replaceMethod(method, mg.getMethod());
            hasBeenAltered = true;
        }

    }

    /**
     * Due to a bug in BCEL, local variable type tables are not correctly parsed
     * (BCEL cannot handle generic/variable types)
     * 
     * Better remove it (unfortunately along with detail debug info).
     * 
     * We have access to the array of attributes. We invalidate the
     * corresponding attribute by setting it to an unknown attribute.
     * 
     * @param method
     *            remove table from this method.
     */
    private void removeLocalVariableTypeTable(Method method) {
        Attribute[] attributes = method.getCode().getAttributes();
        int nameIndex = classGen.getConstantPool().addUtf8("RemovedAttribute");
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] instanceof LocalVariableTypeTable) {
                attributes[i] = new Unknown(nameIndex, 0, new byte[0], 
                        classGen.getConstantPool().getConstantPool());
            }
        }
    }

    /**
     * is there at least one method that has been changed in this process?
     * @return true if at least one method was touched by {@link #process()}.
     */
    public boolean hasBeenAltered() {
        return hasBeenAltered;
    }


    /**
     * given the annotation state of an element check whether a check should be
     * inserted.
     *  
     * This is the case if either:
     * <ol>
     * <li>There is a NonNull annotation on the object
     * <li>There is a NonNullElements annotation on the object
     * <li>or there is a NonNull annotation on the class and no
     * annotation on the object
     * <li>or there is a NonNullElements annotation on the class and no
     * annotation on the object
     * </ol>
     * 
     * {@link DeepNonNull} implies {@link NonNull}.
     * @param ann
     *                the annotation to check
     * @return true if a check is to be required for the element with the given
     *         annotation
     */
    private boolean requiresCheck(AnnotationKind ann) {
        return ann == AnnotationKind.NONNULL || ann == AnnotationKind.DEEP_NONNULL;
    }
    
    /**
     * given the annotation state of an element check whether an
     * array-per-element check should be inserted.
     * 
     * This is the case if either:
     * <ol>
     * <li>There is a NonNullElements annotation on the object
     * <li>or there is a NonNullElements annotation on the class and no
     * annotation on the object
     * 
     * @param ann
     *            the annotation to check
     * @return true if a check is to be required for the element with the given
     *         annotation
     */
    private boolean requiresElementsCheck(AnnotationKind ann) {
        return ann == AnnotationKind.DEEP_NONNULL; 
    }

    /**
     * Adds a parameter check to a {@link MethodGen}.
     * 
     * The code is inserted at the beginning of the method and corresponds to
     * the sniplet
     * 
     * <pre>
     *     NonNullError.checkNonNull(parameter, parameter_number, deepChecking);
     * </pre>
     * 
     * @param mg
     *                the method to be altered
     * @param instFact
     *                the according factory to be used to create instructions
     * @param parameter
     *                the number of the parameter to report failure for
     * @param deep
     *                this parameter is passed to the check method as a literal parameter
     */
    private void addParameterCheck(@NonNull MethodGen mg, @NonNull InstructionFactory instFact,
            int parameter, int offset, boolean deep) {

        Type type = mg.getArgumentType(parameter);

        if (!isObjectType(type))
            return;

        String variableName = getNameForParamater(mg, offset);
        
        if(variableName == null) {
            // no debug name found: Use arg0, arg1, arg2 instead.
            variableName = "arg" + parameter;
        } 
        
        InstructionList ilist = new InstructionList();

        ilist.append(InstructionFactory
                .createLoad(type, offset));
        ilist.append(instFact.createConstant(variableName));
        ilist.append(instFact.createConstant(deep ? 1 : 0));
        ilist.append(instFact.createInvoke(EXCEPTION_CLASS.getClassName(),
                "checkNonNull", Type.VOID, PARAMETER_CHECKNONNULL_SIG,
                Constants.INVOKESTATIC));

        mg.getInstructionList().insert(ilist);

        // adjust line numbers
        LineNumberGen[] lineNumbers = mg.getLineNumbers();
        if(lineNumbers != null && lineNumbers.length > 0 ) {
            LineNumberGen first = lineNumbers[0];
            first.setInstruction(mg.getInstructionList().getStart());
        }
    }

    /**
     * query the constant pool for the name of a method parameter
     * 
     * @param mg
     *            the method generator under usage
     * @param parameter
     *            the number of the parameter (= local variable)
     * @return the name of the parameter
     */
    private String getNameForParamater(MethodGen mg, int parameter) {
        LocalVariableTable locVarTable = mg.getLocalVariableTable(mg.getConstantPool());
        if(locVarTable != null) {
            LocalVariable locVar = locVarTable.getLocalVariable(parameter, 0);
            if(locVar != null) {
                // System.out.println("Check for #" + locVar.getNameIndex());
                return locVar.getName();
            }
        }
        return null;
    }

    /**
     * Adds a return check to a {@link MethodGen}.
     * 
     * The instruction list is iterated and every object return statement
     * <code>return exp;</code>(areturn) is replaced corresponding to the
     * sniplet
     * 
     * <pre>
     *     NonNullError.checkNonNull(retval, deepChecking);
     * </pre>
     * 
     * @param mg
     *            the method to be altered
     * @param instFact
     *            the according factory to be used to create instructions
     * @param deep
     *            this parameter is passed to the check method as a literal
     *            parameter
     */
    private void addReturnCheck(MethodGen mg, InstructionFactory instFact, boolean deep) {

        Type type = mg.getReturnType();

        if (!isObjectType(type))
            return;
        

        InstructionHandle handle = mg.getInstructionList().getStart();
        while (handle != null) {

            Instruction instr = handle.getInstruction();
            if (instr instanceof ARETURN) {
                InstructionList ilist = new InstructionList();
                ilist.append(InstructionFactory.createDup(1));
                ilist.append(instFact.createConstant(deep ? 1 : 0));
                ilist.append(instFact.createInvoke(EXCEPTION_CLASS.getClassName(), 
                        "checkNonNull", Type.VOID, RETURN_CHECKNONNULL_SIG,
                        Constants.INVOKESTATIC));

                mg.getInstructionList().insert(handle, ilist);
            }

            handle = handle.getNext();

        }
    }

    /**
     * remove any stack map attribute if present.
     * 
     * It is probably been corrupted by our changes.
     * 
     * @param mg
     */
    private void removeStackMap(@NonNull MethodGen mg) {
        for (Attribute attr : mg.getCodeAttributes()) {
            if (attr instanceof StackMapTable) {
                mg.removeCodeAttribute(attr);
            }
        }
    }

    /**
     * Checks if a type is an object type. This is the case iff it is not a
     * primitive type or void.
     * 
     * @param type
     *            the type
     * 
     * @return true iff is object type
     */
    private boolean isObjectType(@NonNull Type type) {

        switch (type.getType()) {
        case Constants.T_BOOLEAN:
        case Constants.T_BYTE:
        case Constants.T_CHAR:
        case Constants.T_FLOAT:
        case Constants.T_DOUBLE:
        case Constants.T_SHORT:
        case Constants.T_INT:
        case Constants.T_LONG:
        case Constants.T_VOID:
            return false;
        default:
            return true;
        }

    }

    /**
     * Checks for NonNull annotation.
     * 
     * @param entries
     *            the entries
     * 
     * @return true, if successful
     */
    private AnnotationKind getNonNullAnnotation(@NonNull AnnotationEntry[] entries) {
        for (AnnotationEntry ann : entries) {
            if (NON_NULL_TYPEREF.equals(ann.getAnnotationType()))
                return AnnotationKind.NONNULL;
            else if (NULLABLE_TYPEREF.equals(ann.getAnnotationType()))
                return AnnotationKind.NULLABLE;
            else if (DEEP_NON_NULL_TYPEREF.equals(ann.getAnnotationType()))
                return AnnotationKind.DEEP_NONNULL;
        }
        return AnnotationKind.NONE;
    }

    /**
     * create a array filled with one value
     * 
     * @param size
     *            size of the resulting array
     * @param value
     *            the value to set for all elements in the array
     * @return a freshly created array
     */
    private static boolean[] fillArray(int size, boolean value) {
        boolean[] result = new boolean[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = value;
        }
        return result;
    }

}
