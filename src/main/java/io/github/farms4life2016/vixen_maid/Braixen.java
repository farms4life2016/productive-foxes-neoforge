package io.github.farms4life2016.vixen_maid;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;

import java.util.List;

// for now just extend vanilla Fox AI, we can customize later...
public class Braixen extends Fox {
    private final List<ResourceKey<DamageType>> immuneTo = List.of(DamageTypes.SWEET_BERRY_BUSH);
    // if we want fire immunity, we can add LAVA, IN_FIRE, CAMPFIRE, HOT_FLOOR, etc.

    public Braixen(EntityType<? extends Fox> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return immuneTo.stream().anyMatch(source::is) || super.isInvulnerableTo(source);
    }
}
