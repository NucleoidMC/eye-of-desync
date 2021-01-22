package xyz.nucleoid.desync.mixin;

import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConfirmScreenActionS2CPacket.class)
public interface ConfirmScreenActionS2CPacketAccessor {
    @Accessor("accepted")
    boolean desync$wasAccepted();
}
