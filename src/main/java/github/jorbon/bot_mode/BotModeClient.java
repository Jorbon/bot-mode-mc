package github.jorbon.bot_mode;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BotModeClient implements ClientModInitializer {
    
    public static boolean bot_mode = false;
    
    private static final KeyBinding.Category BOT_MODE_CATEGORY = KeyBinding.Category.create(Identifier.of("bot_mode"));
    private static KeyBinding key_run = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bot_mode.run", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, BOT_MODE_CATEGORY));
    
    public static boolean has_target;
    public static int search_distance;
    public static BlockPos target;
    
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (key_run.wasPressed()) {
                bot_mode = true;
                has_target = false;
                search_distance = 0;
                
                var client = MinecraftClient.getInstance();
                target = client.player.getBlockPos();
            }
        });
    }
    
    public static boolean bot_mode_do() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var crosshairTarget = client.crosshairTarget;
        var inventory = player.getInventory();
        
        var holding_left_click = false;
        var attack_just_broke_a_block = false;
        
        var pos = player.getEntityPos();
        var player_block_pos = player.getBlockPos();
        
        // if (bpos.getY() != 15 && bpos.getY() != 16) {
        //     bot_mode = false;
        //     return false;
        // }
        
        if (has_target) {
            player.lookAt(EntityAnchor.EYES, target.toCenterPos());
            player.input.playerInput = new PlayerInput(true, false, false, false, false, false, true);
            
            if (crosshairTarget != null) {
                if (crosshairTarget.getType() == HitResult.Type.BLOCK && crosshairTarget instanceof BlockHitResult result) {
                    var result_pos = result.getBlockPos();
                    var result_block = world.getBlockState(result_pos).getBlock();
                    
                    if (result_pos.getY() >= 15 && result_pos.getY() <= 17) {
                        if (
                            result_block == Blocks.SOUL_SAND || 
                            result_block == Blocks.SOUL_SOIL || 
                            result_block == Blocks.GRAVEL
                        ) {
                            inventory.setSelectedSlot(2);
                        } else {
                            inventory.setSelectedSlot(1);
                        }
                        
                        holding_left_click = true;
                        player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
                    }
                }
            }
            
            var target_block = world.getBlockState(target);
            if (target_block.isAir() || !target_block.getFluidState().isEmpty()) {
                has_target = false;
                search_distance = 0;
            }
        }
        
        boolean chose_movement = false;
        
        for (var entity : client.world.getEntitiesByType(
            TypeFilter.instanceOf(FallingBlockEntity.class),
            new Box(
                player_block_pos.getX() - 2,
                player_block_pos.getY() - 0,
                player_block_pos.getZ() - 2,
                player_block_pos.getX() + 2,
                player_block_pos.getY() + 12,
                player_block_pos.getZ() + 2
            ),
            entity -> true
        )) {
            if (entity instanceof FallingBlockEntity fbe) {
                // TODO
            }
        }
        
        // Policy: if the player moves a block in a movement key direction would put them partially in a danger block, don't allow it
        // If they are already in a danger block, search for the nearest safety block and go there
        // If the search fails, bail
        
        client.getNetworkHandler().sendChatCommand("home");
        
        
        // Next look at hostile entities within range
        // try to seek out and attack by keeping them at a comfort distance
        // time attacks with manual cooldown
        // Bail if there are too many or if they are too close
        
        
        if (!chose_movement) {
            ItemEntity best = null;
            double best_distance = Double.MAX_VALUE;
            for (var entity : client.world.getEntitiesByType(
                TypeFilter.instanceOf(ItemEntity.class),
                new Box(
                    player_block_pos.getX() - 64,
                    player_block_pos.getY() - 32,
                    player_block_pos.getZ() - 64,
                    player_block_pos.getX() + 64,
                    player_block_pos.getY() + 32,
                    player_block_pos.getZ() + 64
                ),
                entity -> true
            )) {
                if (entity instanceof ItemEntity item_entity) {
                    Item item = item_entity.getStack().getItem();
                    double distance = item_entity.getEntityPos().distanceTo(pos);
                    if (item == Items.ANCIENT_DEBRIS || item == Items.QUARTZ) {
                        if (best == null || distance < best_distance) {
                            best = item_entity;
                            best_distance = distance;
                        }
                    }
                }
            }
            
            if (best != null) {
                var look = player.getRotationVector().getHorizontal().normalize();
                var left = new Vec3d(look.z, 0.0, -look.x);
                var item = best.getEntityPos().subtract(pos).getHorizontal();
                
                player.input.playerInput = new PlayerInput(
                    look.dotProduct(item) >  0.5,
                    look.dotProduct(item) < -0.5,
                    left.dotProduct(item) >  0.5,
                    left.dotProduct(item) < -0.5,
                    false,
                    false,
                    false
                );
                chose_movement = true;
            }
        }
        
        
        // TODO: rules around controlling lava flow
        
        if (!has_target) {
            
            var directions = new ArrayList<Pair<Vec3i, Vec3i>>(List.of(
                new Pair<>(new Vec3i( 1, 0,  0), new Vec3i( 0, 0,  1)),
                new Pair<>(new Vec3i( 1, 0,  0), new Vec3i( 0, 0, -1)),
                new Pair<>(new Vec3i(-1, 0,  0), new Vec3i( 0, 0,  1)),
                new Pair<>(new Vec3i(-1, 0,  0), new Vec3i( 0, 0, -1)),
                new Pair<>(new Vec3i( 0, 0,  1), new Vec3i( 1, 0,  0)),
                new Pair<>(new Vec3i( 0, 0,  1), new Vec3i(-1, 0,  0)),
                new Pair<>(new Vec3i( 0, 0, -1), new Vec3i( 1, 0,  0)),
                new Pair<>(new Vec3i( 0, 0, -1), new Vec3i(-1, 0,  0))
            ));
            
            java.util.Collections.shuffle(directions);
            
            while (true) {
                for (var pair : directions) {
                    var v = pair.getLeft().multiply(search_distance);
                    var u = pair.getRight();
                    for (var a = 0; a <= search_distance; a++) {
                        var p = v.add(u.multiply(a));
                        for (var y = 15; y <= 17; y++) {
                            var block_pos = new BlockPos(player_block_pos.getX() + p.getX(), y, player_block_pos.getZ() + p.getZ());
                            var block = world.getBlockState(block_pos);
                            if (!block.isAir() && block.getFluidState().isEmpty()) {
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
                search_distance += 1;
                if (search_distance > 5) break;
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
