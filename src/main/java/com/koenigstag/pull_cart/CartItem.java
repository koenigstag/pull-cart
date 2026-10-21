package com.koenigstag.pull_cart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Right-click a block to set the cart down on it, facing the way you look. */
public class CartItem extends Item {
    public CartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(face == Direction.DOWN ? Direction.UP : face);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        CartEntity cart = PullCartMod.CART.get().create(server);
        if (cart == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        cart.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player != null ? player.getYRot() : 0, 0);
        if (!level.noCollision(cart, cart.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(cart);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        if (player == null || !player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
