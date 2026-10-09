package org.tastytrash.spatialGUI.mixin.client;

//? if >=26.1.2 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
    @Shadow @Final private RecipeBookComponent<?> recipeBookComponent;

    @Inject(method = "init", at = @At("TAIL"))
    private void spatialGUI$syncRecipeBookOnInit(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void spatialGUI$updateIsRecipeBookOpen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void spatialGUI$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
        if (!this.recipeBookComponent.isVisible()) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object) this;
            screen.clearFocus();
            var focused = screen.getFocused();
            if (focused != null) {
                focused.setFocused(false);
                screen.setFocused(null);
            }
        }
    }
}
//?} else if >1.21.1 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

@Mixin(AbstractRecipeBookScreen.class)
public class AbstractRecipeBookScreenMixin {
    @Shadow @Final private RecipeBookComponent<?> recipeBookComponent;

    @Inject(method = "init", at = @At("TAIL"))
    private void spatialGUI$syncRecipeBookOnInit(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$updateIsRecipeBookOpen(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void spatialGUI$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            renderer.getInventoryRenderer().setRecipeBookOpen(this.recipeBookComponent.isVisible());
        }
        if (!this.recipeBookComponent.isVisible()) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object) this;
            screen.clearFocus();
            var focused = screen.getFocused();
            if (focused != null) {
                focused.setFocused(false);
                screen.setFocused(null);
            }
        }
    }
}
*///?} else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.mixin.gui.ScreenInvoker;

@Mixin({InventoryScreen.class, CraftingScreen.class, AbstractFurnaceScreen.class})
public class AbstractRecipeBookScreenMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void spatialGUI$syncRecipeBookOnInit(CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            RecipeUpdateListener listener = (RecipeUpdateListener) (Object) this;
            renderer.getInventoryRenderer().setRecipeBookOpen(listener.getRecipeBookComponent().isVisible());
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void spatialGUI$updateIsRecipeBookOpen(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            RecipeUpdateListener listener = (RecipeUpdateListener) (Object) this;
            renderer.getInventoryRenderer().setRecipeBookOpen(listener.getRecipeBookComponent().isVisible());
        }
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"))
    private void spatialGUI$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        var renderer = SpatialGUIClient.renderer();
        if (renderer != null) {
            RecipeUpdateListener listener = (RecipeUpdateListener) (Object) this;
            boolean visible = listener.getRecipeBookComponent().isVisible();
            renderer.getInventoryRenderer().setRecipeBookOpen(visible);
            if (!visible) {
                AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object) this;
                //? if >1.20.1 {
                screen.clearFocus();
                //?} else {
                /^((ScreenInvoker) screen).invokeClearFocus();
                ^///?}
                var focused = screen.getFocused();
                if (focused != null) {
                    focused.setFocused(false);
                    screen.setFocused(null);
                }
            }
        }
    }
}
*///?}