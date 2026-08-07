package com.xiaohe66.mc.meteor.lotus.event;

public class Event {
   private final Event.Stage stage;
   private boolean cancel = false;

   public Event(Event.Stage stage) {
      this.stage = stage;
   }

   public void cancel() {
      this.setCancelled(true);
   }

   public boolean isCancel() {
      return this.cancel;
   }

   public void setCancelled(boolean cancel) {
      this.cancel = cancel;
   }

   public boolean isCancelled() {
      return this.cancel;
   }

   public Event.Stage getStage() {
      return this.stage;
   }

   public boolean isPost() {
      return this.stage == Event.Stage.Post;
   }

   public boolean isPre() {
      return this.stage == Event.Stage.Pre;
   }

   public enum Stage {
      Pre,
      Post;
   }
}
