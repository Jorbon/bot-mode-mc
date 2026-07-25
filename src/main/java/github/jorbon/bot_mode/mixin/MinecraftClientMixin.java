package github.jorbon.bot_mode.mixin;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import github.jorbon.bot_mode.BotModeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;", opcode = Opcodes.GETFIELD, ordinal = 2), require = 1, allow = 1, expect = 1)
    public void tick(CallbackInfo ci) {
        if (!BotModeClient.bot_mode) return;
        if (this.player == null || this.world == null) {
            BotModeClient.bot_mode = false;
            return;
        }
        
        var breaking = BotModeClient.bot_mode_do();
        this.attackCooldown = 0;
        this.handleBlockBreaking(breaking);
    }
    
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void handleInputEvents(CallbackInfo ci) {
        if (!BotModeClient.bot_mode) return;
        
        if (
            this.options.attackKey  .wasPressed() ||
            this.options.useKey     .wasPressed() ||
            this.options.pickItemKey.wasPressed() ||
            this.options.dropKey    .wasPressed()
        ) {
            BotModeClient.bot_mode = false;
        }
    }
    
    @Shadow public final GameOptions options;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
    @Shadow public int attackCooldown;
    @Shadow private void handleBlockBreaking(boolean breaking) {}
    
    
    MinecraftClientMixin(GameOptions options) {
        super("Client");
        this.options = options;
    }
    
}
