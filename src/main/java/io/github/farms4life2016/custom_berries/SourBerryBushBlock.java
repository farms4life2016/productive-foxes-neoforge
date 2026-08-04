package io.github.farms4life2016.custom_berries;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import io.github.farms4life2016.ProductiveFoxesItems;

// ponytail: inherits mayPlaceOn from BushBlock (dirt tag + farmland), so the bush
// places on dirt-like blocks just like vanilla sweet berry bushes.
public class SourBerryBushBlock extends SweetBerryBushBlock {

    public SourBerryBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ProductiveFoxesItems.SOUR_BERRIES.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        boolean fullyMature = age == 3;
        if (age > 1) {
            int amount = 1 + level.random.nextInt(2);
            popResource(level, pos, new ItemStack(ProductiveFoxesItems.SOUR_BERRIES.get(), amount + (fullyMature ? 1 : 0)));
            level.playSound(
                    null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F
            );
            BlockState newState = state.setValue(AGE, 1);
            level.setBlock(pos, newState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
    }
}
