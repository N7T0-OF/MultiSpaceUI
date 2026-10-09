package org.tastytrash.spatialGUI.mixin.gui;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("mouseGrabbed")
    void setMouseGrabbed(boolean value);

    @Accessor("mouseGrabbed")
    boolean getMouseGrabbed();

    @Accessor("ignoreFirstMove")
    boolean getIgnoreFirstMove();

    @Accessor("ignoreFirstMove")
    void setIgnoreFirstMove(boolean value);

    @Accessor("xpos")
    double getRawXpos();

    @Accessor("ypos")
    double getRawYpos();

    @Accessor("xpos")
    void setRawXpos(double value);

    @Accessor("ypos")
    void setRawYpos(double value);

    @Accessor("accumulatedDX")
    double getAccumulatedDX();

    @Accessor("accumulatedDY")
    double getAccumulatedDY();

    @Accessor("accumulatedDX")
    void setAccumulatedDX(double value);

    @Accessor("accumulatedDY")
    void setAccumulatedDY(double value);
}
