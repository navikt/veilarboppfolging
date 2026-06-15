package no.nav.veilarboppfolging.test.testdriver;



import java.lang.reflect.Method;

public class ReflectionUtils {

    
    public static Method getMethod(Class<?> proxyClass, String methodName, Class<?>... args) {
        return proxyClass.getMethod(methodName,args);
    }

}
