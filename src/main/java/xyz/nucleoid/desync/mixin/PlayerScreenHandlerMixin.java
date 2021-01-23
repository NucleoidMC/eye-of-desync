package xyz.nucleoid.desync.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.nucleoid.desync.ClientInventoryTracker;
import xyz.nucleoid.desync.InventoryTrackerHolder;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerMixin extends ScreenHandler {
    private static final Logger LOGGER = LogManager.getLogger("desync-eye");

    @Shadow
    @Final
    private PlayerEntity owner;

    private PlayerScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

        if (this.owner instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) this.owner;
            ServerPlayNetworkHandler networkHandler = player.networkHandler;
            ClientInventoryTracker inventoryTracker = ((InventoryTrackerHolder) networkHandler).getClientInventoryTracker();

            if (inventoryTracker.testForDesync()) {
                MutableText link = new LiteralText("If this is the case, click here.")
                        .styled(style -> {
                            return style.withFormatting(Formatting.BLUE, Formatting.UNDERLINE)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/desync"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/desync")));
                        });

                MutableText message = new LiteralText("Oh no! It looks like your inventory has become desynchronized.")
                        .formatted(Formatting.RED)
                        .append(link)
                        .append(new LiteralText("\nSome debugging information has been sent to the console. Let an admin know!").formatted(Formatting.GRAY, Formatting.ITALIC))
                        .append(new LiteralText("\nBut if this message is wrong, you can ignore it.").formatted(Formatting.GRAY, Formatting.ITALIC));

                this.owner.sendMessage(message, false);
                inventoryTracker.resetTrackedState();

                LOGGER.error("Detected inventory desync for {}", this.owner.getEntityName(), new Exception());
                inventoryTracker.printPacketHistory();
            }
        }
    }
}
