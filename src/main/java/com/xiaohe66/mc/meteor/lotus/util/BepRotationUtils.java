package com.xiaohe66.mc.meteor.lotus.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent.Send;
import meteordevelopment.meteorclient.events.world.TickEvent.Pre;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

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
      if (MeteorClient.mc.player != null && MeteorClient.mc.world != null) {
         if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook()) {
            float packetYaw = packet.getYaw(0.0F);
            float packetPitch = packet.getPitch(0.0F);
            this.serverYaw = packetYaw;
            this.serverPitch = packetPitch;
         }
      }
   }

   @EventHandler(priority = -200)
   public void onTick(Pre event) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.world != null) {
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
         double fix = Math.pow((Double)MeteorClient.mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
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
         MeteorClient.mc.player.setYaw(yaw);
         MeteorClient.mc.player.setPitch(MathHelper.clamp(pitch, -90.0F, 90.0F));
      }
   }

   public void setRotationSilent(float yaw, float pitch) {
      this.setRotationSilent(yaw, pitch, Integer.MAX_VALUE);
   }

   public void setRotationSilent(float yaw, float pitch, int priority) {
      this.setRotation(new BepRotationUtils.Rotation(priority, yaw, pitch, true));
      MeteorClient.mc
         .getNetworkHandler()
         .sendPacket(
            new Full(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               yaw,
               pitch,
               MeteorClient.mc.player.isOnGround(),
               false
            )
         );
   }

   public void setRotationSilentSync() {
      float yaw = MeteorClient.mc.player.getYaw();
      float pitch = MeteorClient.mc.player.getPitch();
      this.setRotation(new BepRotationUtils.Rotation(Integer.MAX_VALUE, yaw, pitch, true));
      MeteorClient.mc
         .getNetworkHandler()
         .sendPacket(
            new Full(
               MeteorClient.mc.player.getX(),
               MeteorClient.mc.player.getY(),
               MeteorClient.mc.player.getZ(),
               yaw,
               pitch,
               MeteorClient.mc.player.isOnGround(),
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
      return this.rotation != null ? this.rotation.getYaw() : MeteorClient.mc.player.getYaw();
   }

   public float getRotationPitch() {
      return this.rotation != null ? this.rotation.getPitch() : MeteorClient.mc.player.getPitch();
   }

   public float getServerYaw() {
      return this.serverYaw;
   }

   public float getWrappedYaw() {
      return MathHelper.wrapDegrees(this.serverYaw);
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
      if (MeteorClient.mc.player != null && MeteorClient.mc.world != null) {
         try {
            return MeteorClient.mc.world.getBlockCollisions(MeteorClient.mc.player, MeteorClient.mc.player.getBoundingBox()).iterator().hasNext();
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

   public static float[] getRotationsTo(Vec3d src, Vec3d dest) {
      float yaw = (float)(Math.toDegrees(Math.atan2(dest.subtract(src).z, dest.subtract(src).x)) - 90.0);
      float pitch = (float)Math.toDegrees(
         -Math.atan2(dest.subtract(src).y, Math.hypot(dest.subtract(src).x, dest.subtract(src).z))
      );
      return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
   }

   public static float[] getRotationsTo(Entity entity, BepRotationUtils.HitVector hitVector) {
      Vec3d targetPos = getHitVector(entity, hitVector);
      return getRotationsTo(MeteorClient.mc.player.getEyePos(), targetPos);
   }

   public static Vec3d getHitVector(Entity entity, BepRotationUtils.HitVector hitVector) {
      Vec3d feetPos = entity.getEntityPos();

      return switch (hitVector) {
         case FEET -> feetPos;
         case TORSO -> feetPos.add(0.0, entity.getHeight() / 2.0F, 0.0);
         case EYES -> entity.getEyePos();
         case CLOSEST -> {
            Vec3d eyePos = MeteorClient.mc.player.getEyePos();
            Vec3d torsoPos = feetPos.add(0.0, entity.getHeight() / 2.0F, 0.0);
            Vec3d eyesPos = entity.getEyePos();
            double feetDist = eyePos.squaredDistanceTo(feetPos);
            double torsoDist = eyePos.squaredDistanceTo(torsoPos);
            double eyesDist = eyePos.squaredDistanceTo(eyesPos);
            yield feetDist <= torsoDist && feetDist <= eyesDist ? feetPos : (torsoDist <= eyesDist ? torsoPos : eyesPos);
         }
      };
   }

   public static float[] smooth(float[] target, float[] previous, float rotationSpeed) {
      float speed = (1.0F - MathHelper.clamp(rotationSpeed / 100.0F, 0.1F, 0.9F)) * 10.0F;
      float[] rotations = new float[]{
         previous[0] + (float)(-getAngleDifference(previous[0], target[0]) / speed), previous[1] + -(previous[1] - target[1]) / speed
      };
      rotations[1] = MathHelper.clamp(rotations[1], -90.0F, 90.0F);
      return rotations;
   }

   public static double getAngleDifference(float client, float yaw) {
      return ((client - yaw) % 360.0 + 540.0) % 360.0 - 180.0;
   }

   public static double getAnglePitchDifference(float client, float pitch) {
      return ((client - pitch) % 180.0 + 270.0) % 180.0 - 90.0;
   }

   public static Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * (float) (Math.PI / 180.0);
      float g = -yaw * (float) (Math.PI / 180.0);
      float h = MathHelper.cos(g);
      float i = MathHelper.sin(g);
      float j = MathHelper.cos(f);
      float k = MathHelper.sin(f);
      return new Vec3d(i * j, -k, h * j);
   }

   public static boolean canSeePosition(Vec3d from, Vec3d to) {
      BlockHitResult result = MeteorClient.mc
         .world
         .raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, MeteorClient.mc.player));
      return result == null || result.getBlockPos().equals(BlockPos.ofFloored(to));
   }

   public static boolean isInFov(Vec3d from, Vec3d to, float fov) {
      if (fov >= 180.0F) {
         return true;
      }

      float[] rotations = getRotationsTo(from, to);
      float yawDiff = MathHelper.wrapDegrees(MeteorClient.mc.player.getYaw() - rotations[0]);
      return Math.abs(yawDiff) <= fov;
   }

   public static float wrapDegrees(float degrees) {
      return MathHelper.wrapDegrees(degrees);
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
