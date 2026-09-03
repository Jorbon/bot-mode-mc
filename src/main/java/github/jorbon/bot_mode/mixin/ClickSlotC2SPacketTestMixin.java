package github.jorbon.bot_mode.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;

@Mixin(ClickSlotC2SPacket.class)
public class ClickSlotC2SPacketTestMixin {
    
    @Inject(at = @At("RETURN"), method = "<init>")
    public void init(int syncId, int revision, short slot, byte button, SlotActionType actionType, Int2ObjectMap<ItemStackHash> modifiedStacks, ItemStackHash cursor, CallbackInfo ci) {
        System.out.println("PACKET syncId:" + syncId + " revision:" + revision + " slot:" + slot + " button:" + button + " actionType:" + actionType + " modifiedStacks:" + modifiedStacks + " cursor:" + cursor);
    }
}
