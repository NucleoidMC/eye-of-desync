package xyz.nucleoid.desync.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ScreenHandlerSlotUpdateS2CPacket.class)
public interface ScreenHandlerSlotUpdateS2CPacketAccessor {
    @Accessor("syncId")
    int desync$getSyncId();

    @Accessor("slot")
    int desync$getSlot();

    @Accessor("stack")
    ItemStack desync$getStack();
}
