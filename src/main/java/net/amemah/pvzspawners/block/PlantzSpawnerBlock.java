package net.amemah.pvzspawners.block;

import com.mojang.serialization.MapCodec;
import net.amemah.pvzspawners.menu.PlantzSpawnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class PlantzSpawnerBlock extends WeedSpawnerBlock implements EntityBlock {
	public static final MapCodec<PlantzSpawnerBlock> CODEC = simpleCodec(PlantzSpawnerBlock::new);
	public PlantzSpawnerBlock(Properties properties) { super(properties); }
	@Override protected MapCodec<? extends Block> codec() { return CODEC; }
	@Override public PlantzSpawnerBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PlantzSpawnerBlockEntity(pos, state);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlantzSpawnerBlockEntity spawner) {
			player.openMenu(spawner);
		}
		return InteractionResult.SUCCESS;
	}
}
