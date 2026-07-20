package github.jorbon.bot_mode.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.Hand;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void handleInputEvents(CallbackInfo ci) {
        
        
        this.options.attackKey
        this.options.useKey
        this.options.pickItemKey
        this.options.dropKey
        
    
        while (this.options.dropKey.wasPressed()) {
			if (!this.player.isSpectator() && this.player.dropSelectedItem(drop_entire_stack)) {
				this.player.swingHand(Hand.MAIN_HAND);
			}
		}
        
        
        boolean bl3 = false;
		if (this.player.isUsingItem()) {
			if (!this.options.useKey.isPressed()) {
				this.interactionManager.stopUsingItem(this.player);
			}

			while (this.options.attackKey.wasPressed()) {
			}

			while (this.options.useKey.wasPressed()) {
			}
		} else {
			while (this.options.attackKey.wasPressed()) {
				bl3 |= this.doAttack();
			}

			while (this.options.useKey.wasPressed()) {
				this.doItemUse();
			}
		}

		if (this.options.useKey.isPressed() && this.itemUseCooldown == 0 && !this.player.isUsingItem()) {
			this.doItemUse();
		}

		this.handleBlockBreaking(this.currentScreen == null && !bl3 && this.options.attackKey.isPressed() && this.mouse.isCursorLocked());
        
    }
    
    
    @Shadow public final GameOptions options;
    @Shadow public final Mouse mouse;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public Screen currentScreen;
    @Shadow public int attackCooldown;
    @Shadow private int itemUseCooldown;
    
    @Shadow private boolean doAttack() { return false; }
    @Shadow private void doItemUse() {}
    @Shadow private void handleBlockBreaking(boolean breaking) {}
    
    
    MinecraftClientMixin(GameOptions options, Mouse mouse) {
        this.options = options;
        this.mouse = mouse;
    }
    
}
