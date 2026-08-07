package com.xiaohe66.mc.meteor.lotus.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BepRotationUtils {
   private static BepRotationUtils INSTANCE;
   private final List<BepRotationUtils.Rotation> requests = new CopyOnWriteArrayList<>();
   private float serverYaw;
   private float serverPitch;
   private float lastServerYaw;
   private float lastServerPitch;
   private float prevYaw;
   private float prevPitch;
   private float prevJumpYaw;
   private boolean rotate;
   private boolean webJumpFix;
   private boolean preJumpFix;
   private BepRotationUtils.Rotation rotation;
   private int rotateTicks;
   private boolean movementFix = true;
   private boolean mouseSensFix = true;
   private int preserveTicks = 3;
   private boolean webJumpFixEnabled = true;

   private BepRotationUtils() {
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   public static BepRotationUtils getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new BepRotationUtils();
      }

      return INSTANCE;
   }

   @EventHandler
   public void onPacketSend(Send event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
            float packetYaw = packet.getYRot(0.0F);
            float packetPitch = packet.getXRot(0.0F);
            this.serverYaw = packetYaw;
            this.serverPitch = packetPitch;
         }
      }
   }

   @EventHandler(priority = -200)
   public void onTick(Pre event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         this.webJumpFix = this.isInWeb();
         this.lastServerYaw = this.serverYaw;
         this.lastServerPitch = this.serverPitch;
         if (this.rotation != null) {
            this.rotateTicks++;
         }

         this.requests.removeIf(req -> req == null);
         if (this.requests.isEmpty()) {
            if (this.isDoneRotating()) {
               this.rotation = null;
               this.rotate = false;
            }
         } else {
            BepRotationUtils.Rotation request = this.getRotationRequest();
            if (request == null) {
               if (this.isDoneRotating()) {
                  this.rotation = null;
                  this.rotate = false;
                  return;
               }
            } else {
               this.rotation = request;
               this.rotateTicks = 0;
               this.rotate = true;
            }

            if (this.rotation != null && this.rotate) {
               this.applyRotation();
            }
         }
      }
   }

   public void setRotation(BepRotationUtils.Rotation rotation) {
      if (this.mouseSensFix) {
         double fix = Math.pow((Double)MeteorClient.mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
         rotation.setYaw((float)(rotation.getYaw() - (rotation.getYaw() - this.serverYaw) % fix));
         rotation.setPitch((float)(rotation.getPitch() - (rotation.getPitch() - this.serverPitch) % fix));
      }

      if (rotation.getPriority() == Integer.MAX_VALUE) {
         this.rotation = rotation;
      }

      this.requests.removeIf(r -> r.getPriority() == rotation.getPriority());
      this.requests.add(rotation);
   }

   public void setRotationClient(float yaw, float pitch) {
      if (MeteorClient.mc.player != null) {
         MeteorClient.mc.player.setYRot(yaw);
         MeteorClient.mc.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
      }
   }

   public void setRotationSilent(float yaw, float pitch) {
      this.setRotationSilent(yaw, pitch, Integer.MAX_VALUE);
   }

   public void setRotationSilent(float yaw, float pitch, int priority) {
      this.setRotation(new BepRotationUtils.Rotation(priority, yaw, pitch, true));
      MeteorClient.mc
         .getConnection()
         .send(
            new PosRot(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               yaw,
               pitch,
               MeteorClient.mc.player.onGround(),
               false
            )
         );
   }

   public void setRotationSilentSync() {
      float yaw = MeteorClient.mc.player.getYRot();
      float pitch = MeteorClient.mc.player.getXRot();
      this.setRotation(new BepRotationUtils.Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      MeteorClient.mc
         .getConnection()
         .send(
            new PosRot(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               yaw,
               pitch,
               MeteorClient.mc.player.onGround(),
               false
            )
         );
   }

   private void applyRotation() {
      if (this.rotation != null) {
         this.removeRotation(this.rotation);
         this.rotate = false;
         if (this.rotation.isSnap()) {
            this.rotation = null;
         }
      }
   }

   public boolean removeRotation(BepRotationUtils.Rotation request) {
      return this.requests.remove(request);
   }

   public void clearRotations() {
      this.requests.clear();
      this.rotation = null;
      this.rotate = false;
      this.rotateTicks = 0;
   }

   public void clearRotationsByPriority(int priority) {
      this.requests.removeIf(req -> req.getPriority() == priority);
      if (this.rotation != null && this.rotation.getPriority() == priority) {
         this.rotation = null;
         this.rotate = false;
      }
   }

   public boolean isRotationBlocked(int priority) {
      return this.rotation != null && priority < this.rotation.getPriority();
   }

   public boolean isDoneRotating() {
      return this.rotateTicks > this.preserveTicks;
   }

   public boolean isRotating() {
      return this.rotation != null;
   }

   public float getRotationYaw() {
      return this.rotation != null ? this.rotation.getYaw() : MeteorClient.mc.player.getYRot();
   }

   public float getRotationPitch() {
      return this.rotation != null ? this.rotation.getPitch() : MeteorClient.mc.player.getXRot();
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getWrappedYaw() {
      return Mth.wrapDegrees(this.serverYaw);
   }

   public float getServerPitch() {
      return this.serverPitch;
   }

   public float getLastServerYaw() {
      return this.lastServerYaw;
   }

   public float getLastServerPitch() {
      return this.lastServerPitch;
   }

   private BepRotationUtils.Rotation getRotationRequest() {
      BepRotationUtils.Rotation rotationRequest = null;
      int priority = 0;

      for (BepRotationUtils.Rotation request : this.requests) {
         if (request.getPriority() > priority) {
            rotationRequest = request;
            priority = request.getPriority();
         }
      }

      return rotationRequest;
   }

   private boolean isInWeb() {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         try {
            return MeteorClient.mc.level.getBlockCollisions(MeteorClient.mc.player, MeteorClient.mc.player.getBoundingBox()).iterator().hasNext();
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean getMovementFix() {
      return this.movementFix;
   }

   public void setMovementFix(boolean movementFix) {
      this.movementFix = movementFix;
   }

   public boolean getMouseSensFix() {
      return this.mouseSensFix;
   }

   public void setMouseSensFix(boolean mouseSensFix) {
      this.mouseSensFix = mouseSensFix;
   }

   public int getPreserveTicks() {
      return this.preserveTicks;
   }

   public void setPreserveTicks(int preserveTicks) {
      this.preserveTicks = preserveTicks;
   }

   public boolean getWebJumpFixEnabled() {
      return this.webJumpFixEnabled;
   }

   public void setWebJumpFixEnabled(boolean webJumpFixEnabled) {
      this.webJumpFixEnabled = webJumpFixEnabled;
   }

   public static float[] getRotationsTo(Vec3 src, Vec3 dest) {
      float yaw = (float)(Math.toDegrees(Math.atan2(dest.subtract(src).z, dest.subtract(src).x)) - 90.0);
      float pitch = (float)Math.toDegrees(
         -Math.atan2(dest.subtract(src).y, Math.hypot(dest.subtract(src).x, dest.subtract(src).z))
      );
      return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
   }

   public static float[] getRotationsTo(Entity entity, BepRotationUtils.HitVector hitVector) {
      Vec3 targetPos = getHitVector(entity, hitVector);
      return getRotationsTo(MeteorClient.mc.player.getEyePosition(), targetPos);
   }

   public static Vec3 getHitVector(Entity entity, BepRotationUtils.HitVector hitVector) {
      Vec3 feetPos = entity.position();

      return switch (hitVector) {
         case FEET -> feetPos;
         case TORSO -> feetPos.add(0.0, entity.getBbHeight() / 2.0F, 0.0);
         case EYES -> entity.getEyePosition();
         case CLOSEST -> {
            Vec3 eyePos = MeteorClient.mc.player.getEyePosition();
            Vec3 torsoPos = feetPos.add(0.0, entity.getBbHeight() / 2.0F, 0.0);
            Vec3 eyesPos = entity.getEyePosition();
            double feetDist = eyePos.distanceToSqr(feetPos);
            double torsoDist = eyePos.distanceToSqr(torsoPos);
            double eyesDist = eyePos.distanceToSqr(eyesPos);
            yield feetDist <= torsoDist && feetDist <= eyesDist ? feetPos : (torsoDist <= eyesDist ? torsoPos : eyesPos);
         }
      };
   }

   public static float[] smooth(float[] target, float[] previous, float rotationSpeed) {
      float speed = (1.0F - Mth.clamp(rotationSpeed / 100.0F, 0.1F, 0.9F)) * 10.0F;
      float[] rotations = new float[]{
         previous[0] + (float)(-getAngleDifference(previous[0], target[0]) / speed), previous[1] + -(previous[1] - target[1]) / speed
      };
      rotations[1] = Mth.clamp(rotations[1], -90.0F, 90.0F);
      return rotations;
   }

   public static double getAngleDifference(float client, float yaw) {
      return ((client - yaw) % 360.0 + 540.0) % 360.0 - 180.0;
   }

   public static double getAnglePitchDifference(float client, float pitch) {
      return ((client - pitch) % 180.0 + 270.0) % 180.0 - 90.0;
   }

   public static Vec3 getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      return new Vec3(i * j, -k, h * j);
   }

   public static boolean canSeePosition(Vec3 from, Vec3 to) {
      BlockHitResult result = MeteorClient.mc
         .level
         .clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, MeteorClient.mc.player));
      return result == null || result.getBlockPos().equals(BlockPos.containing(to));
   }

   public static boolean isInFov(Vec3 from, Vec3 to, float fov) {
      if (fov >= 180.0F) {
         return true;
      }

      float[] rotations = getRotationsTo(from, to);
      float yawDiff = Mth.wrapDegrees(MeteorClient.mc.player.getYRot() - rotations[0]);
      return Math.abs(yawDiff) <= fov;
   }

   public static float wrapDegrees(float degrees) {
      return Mth.wrapDegrees(degrees);
   }

   public enum HitVector {
      FEET,
      TORSO,
      EYES,
      CLOSEST;
   }

   public static class Rotation {
      private final int priority;
      private float yaw;
      private float pitch;
      private boolean snap;

      public Rotation(int priority, float yaw, float pitch, boolean snap) {
         this.priority = priority;
         this.yaw = yaw;
         this.pitch = pitch;
         this.snap = snap;
      }

      public Rotation(int priority, float yaw, float pitch) {
         this(priority, yaw, pitch, false);
      }

      public int getPriority() {
         return this.priority;
      }

      public void setYaw(float yaw) {
         this.yaw = yaw;
      }

      public void setPitch(float pitch) {
         this.pitch = pitch;
      }

      public float getYaw() {
         return this.yaw;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setSnap(boolean snap) {
         this.snap = snap;
      }

      public boolean isSnap() {
         return this.snap;
      }
   }
}
