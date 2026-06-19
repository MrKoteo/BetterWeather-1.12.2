package paulevs.betterweather.util;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class LightningLightBlock extends Block {

	public LightningLightBlock() {
		super(Material.AIR);
		setLightLevel(1.0F);
		setBlockUnbreakable();
		setTickRandomly(true);
		setTranslationKey("lightning_light");
		setRegistryName(new ResourceLocation("better_weather", "lightning_light"));
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.INVISIBLE;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
		return true;
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		if (world.isRemote) {
			return;
		}
		List<EntityLightningBolt> bolts = world.getEntitiesWithinAABB(
			EntityLightningBolt.class,
			new AxisAlignedBB(pos)
		);
		if (bolts.isEmpty()) {
			world.setBlockState(pos, Blocks.AIR.getDefaultState());
			world.markBlockRangeForRenderUpdate(
				pos.add(-15, -15, -15),
				pos.add(15, 15, 15)
			);
			world.checkLight(pos);
		}
	}
}
