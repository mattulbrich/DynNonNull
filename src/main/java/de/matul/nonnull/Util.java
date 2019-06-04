package de.matul.nonnull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;

public class Util {

    public static Set<String> readStringSet(InputStream is) throws IOException {
        Set<String> result = new HashSet<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line=br.readLine()) != null) {
            line = line.trim();
            if(!line.startsWith("#") && line.length() > 0) {
                result.add(line);
            }
        }
        return result;
    }

    public static byte[] drainStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while((read=is.read(buffer)) >= 0) {
            os.write(buffer, 0, read);
        }
        return os.toByteArray();
    }

    public static String getMethodSignature(String methodDesc) {
        Type argTypes[] = Type.getArgumentTypes(methodDesc);
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < argTypes.length; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(argTypes[i].getClassName());
        }
        sb.append(")");
        return sb.toString();
    }

}
