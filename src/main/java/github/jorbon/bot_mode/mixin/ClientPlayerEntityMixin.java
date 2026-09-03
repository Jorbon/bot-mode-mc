package github.jorbon.bot_mode.mixin;

import com.mojang.authlib.GameProfile;

import github.jorbon.bot_mode.BotModeClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.stat.StatHandler;
import net.minecraft.util.PlayerInput;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
    
    public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) { super(world, profile); }
    
    @Inject(at = @At("RETURN"), method = "<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/network/ClientPlayNetworkHandler;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;Lnet/minecraft/util/PlayerInput;Z)V")
    public void init(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, PlayerInput lastPlayerInput, boolean lastSprinting, CallbackInfo ci) {
        // BotModeClient.bot_mode = BotModeClient.Mode.OFF;
    }
    
    @Override
    public void changeLookDirection(double cursorDeltaX, double cursorDeltaY) {
        if (BotModeClient.bot_mode == BotModeClient.Mode.OFF) {
            super.changeLookDirection(cursorDeltaX, cursorDeltaY);
        }
    }
    
}