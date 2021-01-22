package xyz.nucleoid.desync.mixin;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.desync.ClientInventoryTracker;
import xyz.nucleoid.desync.InventoryTrackerHolder;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin implements InventoryTrackerHolder {
    @Unique
    private ClientInventoryTracker inventoryTracker;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftServer server, ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        this.inventoryTracker = ClientInventoryTracker.empty(player);
    }

    @Redirect(method = "onClientStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;respawnPlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Z)Lnet/minecraft/server/network/ServerPlayerEntity;"))
    private ServerPlayerEntity respawnPlayer(PlayerManager playerManager, ServerPlayerEntity oldPlayer, boolean alive) {
        ServerPlayerEntity newPlayer = playerManager.respawnPlayer(oldPlayer, alive);
        this.inventoryTracker = ClientInventoryTracker.empty(newPlayer);
        return newPlayer;
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At("HEAD"))
    private void sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {
        if (packet instanceof ScreenHandlerSlotUpdateS2CPacket) {
            this.inventoryTracker.onScreenHandlerSlotUpdate((ScreenHandlerSlotUpdateS2CPacket) packet);
        } else if (packet instanceof InventoryS2CPacket) {
            this.inventoryTracker.onInventoryUpdate((InventoryS2CPacket) packet);
        } else if (packet instanceof ConfirmScreenActionS2CPacket) {
            this.inventoryTracker.onConfirmScreenAction((ConfirmScreenActionS2CPacket) packet);
        }
    }

    @Override
    public ClientInventoryTracker getClientInventoryTracker() {
        return this.inventoryTracker;
    }
}
