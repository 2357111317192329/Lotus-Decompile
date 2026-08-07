package com.xiaohe66.mc.meteor.lotus.event;

public class ScreenCloseEvent extends Event {
   private static final ScreenCloseEvent INSTANCE = new ScreenCloseEvent();

   public static ScreenCloseEvent get() {
      return INSTANCE;
   }

   public ScreenCloseEvent() {
      super(Event.Stage.Pre);
   }
}
