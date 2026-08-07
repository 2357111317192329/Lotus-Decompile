package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Step;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import java.util.function.Consumer;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.IntSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.EntityHitResult;

public abstract class BaseModule extends Module {
   protected final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Integer> delay;
   private int delayTimer;
   private int curPlayerSlot;
   private int curScreenSlot;
   private Step openStatus = Steps.IDLE;
   private long lastOpenTime = 0L;

   public BaseModule(String name, String description) {
      this(name, description, 5);
   }

   public BaseModule(String name, String description, int defaultDelay) {
      this(name, description, defaultDelay, 40);
   }

   public BaseModule(String name, String description, int defaultDelay, int max) {
      super(Const.CATEGORY, name, description);
      this.delay = this.sgGeneral.add(((Builder)((Builder)new Builder().name("延迟")).min(0).sliderMax(max).defaultValue(defaultDelay)).build());
   }

   protected boolean checkAndDecrement() {
      if (this.delayTimer > 0) {
         this.delayTimer--;
         return false;
      } else {
         return true;
      }
   }

   protected PlayerInventory getPlayerInventory() {
      return this.mc.player.getInventory();
   }

   protected ItemStack getItemStack(int slot) {
      return this.getPlayerInventory().getStack(slot);
   }

   protected ItemStack getItemStackBySlotId(int slotId) {
      ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
      return screenHandler.getSlot(slotId).getStack();
   }

   protected ItemStack nextPlayerStack(Predicate<ItemStack> predicate) {
      int startSlot = this.curPlayerSlot;
      int max = this.getPlayerMainSize() - 1;

      do {
         if (this.curPlayerSlot >= max) {
            this.curPlayerSlot = 0;
         } else {
            this.curPlayerSlot++;
         }

         ItemStack itemStack = this.getItemStack(this.curPlayerSlot);
         if (!itemStack.isEmpty() && predicate.test(itemStack)) {
            return itemStack;
         }
      } while (this.curPlayerSlot != startSlot);

      return ItemStack.EMPTY;
   }

   protected ItemStack nextScreenStack(Predicate<ItemStack> predicate) {
      ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
      int max = this.getScreenMainSize() - 1;
      int startSlot = this.curScreenSlot;

      do {
         if (this.curScreenSlot >= max) {
            this.curScreenSlot = 0;
         } else {
            this.curScreenSlot++;
         }

         ItemStack itemStack = screenHandler.getSlot(this.curScreenSlot).getStack();
         if (!itemStack.isEmpty() && predicate.test(itemStack)) {
            return itemStack;
         }
      } while (this.curScreenSlot != startSlot);

      return ItemStack.EMPTY;
   }

