package com.koenigstag.pull_cart;

import com.koenigstag.pull_cart.compat.TfcWeight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;

/**
 * The wooden cart of MC+ Cart (andykuo1, GPL-2.0), on Minecraft 1.20.1.
 * <ul>
 * <li>Right-click it to pull it behind you, again to let go; riding a horse, the horse pulls it.</li>
 * <li>Right-click it with a chest, crafting table, ender chest, furnace or jukebox to load that block, and
 * Shift + right-click to use it (a jukebox takes the disc in your hand, or gives it back).</li>
 * <li>Right-click it while you lead a small animal on a lead to put the animal in; Shift + right-click lets it out.</li>
 * <li>Hit it a few times to break it, as a minecart or boat (it drops itself and what it carries); Shift + hit
 * takes the block off.</li>
 * <li>Under water it breaks. While a disc plays, it stays where it is.</li>
 * </ul>
 * With TerraFirmaCraft, a chest holding too many huge, very heavy items can't be pulled.
 */
public class CartEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> CARGO =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> LIT =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PLAYING =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> HURT_TIME =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT_DIR =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE =
            SynchedEntityData.defineId(CartEntity.class, EntityDataSerializers.FLOAT);

    /** As a minecart or boat: a hit adds ten times its damage, it heals 1 a tick, and breaks beyond this. */
    private static final float BREAK_DAMAGE = 40;
    private static final double MAX_STEP = 0.8;
    /** Animals taller than this don't fit, as in MC+ Cart. */
    private static final double MAX_ANIMAL_HEIGHT = 1.5;
    private static final double LEAD_RANGE = 7;
    public static final double WHEEL_DIAMETER = 0.8;

    /** The chest's 27 slots, as in MC+ Cart; a menu on them closes when the cart leaves or loses the chest. */
    private final SimpleContainer chest = new SimpleContainer(27) {
        @Override
        public boolean stillValid(Player player) {
            return isNear(player) && cargo() == CargoType.CHEST;
        }
    };
    private final CartFurnace furnace = new CartFurnace(this);
    private ItemStack record = ItemStack.EMPTY;
    private int songTicks;
    @Nullable
    private BlockPos songPos;
    /** Server: the player pulling it; not saved, as in MC+ Cart. */
    @Nullable
    private Player puller;

    private float wheelAngle;
    private float wheelAngleO;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;

    public CartEntity(EntityType<? extends CartEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0f);
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(CARGO, ItemStack.EMPTY);
        entityData.define(LIT, false);
        entityData.define(PLAYING, false);
        entityData.define(HURT_TIME, 0);
        entityData.define(HURT_DIR, 1);
        entityData.define(DAMAGE, 0f);
    }

    /** The block it carries, empty for none. */
    public ItemStack cargoItem() {
        return entityData.get(CARGO);
    }

    public CargoType cargo() {
        return CargoType.of(cargoItem());
    }

    private void setCargo(ItemStack stack) {
        entityData.set(CARGO, stack);
    }

    void setLit(boolean lit) {
        if (entityData.get(LIT) != lit) {
            entityData.set(LIT, lit);
        }
    }

    /** The carried block as it should look: a burning furnace, a jukebox with a disc. */
    @Nullable
    public BlockState cargoState() {
        if (!(cargoItem().getItem() instanceof BlockItem item)) {
            return null;
        }
        BlockState state = item.getBlock().defaultBlockState();
        if (state.hasProperty(BlockStateProperties.LIT)) {
            state = state.setValue(BlockStateProperties.LIT, entityData.get(LIT));
        }
        if (state.hasProperty(BlockStateProperties.HAS_RECORD)) {
            state = state.setValue(BlockStateProperties.HAS_RECORD, entityData.get(PLAYING));
        }
        return state;
    }

    public float wheelAngle(float partialTick) {
        return Mth.lerp(partialTick, wheelAngleO, wheelAngle);
    }

    /** How far it rocks about its length after a hit, in degrees, as a minecart or boat does. */
    public float wobble(float partialTick) {
        float time = entityData.get(HURT_TIME) - partialTick;
        if (time <= 0) {
            return 0;
        }
        float damage = Math.max(0, entityData.get(DAMAGE) - partialTick);
        return Mth.sin(time) * time * damage / 10f * entityData.get(HURT_DIR);
    }

    boolean isNear(Player player) {
        return isAlive() && player.distanceToSqr(this) <= 64;
    }

    // ----------------------------------------------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            tickLerp();
            double moved = Math.sqrt(Mth.square(getX() - xo) + Mth.square(getZ() - zo));
            wheelAngleO = wheelAngle;
            wheelAngle += (float) (moved / (Math.PI * WHEEL_DIAMETER) * 360);
            if (cargo() == CargoType.ENDER_CHEST && random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.PORTAL, getRandomX(0.5), getY() + 0.6 + random.nextDouble() * 0.8,
                        getRandomZ(0.5), (random.nextDouble() - 0.5) * 2, -random.nextDouble(), (random.nextDouble() - 0.5) * 2);
            }
            if (entityData.get(LIT) && random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 1.3, getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (isUnderWater()) {
            breakCart(true);
            return;
        }
        if (entityData.get(HURT_TIME) > 0) {
            entityData.set(HURT_TIME, entityData.get(HURT_TIME) - 1);
        }
        if (entityData.get(DAMAGE) > 0) {
            entityData.set(DAMAGE, Math.max(0, entityData.get(DAMAGE) - 1));
        }
        if (cargo() == CargoType.FURNACE) {
            furnace.tick(level());
        }
        if (entityData.get(PLAYING) && --songTicks <= 0) {
            entityData.set(PLAYING, false); // the song is over; the disc stays in
        }

        Entity leader = leader();
        double mx = 0;
        double mz = 0;
        if (leader != null) {
            double dx = leader.getX() - getX();
            double dz = leader.getZ() - getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            double gap = distance - PullCartConfig.FOLLOW_GAP.get() - leader.getBbWidth() / 2 - getBbWidth() / 2;
            if (gap > 0 && distance > 1.0E-3) {
                double step = Math.min(gap, MAX_STEP);
                mx = dx / distance * step;
                mz = dz / distance * step;
            }
            if (distance > 1.0E-3) {
                setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f);
            }
        }
        double my = getDeltaMovement().y - (isInWater() ? 0.02 : 0.08);
        setDeltaMovement(mx, my * 0.98, mz);
        move(MoverType.SELF, getDeltaMovement());
    }

    /** Who pulls it now (the horse when the player rides one), or null; lets go when that stopped making sense. */
    @Nullable
    private Entity leader() {
        if (puller == null) {
            return null;
        }
        if (!puller.isAlive() || puller.level() != level() || puller.isSpectator()) {
            puller = null;
            return null;
        }
        if (puller.distanceTo(this) > PullCartConfig.LET_GO_DISTANCE.get()) {
            letGo("message.pull_cart.too_far");
            return null;
        }
        if (entityData.get(PLAYING)) {
            letGo("message.pull_cart.playing");
            return null;
        }
        if (tickCount % 20 == 0 && tooHeavy() > 0) {
            letGo(heavyMessage());
            return null;
        }
        return puller.getRootVehicle();
    }

    /** With TFC: how many overburdening items the chest holds beyond what may be pulled; 0 when it's fine. */
    private int tooHeavy() {
        if (!PullCartMod.TFC || cargo() != CargoType.CHEST) {
            return 0;
        }
        return Math.max(0, TfcWeight.overburdening(chest) - PullCartConfig.MAX_HEAVY_ITEMS.get());
    }

    private Component heavyMessage() {
        return Component.translatable("message.pull_cart.too_heavy", TfcWeight.overburdening(chest),
                PullCartConfig.MAX_HEAVY_ITEMS.get());
    }

    private void letGo(String messageKey) {
        letGo(Component.translatable(messageKey));
    }

    private void letGo(Component message) {
        if (puller != null) {
            puller.displayClientMessage(message, true);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.NEUTRAL, 1f, 1f);
        }
        puller = null;
    }

    // ------------------------------------------------------------------------------------------------- interaction

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (cargo() != CargoType.NONE) {
                useCargo(player, held);
            } else if (isVehicle()) {
                ejectPassengers();
            }
            return InteractionResult.CONSUME;
        }
        if (cargo() == CargoType.NONE && !isVehicle() && CargoType.of(held) != CargoType.NONE) {
            setCargo(held.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.CONSUME;
        }
        Mob led = ledAnimal(player);
        if (led != null) {
            loadAnimal(player, led);
            return InteractionResult.CONSUME;
        }
        if (puller == player) {
            letGo("message.pull_cart.let_go");
        } else if (puller != null) {
            player.displayClientMessage(Component.translatable("message.pull_cart.someone_else", puller.getDisplayName()), true);
        } else {
            hitch(player);
        }
        return InteractionResult.CONSUME;
    }

    private void hitch(Player player) {
        if (tooHeavy() > 0) {
            player.displayClientMessage(heavyMessage(), true);
            return;
        }
        // One cart per player.
        for (CartEntity other : level().getEntitiesOfClass(CartEntity.class, player.getBoundingBox().inflate(32),
                cart -> cart != this && cart.puller == player)) {
            other.puller = null;
        }
        puller = player;
        player.displayClientMessage(Component.translatable("message.pull_cart.hitched"), true);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 1f, 1f);
    }

    @Nullable
    private Mob ledAnimal(Player player) {
        return level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(LEAD_RANGE),
                mob -> mob.isLeashed() && mob.getLeashHolder() == player).stream().findFirst().orElse(null);
    }

    private void loadAnimal(Player player, Mob animal) {
        if (isVehicle() || cargo() != CargoType.NONE) {
            player.displayClientMessage(Component.translatable("message.pull_cart.loaded"), true);
            return;
        }
        if (animal.getBbHeight() > MAX_ANIMAL_HEIGHT) {
            player.displayClientMessage(Component.translatable("message.pull_cart.too_big", animal.getDisplayName()), true);
            return;
        }
        animal.dropLeash(true, !player.getAbilities().instabuild);
        animal.startRiding(this, true);
    }

    private void useCargo(Player player, ItemStack held) {
        switch (cargo()) {
            case CHEST -> player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, chest),
                    cargoItem().getHoverName()));
            case WORKBENCH -> player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new CartCraftingMenu(id, inventory, this),
                    Component.translatable("container.crafting")));
            case FURNACE -> player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new FurnaceMenu(id, inventory, furnace, furnace.data),
                    Component.translatable("container.furnace")));
            case ENDER_CHEST -> {
                PlayerEnderChestContainer ender = player.getEnderChestInventory();
                ender.setActiveChest(null);
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, ender),
                        Component.translatable("container.enderchest")));
            }
            case JUKEBOX -> {
                if (!record.isEmpty()) {
                    ejectRecord();
                } else if (held.getItem() instanceof RecordItem disc) {
                    record = held.copyWithCount(1);
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    songPos = blockPosition();
                    songTicks = disc.getLengthInTicks();
                    entityData.set(PLAYING, true);
                    level().levelEvent(null, LevelEvent.SOUND_PLAY_JUKEBOX_SONG, songPos, Item.getId(disc));
                    letGo("message.pull_cart.playing");
                }
            }
            default -> {
            }
        }
    }

    private void ejectRecord() {
        if (entityData.get(PLAYING) && songPos != null) {
            level().levelEvent(LevelEvent.SOUND_STOP_JUKEBOX_SONG, songPos, 0);
        }
        entityData.set(PLAYING, false);
        if (!record.isEmpty()) {
            spawnAtLocation(record, 0.8f);
        }
        record = ItemStack.EMPTY;
        songPos = null;
    }

    // ---------------------------------------------------------------------------------------------- breaking, drops

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (source.getDirectEntity() instanceof Player player && player.isSecondaryUseActive() && cargo() != CargoType.NONE) {
            dropCargo();
            return true;
        }
        boolean creative = source.getEntity() instanceof Player player && player.getAbilities().instabuild;
        entityData.set(HURT_DIR, -entityData.get(HURT_DIR));
        entityData.set(HURT_TIME, 10);
        entityData.set(DAMAGE, entityData.get(DAMAGE) + amount * 10);
        markHurt();
        gameEvent(GameEvent.ENTITY_DAMAGE, source.getEntity());
        if (creative || entityData.get(DAMAGE) > BREAK_DAMAGE) {
            breakCart(!creative);
        }
        return true;
    }

    private void breakCart(boolean dropSelf) {
        dropCargo();
        ejectPassengers();
        if (dropSelf) {
            spawnAtLocation(PullCartMod.CART_ITEM.get());
        }
        puller = null;
        discard();
    }

    /** Takes the block off the cart: it drops, with what it held. */
    private void dropCargo() {
        switch (cargo()) {
            case CHEST -> {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    spawnAtLocation(chest.removeItemNoUpdate(slot), 0.5f);
                }
            }
            case FURNACE -> furnace.takeAll().forEach(stack -> spawnAtLocation(stack, 0.5f));
            case JUKEBOX -> ejectRecord();
            default -> {
            }
        }
        if (!cargoItem().isEmpty()) {
            spawnAtLocation(cargoItem().copy(), 0.5f);
        }
        setCargo(ItemStack.EMPTY);
        setLit(false);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(PullCartMod.CART_ITEM.get());
    }

    // ---------------------------------------------------------------------------------------------- entity settings

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && cargo() == CargoType.NONE;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.45;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = 3;
    }

    private void tickLerp() {
        if (lerpSteps <= 0) {
            return;
        }
        double x = getX() + (lerpX - getX()) / lerpSteps;
        double y = getY() + (lerpY - getY()) / lerpSteps;
        double z = getZ() + (lerpZ - getZ()) / lerpSteps;
        setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
        lerpSteps--;
        setPos(x, y, z);
        setRot(getYRot(), getXRot());
    }

    // ----------------------------------------------------------------------------------------------------- saving

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCargo(ItemStack.of(tag.getCompound("Cargo")));
        chest.clearContent();
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag item = items.getCompound(i);
            int slot = item.getByte("Slot") & 255;
            if (slot < chest.getContainerSize()) {
                chest.setItem(slot, ItemStack.of(item));
            }
        }
        furnace.load(tag.getCompound("Furnace"));
        record = ItemStack.of(tag.getCompound("Record"));
        setLit(furnace.lit());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Cargo", cargoItem().save(new CompoundTag()));
        ListTag items = new ListTag();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = stack.save(new CompoundTag());
                item.putByte("Slot", (byte) slot);
                items.add(item);
            }
        }
        tag.put("Items", items);
        CompoundTag furnaceTag = new CompoundTag();
        furnace.save(furnaceTag);
        tag.put("Furnace", furnaceTag);
        if (!record.isEmpty()) {
            tag.put("Record", record.save(new CompoundTag()));
        }
    }
}
