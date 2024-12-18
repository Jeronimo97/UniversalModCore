package cam72cam.mod.entity;

import java.util.UUID;

import cam72cam.mod.ModCore;
import cam72cam.mod.serialization.TagCompound;
import cam72cam.mod.world.World;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Seat construct to make multiple riders actually work */
public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    static final ResourceLocation ID = new ResourceLocation(ModCore.MODID, "seat");
    // What it's a part of
    private UUID parent;
    // What is in the seat
    private UUID passenger;
    // If we should try to render the rider as standing or sitting (partial
    // support!)
    boolean shouldSit = true;
    // If a passenger has mounted and then dismounted (if so, we can go away)
    private boolean hasHadPassenger = false;
    // ticks alive?
    private int ticks = 0;

    /** MC reflection */
    public SeatEntity(final net.minecraft.world.World worldIn) {
        super(worldIn);
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(final NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeEntityToNBT(final NBTTagCompound compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    public void onUpdate() {
        ticks++;
        if (world.isRemote || ticks < 5)
            return;

        if (parent == null) {
            ModCore.debug("No parent, goodbye");
            this.setDead();
            return;
        }
        if (passenger == null) {
            ModCore.debug("No passenger, goodbye");
            this.setDead();
            return;
        }

        if (getPassengers().isEmpty()) {
            if (this.ticks < 20) {
                if (!hasHadPassenger) {
                    cam72cam.mod.entity.Entity toRide =
                            World.get(world).getEntity(passenger, cam72cam.mod.entity.Entity.class);
                    if (toRide != null) {
                        ModCore.debug("FORCE RIDER");
                        toRide.internal.startRiding(this, true);
                        hasHadPassenger = true;
                    }
                }
            } else {
                ModCore.debug("No passengers, goodbye");
                this.setDead();
                return;
            }
        }

        if (getParent() == null) {
            if (ticks > 20) {
                ModCore.debug("No parent found, goodbye");
                this.setDead();
            }
        }
    }

    public void setup(final ModdedEntity moddedEntity, final Entity passenger) {
        this.parent = moddedEntity.getUniqueID();
        this.setPosition(moddedEntity.posX, moddedEntity.posY, moddedEntity.posZ);
        this.passenger = passenger.getUniqueID();
    }

    public void moveTo(final ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getUniqueID();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked =
                World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity)
            return linked;
        return null;
    }

    @Override
    public double getMountedYOffset() {
        return 0;
    }

    @Override
    public final void updatePassenger(final net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked =
                World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).updateSeat(this);
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return shouldSit;
    }

    @Override
    public final void removePassenger(final net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked =
                World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).removeSeat(this);
        }
        super.removePassenger(passenger);
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (this.isDead)
            return null;
        if (this.getPassengers().size() == 0)
            return null;
        getPassengers().forEach(p -> System.out.println("Passengers: " + p));
        return World.get(world).getEntity(getPassengers().get(0));
    }

    @Override
    public void writeSpawnData(final ByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        data.setUUID("passenger", passenger);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    @Override
    public void readSpawnData(final ByteBuf additionalData) {
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        parent = data.getUUID("parent");
        passenger = data.getUUID("passenger");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(final double distance) {
        return false;
    }
}
