package net.amemah.pvzspawners.block;

import com.mojang.serialization.MapCodec;
import joshxviii.plantz.PazBlocks;
import joshxviii.plantz.PlantHeadAttachment;
import net.amemah.pvzspawners.pot.RenewablePotData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Normal pot geometry, waterlogging, facing and attachment interaction, with a separate renewal ledger. */
public final class RenewablePlantPotBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
	public static final MapCodec<RenewablePlantPotBlock> CODEC = simpleCodec(RenewablePlantPotBlock::new);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public RenewablePlantPotBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}
	@Override protected MapCodec<RenewablePlantPotBlock> codec() { return CODEC; }
	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
			.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
	}
	@Override protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}
	@Override protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PazBlocks.PLANT_POT.defaultBlockState().getShape(level, pos, context);
	}
	@Override protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return PazBlocks.PLANT_POT.defaultBlockState().getInteractionShape(level, pos);
	}
	@Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return PlantHeadAttachment.Companion.potBlockInteraction(state, level, pos, player);
	}
	@Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }
	@Override protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}
	@Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
		Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
	}
	@Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		super.affectNeighborsAfterRemoval(state, level, pos, moved);
		if (!level.getBlockState(pos).is(this)) RenewablePotData.get(level).remove(level, pos);
	}
}
