package xyz.nucleoid.desync;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import xyz.nucleoid.desync.mixin.ConfirmScreenActionS2CPacketAccessor;
import xyz.nucleoid.desync.mixin.InventoryS2CPacketAccessor;
import xyz.nucleoid.desync.mixin.ScreenHandlerSlotUpdateS2CPacketAccessor;

import java.util.List;

public final class ClientInventoryTracker {
    private static final int PACKET_HISTORY_SIZE = 16;

    private final ServerPlayerEntity player;
    private final PlayerInventory inventory;
    private final PlayerScreenHandler screenHandler;

    private final Object[] packetHistory = new Object[PACKET_HISTORY_SIZE];
    private int packetPointer;

    private ClientInventoryTracker(ServerPlayerEntity player) {
        this.player = player;
        this.inventory = new PlayerInventory(player);
        this.screenHandler = new PlayerScreenHandler(this.inventory, false, player);
    }

    public static ClientInventoryTracker empty(ServerPlayerEntity player) {
        return new ClientInventoryTracker(player);
    }

    private void pushPacket(Object packet) {
        int pointer = (this.packetPointer++) % PACKET_HISTORY_SIZE;
        this.packetHistory[pointer] = packet;
    }

    private void updateSlotStacks(List<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            Slot slot = this.screenHandler.getSlot(i);
            slot.setStack(stacks.get(i).copy());
        }
    }

    public void resetTrackedState() {
        this.updateSlotStacks(this.player.playerScreenHandler.getStacks());
    }

    public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacketAccessor packet) {
        this.pushPacket(packet);

        ItemStack stack = packet.desync$getStack().copy();
        int slot = packet.desync$getSlot();
        int syncId = packet.desync$getSyncId();
        if (syncId == -1) {
            this.inventory.setCursorStack(stack);
        } else if (syncId == -2) {
            this.inventory.setStack(slot, stack);
        } else if (syncId == this.screenHandler.syncId) {
            if (!stack.isEmpty() && slot >= 36 && slot < 45) {
                ItemStack existingStack = this.screenHandler.getSlot(slot).getStack();
                if (existingStack.isEmpty() || existingStack.getCount() < stack.getCount()) {
                    stack.setCooldown(5);
                }
            }

            this.screenHandler.setStackInSlot(slot, stack);
        }
    }

    public void onInventoryUpdate(InventoryS2CPacketAccessor packet) {
        this.pushPacket(packet);

        if (packet.desync$getSyncId() == this.screenHandler.syncId) {
            this.updateSlotStacks(packet.desync$getContents());
        }
    }

    public void onConfirmScreenAction(ConfirmScreenActionS2CPacketAccessor packet) {
        if (packet.desync$wasAccepted()) {
            this.resetTrackedState();
        }
    }

    public boolean testForDesync() {
        DefaultedList<ItemStack> serverStacks = this.player.playerScreenHandler.getStacks();
        DefaultedList<ItemStack> trackedStacks = this.screenHandler.getStacks();
        for (int i = 0; i < serverStacks.size(); i++) {
            if (!ItemStack.areEqual(serverStacks.get(i), trackedStacks.get(i))) {
                return true;
            }
        }
        return false;
    }

    public void printPacketHistory() {
        int pointer = this.packetPointer;
        for (int i = 0; i < PACKET_HISTORY_SIZE; i++) {
            Object packet = this.packetHistory[(i + pointer) % PACKET_HISTORY_SIZE];
            this.printPacket(packet);
        }
    }

    private void printPacket(Object packet) {
        if (packet instanceof InventoryS2CPacket) {
            InventoryS2CPacketAccessor accessor = (InventoryS2CPacketAccessor) packet;
            System.out.println("INVENTORY: " + accessor.desync$getSyncId() + ": " + accessor.desync$getContents());
        } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket) {
            ScreenHandlerSlotUpdateS2CPacketAccessor accessor = (ScreenHandlerSlotUpdateS2CPacketAccessor) packet;
            System.out.println("SLOT UPDATE: " + accessor.desync$getSyncId() + " in " + accessor.desync$getSlot() + ": " + accessor.desync$getStack());
        }
    }
}
