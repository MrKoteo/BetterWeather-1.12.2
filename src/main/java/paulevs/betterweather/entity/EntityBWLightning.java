package paulevs.betterweather.entity;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.OreDictionary;
import paulevs.betterweather.BWBlocks;

/**
 * BetterWeather lightning bolt - extends vanilla {@link EntityLightningBolt} with
 * custom rod detection, reduced thunder distance, muted explosion sound, and
 * invisible-light-block placement/cleanup at the strike point.
 * <p>
 * Because {@code lightningState}, {@code boltLivingTime}, and {@code effectOnly}
 * are private in the superclass, this class replicates the vanilla
 * {@code onUpdate} lifecycle with its own state fields. The superclass
 * constructor receives {@code effectOnly=true} so vanilla fire/entity logic
 * does not compete with ours.
 * <p>
 * The {@link #EntityBWLightning(World)} constructor exists for Forge entity
 * registration (the registry invokes a {@code (World)} factory).
 */
public class EntityBWLightning extends EntityLightningBolt {

    /** Tracks the bolt lifecycle phase, mirroring vanilla {@code lightningState}. */
    private int bwState = 2;

    /** Remaining bolt flashes; on a rod this is set much higher. */
    private int bwBoltLivingTime;

    /** Whether the bolt struck a lightning-rod block. */
    private boolean onRod;

    /** Whether we placed our invisible light block on spawn. */
    private boolean lightPlaced;

    /* ------------------------------------------------------------------ */
    /*  Constructors                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Registration constructor (Forge entity registry factory).
     * Position is immediately overridden by the spawn call; this only needs to
     * satisfy the {@code (World)} signature.
     */
    public EntityBWLightning(World worldIn) {
        super(worldIn, 0.0, 0.0, 0.0, true);
    }

    /**
     * Functional constructor called from {@code LightningUtil#processChunk}.
     * <p>
     * Passes {@code effectOnly=true} to super so the vanilla constructor skips
     * fire placement - we handle fire ourselves in {@link #onUpdate()}.
     */
    public EntityBWLightning(World worldIn, double x, double y, double z) {
        super(worldIn, x, y, z, true);

        BlockPos strikePos = new BlockPos(this);
        BlockPos below = strikePos.down();

        // --- rod detection via OreDictionary 'blockLightningRod' ---
        this.onRod = isLightningRodBlock(worldIn, below);

        // --- bolt life: 200 ticks on a rod, else vanilla 1–2 ---
        this.bwBoltLivingTime = this.onRod ? 200 : (this.rand.nextInt(2) + 1);

        // --- place invisible light block at strike point ---
        if (!worldIn.isRemote) {
            if (worldIn.getBlockState(strikePos).getMaterial() == Material.AIR) {
                worldIn.setBlockState(strikePos, BWBlocks.lightningLight.getDefaultState());
                this.lightPlaced = true;
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Lifecycle - full onUpdate override                                 */
    /* ------------------------------------------------------------------ */

    @Override
    public void onUpdate() {
        // -- replicate the minimal Entity-level tick (EntityLightningBolt's
        //    onEntityUpdate is empty, so the base Entity update is harmless
        //    to call directly; we skip super.onUpdate() to avoid vanilla
        //    sound/fire/entity-striking logic which we replace below)
        if (!this.world.isRemote) {
            this.setFlag(6, this.isGlowing());
        }
        this.onEntityUpdate();

        // ============================================================
        //  State 2 - first tick: play thunder sound at reduced range
        //           (explosion/impact sound is muted entirely)
        // ============================================================
        if (this.bwState == 2) {
            this.world.playSound(
                (EntityPlayer) null,
                this.posX, this.posY, this.posZ,
                SoundEvents.ENTITY_LIGHTNING_THUNDER,
                SoundCategory.WEATHER,
                200.0F,                                    // vanilla 10000 → 200
                0.8F + this.rand.nextFloat() * 0.2F
            );
            // explosion sound muted (was playSound ordinal=1 in the original mixin redirect)
        }

        --this.bwState;

        // ============================================================
        //  State < 0 - flash phase; die when living-time exhausted,
        //            else reset to state 1 with optional fire placement
        // ============================================================
        if (this.bwState < 0) {
            if (this.bwBoltLivingTime == 0) {
                cleanupLightBlock();
                this.setDead();
                return;
            }

            if (this.bwState < -this.rand.nextInt(10)) {
                --this.bwBoltLivingTime;
                this.bwState = 1;

                // new random bolt vertex for rendering
                this.boltVertex = this.rand.nextLong();

                // fire setting - skip when standing on a lightning rod
                if (!this.onRod && !this.world.isRemote) {
                    BlockPos pos = new BlockPos(this);
                    if (this.world.getGameRules().getBoolean("doFireTick")
                        && this.world.isAreaLoaded(pos, 10)
                        && this.world.getBlockState(pos).getMaterial() == Material.AIR
                        && Blocks.FIRE.canPlaceBlockAt(this.world, pos)) {
                        this.world.setBlockState(pos, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }

        // ============================================================
        //  State >= 0 - client sky flash / server entity striking
        // ============================================================
        if (this.bwState >= 0) {
            if (this.world.isRemote) {
                this.world.setLastLightningBolt(2);
            } else {
                // entity striking - always active (not tied to vanilla effectOnly)
                double range = 3.0D;
                List<Entity> list = this.world.getEntitiesWithinAABBExcludingEntity(
                    this,
                    new AxisAlignedBB(
                        this.posX - range, this.posY - range, this.posZ - range,
                        this.posX + range, this.posY + 6.0D + range, this.posZ + range
                    )
                );
                for (int i = 0; i < list.size(); ++i) {
                    Entity entity = list.get(i);
                    if (!ForgeEventFactory.onEntityStruckByLightning(entity, this)) {
                        entity.onStruckByLightning(this);
                    }
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    /**
     * If we placed the invisible light block, remove it and trigger light /
     * render updates in a ±15 radius.
     */
    private void cleanupLightBlock() {
        BlockPos pos = new BlockPos(this);
        if (this.world.getBlockState(pos).getBlock() == BWBlocks.lightningLight) {
            this.world.setBlockToAir(pos);
            // markBlockRangeForRenderUpdate uses min/max corners
            BlockPos min = pos.add(-15, -15, -15);
            BlockPos max = pos.add(15, 15, 15);
            this.world.markBlockRangeForRenderUpdate(min, max);
            this.world.checkLight(pos);
        }
    }

    /**
     * Returns {@code true} if the block at {@code pos} has the OreDictionary
     * name {@code blockLightningRod}.
     */
    private static boolean isLightningRodBlock(World world, BlockPos pos) {
        ItemStack stack = new ItemStack(world.getBlockState(pos).getBlock());
        // OreDictionary.getOreIDs throws "Stack can not be invalid!" on an empty stack.
        // Air (and any block with no item form) yields an empty stack - skip it.
        if (stack.isEmpty()) return false;
        for (int id : OreDictionary.getOreIDs(stack)) {
            if ("blockLightningRod".equals(OreDictionary.getOreName(id))) {
                return true;
            }
        }
        return false;
    }
}
