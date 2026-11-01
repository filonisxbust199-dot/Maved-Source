package ess.ll.client.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomUtils {
   private static final SecureRandom SECURE = createSecureRandom();
   private static final AtomicLong SEED_COUNTER = new AtomicLong(mix64(System.nanoTime() ^ System.currentTimeMillis()));
   private static final ThreadLocal<RandomUtils.GaussianState> GAUSSIAN_STATE = ThreadLocal.withInitial(RandomUtils.GaussianState::new);
   private static final String BOUND_POSITIVE_MESSAGE = "bound must be > 0";
   private static final String BOUND_ORDER_LONG_MESSAGE = "bound must be greater than origin";
   private static final double DOUBLE_UNIT = 1.110223E-16F;
   private static final String BOUND_ORDER_DOUBLE_MESSAGE = "bound must be greater than origin";
   private static final String BOUND_ORDER_FLOAT_MESSAGE = "bound must be greater than origin";
   private static final double MIN_UNIFORM = Double.MIN_NORMAL;
   private static final double BOX_MULLER_FACTOR = -2.0;
   private static final double TWO_PI = Math.PI * 2;
   private static final String GAUSSIAN_ARGS_MESSAGE = "standardDeviation must be > 0 and arguments must not be NaN";
   private static final String SIZE_MESSAGE = "size must be >= 0";
   private static final String TARGET_NAME = "target";
   private static final long GOLDEN_GAMMA = -7046029254386353131L;
   private static final String SECURE_TARGET_NAME = "target";
   private static final long MIX_1 = -4658895280553007687L;
   private static final long MIX_2 = -7723592293110705685L;
   private static final String UTILITY_CLASS_MESSAGE = "This is a utility class and cannot be instantiated";

   public static double nextDouble() {
      return (nextLong() >>> 11) * DOUBLE_UNIT;
   }

   public static void nextSecureBytes(byte[] bytes) {
      Objects.requireNonNull(bytes, SECURE_TARGET_NAME);
      SECURE.nextBytes(bytes);
   }

   public static int nextInt() {
      return (int)nextLong();
   }

   private static SecureRandom createSecureRandom() {
      try {
         return SecureRandom.getInstanceStrong();
      } catch (NoSuchAlgorithmException e) {
         return new SecureRandom();
      }
   }

   private static long mix64(long value) {
      value = (value ^ value >>> 30) * MIX_1;
      value = (value ^ value >>> 27) * MIX_2;
      return value ^ value >>> 31;
   }

   public static float nextFloat() {
      return (float)nextDouble();
   }

   public static double nextGaussian(double mean, double deviation) {
      if (deviation > 0.0 && !Double.isNaN(mean) && !Double.isNaN(deviation)) {
         return mean + deviation * nextStandardGaussian();
      } else {
         throw new IllegalArgumentException(GAUSSIAN_ARGS_MESSAGE);
      }
   }

   private RandomUtils() {
      throw new UnsupportedOperationException(UTILITY_CLASS_MESSAGE);
   }

   public static byte[] randomBytes(int size) {
      if (size < 0) {
         throw new IllegalArgumentException(SIZE_MESSAGE);
      }

      byte[] bytes = new byte[size];
      nextBytes(bytes);
      return bytes;
   }

   public static int nextInt(int origin, int bound) {
      return (int)nextLong(origin, bound);
   }

   public static void nextBytes(byte[] bytes) {
      Objects.requireNonNull(bytes, TARGET_NAME);
      SECURE.nextBytes(bytes);
      long state = entropy();

      for (int i = 0; i < bytes.length; i++) {
         state = mix64(state + GOLDEN_GAMMA + i);
         bytes[i] ^= (byte)state;
      }
   }

   public static double nextDouble(double origin, double bound) {
      if (origin >= bound) {
         throw new IllegalArgumentException(BOUND_ORDER_DOUBLE_MESSAGE);
      } else {
         return origin + (bound - origin) * nextDouble();
      }
   }

   public static long nextLong(long origin, long bound) {
      if (origin >= bound) {
         throw new IllegalArgumentException(BOUND_ORDER_LONG_MESSAGE);
      }

      long value = nextLong();
      long range = bound - origin;
      long mask = range - 1L;
      if ((range & mask) == 0L) {
         value = (value & mask) + origin;
      } else if (range > 0L) {
         value += origin;
      } else {
         while (value < origin || value >= bound) {
            value = nextLong();
         }
      }

      return value;
   }

   public static long nextLong(long bound) {
      if (bound <= 0L) {
         throw new IllegalArgumentException(BOUND_POSITIVE_MESSAGE);
      }

      long value = nextLong();
      long mask = bound - 1L;
      if ((bound & mask) == 0L) {
         return value & mask;
      }

      long bits = value >>> 1;

      while (bits + mask - (value = bits % bound) < 0L) {
         bits = nextLong() >>> 1;
      }

      return value;
   }

   public static double nextStandardGaussian() {
      RandomUtils.GaussianState state = GAUSSIAN_STATE.get();
      if (state.hasSpare) {
         state.hasSpare = false;
         return state.spare;
      }

      double u1;
      do {
         u1 = nextDouble();
      } while (u1 <= MIN_UNIFORM);

      double u2 = nextDouble();
      double radius = Math.sqrt(BOX_MULLER_FACTOR * Math.log(u1));
      double angle = TWO_PI * u2;
      state.spare = radius * Math.sin(angle);
      state.hasSpare = true;
      return radius * Math.cos(angle);
   }

   public static boolean nextBoolean() {
      return (nextLong() & 1L) == 1L;
   }

   public static long entropy() {
      long counter = SEED_COUNTER.getAndIncrement();
      long nanos = System.nanoTime();
      long millis = System.currentTimeMillis();
      long threadId = Thread.currentThread().threadId();
      long memory = Runtime.getRuntime().freeMemory() << 7 ^ Runtime.getRuntime().totalMemory();
      long identity = System.identityHashCode(Thread.currentThread());
      return mix64(counter ^ Long.rotateLeft(nanos, 17) ^ Long.rotateLeft(millis, 41) ^ threadId << 32 ^ memory ^ identity);
   }

   public static int nextInt(int bound) {
      return (int)nextLong(bound);
   }

   public static float nextFloat(float origin, float bound) {
      if (origin >= bound) {
         throw new IllegalArgumentException(BOUND_ORDER_FLOAT_MESSAGE);
      } else {
         return origin + (bound - origin) * nextFloat();
      }
   }

   public static long nextLong() {
      return mix64(SECURE.nextLong() ^ entropy());
   }

   private static final class GaussianState {
      private boolean hasSpare;
      private double spare;
   }
}
