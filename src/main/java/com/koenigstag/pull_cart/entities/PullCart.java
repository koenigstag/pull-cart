package com.koenigstag.pull_cart.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.koenigstag.pull_cart.PullCartMod;

@Mod.EventBusSubscriber(modid = PullCartMod.MODID, value = Dist.DEDICATED_SERVER)
public class PullCart extends ChestBoat {

    private UUID followingPlayerId = null;

    private static Map<UUID, PullCart> playerCartMap = new HashMap<>();

    public PullCart(EntityType<? extends ChestBoat> type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();

        // Замедлить и топить в воде
        if (this.isInWater()) {
            Vec3 deltaMovement = this.getDeltaMovement();
            this.setDeltaMovement(0, deltaMovement.y - 0.05, 0);
            return;
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    public void pullToPlayerPos(Player player) {
        // Запретить тянуть если в воде или чужая повозка
        if (isInWater() || player == null || !isFollowingPlayer(player))
            return;

        Vec3 direction = player.position().subtract(this.position()).normalize().scale(0.1); // Настроить скорость
        this.setDeltaMovement(direction);
    }

    public boolean isFollowingPlayer(Player player) {
        return followingPlayerId != null && followingPlayerId.equals(player.getUUID());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        Level level = player.level();

        if (!level.isClientSide && hand == InteractionHand.OFF_HAND) {
            // Открываем инвентарь только при Shift + ПКМ
            if (player.isShiftKeyDown()) {
                InteractionResult interactionResult = this.interactWithContainerVehicle(player);

                if (interactionResult.consumesAction()) {
                    this.gameEvent(GameEvent.CONTAINER_OPEN, player);
                    PiglinAi.angerNearbyPiglins(player, true);
                }

                return interactionResult;
            }

            if (followingPlayerId == null) {
                startFollowingPlayer(player);
            } else {
                stopFollowingPlayer(player);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public UUID getFollowingPlayerId() {
        return followingPlayerId;
    }

    private void startFollowingPlayer(Player player) {
        if (followingPlayerId != null)
            return;

        UUID id = player.getUUID();
        followingPlayerId = id;
        PullCart.playerCartMap.put(id, this);
        player.displayClientMessage(Component.literal("Теперь повозка следует за вами."), true);
    }

    public void stopFollowingPlayer(Player player) {
        if (isFollowingPlayer(player)) {
            followingPlayerId = null;
            PullCart.playerCartMap.remove(player.getUUID());

            player.displayClientMessage(Component.literal("Теперь повозка остановилась."), true);
        }
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        player.openMenu(this);
        Level level = player.level();
        if (!level.isClientSide) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, player);
            PiglinAi.angerNearbyPiglins(player, true);
        }
    }

    // Убираем обработку события с посадкой внутрь
    @Override
    public boolean isVehicle() {
        return false; // Повозка не транспорт
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();

        if (!level.isClientSide) {
            PullCart cart = playerCartMap.get(player.getUUID());
            if (cart != null) {
                Vec3 playerPos = player.position();
                Vec3 cartPos = cart.position();
                double distance = playerPos.distanceTo(cartPos);

                // Если повозка далеко, отцепляем её от игрока
                if (distance > 5 || !cart.isFollowingPlayer(player)) {
                    cart.stopFollowingPlayer(player);
                } else if (distance > 1 && distance <= 5) {
                    cart.pullToPlayerPos(player);
                }
            }
        }
    }
}