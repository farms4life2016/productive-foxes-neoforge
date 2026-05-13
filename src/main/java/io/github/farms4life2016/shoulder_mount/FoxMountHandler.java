package io.github.farms4life2016.shoulder_mount;

import io.github.farms4life2016.ProductiveFoxes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Objects;

@EventBusSubscriber(modid = ProductiveFoxes.MODID)
public class FoxMountHandler {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();

        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Fox fox)) {
            return;
        }

        if (player.isCrouching() && player.getMainHandItem().isEmpty() && fox.isBaby()) {
            attachFoxToShoulder(player, fox);
            event.setCanceled(true);
        }
    }

    public static void attachFoxToShoulder(Player player, Fox fox) {
        if (player.getShoulderEntityLeft().isEmpty() || player.getShoulderEntityRight().isEmpty()) {
            CompoundTag foxData = new CompoundTag();
            fox.saveWithoutId(foxData);
            foxData.putString("id", Objects.requireNonNull(fox.getEncodeId()));

            foxData.putBoolean("Sleeping", false);
            foxData.putBoolean("Sitting", true);
            foxData.putBoolean("Crouching", false);

            player.setEntityOnShoulder(foxData);
            fox.discard();

            player.displayClientMessage(Component.translatable("message.productivefoxes.fox_on_shoulder"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.productivefoxes.shoulders_occupied"), true);
        }
    }
}
