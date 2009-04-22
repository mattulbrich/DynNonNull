package nonnull;

import org.apache.bcel.classfile.Utility;

public class NonNullError extends Error {
    
    private String methodSignature;
    private String className;
    private int parameter = -1;

    public NonNullError(String className, String methodSignature) {
        this.className = className;
        this.methodSignature = methodSignature;
    }

    public NonNullError(String className, String methodSignature, int parameter) {
        this.className = className;
        this.methodSignature = methodSignature;
        this.parameter = parameter;
    }
    
    private String getMethod() {
        
        int sep = methodSignature.indexOf('(');
        
        return Utility.methodSignatureToString(methodSignature.substring(sep),
                methodSignature.substring(0, sep-1), "");
    }
    
    @Override
    public String getMessage() {
        String retval = "Unallowed null reference in class " + className + ", method " + getMethod();
        if(parameter >= 0) {
            retval += " in parameter " + parameter;
        } else {
            retval += " in return statement";
        }
        
        return retval;
    }
    
}
