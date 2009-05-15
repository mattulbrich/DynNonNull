/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */
package nonnull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

//TODO DOC

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

        System.out.print("Patching " + from + " to ");
        if(from.equals(to))
            System.out.println("itself");
        else
            System.out.println(to);

        JavaClass jclass = new ClassParser(fromName).parse();

        NonNullModifier mod = new NonNullModifier(jclass);
        
        if(mod.alreadyPatched()) {
            System.out.println("   ... already patched");
            copyFile(from, to);
            return;
        }

        JavaClass modifiedJclass = mod.process();

        if(mod.hasBeenAltered())
            modifiedJclass.dump(to);
        else {
            System.out.println("   ... no changes");
            copyFile(from, to);
        }
    }

    private static void copyFile(File from, File to) throws IOException {
        if(from.equals(to))
            return;
        
        byte[] buffer = new byte[4096];
        InputStream is = new FileInputStream(from);
        OutputStream os = new FileOutputStream(to);
        int read = is.read(buffer);
        while(read > 0) {
            os.write(buffer, 0 , read);
            read = is.read(buffer);
        }
        is.close();
        os.close();
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
