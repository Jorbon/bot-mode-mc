package github.jorbon.bot_mode;

import java.util.ArrayList;
import java.util.HashMap;
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
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BotModeClient implements ClientModInitializer {
    
    public static boolean bot_mode = false;
    
    private static final KeyBinding.Category BOT_MODE_CATEGORY = KeyBinding.Category.create(Identifier.of("bot_mode"));
    private static KeyBinding key_run = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.bot_mode.run", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, BOT_MODE_CATEGORY));
    
    public static int target_importance;
    public static int search_distance;
    public static BlockPos block_target;
    public static LivingEntity entity_target;
    public static int attack_cooldown;
    public static final HashMap<BlockPos, Integer> danger_blocks = new HashMap<>();
    
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (key_run.wasPressed()) {
                bot_mode = true;
                block_target = null;
                target_importance = 0;
                search_distance = 0;
                attack_cooldown = 0;
                entity_target = null;
                danger_blocks.clear();
            }
        });
    }
    
    static void bail() {
        // MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("home");
        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamemode creative");
        bot_mode = false;
    }
    
    static boolean handle_danger() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var player_block_pos = player.getBlockPos();
        var pos = player.getEntityPos();
        
        // scan danger blocks
        danger_blocks.clear();
        
        var danger_min_x = player_block_pos.getX() - 2;
        var danger_min_z = player_block_pos.getZ() - 2;
        var danger_max_x = player_block_pos.getX() + 2;
        var danger_max_z = player_block_pos.getZ() + 2;
        
        for (var x = danger_min_x; x <= danger_max_x; x++) {
            for (var z = danger_min_z; z <= danger_max_z; z++) {
                var key = new BlockPos(x, 16, z);
                var value = 0;
                
                // Lava
                for (var y = 14; y <= 25; y++) {
                    var block_state = world.getBlockState(new BlockPos(x, y, z));
                    if (y >= 18 && !block_state.isAir() && block_state.getFluidState().isEmpty()) {
                        break;
                    }
                    
                    var fluid = block_state.getFluidState().getFluid();
                    if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
                        value = 2;
                        break;
                    }
                }
                
                if (value == 0) {
                    // Missing floor blocks
                    var floor_block_pos = new BlockPos(x, 14, z);
                    if (!world.getBlockState(floor_block_pos).isSolidSurface(world, floor_block_pos, player, Direction.UP)) {
                        value = 1;
                    }
                }
                
                danger_blocks.put(key, value);
            }
        }
        
        // Falling gravel
        for (var entity : client.world.getEntitiesByType(
            TypeFilter.instanceOf(FallingBlockEntity.class),
            new Box(danger_min_x, 15, danger_min_z, danger_max_x, 25, danger_max_z),
            entity -> true
        )) {
            var block_pos = entity.getBlockPos();
            var key = new BlockPos(block_pos.getX(), 16, block_pos.getZ());
            danger_blocks.put(key, 2);
        }
        
        // Assess if we are actively in a danger block
        var box = player.getBoundingBox();
        var in_danger = false;
        var corner_danger = 0b0000;
        BlockPos[] corners = {
            new BlockPos((int) Math.floor(box.maxX), 16, (int) Math.floor(box.maxZ)),
            new BlockPos((int) Math.floor(box.maxX), 16, (int) Math.floor(box.minZ)),
            new BlockPos((int) Math.floor(box.minX), 16, (int) Math.floor(box.minZ)),
            new BlockPos((int) Math.floor(box.minX), 16, (int) Math.floor(box.maxZ))
        };
        
        for (var i = 0; i < 4; i++) {
            var danger_level = danger_blocks.get(corners[i]);
            if (danger_level >= 2) {
                in_danger = true;
            }
            if (danger_level >= 1) {
                corner_danger |= 1 << i;
            }
        }
        
        if (!in_danger) return false;
        
        // Get out of danger blocks
        var v = Vec3d.ZERO;
        switch (corner_danger) {
            case 0b1100:
                v = Vec3d.X;
                break;
            case 0b0011:
                v = Vec3d.X.negate();
                break;
            case 0b0110:
                v = Vec3d.Z;
                break;
            case 0b1001:
                v = Vec3d.Z.negate();
                break;
            
            case 0b0100:
            case 0b1110:
                v = Vec3d.X.add(Vec3d.Z);
                break;
            case 0b0010:
            case 0b0111:
                v = Vec3d.X.negate().add(Vec3d.Z);
                break;
            case 0b1000:
            case 0b1101:
                v = Vec3d.X.add(Vec3d.Z.negate());
                break;
            case 0b0001:
            case 0b1011:
                v = Vec3d.X.negate().add(Vec3d.Z.negate());
                break;
            
            case 0b0101:
            case 0b1010:
            case 0b1111:
                // Surrounded by danger: seek nearest safe block
                BlockPos safe_block = null;
                var min_distance = Double.MAX_VALUE;
                for (var entry : danger_blocks.entrySet()) {
                    if (entry.getValue() != 0) continue;
                    var distance = pos.distanceTo(entry.getKey().toBottomCenterPos());
                    if (min_distance > distance) {
                        min_distance = distance;
                        safe_block = entry.getKey();
                    }
                }
                
                if (safe_block == null) {
                    // No safe blocks in range
                    bail();
                    return true;
                } else {
                    v = safe_block.toBottomCenterPos().subtract(pos);
                }
        }
        
        if (v == Vec3d.ZERO) {
            // Unreachable, in danger but the danger is nowhere?
        } else {
            player.lookAt(EntityAnchor.FEET, pos.add(v.x, 0.0, v.z));
            player.input.playerInput = new PlayerInput(true, false, false, false, pos.y < 15.5, false, true);
        }
        
        target_importance = 0;
        search_distance = 0;
        return true;
    }
    
    static void choose_entity_target() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var player_block_pos = player.getBlockPos();
        var pos = player.getEntityPos();
        
        entity_target = null;
        double min_distance = Double.MAX_VALUE;
        for (var entity : world.getEntitiesByType(
            TypeFilter.instanceOf(LivingEntity.class),
            new Box(player_block_pos.getX() - 32, 14, player_block_pos.getZ() - 32, player_block_pos.getX() + 32, 18, player_block_pos.getZ() + 32),
            entity -> entity.isAlive() && ((entity instanceof PiglinEntity && !entity.isBaby()) || entity instanceof SkeletonEntity || entity instanceof HoglinEntity || entity instanceof MagmaCubeEntity)
        )) {
            var distance = entity.getEntityPos().distanceTo(pos);
            if (min_distance > distance) {
                min_distance = distance;
                entity_target = entity;
            }
        }
    }
    
    static void choose_mining_target() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var player_block_pos = player.getBlockPos();
        
        if (target_importance > 0) {
            var block_state = world.getBlockState(block_target);
            if (block_state.isAir() || !block_state.getFluidState().isEmpty()) {
                target_importance = 0;
                search_distance = 0;
            }
        }
        
        if (target_importance >= 2) return;
        
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
                    
                    // Target block under lava, continuing to search even if a lower importance target exists
                    BlockPos top_block_pos_under_lava = null;
                    for (var y = 14; y <= 25; y++) {
                        var block_pos = new BlockPos(player_block_pos.getX() + p.getX(), y, player_block_pos.getZ() + p.getZ());
                        var block = world.getBlockState(block_pos);
                        var biome = world.getBiome(block_pos);
                        
                        if (!block.isAir() && block.getFluidState().isEmpty()) {
                            if (y >= 18) break;
                            if (!(biome.matchesId(Identifier.ofVanilla("crimson_forest")) || biome.matchesId(Identifier.ofVanilla("basalt_deltas")))) {
                                top_block_pos_under_lava = null;
                                for (var direction : List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH)) {
                                    if (world.getBlockState(block_pos.up().offset(direction)).isAir()) {
                                        top_block_pos_under_lava = block_pos;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (!block.getFluidState().isEmpty() && top_block_pos_under_lava != null) {
                            block_target = top_block_pos_under_lava;
                            target_importance = 2;
                            return;
                        }
                    }
                    
                    // Target general block
                    if (target_importance < 1) {
                        for (var y = 15; y <= 17; y++) {
                            var block_pos = new BlockPos(player_block_pos.getX() + p.getX(), y, player_block_pos.getZ() + p.getZ());
                            var block = world.getBlockState(block_pos);
                            var biome = world.getBiome(block_pos);
                            if (!block.isAir() && block.getFluidState().isEmpty() && world.getFluidState(block_pos.subtract(v)).isEmpty() && !(biome.matchesId(Identifier.ofVanilla("crimson_forest")) || biome.matchesId(Identifier.ofVanilla("basalt_deltas")))) {
                                block_target = block_pos;
                                target_importance = 1;
                                break;
                            }
                        }
                    }
                }
            }
            search_distance += 1;
            if (target_importance == 1 && search_distance > 2 + Math.max(Math.abs(player_block_pos.getX() - block_target.getX()), Math.abs(player_block_pos.getZ() - block_target.getZ()))) {
                search_distance = 0;
                return;
            }
            if (search_distance > 5) {
                return;
            }
        }
    }
    
    public static Pair<Boolean, Boolean> bot_mode_do_before_interact() {
        if (attack_cooldown > 0) attack_cooldown -= 1;
        
        var client = MinecraftClient.getInstance();
        var player = client.player;
        var world = client.world;
        var player_block_pos = player.getBlockPos();
        var crosshairTarget = client.crosshairTarget;
        var inventory = player.getInventory();
        
        
        
        if (player.getHealth() < 8.0 || player_block_pos.getY() < 14 || player_block_pos.getY() > 17) {
            bail();
            return new Pair<>(false, false);
        }
        
        choose_entity_target();
        choose_mining_target();
        
        if (entity_target != null && target_importance < 2) {
            var attacking = false;
            player.lookAt(EntityAnchor.EYES, entity_target.getEyePos());
            if (player.getInventory().getSelectedSlot() != 0) {
                player.getInventory().setSelectedSlot(0);
                attack_cooldown = 10;
            }
            if (attack_cooldown == 0 && crosshairTarget.getType() == Type.ENTITY && crosshairTarget instanceof EntityHitResult result) {
                if (result.getEntity() == entity_target && !player.isUsingItem()) {
                    attacking = true;
                    attack_cooldown = 10;
                }
            }
            target_importance = 0;
            search_distance = 0;
            return new Pair<>(false, attacking);
        }
        
        if (target_importance > 0) {
            var breaking = false;
            
            var look_target = block_target.toCenterPos();
            if (block_target.getY() <= 14) {
                look_target = new Vec3d(look_target.x, look_target.y + 0.5, look_target.z);
            } else if (block_target.getY() >= 18) {
                look_target = new Vec3d(look_target.x, look_target.y + 0.5, look_target.z);
            }
            player.lookAt(EntityAnchor.EYES, look_target);
            
            if (crosshairTarget != null) {
                if (crosshairTarget.getType() == HitResult.Type.BLOCK && crosshairTarget instanceof BlockHitResult result) {
                    var result_pos = result.getBlockPos();
                    var result_block = world.getBlockState(result_pos).getBlock();
                    var biome = world.getBiome(result_pos);
                    if (((result_pos.equals(block_target)) || result_pos.getY() >= 15 && result_pos.getY() <= 17) && !(biome.matchesId(Identifier.ofVanilla("crimson_forest")) || biome.matchesId(Identifier.ofVanilla("basalt_deltas")))) {
                        if (
                            result_block == Blocks.SOUL_SAND || 
                            result_block == Blocks.SOUL_SOIL || 
                            result_block == Blocks.GRAVEL
                        ) {
                            inventory.setSelectedSlot(2);
                        } else {
                            inventory.setSelectedSlot(1);
                        }
                        breaking = true;
                    }
                }
            }
            
            return new Pair<>(breaking, false);
        }
        
        return new Pair<>(false, false);
        
        // Drop:
        // if (!this.player.isSpectator() && this.player.dropSelectedItem(drop_entire_stack)) {
        //     this.player.swingHand(Hand.MAIN_HAND);
        // }
        
        // Press right click:
        // if (!this.player.isUsingItem()) {
        //     this.doItemUse();
        // }
        
        // Release right click:
        // this.interactionManager.stopUsingItem(this.player);
    }
    
    public static void bot_mode_do_after_interact() {
        var client = MinecraftClient.getInstance();
        var player = client.player;
        
        var pos = player.getEntityPos();
        var player_block_pos = player.getBlockPos();
        
        if (handle_danger()) return;
        
        if (entity_target != null && entity_target.isDead()) {
            entity_target = null;
        }
        
        choose_mining_target();
        
        Vec3d movement_target = Vec3d.ZERO;
        
        if (target_importance >= 2) {
            movement_target = block_target.toCenterPos().subtract(pos);
            if (movement_target.length() < 4.0) {
                movement_target = Vec3d.ZERO;
            }
            
        } else if (entity_target != null) {
            var v = entity_target.getEntityPos().subtract(pos);
            var distance = v.length();
            if (distance > 3.0) {
                movement_target = v;
            } else if (distance < 2.0) {
                movement_target = v.negate();
            }
            
        } else {
            // See if there are items to pick up
            ItemEntity best = null;
            double best_distance = Double.MAX_VALUE;
            for (var item_entity : client.world.getEntitiesByType(
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
                var item = item_entity.getStack().getItem();
                double distance = item_entity.getEntityPos().distanceTo(pos);
                if ((best == null || best_distance < distance) && (item == Items.ANCIENT_DEBRIS || item == Items.QUARTZ)) {
                    best = item_entity;
                    best_distance = distance;
                }
            }
            
            if (best != null) {
                if (best_distance > 0.5) {
                    movement_target = best.getEntityPos().subtract(pos);
                }
                
            } else if (target_importance > 0) {
                movement_target = block_target.toCenterPos().subtract(player.getEyePos());
                if (movement_target.length() < 4.0) {
                    movement_target = Vec3d.ZERO;
                }
            }
        }
        
        // Handle movement and stop from walking into danger
        
        if (movement_target == Vec3d.ZERO) {
            player.input.playerInput = PlayerInput.DEFAULT;
            return;
        }
        
        // Policy: if the player moves 1.0 block in a movement key direction would put them partially in a danger block, don't allow it
        
        var look = player.getRotationVector(0.0f, player.getYaw());
        var left = new Vec3d(look.z, 0.0, -look.x);
        
        var inputs = new ArrayList<>(List.of(
            new PlayerInput(true , false, false, false, false, false, false),
            new PlayerInput(true , false, true , false, false, false, false),
            new PlayerInput(false, false, true , false, false, false, false),
            new PlayerInput(false, true , true , false, false, false, false),
            new PlayerInput(false, true , false, false, false, false, false),
            new PlayerInput(false, true , false, true , false, false, false),
            new PlayerInput(false, false, false, true , false, false, false),
            new PlayerInput(true , false, false, true , false, false, false)
        ));
        
        final var movement_target_final = movement_target;
        inputs.sort((a, b) -> {
            var va = Vec3d.ZERO;
            var vb = Vec3d.ZERO;
            if (a.forward ()) va = va.add(look);
            if (a.backward()) va = va.add(look.negate());
            if (a.left    ()) va = va.add(left);
            if (a.right   ()) va = va.add(left.negate());
            if (b.forward ()) vb = vb.add(look);
            if (b.backward()) vb = vb.add(look.negate());
            if (b.left    ()) vb = vb.add(left);
            if (b.right   ()) vb = vb.add(left.negate());
            var diff = va.normalize().dotProduct(movement_target_final) - vb.normalize().dotProduct(movement_target_final);
            return diff > 0.0 ? -1 : (diff < 0.0 ? 1 : 0);
        });
        
        var chose_input = false;
        outer:
        for (var input : inputs) {
            var v = Vec3d.ZERO;
            if (input.forward ()) v = v.add(look);
            if (input.backward()) v = v.add(look.negate());
            if (input.left    ()) v = v.add(left);
            if (input.right   ()) v = v.add(left.negate());
            v = v.normalize();
            
            var box = player.getBoundingBox();
            var has_place_to_stand = false;
            
            for (var pair : List.of(
                new Pair<>(box.maxX, box.maxZ),
                new Pair<>(box.maxX, box.minZ),
                new Pair<>(box.minX, box.minZ),
                new Pair<>(box.minX, box.maxZ)
            )) {
                var x = (int) Math.floor(pair.getLeft () + v.x * 0.8);
                var z = (int) Math.floor(pair.getRight() + v.z * 0.8);
                
                var danger_level = danger_blocks.get(new BlockPos(x, 16, z));
                if (danger_level >= 2) {
                    continue outer;
                }
                if (danger_level == 0) {
                    has_place_to_stand = true;
                }
            }
            
            if (has_place_to_stand) {
                player.input.playerInput = input;
                chose_input = true;
                break;
            }
        }
        
        if (!chose_input) {
            player.input.playerInput = PlayerInput.DEFAULT;
        }
        
    }
    
}
