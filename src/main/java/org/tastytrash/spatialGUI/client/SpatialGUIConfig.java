package org.tastytrash.spatialGUI.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = "spatial-gui")
public class SpatialGUIConfig implements ConfigData {
    /**
     * NEVER PUT ANYTHING BUT BOOLEANS IN COLLAPSIBLE MENUS
     * CLOTH CONFIG BREAKS THEM
     * Auto FOV Tuning is an exception because people are unlikely to touch it
     */

    // general
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonModeInventory = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean firstPersonModeContainers = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean useCrosshairForFirstPerson = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean autoDetectCameraMode = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean autoCalculateGuiScale = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 6)
    public int guiScale = 4;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean mirrorThirdPerson = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.CollapsibleObject
    public HUD hudOptions = new HUD();

    public static class HUD {
        @ConfigEntry.Category("general")
        @ConfigEntry.Gui.Tooltip
        public boolean hideHotbar = false;

        @ConfigEntry.Category("general")
        @ConfigEntry.Gui.Tooltip
        public boolean hideHud = false;
    }

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.Tooltip
    public boolean autoScaleByFov = true;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.CollapsibleObject
    public AutoFovTuning autoFovTuning = new AutoFovTuning();

    public static class AutoFovTuning {

        @ConfigEntry.Gui.Tooltip
        public double autoScaleBaselineFov = 70.0;

        @ConfigEntry.Gui.Tooltip
        public double autoScaleScreenPower = 1.2;

        @ConfigEntry.Gui.Tooltip
        public double autoScaleSideOffsetMultiplier = -0.3;

        @ConfigEntry.Gui.Tooltip
        public double autoScaleDistanceMultiplier = 0.8;

        @ConfigEntry.Gui.Tooltip
        public double autoScaleThirdPersonScreenMultiplier = 0.82;
    }

    public int calculateAutoGuiScale(int windowWidth, int windowHeight) {
        if (windowHeight <= 0 || windowWidth <= 0) return 1;
        int scale = 1;
        while (windowWidth / (scale + 1) >= 320 && windowHeight / (scale + 1) >= 240) {
            scale++;
        }
        if (scale == 1 && windowHeight >= 430 && windowWidth >= 640) {
            scale = 2;
        }
        return scale;
    }

    public int calculateAutoGuiScale(int windowHeight) {
        if (windowHeight <= 0) return 1;
        int scale = 1;
        while (windowHeight / (scale + 1) >= 240) {
            scale++;
        }
        if (scale == 1 && windowHeight >= 430) {
            scale = 2;
        }
        return scale;
    }

    public int getEffectiveGuiScale(int windowWidth, int windowHeight) {
        if (autoCalculateGuiScale) {
            return calculateAutoGuiScale(windowWidth, windowHeight);
        }
        int clampedGuiScale = Math.max(1, Math.min(guiScale, 6));
        int autoScale = calculateAutoGuiScale(windowWidth, windowHeight);
        int relativeScale = Math.round(clampedGuiScale * (autoScale / 4.0f));
        return Math.max(1, relativeScale);
    }

    public int getEffectiveGuiScale(int windowHeight) {
        if (autoCalculateGuiScale) {
            return calculateAutoGuiScale(windowHeight);
        }
        int clampedGuiScale = Math.max(1, Math.min(guiScale, 6));
        int autoScale = calculateAutoGuiScale(windowHeight);
        int relativeScale = Math.round(clampedGuiScale * (autoScale / 4.0f));
        return Math.max(1, relativeScale);
    }

    // screens
    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public boolean inventory = true;

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public boolean containers = true;

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public boolean pauseScreen = false;

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public boolean allScreens = false;

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public List<String> seenScreens = new ArrayList<>();

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public List<String> enabledScreens = defaultEnabledScreens();

    @ConfigEntry.Category("screens")
    @ConfigEntry.Gui.Tooltip
    public List<String> disabledScreens = new ArrayList<>();

    private static List<String> defaultEnabledScreens() {
        String pkg = "net.minecraft.client.gui.screens.inventory.";
        List<String> list = new ArrayList<>();
        for (String name : new String[] {
                "BookViewScreen", "BookEditScreen", "BookSignScreen",
                "CraftingScreen",
                "FurnaceScreen", "SmokerScreen", "BlastFurnaceScreen",
                "AnvilScreen", "EnchantmentScreen", "BeaconScreen", "BrewingStandScreen",
                "MerchantScreen", "ContainerScreen", "ShulkerBoxScreen", "HopperScreen",
                "DispenserScreen", "GrindstoneScreen", "SmithingScreen", "CartographyTableScreen",
                "LoomScreen", "StonecutterScreen", "LecternScreen",
                "InventoryScreen", "CreativeModeInventoryScreen"
        }) {
            list.add(pkg + name);
        }
        return list;
    }

    // rendering
    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 25, max = 100)
    public int renderScalePercent = 100;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.CollapsibleObject
    public FirstPersonHands firstPersonHands = new FirstPersonHands();

    public static class FirstPersonHands {
        @ConfigEntry.Category("rendering")
        @ConfigEntry.Gui.Tooltip
        public boolean hideHandsInFirstPerson = false;

        @ConfigEntry.Category("rendering")
        @ConfigEntry.Gui.Tooltip
        public boolean hideShieldInFirstPerson = true;

        @ConfigEntry.Category("rendering")
        @ConfigEntry.Gui.Tooltip
        public boolean swingArmOnFirstPersonClick = false;
    }

    //? if >=26.1.2 {
    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.Tooltip
    public boolean blurWorldBackground = false;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
    public int worldBlurStrength = 5;
    //?}

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 255)
    public int screenAlpha = 255;

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.CollapsibleObject
    public Filtering filtering = new Filtering();

    public static class Filtering {
        @ConfigEntry.Gui.Tooltip
        public boolean useLinearFiltering = true;

        @ConfigEntry.Gui.Tooltip
        public boolean useAnisotropicFiltering = true;
    }

    @ConfigEntry.Category("rendering")
    @ConfigEntry.Gui.Tooltip
    public double recipeBookShrinkFactor = 1.5;

    // thirdPersonScreen
    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenDistance = 2.5;

    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenSideOffset = -0.6;

    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenHeightOffset = -0.4;

    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenYawOffset = 160.0;

    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenPitchOffset = 0.0;

    @ConfigEntry.Category("thirdPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double screenScale = 3.5;

    // firstPersonScreen
    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public boolean curvedScreenEnabled = false;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 5, max = 150)
    public int curvedScreenArcDegrees = 30;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenDistance = 1.5;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenSideOffset = 0.0;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenHeightOffset = 0.0;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenYawOffset = 180.0;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenPitchOffset = 0.0;

    @ConfigEntry.Category("firstPersonScreen")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonScreenScale = 1.8;

    // thirdPersonCamera
    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double cameraDistance = 1.7;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double cameraSideOffset = -1.2;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double cameraHeightOffset = 1.5;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double cameraTargetPitch = 10.0;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double thirdPersonMouseSensitivityYaw = 0.1;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double thirdPersonMouseSensitivityPitch = 0.03;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean fallbackToFirstPersonOnBlockCollision = false;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean disableThirdPersonParallax = false;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean lerpCameraPitch = true;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double avatarBaseYawOffset = 30.0;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double avatarBodyRotationOffset = 20.0;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 5000)
    public int transitionDurationMs = 300;

    @ConfigEntry.Category("thirdPersonCamera")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int transitionSkipPercentage = 15;

    // firstPersonCamera
    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean keepFirstPersonCameraAngle = true;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean inheritScreenOriginOnSwap = true;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonMouseSensitivityYaw = 0.4;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public double firstPersonMouseSensitivityPitch = 0.12;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 90)
    public int firstPersonPitchClamp = 40;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean lockFirstPersonYaw = false;

    @ConfigEntry.Category("firstPersonCamera")
    @ConfigEntry.Gui.Tooltip
    public boolean disableFirstPersonParallax = false;

    // animations
    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    public boolean enableFadeAnimation = true;

    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    public int fadeDurationMs = 150;

    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    public boolean enableScaleAnimation = true;

    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 50, max = 1000)
    public int openAnimationDurationMs = 300;

    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public EasingType animationEasing = EasingType.Back;

    public enum EasingType {
        Cubic,
        Elastic,
        Bounce,
        Back,
        Exponential,
        Quadratic,
        Quartic
    }

    @ConfigEntry.Category("animations")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int animationStartScalePercent = 75;

    // fov
    @ConfigEntry.Category("fov")
    @ConfigEntry.Gui.Tooltip
    public boolean overrideFov = false;

    @ConfigEntry.Category("fov")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 30, max = 110)
    public int targetFov = 70;

    @ConfigEntry.Category("fov")
    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
    public int fovLerpSpeed = 10;

}
