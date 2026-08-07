package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.EnumSetting.Builder;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ElytraFlyPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSpeed = this.settings.createGroup("Speed");
   private final Setting<ElytraFlyPlus.Mode> mode = this.sgGeneral
      .add(((Builder)((Builder)((Builder)new Builder().name("模式")).description(".")).defaultValue(ElytraFlyPlus.Mode.Wasp)).build());
   private final Setting<Boolean> stopWater = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("水停止"))
                  .description("Doesn't modify movement while in water."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Boolean> stopLava = this.sgGeneral
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                     .name("岩浆停止"))
                  .description("Doesn't modify movement while in lava."))
               .defaultValue(true))
            .build()
      );
   private final Setting<Double> horizontal = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("水平速度"))
                  .description("每tick移动多少格."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> up = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("上升速度"))
                  .description("每tick移动多少格."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> speed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("速度"))
                  .description("每tick移动多少格."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<Double> upMultiplier = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("上升倍数"))
                  .description("How many times faster should we fly up."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<Double> down = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("下降速度"))
                  .description("每tick向下移动多少个方块"))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control || this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Boolean> smartFall = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("智能降落"))
                     .description("只有向下看的时候才会降落"))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> fallSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("下降速度"))
                  .description("每tick向下移动多少个方块"))
               .defaultValue(0.01)
               .min(0.0)
               .sliderRange(0.0, 1.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control || this.mode.get() == ElytraFlyPlus.Mode.Wasp))
            .build()
      );
   private final Setting<Double> constSpeed = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Const Speed"))
                  .description("Maximum speed for constantiam mode."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private final Setting<Double> constAcceleration = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)((meteordevelopment.meteorclient.settings.DoubleSetting.Builder)new meteordevelopment.meteorclient.settings.DoubleSetting.Builder()
                     .name("Const Acceleration"))
                  .description("Maximum speed for constantiam mode."))
               .defaultValue(1.0)
               .min(0.0)
               .sliderRange(0.0, 5.0)
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private final Setting<Boolean> constStop = this.sgSpeed
      .add(
         ((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)((meteordevelopment.meteorclient.settings.BoolSetting.Builder)new meteordevelopment.meteorclient.settings.BoolSetting.Builder()
                        .name("Const Stop"))
                     .description("Stops movement when no input."))
                  .defaultValue(true))
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Constantiam))
            .build()
      );
   private boolean moving;
   private float yaw;
   private float pitch;
   private float p;
   private double velocity;
   private int activeFor;

   public ElytraFlyPlus() {
      super(Const.CATEGORY, "Bo-鞘翅飞行+", "移植自BlackOut, 在xin服中可以向上平飞");
   }

   @EventHandler(priority = 200)
   private void onMove(PlayerMoveEvent event) {
      if (this.active()) {
         this.activeFor++;
         if (this.activeFor >= 5) {
            switch ((ElytraFlyPlus.Mode)this.mode.get()) {
               case Wasp:
                  this.waspTick(event);
                  break;
               case Control:
                  this.controlTick(event);
                  break;
               case Constantiam:
                  this.constantiamTick(event);
            }
         }
      }
   }

   private void constantiamTick(PlayerMoveEvent event) {
      Vec3 motion = this.getMotion(this.mc.player.getDeltaMovement());
      if (motion != null) {
         ((IVec3)event.movement).meteor$set(motion.x(), motion.y(), motion.z());
         event.movement = motion;
      }
   }

   private Vec3 getMotion(Vec3 velocity) {
      Vec2 vec2f = this.mc.player.input.getMoveVector();
      if (vec2f.y == 0.0F) {
         return this.constStop.get() ? new Vec3(0.0, 0.0, 0.0) : null;
      }

      boolean forward = vec2f.y > 0.0F;
      double yaw = Math.toRadians(this.mc.player.getYRot() + (forward ? 90 : -90));
      double x = Math.cos(yaw);
      double z = Math.sin(yaw);
      double maxAcc = this.calcAcceleration(velocity.x, velocity.z, x, z);
      double delta = Math.clamp(Mth.inverseLerp(velocity.horizontalDistance(), 0.0, 0.5), 0.0, 1.0);
      double acc = Math.min(maxAcc, (Double)this.constAcceleration.get() / 20.0 * (0.1 + delta * 0.9));
      return new Vec3(velocity.x() + x * acc, velocity.y(), velocity.z() + z * acc);
   }

   private double calcAcceleration(double vx, double vz, double x, double z) {
      double xz = x * x + z * z;
      return (
            Math.sqrt(xz * (Double)this.constSpeed.get() * (Double)this.constSpeed.get() - x * x * vz * vz - z * z * vx * vx + 2.0 * x * z * vx * vz)
               - x * vx
               - z * vz
         )
         / xz;
   }

   private void waspTick(PlayerMoveEvent event) {
      if (this.mc.player.isFallFlying()) {
         this.updateWaspMovement();
         this.pitch = this.mc.player.getXRot();
         double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
         double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
         double x = this.moving ? cos * (Double)this.horizontal.get() : 0.0;
         double y = -(Double)this.fallSpeed.get();
         double z = this.moving ? sin * (Double)this.horizontal.get() : 0.0;
         if ((Boolean)this.smartFall.get()) {
            y *= Math.abs(Math.sin(Math.toRadians(this.pitch)));
         }

         if (this.mc.options.keyShift.isDown() && !this.mc.options.keyJump.isDown()) {
            y = -(Double)this.down.get();
         }

         if (!this.mc.options.keyShift.isDown() && this.mc.options.keyJump.isDown()) {
            y = (Double)this.up.get();
         }

         ((IVec3)event.movement).meteor$set(x, y, z);
         this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
      }
   }

   private void updateWaspMovement() {
      float yaw = this.mc.player.getYRot();
      Vec2 vec2f = this.mc.player.input.getMoveVector();
      float f = vec2f.y;
      float s = vec2f.x;
      if (f > 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
      } else if (f < 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
      } else {
         this.moving = s != 0.0F;
         yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
      }

      this.yaw = yaw;
   }

   private void controlTick(PlayerMoveEvent event) {
      if (this.mc.player.isFallFlying()) {
         this.updateControlMovement();
         this.pitch = 0.0F;
         boolean movingUp = false;
         if (!this.mc.options.keyShift.isDown() && this.mc.options.keyJump.isDown() && this.velocity > (Double)this.speed.get() * 0.4) {
            this.p = (float)Math.min(this.p + 0.1 * (1.0F - this.p) * (1.0F - this.p) * (1.0F - this.p), 1.0);
            this.pitch = Math.max(Math.max(this.p, 0.0F) * -90.0F, -90.0F);
            movingUp = true;
            this.moving = false;
         } else {
            this.velocity = (Double)this.speed.get();
            this.p = -0.2F;
         }

         this.velocity = this.moving
            ? (Double)this.speed.get()
            : Math.min(this.velocity + Math.sin(Math.toRadians(this.pitch)) * 0.08, (Double)this.speed.get());
         double cos = Math.cos(Math.toRadians(this.yaw + 90.0F));
         double sin = Math.sin(Math.toRadians(this.yaw + 90.0F));
         double x = this.moving && !movingUp ? cos * (Double)this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * cos : 0.0);
         double y = this.pitch < 0.0F
            ? this.velocity * (Double)this.upMultiplier.get() * -Math.sin(Math.toRadians(this.pitch)) * this.velocity
            : -(Double)this.fallSpeed.get();
         double z = this.moving && !movingUp ? sin * (Double)this.speed.get() : (movingUp ? this.velocity * Math.cos(Math.toRadians(this.pitch)) * sin : 0.0);
         y *= Math.abs(Math.sin(Math.toRadians(movingUp ? this.pitch : this.mc.player.getXRot())));
         if (this.mc.options.keyShift.isDown() && !this.mc.options.keyJump.isDown()) {
            y = -(Double)this.down.get();
         }

         ((IVec3)event.movement).meteor$set(x, y, z);
         this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
      }
   }

   private void updateControlMovement() {
      float yaw = this.mc.player.getYRot();
      Vec2 vec2f = this.mc.player.input.getMoveVector();
      float f = vec2f.y;
      float s = vec2f.x;
      if (f > 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -45.0F : (s < 0.0F ? 45.0F : 0.0F);
      } else if (f < 0.0F) {
         this.moving = true;
         yaw += s > 0.0F ? -135.0F : (s < 0.0F ? 135.0F : 180.0F);
      } else {
         this.moving = s != 0.0F;
         yaw += s > 0.0F ? -90.0F : (s < 0.0F ? 90.0F : 0.0F);
      }

      this.yaw = yaw;
   }

   public boolean active() {
      if ((Boolean)this.stopWater.get() && this.mc.player.isInWater()) {
         this.activeFor = 0;
         return false;
      } else if ((Boolean)this.stopLava.get() && this.mc.player.isInLava()) {
         this.activeFor = 0;
         return false;
      } else {
         return this.mc.player.isFallFlying();
      }
   }

   public enum Mode {
      Wasp,
      Control,
      Constantiam;
   }
}
