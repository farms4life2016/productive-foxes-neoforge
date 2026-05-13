package io.github.farms4life2016.shoulder_mount;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class FoxLayerHandler {
    public static void onRegisterLayers(EntityRenderersEvent.AddLayers event) {
        EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();

        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new FoxShoulderLayer<>(renderer, modelSet));
            }
        }
    }
}
