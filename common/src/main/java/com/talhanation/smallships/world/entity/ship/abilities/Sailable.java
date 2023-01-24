package com.talhanation.smallships.world.entity.ship.abilities;

import com.mojang.datafixers.util.Pair;
import com.talhanation.smallships.client.model.sail.SailModel;
import com.talhanation.smallships.world.entity.ship.Ship;
import com.talhanation.smallships.world.sound.ModSoundTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

public interface Sailable extends Ability {
    default void tickSailShip() {
        if (self().getPassengers().size() > 0 && self().getControllingPassenger() instanceof Player) {
            float speed = self().getData(Ship.SPEED);
            int currentState = self().getData(Ship.SAIL_STATE);

            if (currentState == 0 && speed > 0) {
                self().setData(Ship.SPEED, Math.max(0F, speed - self().getAttributes().acceleration * 1.1F));
            } else if (currentState > 0 && self().tickCount % 20 == 0) {
                float maxSpeedRatio = self().getAttributes().maxSpeed / 4;

                if (speed < maxSpeedRatio) self().setData(Ship.SAIL_STATE, (byte) 1);
                else if (speed < maxSpeedRatio * 2) self().setData(Ship.SAIL_STATE, (byte) 2);
                else if (speed < maxSpeedRatio * 3) self().setData(Ship.SAIL_STATE, (byte) 3);
                else self().setData(Ship.SAIL_STATE, (byte) 4);

                int newState = self().getData(Ship.SAIL_STATE);
                if (currentState != newState) this.playSailSound(newState);
            }
        }
    }

    default void defineSailShipSynchedData() {
        self().getEntityData().define(Ship.SAIL_STATE, (byte) 0);
        self().getEntityData().define(Ship.SAIL_COLOR, SailModel.Color.WHITE.toString());
    }

    default void readSailShipSaveData(CompoundTag tag) {
        CompoundTag compoundTag = tag.getCompound("Sail");
        self().setData(Ship.SAIL_STATE, compoundTag.getByte("State"));
        self().setData(Ship.SAIL_COLOR, compoundTag.getString("Color"));

    }

    default void addSailShipSaveData(CompoundTag tag) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("State", self().getData(Ship.SAIL_STATE));
        compoundTag.putString("Color", self().getData(Ship.SAIL_COLOR));
        tag.put("Sail", compoundTag);
    }

    default boolean interactSail(Player player, InteractionHand interactionHand) {
        ItemStack item = player.getItemInHand(interactionHand);
        if (item.getItem() instanceof DyeItem dyeItem) {
            String color = dyeItem.getDyeColor().getName();
            if (color.equals(self().getData(Ship.SAIL_COLOR))) return false;
            self().setData(Ship.SAIL_COLOR, color);
            if (!player.isCreative()) item.shrink(1);
            self().getLevel().playSound(player, self().getX(), self().getY() + 4 , self().getZ(), SoundEvents.WOOL_HIT, self().getSoundSource(), 15.0F, 1.5F);
            return true;
        }
        return false;
    }

    default void toggleSail() {
        byte state = self().getData(Ship.SAIL_STATE);
        if (state > 0) state = 0;
        else state = 1;
        self().setData(Ship.SAIL_STATE, state);
        this.playSailSound(state);
    }

    default void playSailSound(int state) {
        BiConsumer<SoundEvent, Pair<Float, Float>> play = (sound, modifier) -> {
            if (!self().getLevel().isClientSide()) self().playSound(sound, modifier.getFirst(), modifier.getSecond());
            else self().getLevel().playLocalSound(self().getX(), self().getY() + 4, self().getZ(), sound, self().getSoundSource(), modifier.getFirst(), modifier.getSecond(), false);
        };
        if (state != 0) play.accept(ModSoundTypes.SAIL_MOVE, Pair.of(15.0F, Math.max(0.5F, 1.4F - ((float) state / 5.0F))));
        else play.accept(ModSoundTypes.SAIL_PULL, Pair.of(10.0F, 1.0F));
    }
}