   protected boolean hasScreenFull() {
      ScreenHandler screenHandler = this.mc.player.currentScreenHandler;
      int size = this.getScreenMainSize();

      for (int i = 0; i < size; i++) {
         ItemStack stack = screenHandler.getSlot(i).getStack();
         if (stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   protected void openChest(BlockPos blockPos, Consumer<Inventory> inventoryConsumer) {
      if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
         this.tryRotateAndOpen(blockPos, null);
      } else if (this.mc.player.currentScreenHandler instanceof GenericContainerScreenHandler screenHandler) {
         this.openStatus = Steps.IDLE;
         Inventory inventory = screenHandler.getInventory();
         inventoryConsumer.accept(inventory);
      } else {
         this.openStatus = Steps.IDLE;
         HeInvUtils.closeCurScreen();
         this.warning("打开的容器错误", new Object[0]);
         this.setDelay();
      }
   }

   protected void openKit(BlockPos blockPos, Consumer<ShulkerBoxScreenHandler> screenHandlerConsumer) {
      this.openKit(blockPos, null, screenHandlerConsumer);
   }

   protected void openKit(BlockPos blockPos, Direction direction, Consumer<ShulkerBoxScreenHandler> screenHandlerConsumer) {
      if (this.mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
         BlockState blockState = this.mc.world.getBlockState(blockPos);
         if (blockState.getBlock() instanceof ShulkerBoxBlock) {
            this.tryRotateAndOpen(blockPos, direction);
         }
      } else if (this.mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler screenHandler) {
         this.openStatus = Steps.IDLE;
         screenHandlerConsumer.accept(screenHandler);
      } else {
         this.openStatus = Steps.IDLE;
         HeInvUtils.closeCurScreen();
         this.warning("打开的容器错误", new Object[0]);
         this.setDelay();
      }
   }

   protected void rotateAndOpen(BlockPos blockPos) {
      this.rotateAndOpen(blockPos, null);
   }

   protected void rotateAndOpen(BlockPos blockPos, Direction direction) {
      Direction finalDirection = direction == null ? BlockUtils.getDirection(blockPos) : direction;
      Vec3i vector = finalDirection.getVector();
      double offset = 0.45;
      Vec3d vec3d = new Vec3d(
         blockPos.getX() + 0.5 + vector.getX() * offset,
         blockPos.getY() + 0.5 + vector.getY() * offset,
         blockPos.getZ() + 0.5 + vector.getZ() * offset
      );
      HeRotationUtils.rotate(vec3d, () -> HeBlockUtils.open(blockPos, finalDirection, vec3d));
   }

   private void tryRotateAndOpen(BlockPos blockPos, Direction direction) {
      long curTime = System.currentTimeMillis();
      if (this.openStatus != Steps.IDLE && curTime - this.lastOpenTime <= 1000L) {
         this.warning("打开没有反应...", new Object[0]);
      } else {
         this.rotateAndOpen(blockPos, direction);
         this.openStatus = Steps.OPENING;
         this.lastOpenTime = curTime;
      }

      this.setDelay();
   }

   protected void rotateAndOpenWithOffset(BlockPos blockPos, Direction direction) {
      Vec3d vec3d;
      if (direction == Direction.UP) {
         double playerX = this.mc.player.getX();
         double playerZ = this.mc.player.getZ();
         double blockCenterX = blockPos.getX() + 0.5;
         double blockCenterZ = blockPos.getZ() + 0.5;
         double distX = Math.abs(playerX - blockCenterX);
         double distZ = Math.abs(playerZ - blockCenterZ);
         double offset = 0.4;
         double hitX = 0.5;
         double hitZ = 0.5;
         if (distX > distZ) {
            hitX = playerX > blockCenterX ? 0.5 + offset : 0.5 - offset;
         } else {
            hitZ = playerZ > blockCenterZ ? 0.5 + offset : 0.5 - offset;
         }

         double hitY = blockPos.getY() + 0.95;
         vec3d = new Vec3d(blockPos.getX() + hitX, hitY, blockPos.getZ() + hitZ);
      } else {
         Vec3i vector = direction.getVector();
         double offset = 0.5;
         vec3d = new Vec3d(
            blockPos.getX() + 0.5 + vector.getX() * offset,
            blockPos.getY() + 0.5 + vector.getY() * offset,
            blockPos.getZ() + 0.5 + vector.getZ() * offset
         );
      }

      HeRotationUtils.rotate(vec3d, () -> HeBlockUtils.open(blockPos, direction, vec3d));
   }

   protected void interactEntity(Entity entity) {
      Vec3d playerPos = this.mc.player.getEntityPos();
      Vec3d entityPos = entity.getEntityPos();
      EntityHitResult entityHitResult = ProjectileUtil.raycast(
         this.mc.player, playerPos, entityPos, entity.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(entityPos)
      );
      if (entityHitResult == null) {
         HeRotationUtils.rotate(entity.getEyePos(), () -> this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND));
      } else {
         HeRotationUtils.rotate(entityHitResult.getPos(), () -> {
            ActionResult actionResult = this.mc.interactionManager.interactEntityAtLocation(this.mc.player, entity, entityHitResult, Hand.MAIN_HAND);
            if (!actionResult.isAccepted()) {
               this.mc.interactionManager.interactEntity(this.mc.player, entity, Hand.MAIN_HAND);
            }
         });
      }
   }

   protected boolean swapToMainHand(int slot) {
      if (slot != this.getMainSlot()) {
         HeInvUtils.swap(slot, this.getMainSlot());
         this.setDelay();
         return true;
      } else {
         return false;
      }
   }

   protected int getMainSlot() {
      return HeInvUtils.getMainSlot();
   }

   protected boolean isReady() {
      return this.mc.player != null && this.mc.world != null;
   }

   protected long mcTime() {
      return this.mc.world.getTimeOfDay();
   }

   protected long mcDay() {
      return this.mc.world.getTimeOfDay() / 24000L;
   }

   protected long mcTimeOfDay() {
      return this.mc.world.getTimeOfDay() % 24000L;
   }

   protected void setDelay() {
      this.delayTimer = (Integer)this.delay.get();
   }

   protected void setDelay(int delay) {
      this.delayTimer = delay;
   }

   public int getDelayTimer() {
      return this.delayTimer;
   }

   public int getPlayerMainSize() {
      return this.mc.player.getInventory().getMainStacks().size();
   }

   public int getScreenMainSize() {
      return this.mc.player.currentScreenHandler instanceof PlayerScreenHandler
         ? this.mc.player.currentScreenHandler.slots.size() - this.getPlayerMainSize() - 1
         : this.mc.player.currentScreenHandler.slots.size() - this.getPlayerMainSize();
   }

   public boolean isContainer(int slotId) {
      return slotId < this.getScreenMainSize();
   }

   public boolean isContainerFull() {
      int n = this.getScreenMainSize();
      ScreenHandler screenHandler = this.mc.player.currentScreenHandler;

      for (int i = 0; i < n; i++) {
         ItemStack itemStack = screenHandler.getSlot(i).getStack();
         if (itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public boolean isInvFull() {
      int n = this.getPlayerMainSize();
      PlayerInventory playerInventory = this.getPlayerInventory();

      for (int i = 0; i < n; i++) {
         ItemStack itemStack = playerInventory.getStack(i);
         if (itemStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void initCurPlayerSlot() {
      this.curPlayerSlot = this.getPlayerMainSize() - 1;
   }

   public int getCurPlayerSlot() {
      return this.curPlayerSlot;
   }

   public void initCurScreenSlot() {
      this.curPlayerSlot = this.getScreenMainSize() - 1;
   }

   public int getCurScreenSlot() {
      return this.curScreenSlot;
   }
}
