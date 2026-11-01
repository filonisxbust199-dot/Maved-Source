package ess.ll.client.event;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class EventBus {
   private static final Map<Class<? extends Event>, EventBus.Subscriber[]> SUBSCRIBERS = new ConcurrentHashMap<>();
   private static final EventListener[] NO_LISTENERS = new EventListener[0];
   private static volatile int version = 0;
   private static final String LAMBDA_METHOD_NAME = "fire";
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static EventListener[] listenersOf(Event event) {
      EventBus.ListenerCache cache = event.listenerCache;
      if (cache != null && cache.version == version) {
         return cache.listeners;
      }

      EventBus.Subscriber[] subscribers = SUBSCRIBERS.get(event.getClass());
      EventListener[] listeners = NO_LISTENERS;
      if (subscribers != null) {
         listeners = new EventListener[subscribers.length];

         for (int i = 0; i < subscribers.length; i++) {
            listeners[i] = subscribers[i].listener;
         }
      }

      event.listenerCache = new EventBus.ListenerCache(version, listeners);
      return listeners;
   }

   private static boolean isNotSubscriber(Method method) {
      return method.getParameterCount() != 1 || !method.isAnnotationPresent(Subscribe.class);
   }

   private static EventListener createListener(Object object, Method method) {
      try {
         Lookup lookup = MethodHandles.privateLookupIn(object.getClass(), MethodHandles.lookup());
         MethodHandle methodHandle = lookup.unreflect(method);
         CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            LAMBDA_METHOD_NAME,
            MethodType.methodType(EventListener.class, object.getClass()),
            MethodType.methodType(void.class, Event.class),
            methodHandle,
            MethodType.methodType(void.class, method.getParameterTypes()[0])
         );
         return (EventListener)callSite.getTarget().invoke((Object)object);
      } catch (Throwable e) {
         try {
            if (!method.isAccessible()) {
               method.setAccessible(true);
            }
         } catch (Throwable ignored) {
         }

         return new EventBus.ReflectiveListener(object, method);
      }
   }

   public static void pruneEntries(boolean emptyOnly) {
      var iterator = SUBSCRIBERS.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry entry = (Entry)iterator.next();
         if (!emptyOnly || ((EventBus.Subscriber[])entry.getValue()).length == 0) {
            iterator.remove();
         }
      }
   }

   public static void register(Object object) {
      for (Method method : object.getClass().getDeclaredMethods()) {
         if (!isNotSubscriber(method)) {
            registerMethod(method, object);
         }
      }
   }

   private static void registerMethod(Method method, Object object) {
      Class<? extends Event> type = (Class<? extends Event>) method.getParameterTypes()[0];
      EventBus.Subscriber subscriber = new EventBus.Subscriber(object, method, method.getAnnotation(Subscribe.class).value());
      EventBus.Subscriber[] remaining = SUBSCRIBERS.get(type);
      if (remaining == null) {
         SUBSCRIBERS.put(type, new EventBus.Subscriber[]{subscriber});
         version++;
      } else {
         for (EventBus.Subscriber other : remaining) {
            if (other.isSame(subscriber)) {
               return;
            }
         }

         int insertAt = remaining.length;

         for (int i = 0; i < remaining.length; i++) {
            if (subscriber.getPriority() < remaining[i].getPriority()) {
               insertAt = i;
               break;
            }
         }

         EventBus.Subscriber[] updated = new EventBus.Subscriber[remaining.length + 1];
         System.arraycopy(remaining, 0, updated, 0, insertAt);
         updated[insertAt] = subscriber;
         System.arraycopy(remaining, insertAt, updated, insertAt + 1, remaining.length - insertAt);
         SUBSCRIBERS.put(type, updated);
         version++;
      }
   }

   private EventBus() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static void unregister(Object object) {
      var iterator = SUBSCRIBERS.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry entry = (Entry)iterator.next();
         EventBus.Subscriber[] value = (EventBus.Subscriber[])entry.getValue();
         int remaining = 0;

         for (EventBus.Subscriber subscriber : value) {
            if (!subscriber.isOwnedBy(object)) {
               remaining++;
            }
         }

         if (remaining != value.length) {
            if (remaining == 0) {
               iterator.remove();
            } else {
               EventBus.Subscriber[] subscribers = new EventBus.Subscriber[remaining];
               int index = 0;

               for (EventBus.Subscriber other : value) {
                  if (!other.isOwnedBy(object)) {
                     subscribers[index++] = other;
                  }
               }

               entry.setValue(subscribers);
            }
         }
      }

      version++;
      pruneEntries(true);
   }

   public static final class ListenerCache {
      private final int version;
      private final EventListener[] listeners;

      ListenerCache(int version, EventListener[] eventListeners) {
         this.version = version;
         this.listeners = eventListeners;
      }
   }

   private static final class ReflectiveListener implements EventListener {
      private final Object owner;
      private final Method method;

      @Override
      public void fire(Event event) {
         try {
            this.method.invoke(this.owner, event);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException invocationTargetException) {
         }
      }

      ReflectiveListener(Object object, Method method) {
         this.owner = object;
         this.method = method;
      }
   }

   private static final class Subscriber {
      private final Object owner;
      private final Method method;
      private final byte priority;
      private final EventListener listener;

      public boolean isSame(EventBus.Subscriber other) {
         return this.owner.equals(other.owner) && this.method.equals(other.method);
      }

      public byte getPriority() {
         return this.priority;
      }

      public Subscriber(Object object, Method method, byte priority) {
         this.owner = object;
         this.method = method;
         this.priority = priority;
         this.listener = EventBus.createListener(object, method);
      }

      public boolean isOwnedBy(Object object) {
         return this.owner.equals(object);
      }
   }
}
