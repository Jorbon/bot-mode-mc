package github.jorbon.bot_mode;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public class BotModeClient implements ClientModInitializer {
    
    public static boolean bot_mode = false;
    
    private static final KeyBinding.Category BOT_MODE_CATEGORY = KeyBinding.Category.create(Identifier.of("bot_mode"));
    private static KeyBinding key_run = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bot_mode.run", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, BOT_MODE_CATEGORY));
    
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key_run.wasPressed()) {
                bot_mode = true;
                has_target = false;
            }
        });
    }
    
    public static boolean has_target = false;
    public static BlockPos target;
    
    public static boolean bot_mode_do() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var crosshairTarget = client.crosshairTarget;
        
        var holding_left_click = false;
        var attack_just_broke_a_block = false;
        
        var pos = player.getEntityPos();
        var bpos = player.getBlockPos();
        
        if (has_target && world.getBlockState(target).getBlock() != Blocks.OAK_LOG) {
            has_target = false;
        }
        
        if (has_target) {
            
            player.getInventory().setSelectedSlot(3);
            player.lookAt(EntityAnchor.EYES, target.toCenterPos());
            
            if (crosshairTarget != null) {
                if (crosshairTarget instanceof BlockHitResult result) {
                    if (result.getBlockPos().equals(target) || (bpos.getX() == target.getX() && bpos.getZ() == target.getZ())) {
                        holding_left_click = true;
                        player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
                    } else {
                        player.input.playerInput = new PlayerInput(true, false, false, false, false, false, false);
                    }
                }
            }
            
        } else {
            player.getInventory().setSelectedSlot(0);
            
            var distance = 8;
            for (var d = 0; d < distance; d++) {
                for (var v : List.of(
                    new Vec3i( 1,  0, 0),
                    new Vec3i( 0,  1, 0),
                    new Vec3i(-1,  0, 0),
                    new Vec3i( 0, -1, 0)
                )) {
                    var u = v.crossProduct(new Vec3i(0, 1, 0));
                    for (var a = -d; a < d; a++) {
                        var p = bpos.add(v.multiply(d)).add(u.multiply(a));
                        for (var y = -distance; y < distance; y++) {
                            var block_pos = p.add(new Vec3i(0, y, 0));
                            if (world.getBlockState(block_pos).getBlock() == Blocks.OAK_LOG) {
                                target = block_pos;
                                has_target = true;
                                break;
                            }
                        }
                        if (has_target) break;
                    }
                    if (has_target) break;
                }
                if (has_target) break;
            }
        }
        
        
        // this.player.setYaw(this.player.getYaw() + 1.0f);
        // this.player.input.playerInput = new PlayerInput(true, false, false, false, false, true, false);
        
        
        // Sense:
        // this.crosshairTarget.getType();
        // BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
        
        // drop:
        // if (!this.player.isSpectator() && this.player.dropSelectedItem(drop_entire_stack)) {
        //     this.player.swingHand(Hand.MAIN_HAND);
        // }
        
        // Press right click:
        // if (!this.player.isUsingItem()) {
        //     this.doItemUse();
        // }
        
        // Release right click:
        // this.interactionManager.stopUsingItem(this.player);
        
        // Attack:
        // if (!this.player.isUsingItem()) {
        //     attack_just_broke_a_block |= this.doAttack();
        // }
        
        return holding_left_click && !attack_just_broke_a_block;
    }
    
}
