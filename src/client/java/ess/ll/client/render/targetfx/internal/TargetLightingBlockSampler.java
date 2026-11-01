package ess.ll.client.render.targetfx.internal;

import ess.ll.client.render.targetfx.api.MinecraftContext;
import java.util.Arrays;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
public final class TargetLightingBlockSampler implements MinecraftContext {
   static final int stateCounter7 = 1024;
   private final int[] intArray = new int[1024];
   private final IdentityHashMap<BlockState, Integer> identityHashMap = new IdentityHashMap<>();
   private final MutableBlockPos mutableBlockPos = new MutableBlockPos();
   private final Vector3f vector3f = new Vector3f();
   private long lastUpdateMillis2;
   private int stateCounter5;
   private int stateCounter8;
   private int stateCounter4;
   private int stateCounter9;
   private int stateCounter;
   private int stateCounter3;
   private int stateCounter10;
   private int stateCounter2;
   private int stateCounter6;
   private static final long lastUpdateMillis3 = Long.MIN_VALUE;
   private static final double currentValue9 = 0.5;
   private static final double currentValue = 0.5;
   private static final double currentValue6 = 0.5;
   private static final long lastUpdateMillis = Long.MIN_VALUE;
   private static final double currentValue2 = 0.25;
   private static final double currentValue5 = 0.5;
   private static final double currentValue3 = 0.25;
   private static final double currentValue8 = 0.5;
   private static final double currentValue7 = 0.25;
   private static final double currentValue4 = 0.5;

   float getIntArray(Vec3 position) {
      return (float)(this.stateCounter4 - position.z);
   }

   private static int getIntArray2(BlockState blockState) {
      int[] instance = new int[]{0};
      blockState.getOcclusionShape().forAllBoxes((amount, amount2, amount3, amount4, amount5, amount6) -> {
         for (int index = 0; index < 8; index++) {
            double amount7 = currentValue2 + (index & 1) * currentValue5;
            double amount8 = currentValue3 + (index >> 1 & 1) * currentValue8;
            double amount9 = currentValue7 + (index >> 2 & 1) * currentValue4;
            if (amount7 >= amount && amount7 < amount4 && amount8 >= amount2 && amount8 < amount5 && amount9 >= amount3 && amount9 < amount6) {
               instance[0] |= 1 << index;
            }
         }
      });
      return instance[0];
   }

   int[] getOcclusionBits() {
      return this.intArray;
   }

   void reset() {
      this.lastUpdateMillis2 = lastUpdateMillis;
      this.identityHashMap.clear();
      Arrays.fill(this.intArray, 0);
   }

   void updateBlockOcclusion(Matrix4f matrix4f, Vector3f[] vector3fArray, float[] instance2, int value, Vec3 position2) {
      double amount10 = Double.POSITIVE_INFINITY;
      double amount11 = amount10;
      double amount12 = amount10;
      double amount13 = Double.NEGATIVE_INFINITY;
      double amount14 = amount13;
      double amount15 = amount13;

      for (int index2 = 0; index2 < value; index2++) {
         matrix4f.transformPosition(vector3fArray[index2], this.vector3f);
         double amount16 = position2.x + this.vector3f.x;
         double amount17 = position2.y + this.vector3f.y;
         double amount18 = position2.z + this.vector3f.z;
         float amount19 = instance2[index2];
         amount10 = Math.min(amount10, amount16 - amount19);
         amount13 = Math.max(amount13, amount16 + amount19);
         amount11 = Math.min(amount11, amount17 - amount19);
         amount14 = Math.max(amount14, amount17 + amount19);
         amount12 = Math.min(amount12, amount18 - amount19);
         amount15 = Math.max(amount15, amount18 + amount19);
      }

      int value2 = (int)Math.floor((amount10 + amount13) * currentValue9) - 8;
      int value3 = (int)Math.floor((amount11 + amount14) * currentValue) - 8;
      int value4 = (int)Math.floor((amount12 + amount15) * currentValue6) - 8;
      int value5 = Math.max(value2, (int)Math.floor(amount10));
      int value6 = Math.min(value2 + 15, (int)Math.floor(amount13));
      int value7 = Math.max(value3, (int)Math.floor(amount11));
      int value8 = Math.min(value3 + 15, (int)Math.floor(amount14));
      int value9 = Math.max(value4, (int)Math.floor(amount12));
      int value10 = Math.min(value4 + 15, (int)Math.floor(amount15));
      long value11 = MINECRAFT.level.getGameTime();
      if (this.lastUpdateMillis2 != value11
         || this.stateCounter5 != value2
         || this.stateCounter8 != value3
         || this.stateCounter4 != value4
         || this.stateCounter9 != value5
         || this.stateCounter10 != value6
         || this.stateCounter != value7
         || this.stateCounter2 != value8
         || this.stateCounter3 != value9
         || this.stateCounter6 != value10) {
         this.stateCounter5 = value2;
         this.stateCounter8 = value3;
         this.stateCounter4 = value4;
         this.stateCounter9 = value5;
         this.stateCounter10 = value6;
         this.stateCounter = value7;
         this.stateCounter2 = value8;
         this.stateCounter3 = value9;
         this.stateCounter6 = value10;
         this.lastUpdateMillis2 = value11;
         Arrays.fill(this.intArray, 0);

         for (int index3 = value9; index3 <= value10; index3++) {
            for (int index4 = value7; index4 <= value8; index4++) {
               for (int index5 = value5; index5 <= value6; index5++) {
                  BlockState blockState2 = MINECRAFT.level.getBlockState(this.mutableBlockPos.set(index5, index4, index3));
                  if (blockState2.canOcclude()) {
                     int value12 = this.identityHashMap.computeIfAbsent(blockState2, TargetLightingBlockSampler::getIntArray2);

                     for (int index6 = 0; index6 < 8; index6++) {
                        if ((value12 & 1 << index6) != 0) {
                           int value13 = (index5 - value2) * 2 + (index6 & 1);
                           int value14 = (index4 - value3) * 2 + (index6 >> 1 & 1);
                           int value15 = (index3 - value4) * 2 + (index6 >> 2 & 1);
                           int value16 = value13 + value14 * 32 + value15 * 1024;
                           this.intArray[value16 >> 5] = this.intArray[value16 >> 5] | 1 << (value16 & 31);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   TargetLightingBlockSampler() {
      this.lastUpdateMillis2 = lastUpdateMillis3;
   }

   float calculateValue2(Vec3 position3) {
      return (float)(this.stateCounter8 - position3.y);
   }

   float calculateValue(Vec3 position4) {
      return (float)(this.stateCounter5 - position4.x);
   }
}
