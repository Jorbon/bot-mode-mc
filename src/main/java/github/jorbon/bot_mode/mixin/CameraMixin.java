package github.jorbon.bot_mode.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
    
    @ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target =
        "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"
    ), index = 2)
    protected float setRotation(float angleY, float angleX, float angleZ) {
        return angleZ + 0.05f;
    }
}
