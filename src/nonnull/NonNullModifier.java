/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package nonnull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Annotations;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.ParameterAnnotationEntry;
import org.apache.bcel.classfile.ParameterAnnotations;
import org.apache.bcel.classfile.StackMapTable;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
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
	 * The object type of the error that is to be created
	 */
    private static final ObjectType EXCEPTION_CLASS = new ObjectType("nonnull/NonNullError");
	
	/**
	 * The parameter signature of the error constructor used for parameter
	 * failures.
	 */
	private static final Type[] PARAMETER_INIT_SIG = { Type.STRING, Type.STRING, Type.INT };
	
	/**
	 * The parameter signature of the error constructor used for return
	 * failures.
	 */
	private static final Type[] RETURN_INIT_SIG = { Type.STRING, Type.STRING};
	
	private static enum AnnotationKind {
	    NONNULL, NULLABLE, NONE;
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
	 * 
	 */
	private boolean hasBeenAltered = false;

    private AnnotationKind classAnnotation;

	/**
	 * Instantiates a new non null modifier.
	 * 
	 * @param jclass
	 *            the class to be patched
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
		
		classGen = new ClassGen(jclass);
		
		classAnnotation = getNonNullAnnotation(jclass.getAnnotationEntries());
		System.out.println(classAnnotation);
		Method methods[] = jclass.getMethods();
		
		for (Method method : methods) {
			doMethod(method);
		}
		
		return classGen.getJavaClass();
	}

	/*
	 * For test reasons only
	 */
	public static void main(String[] args) throws ClassFormatException, Exception {
		JavaClass jclass = new ClassParser("bin/nonnull/NonNullPatch.class").parse();
		
		NonNullModifier main = new NonNullModifier(jclass);
		
        System.out.println(System.getProperties());
		
		JavaClass jclass2 = main.process();
		System.out.println(jclass2);		
		
		for(Method m : jclass2.getMethods()) {
			System.out.println(m);
			System.out.println(m.getCode());
			for(Attribute a : m.getCode().getAttributes()) {
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
	 *            the method to patch
	 */
	private void doMethod(@NonNull Method method) {
		
		AnnotationKind nonNullReturn = AnnotationKind.NONE;
		AnnotationKind nonNullParam[] = new AnnotationKind[method.getArgumentTypes().length];
		for (int i = 0; i < nonNullParam.length; i++) {
            nonNullParam[i] = AnnotationKind.NONE;
        }
		
		//
		// check for NonNull annotations
		for (Attribute attr : method.getAttributes()) {
			if (attr instanceof Annotations) {
				Annotations annotations = (Annotations) attr;
				nonNullReturn = getNonNullAnnotation(annotations.getAnnotationEntries());
			}
			
			if (attr instanceof ParameterAnnotations) {
				ParameterAnnotations annotations = (ParameterAnnotations) attr;
				ParameterAnnotationEntry[] entries = annotations.getParameterAnnotationEntries();
				
				for (int i = 0; i < entries.length; i++) {
					AnnotationKind nonNullann = getNonNullAnnotation(entries[i].getAnnotationEntries());
					nonNullParam[i] = nonNullann;
				}
			}
		}
		

		boolean changed = false;

		MethodGen mg = new MethodGen(method, jclass.getClassName(), classGen.getConstantPool());
		InstructionFactory instFact = new InstructionFactory(classGen);

		if(requiresCheck(nonNullReturn)) {
		    addReturnCheck(mg, instFact);
		    changed = true;
		}

		for (int i = nonNullParam.length - 1;  i >= 0; i--) {
		    if(requiresCheck(nonNullParam[i])) {
		        addParameterCheck(mg, instFact, i);
		    }
		    changed = true;
		}

		if(changed) {
		    mg.setMaxLocals();
		    mg.setMaxStack();
		    removeStackMap(mg);
		    classGen.replaceMethod(method, mg.getMethod());
		    hasBeenAltered = true;
		}
			
		} 
		
	private boolean requiresCheck(AnnotationKind ann) {
        return ann == AnnotationKind.NONNULL || 
          (classAnnotation == AnnotationKind.NONNULL && ann != AnnotationKind.NULLABLE);
    }
	

	/**
	 * Adds a parameter check to a {@link MethodGen}.
	 * 
	 * The code is inserted at the beginning of the method and corresponds to
	 * the sniplet
	 * 
	 * <pre>
	 * if (arg_n == null) {
	 * 	throw new EXCEPTION_CLASS(&quot;className&quot;, &quot;methodSignature&quot;, parameter_number);
	 * }
	 * </pre>
	 * 
	 * @param mg
	 *            the method to be altered
	 * @param instFact
	 *            the according factory to be used to create instructions
	 * @param parameter
	 *            the number of the parameter to report failure for
	 */
	private void addParameterCheck(@NonNull MethodGen mg, @NonNull InstructionFactory instFact, int parameter) {
	    
	    Type type = mg.getArgumentType(parameter);
	    
        if(!isObjectType(type))
            return;    
	    
	    int thisOffset = mg.isStatic() ? 0 : 1;
	    
	    InstructionList ilist = new InstructionList();
	    InstructionHandle followingInstrHandle = mg.getInstructionList()
                .getStart();

        ilist.append(InstructionFactory
                .createLoad(type, parameter + thisOffset));
        ilist.append(InstructionFactory.createBranchInstruction(
                Constants.IFNONNULL, followingInstrHandle));
        ilist.append(instFact.createNew(EXCEPTION_CLASS));
        ilist.append(InstructionFactory.createDup(1));
        ilist.append(instFact.createConstant(jclass.getClassName()));
        ilist.append(instFact.createConstant(mg.getName() + mg.getSignature()));
        ilist.append(instFact.createConstant(parameter));
        ilist.append(instFact.createInvoke(EXCEPTION_CLASS.getClassName(),
                "<init>", Type.VOID, PARAMETER_INIT_SIG,
                Constants.INVOKESPECIAL));
        ilist.append(new ATHROW());

        mg.getInstructionList().insert(ilist);
	}

	/**
	 * Adds a return check to a {@link MethodGen}.
	 * 
	 * The instruction list is iterated and every object return statement
	 * <code>return exp;</code>(areturn) is replaced corresponding to the
	 * sniplet
	 * 
	 * <pre>
	 * {
	 *   [ReturnType] o = [exp];
	 *   if (o == null) {
	 * 	   throw new EXCEPTION_CLASS(&quot;className&quot;, &quot;methodSignature&quot;);
	 *   }
	 *   return o;
	 * }
	 * </pre>
	 * 
	 * @param mg
	 *            the method to be altered
	 * @param instFact
	 *            the according factory to be used to create instructions
	 */
	private void addReturnCheck(MethodGen mg, InstructionFactory instFact) {

	    Type type = mg.getReturnType();
	    
	    if(!isObjectType(type))
	        return;

	    InstructionHandle handle = mg.getInstructionList().getStart();
	    while(handle != null) {
	        
	        Instruction instr = handle.getInstruction();
	        if (instr instanceof ARETURN) {
	            InstructionList ilist = new InstructionList();
	            ilist.append(InstructionFactory.createDup(1));
                ilist.append(InstructionFactory.createBranchInstruction(
                        Constants.IFNONNULL, handle));
                ilist.append(instFact.createNew(EXCEPTION_CLASS));
                ilist.append(InstructionFactory.createDup(1));
                ilist.append(instFact.createConstant(jclass.getClassName()));
                ilist.append(instFact.createConstant(mg.getName()
                        + mg.getSignature()));
                ilist.append(instFact.createInvoke(EXCEPTION_CLASS
                        .getClassName(), "<init>", Type.VOID,
                        RETURN_INIT_SIG, Constants.INVOKESPECIAL));
                ilist.append(new ATHROW());
	            mg.getInstructionList().insert(handle, ilist);
            }
	        
	        handle = handle.getNext();
	        
	    }
	}
	
	/**
	 * remove any stack map attribute if present.
	 * 
	 * It is probably been corrupted by our changes.
	 * @param mg
	 */
	private void removeStackMap(@NonNull MethodGen mg) {
		for(Attribute attr : mg.getCodeAttributes()) {
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
		for (AnnotationEntry ann : entries) 	{
			if(NON_NULL_TYPEREF.equals(ann.getAnnotationType()))
				return AnnotationKind.NONNULL;
			else if(NULLABLE_TYPEREF.equals(ann.getAnnotationType()))
			    return AnnotationKind.NULLABLE;
		}
		return AnnotationKind.NONE;
	}

}
