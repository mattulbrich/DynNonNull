/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */
package nonnull;

import java.io.File;
import java.io.IOException;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

/* ant sniplet:

  <target name="nonnull" depends="build" description="patches the class files with r/t checks">
    <java classname="nonnull.NonNullPatch">
    <arg value="classes"/>
    <classpath>
      <pathelement location="nonnull.jar"/>
    </classpath>
    </java> 
  </target>

*/

// TODO DOC

/**
 * The Class NonNullPatch can be used to patch class files offline.
 * 
 * If a directory is provided as argument, all class files within it are patched.
 * 
 * A second argument is the target (either file or directory).
 */
public class NonNullPatch {
	
	private static void patch(@NonNull File from, @NonNull File to) throws ClassFormatException, IOException {

		String fromName = from.toString();

		if(!fromName.toLowerCase().endsWith(".class")) {
			return;
		}
		
		System.out.println("Patching " + from + " to " + to);
		
		JavaClass jclass = new ClassParser(fromName).parse();
        
        NonNullModifier mod = new NonNullModifier(jclass);
        
        jclass = mod.process();
        
        jclass.dump(to);
	}
	
	private static void recurse(@NonNull File from, @NonNull File target) throws ClassFormatException, IOException {
		assert from.isDirectory();
		
		for (File f : from.listFiles()) {
			File newTarget = new File(target, f.getName());
			if(f.isDirectory()) {
				if(!newTarget.exists())
					newTarget.mkdirs();
				recurse(f, newTarget);
			} else {
				patch(f, newTarget);
			}
		}
	}

    public static void main(String[] args) throws Exception {
    	
    	File from = null;
		File target = null;
		
		System.err.println("Patch runtime non-null checking V0.11");
    	
    	switch (args.length) {
		case 1:
			target = from = new File(args[0]);
			break;
		case 2:
			from = new File(args[0]);
			target = new File(args[1]);
			break;
		default:
			System.err
					.println("Usage: one argument to write in place, two arguments to store away");
			System.exit(0);
		}
    	
    	if(from.isDirectory()) {
    		recurse(from, target);
    	} else {
    		patch(from, target);
    	}
    	
    }

	
    
}
