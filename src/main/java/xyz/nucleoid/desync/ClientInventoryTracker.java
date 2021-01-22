package xyz.nucleoid.desync;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public final class ClientInventoryTracker {
    private final ServerPlayerEntity player;
    private final PlayerInventory inventory;
    private final PlayerScreenHandler screenHandler;

    private ClientInventoryTracker(ServerPlayerEntity player) {
        this.player = player;
        this.inventory = new PlayerInventory(player);
        this.screenHandler = new PlayerScreenHandler(this.inventory, false, player);
    }

    public static ClientInventoryTracker empty(ServerPlayerEntity player) {
        return new ClientInventoryTracker(player);
    }

    public void resetTrackedState() {
        this.screenHandler.updateSlotStacks(this.player.playerScreenHandler.getStacks());
    }

    public void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet) {
        ItemStack stack = packet.getItemStack();
        int slot = packet.getSlot();
        if (packet.getSyncId() == -1) {
            this.inventory.setCursorStack(stack);
        } else if (packet.getSyncId() == -2) {
            this.inventory.setStack(slot, stack);
        } else if (packet.getSyncId() == this.screenHandler.syncId) {
            if (!stack.isEmpty() && slot >= 36 && slot < 45) {
                ItemStack existingStack = this.screenHandler.getSlot(slot).getStack();
                if (existingStack.isEmpty() || existingStack.getCount() < stack.getCount()) {
                    stack.setCooldown(5);
                }
            }

            this.screenHandler.setStackInSlot(slot, stack);
        }
    }

    public void onInventoryUpdate(InventoryS2CPacket packet) {
        if (packet.getSyncId() == this.screenHandler.syncId) {
            this.screenHandler.updateSlotStacks(packet.getContents());
        }
    }

    public void onConfirmScreenAction(ConfirmScreenActionS2CPacket packet) {
        if (packet.wasAccepted()) {
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
}
