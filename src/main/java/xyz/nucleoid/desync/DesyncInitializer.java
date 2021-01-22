package xyz.nucleoid.desync;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public final class DesyncInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("desync").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                player.onHandlerRegistered(player.playerScreenHandler, player.playerScreenHandler.getStacks());
                player.sendMessage(new LiteralText("Inventory synchronized!").formatted(Formatting.GREEN), false);
                return Command.SINGLE_SUCCESS;
            }));
        });
    }
}
