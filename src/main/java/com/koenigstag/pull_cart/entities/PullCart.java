package com.koenigstag.pull_cart.entities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.koenigstag.pull_cart.PullCartMod;

// @Mod.EventBusSubscriber(modid = PullCartMod.MODID, value = Dist.DEDICATED_SERVER)
public class PullCart extends MinecartChest {

    private Entity pulling = null;

    private double minPullDistance = 2;
    private double maxPullDistance = 5;
    private double pullSpeed = 0.5;

    private static Map<UUID, PullCart> playerCartMap = new HashMap<>();

    public PullCart(EntityType<? extends MinecartChest> type, Level world) {
        super(type, world);
    }

    @Override
    public void tick() {
        super.tick();

        if (pulling == null) {
            this.setDeltaMovement(new Vec3(0, -0.5, 0));
        } else if (!pulling.isRemoved()) {
            Level level = pulling.level();

            if (!level.isClientSide) {
                PullCart cart = playerCartMap.get(pulling.getUUID());
                if (cart != null) {
                    Vec3 playerPos = pulling.position();
                    Vec3 cartPos = cart.position();
                    double distance = playerPos.distanceTo(cartPos);

                    // Если повозка далеко, отцепляем её от игрока
                    if (distance >= maxPullDistance || !cart.isFollowingEntity(pulling)) {
                        cart.stopFollowingPlayer(pulling);
                    } else if (distance > minPullDistance && distance < maxPullDistance) {
                        this.pullToEntityPos(pulling);
                    }
                }
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    public void pullToEntityPos(Entity entity) {
        if (entity == null || !isFollowingEntity(entity))
            return;

        // Тянем повозку к игроку
        Vec3 direction = entity.position().subtract(this.position());
        double currentDistance = direction.length(); // Текущее расстояние до игрока

        if (currentDistance > minPullDistance) {
            // Нормализуем вектор и масштабируем его, чтобы оставалось 2 блока до игрока
            Vec3 movement = direction.normalize().scale(currentDistance - minPullDistance);
            // Меняем скорость движения повозки по x и z
            // Обновляем y-координату движения, чтобы выровнять повозку по высоте игрока
            movement = new Vec3(movement.scale(pullSpeed).x, entity.position().y - this.position().y,
                    movement.scale(pullSpeed).z);
            this.setDeltaMovement(movement); // Дополнительно можно уменьшить скорость
        } else {
            this.setDeltaMovement(Vec3.ZERO); // Останавливаем повозку, если она на нужном расстоянии
        }

        // Поворачиваем повозку в сторону игрока
        // Рассчитываем направление движения повозки
        Vec3 deltaMovement = this.getDeltaMovement();
        if (deltaMovement.lengthSqr() > 0.0001) { // Проверяем, что движение происходит
            // Вычисляем угол поворота в зависимости от направления движения
            double rotationAngle = Math.toDegrees(Math.atan2(-deltaMovement.x, deltaMovement.z));
            this.setYRot((float) rotationAngle);
            this.yRotO = this.getYRot(); // Синхронизация поворота с предыдущим тиком, чтобы избежать дерганий
        }
    }

    public boolean isFollowingEntity(Entity entity) {
        return pulling != null && pulling.getUUID().equals(entity.getUUID());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        Level level = player.level();

        if (!level.isClientSide && hand == InteractionHand.OFF_HAND) {
            double distance = player.position().distanceTo(this.position());

            if (distance >= minPullDistance) {
                return InteractionResult.PASS;
            }

            // Открываем инвентарь только при Shift + ПКМ
            if (player.isShiftKeyDown()) {
                InteractionResult interactionResult = this.interactWithContainerVehicle(player);

                if (interactionResult.consumesAction()) {
                    this.gameEvent(GameEvent.CONTAINER_OPEN, player);
                    PiglinAi.angerNearbyPiglins(player, true);
                }

                return interactionResult;
            }

            if (pulling == null) {
                startFollowingPlayer(player);
            } else {
                stopFollowingPlayer(player);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    public Entity getPullingEntity() {
        return pulling;
    }

    private void startFollowingPlayer(Entity entity) {
        if (pulling != null || PullCart.playerCartMap.containsKey(entity.getUUID()))
            return;

        pulling = entity;
        PullCart.playerCartMap.put(entity.getUUID(), this);

        if (entity instanceof Player)
            ((Player) entity).displayClientMessage(Component.literal("Теперь повозка следует за вами."), true);
    }

    public void stopFollowingPlayer(Entity entity) {
        if (isFollowingEntity(entity)) {
            pulling = null;
            PullCart.playerCartMap.remove(entity.getUUID());

            if (entity instanceof Player)
                ((Player) entity).displayClientMessage(Component.literal("Теперь повозка остановилась."), true);
        }
    }

    // Убираем обработку события с посадкой внутрь
    @Override
    public boolean isVehicle() {
        return false; // Повозка не транспорт
    }

    protected Item getDropItem() {
        return PullCartMod.PULL_CART_ITEM.get();
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return pulling == null;
    }

    @Override
    public boolean canBeCollidedWith() {
        return pulling == null;
    }
}