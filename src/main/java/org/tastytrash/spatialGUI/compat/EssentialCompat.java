package org.tastytrash.spatialGUI.compat;

import com.mojang.blaze3d.platform.Window;

import java.util.Set;

public class EssentialCompat {

    private EssentialCompat() {}

    public static boolean isEssentialCaller() {
        return StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE))
                .walk(s -> s.dropWhile(f -> {
                    String name = f.getDeclaringClass().getName();
                    return name.equals(EssentialCompat.class.getName())
                            || name.equals(Window.class.getName())
                            || name.equals("org.tastytrash.spatialGUI.mixin.gui.WindowMixin");
                }).findFirst())
                .map(f -> f.getDeclaringClass().getName().startsWith("gg.essential."))
                .orElse(false);
    }
}
