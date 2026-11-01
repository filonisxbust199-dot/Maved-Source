package ess.ll.client.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class SortableArrayList<T> extends ArrayList<T> {
   private static final String NULL_INT_KEY_MESSAGE = "keyExtractor cannot be null";
   private static final int INT_SIGN_FLIP = Integer.MIN_VALUE;
   private static final String NULL_LONG_KEY_MESSAGE = "keyExtractor cannot be null";
   private static final long LONG_SIGN_FLIP = Long.MIN_VALUE;
   private static final long BYTE_MASK_LONG = 255L;
   private static final long BYTE_MASK = 255L;
   private static final String NULL_DOUBLE_KEY_MESSAGE = "keyExtractor cannot be null";
   private static final String NULL_STRING_KEY_MESSAGE = "keyExtractor cannot be null";
   private static final String NULL_STRING_MESSAGE = "radixSortLsdStrings does not support null keys";
   private static final int CHAR_BUCKETS = 65537;
   private static final String NULL_STRING_ELEMENT_MESSAGE = "radixSortLsdStrings does not support null elements";
   private static final long DOUBLE_ORDER_MASK = Long.MAX_VALUE;
   private static final String NULL_INTEGER_ELEMENT_MESSAGE = "radixSortLsdIntegers does not support null elements";

   public SortableArrayList(int capacity) {
      super(capacity);
   }

   public void sortByLong(ToLongFunction<? super T> toLongFunction) {
      if (toLongFunction == null) {
         throw new IllegalArgumentException(NULL_LONG_KEY_MESSAGE);
      }

      if (this.size() >= 2) {
         Object[] objects = this.toArray();
         int size = objects.length;
         long[] keys = new long[size];

         for (int i = 0; i < size; i++) {
            T t = (T) objects[i];
            keys[i] = toLongFunction.applyAsLong(t) ^ LONG_SIGN_FLIP;
         }

         Object[] objects2 = new Object[size];
         long[] sortedKeys = new long[size];
         int[] counts = new int[256];

         for (byte shift = 0; shift < 64; shift += 8) {
            Arrays.fill(counts, 0);

            for (long key : keys) {
               counts[(int)(key >>> shift & BYTE_MASK_LONG)]++;
            }

            for (int j = 1; j < counts.length; j++) {
               counts[j] += counts[j - 1];
            }

            for (int j2 = size - 1; j2 >= 0; j2--) {
               long sortedKey = keys[j2];
               int bucket = (int)(sortedKey >>> shift & BYTE_MASK);
               int target = --counts[bucket];
               objects2[target] = objects[j2];
               sortedKeys[target] = sortedKey;
            }

            Object[] objects3 = objects;
            objects = objects2;
            objects2 = objects3;
            long[] swapKeys = keys;
            keys = sortedKeys;
            sortedKeys = swapKeys;
         }

         for (int i2 = 0; i2 < size; i2++) {
            Object object = objects[i2];
            this.set(i2, (T)(Object)object);
         }
      }
   }

   public void sortByDouble(ToDoubleFunction<? super T> toDoubleFunction) {
      if (toDoubleFunction == null) {
         throw new IllegalArgumentException(NULL_DOUBLE_KEY_MESSAGE);
      }

      this.sortByLong(element -> {
         double value = toDoubleFunction.applyAsDouble((T)(Object)element);
         long bits = Double.doubleToRawLongBits(value);
         return bits ^ bits >> 63 & DOUBLE_ORDER_MASK;
      });
   }

   public SortableArrayList(Collection<? extends T> extendses) {
      super(extendses);
   }

   public void sortByInt(ToIntFunction<? super T> toIntFunction) {
      if (toIntFunction == null) {
         throw new IllegalArgumentException(NULL_INT_KEY_MESSAGE);
      }

      if (this.size() >= 2) {
         Object[] objects = this.toArray();
         int size = objects.length;
         int[] keys = new int[size];

         for (int i = 0; i < size; i++) {
            T t = (T) objects[i];
            keys[i] = toIntFunction.applyAsInt(t) ^ INT_SIGN_FLIP;
         }

         Object[] objects2 = new Object[size];
         int[] sortedKeys = new int[size];
         int[] counts = new int[256];

         for (byte shift = 0; shift < 32; shift += 8) {
            Arrays.fill(counts, 0);

            for (int key : keys) {
               counts[key >>> shift & 0xFF]++;
            }

            for (int j = 1; j < counts.length; j++) {
               counts[j] += counts[j - 1];
            }

            for (int j2 = size - 1; j2 >= 0; j2--) {
               int sortedKey = keys[j2];
               int bucket = sortedKey >>> shift & 0xFF;
               int target = --counts[bucket];
               objects2[target] = objects[j2];
               sortedKeys[target] = sortedKey;
            }

            Object[] objects3 = objects;
            objects = objects2;
            objects2 = objects3;
            int[] swapKeys = keys;
            keys = sortedKeys;
            sortedKeys = swapKeys;
         }

         for (int i2 = 0; i2 < size; i2++) {
            Object object = objects[i2];
            this.set(i2, (T)(Object)object);
         }
      }
   }

   public void sortIntegers() {
      this.sortByInt(element -> {
         if (element == null) {
            throw new IllegalStateException(NULL_INTEGER_ELEMENT_MESSAGE);
         } else if (!(element instanceof Integer)) {
            throw new IllegalStateException("radixSortLsdIntegers requires Integer elements, but found: " + element.getClass().getName());
         } else {
            return (Integer)element;
         }
      });
   }

   public void sortByString(Function<? super T, String> function) {
      if (function == null) {
         throw new IllegalArgumentException(NULL_STRING_KEY_MESSAGE);
      }

      if (this.size() >= 2) {
         Object[] objects = this.toArray();
         int size = objects.length;
         String[] texts = new String[size];
         int maxLength = 0;

         for (int i = 0; i < size; i++) {
            T t = (T) objects[i];
            String text = (String)function.apply(t);
            if (text == null) {
               throw new IllegalStateException(NULL_STRING_MESSAGE);
            }

            texts[i] = text;
            if (text.length() > maxLength) {
               maxLength = text.length();
            }
         }

         if (maxLength != 0) {
            Object[] objects2 = new Object[size];
            String[] texts2 = new String[size];
            int[] counts = new int[CHAR_BUCKETS];

            for (int i2 = maxLength - 1; i2 >= 0; i2--) {
               Arrays.fill(counts, 0);

               for (int j = 0; j < size; j++) {
                  String text2 = texts[j];
                  int bucket = i2 < text2.length() ? text2.charAt(i2) + 1 : 0;
                  counts[bucket]++;
               }

               for (int j2 = 1; j2 < counts.length; j2++) {
                  counts[j2] += counts[j2 - 1];
               }

               for (int j3 = size - 1; j3 >= 0; j3--) {
                  String text3 = texts[j3];
                  int sortedBucket = i2 < text3.length() ? text3.charAt(i2) + 1 : 0;
                  int target = --counts[sortedBucket];
                  objects2[target] = objects[j3];
                  texts2[target] = text3;
               }

               Object[] objects3 = objects;
               objects = objects2;
               objects2 = objects3;
               String[] texts3 = texts;
               texts = texts2;
               texts2 = texts3;
            }

            for (int i3 = 0; i3 < size; i3++) {
               Object object = objects[i3];
               this.set(i3, (T)(Object)object);
            }
         }
      }
   }

   public SortableArrayList() {
   }

   public void sortStrings() {
      this.sortByString(element -> {
         if (element == null) {
            throw new IllegalStateException(NULL_STRING_ELEMENT_MESSAGE);
         } else if (!(element instanceof String)) {
            throw new IllegalStateException("radixSortLsdStrings requires String elements, but found: " + element.getClass().getName());
         } else {
            return (String)element;
         }
      });
   }
}
