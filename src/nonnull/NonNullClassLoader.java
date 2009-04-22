package nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

public class NonNullClassLoader extends ClassLoader {
    
    public NonNullClassLoader(ClassLoader parent) {
        super(parent);
    }

    private Hashtable<String, Class<?>> classes = new Hashtable<String, Class<?>>(); 
    // Hashtable is synchronized thus thread-safe
    
    protected Class<?> loadClass( String class_name, boolean resolve ) throws ClassNotFoundException {
        Class<?> cl;
        
        cl = (Class<?>) classes.get(class_name);
        
        if (cl == null) {
            URL url = getParent().getResource(class_name + ".class");

            if (url != null) {
                try {
                    ClassParser parser = new ClassParser(url.openStream(), url
                            .toString());
                    JavaClass jclass = parser.parse();
                    NonNullModifier modifier = new NonNullModifier(jclass);
                    jclass = modifier.process();
                    for(org.apache.bcel.classfile.Method m : jclass.getMethods()) {
                        System.out.println(m);
                        System.out.println(m.getCode());
                    }
                    byte[] byteArray = jclass.getBytes();
                    cl = defineClass(class_name, byteArray, 0, byteArray.length);
                } catch (Exception e) {
                    // error during this are just ignored ask parent
                    e.printStackTrace();
                }
            }
        }
        
        if(cl == null) {
            cl = getParent().loadClass(class_name); 
        }
        
        if(cl != null) {
            resolveClass(cl);
        }
        
        classes.put(class_name, cl);
        return cl; 
    }
    
    public static void main(String[] args) throws Throwable {
        NonNullClassLoader nncl = new NonNullClassLoader(ClassLoader.getSystemClassLoader());
        String className = args[0];
        
        Object args2 = new String[args.length - 1];
        System.arraycopy(args, 1, args2, 0, args.length - 1);
        
        Class<?> mainClass = nncl.loadClass(className);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        
        try {
            mainMethod.invoke(null, args2);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }


}
