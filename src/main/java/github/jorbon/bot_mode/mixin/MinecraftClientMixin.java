package github.jorbon.bot_mode.mixin;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import github.jorbon.bot_mode.BotModeClient;
import github.jorbon.bot_mode.BotModeClient.InteractState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    
    private boolean was_holding = false;
    
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;", opcode = Opcodes.GETFIELD, ordinal = 2), require = 1, allow = 1, expect = 1)
    public void tick(CallbackInfo ci) {
        if (BotModeClient.bot_mode == BotModeClient.Mode.OFF) return;
        if (this.player == null || this.world == null) {
            BotModeClient.bot_mode = BotModeClient.Mode.OFF;
            return;
        }
        
        InteractState state = BotModeClient.bot_mode_do_before_interact();
        var breaking = state.breaking;
        
        if (this.player.isUsingItem()) {
            if (!state.holding && !state.using) {
				this.interactionManager.stopUsingItem(this.player);
			}
        } else {
            if ((state.holding && !this.was_holding) || state.using) {
				this.doItemUse();
			}
        }
        
        if ((state.holding || state.using) && this.itemUseCooldown == 0 && !this.player.isUsingItem()) {
			this.doItemUse();
		}
        
        this.was_holding = state.holding;
        
        
        if (state.attacking && !this.player.isUsingItem()) {
            if (this.doAttack()) {
                breaking = false;
            }
        }
        
        this.attackCooldown = 0;
        this.handleBlockBreaking(breaking);
        
        BotModeClient.bot_mode_do_after_interact();
    }
    
    @Inject(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0, shift = At.Shift.BEFORE), require = 1, allow = 1, expect = 1, cancellable = true)
    private void handleInputEvents(CallbackInfo ci) {
        if (BotModeClient.bot_mode == BotModeClient.Mode.OFF) return;
        
        if (
            this.options.attackKey  .wasPressed() ||
            this.options.useKey     .wasPressed() ||
            this.options.pickItemKey.wasPressed() ||
            this.options.dropKey    .wasPressed()
        ) {
            BotModeClient.bot_mode = BotModeClient.Mode.OFF;
        }
        
        ci.cancel();
    }
    
    @Shadow public final GameOptions options;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow public int attackCooldown;
    @Shadow private int itemUseCooldown;
    @Shadow private void handleBlockBreaking(boolean breaking) {}
    @Shadow private boolean doAttack() { return false; }
    @Shadow private void doItemUse() {}
    
    
    MinecraftClientMixin(GameOptions options) {
        super("Client");
        this.options = options;
    }
    
}
