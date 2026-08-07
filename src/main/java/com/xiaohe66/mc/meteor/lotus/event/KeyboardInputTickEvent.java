package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.world.entity.player.Input;

public class KeyboardInputTickEvent extends Event {
   private Input playerInput;

   public KeyboardInputTickEvent() {
      super(Event.Stage.Post);
   }

   public Input getPlayerInput() {
      return this.playerInput;
   }

   public void setPlayerInput(Input playerInput) {
      this.playerInput = playerInput;
   }
}
