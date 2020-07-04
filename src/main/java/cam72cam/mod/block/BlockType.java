package cam72cam.mod.block;

import cam72cam.mod.entity.Player;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.item.ItemStack;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.util.Facing;
import cam72cam.mod.util.Hand;
import cam72cam.mod.world.World;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class BlockType {
    public static Map<BlockType, Integer> blocks = new HashMap<>();
    public final net.minecraft.block.Block internal;
    private final String name;
    private final String modID;

    public BlockType(String modID, String name) {
        this.modID = modID;
        this.name = name;

        internal = getBlock();

        CommonEvents.Block.REGISTER.subscribe(() -> GameRegistry.registerBlock(internal, new ResourceLocation(modID, name).toString()));

        CommonEvents.Block.BROKEN.subscribe((world, pos, player) -> {
            net.minecraft.block.Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (block instanceof BlockInternal) {
                return ((BlockInternal) block).tryBreak(world, pos.x, pos.y, pos.z, player);
            }
            return true;
        });
    }

    public final String getName() {
        return name;
    }

    protected BlockInternal getBlock() {
        return new BlockInternal();
    }

    public abstract boolean tryBreak(World world, Vec3i pos, Player player);

    /*
    Properties
     */
    public Material getMaterial() {
        return Material.METAL;
    }
    public float getHardness() {
        return 1.0f;
    }
    public float getExplosionResistance() {
        return getHardness() * 5;
    }
    public boolean isConnectable() {
        return true;
    }
    public boolean isRedstoneProvider() {
        return false;
    }


    /*
    Public functionality
     */

    public abstract void onBreak(World world, Vec3i pos);

    public abstract boolean onClick(World world, Vec3i pos, Player player, Hand hand, Facing facing, Vec3d hit);

    public abstract ItemStack onPick(World world, Vec3i pos);

    public abstract void onNeighborChange(World world, Vec3i pos, Vec3i neighbor);

    public double getHeight() {
        return 1;
    }

    public int getStrongPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    public int getWeakPower(World world, Vec3i vec3i, Facing from) {
        return 0;
    }

    protected class BlockInternal extends net.minecraft.block.Block {
        public BlockInternal() {
            super(BlockType.this.getMaterial().internal);
            BlockType type = BlockType.this;
            setHardness(type.getHardness());
            setStepSound(type.getMaterial().soundType);
            setUnlocalizedName(type.modID + ":" + type.name);
            // REMOVED 1.7.10 setRegistryName(new ResourceLocation(type.modID, type.name));
        }

        @Override
        public int getRenderType() {
            return blocks.getOrDefault(BlockType.this, -1);
        }

        @Override
        public boolean renderAsNormalBlock() {
            return false;
        }

        @Override
        public final void breakBlock(net.minecraft.world.World world, int posX, int posY, int posZ, Block block, int meta) {
            BlockType.this.onBreak(World.get(world), new Vec3i(posX, posY, posZ));
            super.breakBlock(world, posX, posY, posZ, block, meta);
        }

        @Override
        public final boolean onBlockActivated(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player, int facing, float hitX, float hitY, float hitZ) {
            return BlockType.this.onClick(World.get(world), new Vec3i(posX, posY, posZ), new Player(player), Hand.PRIMARY, Facing.from(EnumFacing.getFront(facing)), new Vec3d(hitX, hitY, hitZ));
        }

        @Override
        public final net.minecraft.item.ItemStack getPickBlock(MovingObjectPosition target, net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player) {
            return BlockType.this.onPick(World.get(world), new Vec3i(posX, posY, posZ)).internal;
        }

        @Override
        public void onNeighborBlockChange(net.minecraft.world.World worldIn, int posX, int posY, int posZ, Block blockIn) {
            // TODO 1.10 this might have some interesting side effects
            this.onNeighborChange(worldIn, posX, posY, posZ, posX, posY, posZ);
        }

        @Override
        public void onNeighborChange(IBlockAccess world, int posX, int posY, int posZ, int neighborX, int neighborY, int neighborZ) {
            BlockType.this.onNeighborChange(World.get((net.minecraft.world.World) world), new Vec3i(posX, posY, posZ), new Vec3i(neighborX, neighborY, neighborZ));
        }

        /*
        Overrides
         */
        @Override
        public final float getExplosionResistance(Entity exploder) {
            return BlockType.this.getExplosionResistance();
        }

        /* TODO 1.7.10
        @Override
        public final EnumBlockRenderType getRenderType(Block block, int meta) {
            // TESR Renderer TODO OPTIONAL!@!!!!
            return EnumBlockRenderType.MODEL;
        }
        */


        @Override
        public final boolean isOpaqueCube() {
            return false;
        }

        @Override
        public final boolean isNormalCube() {
            return false;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBoxFromPool(net.minecraft.world.World source, int posX, int posY, int posZ) {
            return AxisAlignedBB.getBoundingBox(0, 0, 0, 1, BlockType.this.getHeight(), 1).offset(posX, posY, posZ);
        }

        public void addCollisionBoxesToList(net.minecraft.world.World source, int posX, int posY, int posZ, AxisAlignedBB other, List list, Entity ent) {
            super.addCollisionBoxesToList(source, posX, posY, posZ, other, list, ent);
        }

        public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_) {
            this.setBlockBounds(0, 0, 0, 1, (float) BlockType.this.getHeight(), 1);
        }

        @Override
        public AxisAlignedBB getSelectedBoundingBoxFromPool(net.minecraft.world.World worldIn, int posX, int posY, int posZ) {
            return getCollisionBoundingBoxFromPool(worldIn, posX, posY, posZ)
                    .expand(0, 0.1, 0)
                    ;
        }

        /* Removed 1.7.10
        @Override
        public int getMetaFromState(Block block, int meta) {
            return 0;
        }
        */

        /*
         * Fence, glass override
         */

        @Deprecated
        @Override
        public boolean isSideSolid(IBlockAccess world, int posX, int posY, int posZ, ForgeDirection side) {
            if (BlockType.this.isConnectable()) {
                return super.isSideSolid(world, posX, posY, posZ, side);
            }

            // SNOW ONLY?
            return side == ForgeDirection.UP;
        }

        public boolean tryBreak(net.minecraft.world.World world, int posX, int posY, int posZ, EntityPlayer player) {
            return BlockType.this.tryBreak(World.get(world), new Vec3i(posX, posY, posZ), new Player(player));
        }

        /* Redstone */

        @Override
        public int isProvidingWeakPower(IBlockAccess blockAccess, int posX, int posY, int posZ, int side)
        {
            return BlockType.this.isRedstoneProvider() ? BlockType.this.getWeakPower(World.get((net.minecraft.world.World)blockAccess), new Vec3i(posX, posY, posZ), Facing.from((byte) side)) : 0;
        }

        @Override
        public int isProvidingStrongPower(IBlockAccess blockAccess, int posX, int posY, int posZ, int side)
        {
            return BlockType.this.isRedstoneProvider() ? BlockType.this.getStrongPower(World.get((net.minecraft.world.World)blockAccess), new Vec3i(posX, posY, posZ), Facing.from((byte) side)) : 0;
        }

        @Override
        public boolean canProvidePower()
        {
            return BlockType.this.isRedstoneProvider();
        }

        /* TODO
        @SideOnly(Side.CLIENT)
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
        */

        public int quantityDropped(Random p_149745_1_)
        {
            return 0;
        }

        public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
        {
            return Item.getItemFromBlock(Blocks.air);
        }

    }
}
