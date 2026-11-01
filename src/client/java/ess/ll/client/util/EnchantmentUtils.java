package ess.ll.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class EnchantmentUtils {
   @SafeVarargs
   public static boolean hasAll(ItemStack stack, Holder<Enchantment>... enchantments) {
      if (stack.isEmpty()) {
         return false;
      }

      Object2IntOpenHashMap levels = new Object2IntOpenHashMap();
      collect(stack, levels);

      for (var enchantment : enchantments) {
         if (!levels.containsKey(enchantment)) {
            return false;
         }
      }

      return true;
   }

   public static boolean has(Object2IntMap<Holder<Enchantment>> object2IntMap, Holder<Enchantment> holder) {
      return object2IntMap.containsKey(holder);
   }

   public static int getLevel(ItemStack stack, ResourceKey<Enchantment> resourceKey) {
      if (stack.isEmpty()) {
         return 0;
      }

      Object2IntOpenHashMap levels = new Object2IntOpenHashMap();
      collect(stack, levels);
      String text = resourceKey.identifier().toString();
      ObjectIterator objectIterator = levels.object2IntEntrySet().iterator();

      while (objectIterator.hasNext()) {
         Entry entry = (Entry)objectIterator.next();
         if (((Holder)entry.getKey()).getRegisteredName().equals(text)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   private EnchantmentUtils() {
   }

   public static int getLevel(Object2IntMap<Holder<Enchantment>> object2IntMap, Holder<Enchantment> holder) {
      return object2IntMap.getInt(holder);
   }

   public static void collect(ItemStack stack, Object2IntMap<Holder<Enchantment>> object2IntMap) {
      object2IntMap.clear();
      if (!stack.isEmpty()) {
         ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
         ItemEnchantments storedEnchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);

         for (var entry : enchantments.entrySet()) {
            object2IntMap.put((Holder)entry.getKey(), entry.getIntValue());
         }

         for (var storedEntry : storedEnchantments.entrySet()) {
            object2IntMap.put((Holder)storedEntry.getKey(), storedEntry.getIntValue());
         }
      }
   }

   public static int getLevel(ItemStack stack, Holder<Enchantment> holder) {
      if (stack.isEmpty()) {
         return 0;
      }

      Object2IntOpenHashMap levels = new Object2IntOpenHashMap();
      collect(stack, levels);
      return levels.getInt(holder);
   }
}
