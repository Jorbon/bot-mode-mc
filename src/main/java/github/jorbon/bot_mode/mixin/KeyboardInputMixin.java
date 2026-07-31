package github.jorbon.bot_mode.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import github.jorbon.bot_mode.BotModeClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.Vec2f;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    
    @Inject(at = @At("HEAD"), cancellable = true, method = "tick")
    public void tick(CallbackInfo ci) {
        if (!BotModeClient.bot_mode) return;
        
        if (
            this.settings.forwardKey.isPressed() || 
            this.settings.backKey   .isPressed() || 
            this.settings.leftKey   .isPressed() || 
            this.settings.rightKey  .isPressed() || 
            this.settings.jumpKey   .isPressed() || 
            this.settings.sneakKey  .isPressed() || 
            this.settings.sprintKey .isPressed()
        ) {
            BotModeClient.bot_mode = false;
            return;
        }
        
        Vec2f vec = new Vec2f(
            getMovementMultiplier(this.playerInput.left(), this.playerInput.right()),
            getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward())
        );
        this.movementVector = (vec.x != 0.0f || vec.y != 0.0f) ? vec.normalize() : vec;
        
        ci.cancel();
    }
    
    @Shadow private final GameOptions settings;
    @Shadow private static float getMovementMultiplier(boolean positive, boolean negative) { return 0.0f; }
    
    public KeyboardInputMixin(GameOptions settings) {
        this.settings = settings;
    }
}
