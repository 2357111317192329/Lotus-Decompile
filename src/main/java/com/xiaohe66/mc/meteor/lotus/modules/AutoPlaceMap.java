package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.placemap.PlaceMapPos;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class AutoPlaceMap extends StepModule {
   private boolean wasRightClicking = false;
   private BlockPos blockPos1;
   private BlockPos blockPos2;
   private Direction playerDirection;
   private final List<PlaceMapPos> placePosList = new ArrayList<>();
   private int placeIndex;

   public AutoPlaceMap() {
      super("自动贴画", "开启功能后, 给2个角放置展示框后自动贴画。由<hn2>友情赞助开发");
      this.addStep(Steps.PLACE, this::place);
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         if (this.mc.player != null && this.isActive() && this.step != Steps.PLACE) {
            boolean isRightClicking = this.mc.options.keyUse.isDown();
            if (isRightClicking && !this.wasRightClicking) {
               this.checkItemFramePlacement();
            }

            this.wasRightClicking = isRightClicking;
         }
      });
   }

   public void onActivate() {
      this.init();
   }

   private void place() {
      int startSlot = this.placeIndex;

      do {
         PlaceMapPos mapPos = this.placePosList.get(this.placeIndex);
         if (!mapPos.isDone()) {
            this.doPlace(mapPos);
            return;
         }

         this.nextIndex();
      } while (this.placeIndex != startSlot);

      this.toggle();
   }

   private void nextIndex() {
      if (this.placeIndex >= this.placePosList.size() - 1) {
         this.placeIndex = 0;
      } else {
         this.placeIndex++;
      }
   }

   private void doPlace(PlaceMapPos placeMapPos) {
      BlockPos placePos = placeMapPos.getBlockPos();
      ItemFrame itemFrame = this.getItemFrameAtPosition(placePos);
      if (itemFrame == null) {
         FindItemResult findFrameResult = this.findFrame();
         if (findFrameResult.found()) {
            if (findFrameResult.isHotbar()) {
               if (findFrameResult.getHand() == null) {
                  InvUtils.swap(findFrameResult.slot(), false);
               } else {
                  this.info("放置:" + this.placeIndex, new Object[0]);
                  BlockUtils.place(placePos, findFrameResult, 0);
                  this.nextIndex();
               }
            } else {
               HeInvUtils.swap(findFrameResult.slot(), 6);
            }

            this.setDelay();
         } else {
            this.info("缺少<展示框>", new Object[0]);
            this.toggle();
         }
      } else {
         ItemStack heldItemStack = itemFrame.getItem();
         if (heldItemStack.isEmpty()) {
            FindItemResult findMapResult = this.findMap(placeMapPos.getName());
            if (findMapResult.found()) {
               if (findMapResult.isHotbar()) {
                  if (findMapResult.getHand() == null) {
                     InvUtils.swap(findMapResult.slot(), false);
                  } else {
                     this.info("放置:" + placeMapPos.getName(), new Object[0]);
                     this.interactEntity(itemFrame);
                     this.nextIndex();
                  }
               } else {
                  HeInvUtils.swap(findMapResult.slot(), 7);
               }

               this.setDelay();
            } else {
               this.info("缺少<地图画>:" + placeMapPos.getName(), new Object[0]);
               this.toggle();
            }
         } else {
            placeMapPos.setDone(true);
            this.nextIndex();
         }
      }
   }

   private FindItemResult findMap(String name) {
      return InvUtils.find(
         itemStack -> itemStack.getItem() == Items.FILLED_MAP && itemStack.getCustomName() != null && name.equals(itemStack.getCustomName().getString())
      );
   }

   private FindItemResult findFrame() {
      return InvUtils.find(itemStack -> itemStack.getItem() == Items.ITEM_FRAME || itemStack.getItem() == Items.GLOW_ITEM_FRAME);
   }

   private void checkItemFramePlacement() {
      LocalPlayer player = this.mc.player;
      ItemStack mainHandStack = player.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
      boolean holdingItemFrame = mainHandStack.is(Items.ITEM_FRAME)
         || offHandStack.is(Items.ITEM_FRAME)
         || mainHandStack.is(Items.GLOW_ITEM_FRAME)
         || offHandStack.is(Items.GLOW_ITEM_FRAME);
      if (holdingItemFrame) {
         if (this.mc.hitResult != null && this.mc.hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)this.mc.hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            Direction side = blockHit.getDirection();
            BlockPos framePos = blockPos.relative(side);
            if (this.blockPos1 == null) {
               this.blockPos1 = new BlockPos(framePos);
               this.playerDirection = this.mc.player.getNearestViewDirection();
            } else {
               this.blockPos2 = new BlockPos(framePos);
               this.readyPlacePos();
               this.readyMap();
               this.setDelay();
               this.step = Steps.PLACE;
            }
         }
      }
   }

   private void readyMap() {
      List<String> nameList = new ArrayList<>();

      for (int i = 0; i < 36; i++) {
         ItemStack itemStack = this.getItemStack(i);
         if (itemStack.getItem() == Items.FILLED_MAP) {
            Component customName = itemStack.getCustomName();
            if (customName != null) {
               nameList.add(customName.getString());
            }
         }
      }

      if (nameList.size() < this.placePosList.size()) {
         this.info("地图画数量不对", new Object[0]);
         this.toggle();
      } else {
         nameList.sort(null);

         for (int i = 0; i < this.placePosList.size(); i++) {
            PlaceMapPos pos = this.placePosList.get(i);
            String name = nameList.get(i);
            pos.setName(name);
         }
      }
   }

   private void readyPlacePos() {
      if (this.playerDirection == Direction.NORTH) {
         if (this.blockPos1.getZ() != this.blockPos2.getZ()) {
            this.info("不在平面上", new Object[0]);
            this.toggle();
            return;
         }

         int y = this.blockPos1.getY();

         for (int y1 = this.blockPos2.getY(); y >= y1; y--) {
            int x = this.blockPos1.getX();

            for (int x1 = this.blockPos2.getX(); x <= x1; x++) {
               this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
            }
         }
      } else if (this.playerDirection == Direction.SOUTH) {
         if (this.blockPos1.getZ() != this.blockPos2.getZ()) {
            this.info("不在平面上", new Object[0]);
            this.toggle();
            return;
         }

         int y = this.blockPos1.getY();

         for (int y1 = this.blockPos2.getY(); y >= y1; y--) {
            int x = this.blockPos1.getX();

            for (int x1 = this.blockPos2.getX(); x >= x1; x--) {
               this.placePosList.add(new PlaceMapPos(x, y, this.blockPos1.getZ()));
            }
         }
      } else if (this.playerDirection == Direction.WEST) {
         if (this.blockPos1.getX() != this.blockPos2.getX()) {
            this.info("不在平面上", new Object[0]);
            this.toggle();
            return;
         }

         int y = this.blockPos1.getY();

         for (int y1 = this.blockPos2.getY(); y >= y1; y--) {
            int z = this.blockPos1.getZ();

            for (int z1 = this.blockPos2.getZ(); z >= z1; z--) {
               this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
            }
         }
      } else if (this.playerDirection == Direction.EAST) {
         if (this.blockPos1.getX() != this.blockPos2.getX()) {
            this.info("不在平面上", new Object[0]);
            this.toggle();
            return;
         }

         int y = this.blockPos1.getY();

         for (int y1 = this.blockPos2.getY(); y >= y1; y--) {
            int z = this.blockPos1.getZ();

            for (int z1 = this.blockPos2.getZ(); z <= z1; z++) {
               this.placePosList.add(new PlaceMapPos(this.blockPos1.getX(), y, z));
            }
         }
      }
   }

   public ItemFrame getItemFrameAtPosition(BlockPos framePos) {
      if (this.mc.level == null) {
         return null;
      }

      AABB searchBox = new AABB(
         framePos.getX(),
         framePos.getY(),
         framePos.getZ(),
         framePos.getX() + 1,
         framePos.getY() + 1,
         framePos.getZ() + 1
      );
      List<ItemFrame> frames = this.mc.level.getEntitiesOfClass(ItemFrame.class, searchBox, itemFrame -> itemFrame.blockPosition().equals(framePos));
      return frames.isEmpty() ? null : frames.get(0);
   }

   @EventHandler
   public void onRenderWorld(Render3DEvent event) {
      if (this.placePosList.isEmpty()) {
         if (this.blockPos1 != null) {
            event.renderer.box(this.blockPos1, Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
         }
      } else {
         for (PlaceMapPos pos : this.placePosList) {
            event.renderer.box(pos.getBlockPos(), Color.ORANGE, Color.BLUE, ShapeMode.Lines, 0);
         }
      }
   }

   private void init() {
      this.blockPos1 = null;
      this.blockPos2 = null;
      this.placePosList.clear();
      this.placeIndex = 1;
      this.step = Steps.NONE;
   }

   public void onDeactivate() {
      this.init();
   }
}
