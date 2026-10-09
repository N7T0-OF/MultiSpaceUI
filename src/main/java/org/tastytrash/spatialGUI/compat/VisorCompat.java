package org.tastytrash.spatialGUI.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class VisorCompat{

    private static boolean initialized = false;
    private static boolean available = false;
    private static Method getMethod;
    private static Method isActiveMethod;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> VisorStateClass = Class.forName("org.vmstudio.visor.core.client.VisorState");
            getMethod = VisorStateClass.getMethod("get");

            Class<?> VRStateModeClass = Class.forName("org.vmstudio.visor.api.client.VRStateMode");
            isActiveMethod = VRStateModeClass.getMethod("isActive");
            available = true;
        }
        catch (Throwable t) {
            available = false;
        }
    }

    public static boolean isActive(){
        if(!initialized){
            init();
        }
        if(available){
            try {
                Object stateInstance = getMethod.invoke(null);
                return (boolean) isActiveMethod.invoke(stateInstance);
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

}