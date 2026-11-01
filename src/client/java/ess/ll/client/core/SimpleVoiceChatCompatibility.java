package ess.ll.client.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class SimpleVoiceChatCompatibility {
   public static boolean available;
   private static Field playerStateManagerField;
   private static Method isPlayerDisabledMethod;
   private static Method isPlayerDisconnectedMethod;
   private static Method instanceMethod;
   private static Method getClientMethod;
   private static Method getTalkCacheMethod;
   private static Method isTalkingMethod;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";
   private static final String CLIENT_MANAGER_CLASS = "de.maxhenkel.voicechat.voice.client.ClientManager";
   private static final String CLIENT_MANAGER_CLASS_NAME = "de.maxhenkel.voicechat.voice.client.ClientManager";
   private static final String INSTANCE_METHOD = "instance";
   private static final String STATE_MANAGER_FIELD = "playerStateManager";
   private static final String IS_DISABLED_METHOD = "isPlayerDisabled";
   private static final String IS_DISCONNECTED_METHOD = "isPlayerDisconnected";
   private static final String GET_CLIENT_METHOD = "getClient";
   private static final String GET_TALK_CACHE_METHOD = "getTalkCache";
   private static final String IS_TALKING_METHOD = "isTalking";

   static {
      boolean found = false;

      try {
         Class.forName(CLIENT_MANAGER_CLASS);
         found = true;
      } catch (ClassNotFoundException e) {
      }

      available = found;
      if (available) {
         try {
            var managerClass = Class.forName(CLIENT_MANAGER_CLASS_NAME);
            instanceMethod = managerClass.getMethod(INSTANCE_METHOD);
            playerStateManagerField = managerClass.getDeclaredField(STATE_MANAGER_FIELD);
            playerStateManagerField.setAccessible(true);
            var stateClass = playerStateManagerField.getType();
            isPlayerDisabledMethod = stateClass.getMethod(IS_DISABLED_METHOD, UUID.class);
            isPlayerDisconnectedMethod = stateClass.getMethod(IS_DISCONNECTED_METHOD, UUID.class);
            getClientMethod = managerClass.getDeclaredMethod(GET_CLIENT_METHOD);
            var clientClass = getClientMethod.getReturnType();
            getTalkCacheMethod = clientClass.getDeclaredMethod(GET_TALK_CACHE_METHOD);
            var talkCacheClass = getTalkCacheMethod.getReturnType();
            isTalkingMethod = talkCacheClass.getDeclaredMethod(IS_TALKING_METHOD, UUID.class);
         } catch (Exception e) {
            available = false;
         }
      }
   }

   public static boolean isReady() {
      return available
         && instanceMethod != null
         && playerStateManagerField != null
         && isPlayerDisabledMethod != null
         && isPlayerDisconnectedMethod != null
         && getClientMethod != null
         && getTalkCacheMethod != null
         && isTalkingMethod != null;
   }

   private static Object stateManager() {
      try {
         Object object = instanceMethod.invoke(null);
         return object == null ? null : playerStateManagerField.get(object);
      } catch (Exception e) {
         return null;
      }
   }

   public static boolean isDisconnected(UUID uuid) {
      if (!isReady()) {
         return false;
      }

      try {
         Object object = stateManager();
         return object == null ? false : (Boolean)isPlayerDisconnectedMethod.invoke(object, uuid);
      } catch (Exception e) {
         return false;
      }
   }

   private SimpleVoiceChatCompatibility() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static boolean isTalking(UUID uuid) {
      if (!isReady()) {
         return false;
      }

      try {
         Object object = instanceMethod.invoke(null);
         if (object == null) {
            return false;
         }

         Object object2 = getClientMethod.invoke(object);
         if (object2 == null) {
            return false;
         }

         Object object3 = getTalkCacheMethod.invoke(object2);
         return object3 == null ? false : (Boolean)isTalkingMethod.invoke(object3, uuid);
      } catch (Exception e) {
         return false;
      }
   }

   public static boolean isDisabled(UUID uuid) {
      if (!isReady()) {
         return false;
      }

      try {
         Object object = stateManager();
         return object == null ? false : (Boolean)isPlayerDisabledMethod.invoke(object, uuid);
      } catch (Exception e) {
         return false;
      }
   }
}
