package io.github.farms4life2016.vixen_maid;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.Level;

// for now just extend vanilla Fox AI, we can customize later...
public class Braixen extends Fox {
    public Braixen(EntityType<? extends Fox> entityType, Level level) {
        super(entityType, level);
    }
}
