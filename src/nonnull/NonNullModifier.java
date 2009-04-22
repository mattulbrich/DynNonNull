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

public class NonNullModifier {
	
	private static final String NON_NULL_TYPEREF = "Lnonnull/NonNull;";
    private static final ObjectType EXCEPTION_CLASS = new ObjectType("nonnull/NonNullError");
	private static final Type[] PARAMETER_INIT_SIG = { Type.STRING, Type.STRING, Type.INT };
	private static final Type[] RETURN_INIT_SIG = { Type.STRING, Type.STRING};
	private JavaClass jclass;
	private ClassGen classGen;

	public NonNullModifier(JavaClass jclass) {
		this.jclass = jclass;
	}
	
	
	public JavaClass process() {
		
		classGen = new ClassGen(jclass);
		
		Method methods[] = jclass.getMethods();
		
		for (Method method : methods) {
			doMethod(method);
		}
		
		return classGen.getJavaClass();
	}

	/**
	 * @param args
	 * @throws Exception 
	 * @throws ClassFormatException 
	 */
	public static void main(String[] args) throws ClassFormatException, Exception {
		JavaClass jclass = new ClassParser("bin/TestOnly.class").parse();
		
		NonNullModifier main = new NonNullModifier(jclass);
		
        System.out.println(System.getProperties());
		
		JavaClass jclass2 = main.process();
		System.out.println(jclass2);		
		
		for(Method m : jclass2.getMethods()) {
			System.out.println(m);
			System.out.println(m.getCode());
		}
	}
	
	public void doMethod(Method method) {
		
		boolean methodNNReturn = false;
		boolean methodNNParam[] = new boolean[0];
		boolean someNN = false;
		
		//
		// check for NonNull annotations
		for (Attribute attr : method.getAttributes()) {
			if (attr instanceof Annotations) {
				Annotations annotations = (Annotations) attr;
				methodNNReturn = hasNonNullAnnotation(annotations.getAnnotationEntries());
				someNN |= methodNNReturn;
			}
			
			if (attr instanceof ParameterAnnotations) {
				ParameterAnnotations annotations = (ParameterAnnotations) attr;
				ParameterAnnotationEntry[] entries = annotations.getParameterAnnotationEntries();
				
				methodNNParam = new boolean[entries.length];
				for (int i = 0; i < methodNNParam.length; i++) {
					boolean nonNullann = hasNonNullAnnotation(entries[i].getAnnotationEntries());
					methodNNParam[i] = nonNullann;
					someNN |= nonNullann;
				}
			}
		}
		
		if(someNN) {
			
			//
			// copy body to new method
			
			MethodGen mg = new MethodGen(method, jclass.getClassName(), classGen.getConstantPool());
			InstructionFactory instFact = new InstructionFactory(classGen);
			
			if(methodNNReturn) {
			    addReturnCheck(mg, instFact);
			}
			
			for (int i = methodNNParam.length - 1;  i >= 0; i--) {
			    if(methodNNParam[i]) {
			        addParameterCheck(mg, instFact, i);
			    }
            }
			
			mg.setMaxLocals();
			mg.setMaxStack();
			classGen.replaceMethod(method, mg.getMethod());
			
		} 
		
	}
	
	private void addParameterCheck(MethodGen mg, InstructionFactory instFact, int parameter) {
	    
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

	private boolean isObjectType(Type type) {
		
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


	private boolean hasNonNullAnnotation(AnnotationEntry[] entries) {
		for (AnnotationEntry ann : entries) 	{
			if(NON_NULL_TYPEREF.equals(ann.getAnnotationType()))
				return true;
		}
		return false;
	}

}
