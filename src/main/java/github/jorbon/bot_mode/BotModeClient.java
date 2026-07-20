package github.jorbon.bot_mode;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class BotModeClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key_run.wasPressed()) {
                BotModeClient.bot_mode = true;
            }
        });
    }
    
    public static boolean bot_mode = false;
    
    
    private static final KeyBinding.Category BOT_MODE_CATEGORY = KeyBinding.Category.create(Identifier.of("bot_mode"));
    private static KeyBinding key_run = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bot_mode.run", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, BOT_MODE_CATEGORY));
    
}
