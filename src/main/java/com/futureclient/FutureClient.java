package com.futureclient;

import com.futureclient.gui.ClickGui;
import com.futureclient.gui.FutureMainMenu;
import com.futureclient.module.ModuleManager;
import com.futureclient.net.FutureNet;
import com.futureclient.util.ClickTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

@Mod(modid = "futureclient", name = "Future Client", version = "1.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class FutureClient {
    public static ModuleManager modules;
    public static KeyBinding openGui;

    @EventHandler
    public void init(FMLInitializationEvent e) {
        modules = new ModuleManager();
        openGui = new KeyBinding("Open Future Client", Keyboard.KEY_RSHIFT, "Future Client");
        ClientRegistry.registerKeyBinding(openGui);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new NametagHandler());
        FutureNet.start();
        FMLCommonHandler.instance().bus().register(this);
        Display.setTitle("Future Client 1.0");
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) modules.tick();
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (openGui.isPressed()) Minecraft.getMinecraft().displayGuiScreen(new ClickGui());
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.buttonstate && (e.button == 0 || e.button == 1)) ClickTracker.click(e.button == 0);
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) return;
        modules.render(e.resolution);
    }

    @SubscribeEvent
    public void onGui(GuiOpenEvent e) {
        if (e.gui != null && e.gui.getClass() == GuiMainMenu.class) e.gui = new FutureMainMenu();
    }
}
