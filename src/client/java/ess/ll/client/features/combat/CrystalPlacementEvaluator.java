package ess.ll.client.features.combat;

import ess.ll.client.features.world.BlockBreakSpeedUtils;
import ess.ll.client.features.world.BlockPlacementManager;
import ess.ll.client.features.world.BlockPlacementTracker;
import ess.ll.client.integration.MinecraftAccess;
import ess.ll.client.module.combat.CrystalAura;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CrystalPlacementEvaluator implements MinecraftAccess {
    private static List<CrystalPlacementEvaluator.DigPreview> lastDigPreviews = List.of();
    private static final CrystalPlacementEvaluator.Decision AWAIT_DECISION = new CrystalPlacementEvaluator.Decision(CrystalPlacementEvaluator.Action.AWAIT, null, null, null, 0.0F, 0.0F, null);
    private static final ExplosionDamageUtils.Visibility ALWAYS_VISIBLE = new ExplosionDamageUtils.Visibility(true, null);

    public static CrystalPlacementEvaluator.Decision evaluate(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var3) {
        return evaluateExplosions(entity, bounds, vector, var3, null);
    }

    public static boolean isSelfDamageSafe(float var0, float var1, CrystalPlacementEvaluator.Settings var2, BlockPos blockPos) {
        boolean var4 = blockPos != null && blockPos.equals(var2.committedBase());
        return isSelfDamageAcceptable(var0, var4 ? var1 * 1.15F : var1, var2);
    }

    private static CrystalPlacementEvaluator.SpawnState spawnStateAt(BlockPos blockPos) {
        CrystalPlacementEvaluator.SpawnState FREE2 = CrystalPlacementEvaluator.SpawnState.FREE;
        for (Entity entity : mc.level.getEntitiesOfClass(Entity.class, ExplosionDamageUtils.crystalBounds(blockPos), ExplosionDamageUtils::isBlockingEntity)) {
            // A crystal already hit is gone on the server by the time a new placement arrives.
            if (entity instanceof EndCrystal && CrystalAura.recentlyHit(entity))
                continue;
            if (!(entity instanceof ItemEntity var4) || var4.onGround()) {
                return CrystalPlacementEvaluator.SpawnState.BLOCKED;
            }
            FREE2 = CrystalPlacementEvaluator.SpawnState.FALLING_ITEM;
        }
        return FREE2;
    }

    private static ItemEntity settledItemAt(BlockPos blockPos) {
        return settledItemIn(ExplosionDamageUtils.crystalBounds(blockPos));
    }

    public static float scorePlacement(float var0, float var1, LivingEntity entity, AABB bounds, Vec3 vector) {
        float var5 = var0 - 2.0F * var1;
        if (isAboveTarget(vector, entity, bounds)) {
            var5 -= 3.0F;
        }
        return isLethal(var0, entity) ? var5 + 1000.0F : var5;
    }

    private static ItemEntity settledItemIn(AABB bounds) {
        ItemEntity itemEntity = null;
        for (Entity entity : mc.level.getEntitiesOfClass(Entity.class, bounds, ExplosionDamageUtils::isBlockingEntity)) {
            if (!(entity instanceof ItemEntity var4)) {
                return null;
            }
            if (var4.onGround()) {
                itemEntity = var4;
            }
        }
        return itemEntity;
    }

    public static boolean isSelfDamageAcceptable(float var0, float var1, CrystalPlacementEvaluator.Settings var2) {
        return var0 >= var2.selfHealthLimit() ? false : var0 <= var2.maxSelfDamage() || var0 < var1;
    }

    private static int fluidLevel(BlockPos blockPos) {
        return mc.level.getFluidState(blockPos).getAmount();
    }

    private static void addExplodeEvaluation(List<CrystalPlacementEvaluator.Evaluation> items, BlockPos blockPos, CrystalPlacementEvaluator.Verdict var2, float var3, float var4, float var5) {
        if (items != null) {
            items.add(new CrystalPlacementEvaluator.Evaluation(blockPos, CrystalPlacementEvaluator.Action.EXPLODE, var2, var3, var4, var5));
        }
    }

    private static boolean isDugBlock(BlockPos blockPos, List<BlockPos> items, BlockPos blockPos2, boolean var3) {
        return items.contains(blockPos) || var3 && blockPos.equals(blockPos2);
    }

    private static CrystalPlacementEvaluator.DigShape classifyDigShape(BlockPos blockPos, List<BlockPos> items) {
        BlockPos blockPos2 = blockPos.above();
        boolean var3 = !items.contains(blockPos2) && mc.level.getBlockState(blockPos2).isAir();
        HashMap hashMap = null;
        ArrayDeque arrayDeque = null;
        for (int var6 = 0; var6 < items.size(); var6++) {
            BlockPos blockPos3 = (BlockPos) items.get(var6);
            int var8 = fluidLevel(blockPos3);
            BlockPos blockPos4 = blockPos3.above();
            if (!items.contains(blockPos4) && fluidLevel(blockPos4) > 0) {
                var8 = 8;
            }
            for (Direction direction : Plane.HORIZONTAL) {
                BlockPos blockPos5 = blockPos3.relative(direction);
                if (!items.contains(blockPos5)) {
                    var8 = Math.max(var8, fluidLevel(blockPos5) - 1);
                }
            }
            if (var8 > 0) {
                if (hashMap == null) {
                    hashMap = new HashMap();
                    arrayDeque = new ArrayDeque();
                }
                enqueueFlow(hashMap, arrayDeque, blockPos3, var8);
            }
        }
        if (hashMap == null) {
            return CrystalPlacementEvaluator.DigShape.DRY;
        }
        while (!arrayDeque.isEmpty()) {
            BlockPos blockPos6 = (BlockPos) arrayDeque.poll();
            int var14 = (Integer) hashMap.get(blockPos6);
            BlockPos blockPos7 = blockPos6.below();
            boolean var16 = isDugBlock(blockPos7, items, blockPos2, var3);
            if (var16) {
                enqueueFlow(hashMap, arrayDeque, blockPos7, 8);
            }
            if (!var16 && var14 > 1) {
                for (Direction direction2 : Plane.HORIZONTAL) {
                    BlockPos blockPos8 = blockPos6.relative(direction2);
                    if (isDugBlock(blockPos8, items, blockPos2, var3)) {
                        enqueueFlow(hashMap, arrayDeque, blockPos8, var14 - 1);
                    }
                }
            }
        }
        return hashMap.containsKey(blockPos2) ? CrystalPlacementEvaluator.DigShape.CELL : CrystalPlacementEvaluator.DigShape.STREAM;
    }

    private static boolean hasBlockingEntity(BlockPos blockPos) {
        return !mc.level.getEntitiesOfClass(Entity.class, ExplosionDamageUtils.crystalBounds(blockPos), var0x -> ExplosionDamageUtils.isBlockingEntity(var0x) && !(var0x instanceof ItemEntity)).isEmpty();
    }

    private static boolean isAboveTarget(Vec3 vector, LivingEntity entity, AABB bounds) {
        boolean var3 = !entity.onGround() && entity.getDeltaMovement().y > 0.08;
        return vector.y > bounds.minY + 0.5 && !var3;
    }

    private CrystalPlacementEvaluator() {
    }

    public static CrystalPlacementEvaluator.Decision evaluateWithMining(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var3, CrystalPlacementEvaluator.MiningContext var4) {
        return evaluatePlacements(entity, bounds, vector, var3, var4, null);
    }

    public static CrystalPlacementEvaluator.Decision evaluateExplosions(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var3, List<CrystalPlacementEvaluator.Evaluation> items, boolean var5) {
        if (mc.player != null && mc.level != null && entity != null) {
            CrystalPlacementEvaluator.Decision var6 = null;
            float var7 = -Float.MAX_VALUE;
            int var8 = -1;
            double var9 = var3.explodeRange() * var3.explodeRange();
            AABB boundingBox = mc.player.getBoundingBox();
            for (Entity entity2 : mc.level.entitiesForRendering()) {
                if (entity2 instanceof EndCrystal var14 && !CrystalAura.recentlyHit(var14)) {
                    BlockPos blockPos = var14.blockPosition();
                    if (ExplosionDamageUtils.distanceSquaredTo(vector, var14.getBoundingBox()) > var9) {
                        addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.OUT_OF_REACH, 0.0F, 0.0F, 0.0F);
                    } else {
                        boolean var16 = var3.matrixSearch();
                        if (var16 && !BlockPlacementManager.isWithinInteractionRange(var14.position())) {
                            addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.OUT_OF_REACH, 0.0F, 0.0F, 0.0F);
                        } else if ((var5 || var16) && ExplosionDamageUtils.crystalAimPoint(var14, vector, var3.explodeRange(), var16) == null) {
                            addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.NOT_VISIBLE, 0.0F, 0.0F, 0.0F);
                        } else {
                            Vec3 position2 = var14.position();
                            float var18 = ExplosionDamageUtils.damageTo(position2, entity, bounds);
                            float var19 = ExplosionDamageUtils.damageTo(position2, mc.player, boundingBox);
                            if (var18 < var3.minEnemyDamage()) {
                                addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.LOW_DAMAGE, var18, var19, var18 - var19);
                            } else if (!isSelfDamageSafe(var19, var18, var3, blockPos.below())) {
                                addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.SELF_DAMAGE, var18, var19, var18 - var19);
                            } else if (ExplosionDamageUtils.maxDamage(position2, var3.friends()) > 0.0F) {
                                addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.FRIEND_DAMAGE, var18, var19, var18 - var19);
                            } else {
                                float var20 = scorePlacement(var18, var19, entity, bounds, position2);
                                addExplodeEvaluation(items, blockPos, CrystalPlacementEvaluator.Verdict.CANDIDATE, var18, var19, var20);
                                if (!(var20 <= var7)) {
                                    var7 = var20;
                                    var6 = new CrystalPlacementEvaluator.Decision(CrystalPlacementEvaluator.Action.EXPLODE, blockPos, null, var14, var18, var19, null);
                                    if (items != null) {
                                        var8 = items.size() - 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (items != null && var8 >= 0) {
                CrystalPlacementEvaluator.Evaluation var21 = (CrystalPlacementEvaluator.Evaluation) items.get(var8);
                items.set(var8, new CrystalPlacementEvaluator.Evaluation(var21.pos(), CrystalPlacementEvaluator.Action.EXPLODE, CrystalPlacementEvaluator.Verdict.CHOSEN, var21.enemyDamage(), var21.selfDamage(), var21.score()));
            }
            return var6;
        } else {
            return null;
        }
    }

    private static void enqueueFlow(Map<BlockPos, Integer> map, ArrayDeque<BlockPos> arrayDeque, BlockPos blockPos, int var3) {
        Integer var4 = (Integer) map.get(blockPos);
        if (var4 == null || var4 < var3) {
            map.put(blockPos, var3);
            arrayDeque.add(blockPos);
        }
    }

    public static List<CrystalPlacementEvaluator.DigPreview> getDigPreviews() {
        return lastDigPreviews;
    }

    public static CrystalPlacementEvaluator.Decision evaluateExplosions(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var3, List<CrystalPlacementEvaluator.Evaluation> items) {
        return evaluateExplosions(entity, bounds, vector, var3, items, !var3.explodeThroughWalls());
    }

    private static boolean isLethal(float var0, LivingEntity entity) {
        return var0 >= entity.getHealth() + entity.getAbsorptionAmount();
    }

    public static CrystalPlacementEvaluator.Decision evaluatePlacements(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var3, CrystalPlacementEvaluator.MiningContext var4, List<CrystalPlacementEvaluator.Evaluation> items) {
        if (mc.player != null && mc.level != null && entity != null) {
            CrystalPlacementEvaluator.PlacementSearch var6 = new CrystalPlacementEvaluator.PlacementSearch(entity, bounds, vector, var3, var4, items);
            int var7 = Mth.ceil(var3.placeRange());
            BlockPos blockPos = mc.player.blockPosition();
            MutableBlockPos blockPos2 = new MutableBlockPos();
            for (int var10 = -var7; var10 <= var7; var10++) {
                for (int var11 = -var7; var11 <= var7; var11++) {
                    for (int var12 = -var7; var12 <= var7; var12++) {
                        blockPos2.set(blockPos.getX() + var10, blockPos.getY() + var12, blockPos.getZ() + var11);
                        var6.evaluateBlock(blockPos2);
                    }
                }
            }
            return var6.finish();
        } else {
            return null;
        }
    }

    private enum DigShape {
        DRY, STREAM, CELL
    }

    private static final class DigCandidate {
        private final List<BlockPos> blocks;
        private final BlockPos base;
        private final float ticks;
        private final boolean active;
        private final float enemyDamage;
        private final float selfDamage;
        private final float score;
        private final boolean hasOccluder;
        private final boolean fluidStream;

        private float score() {
            return this.score - this.ticks * 0.25F - 3.0F * (this.blocks.size() - 1) - (this.hasOccluder ? 3.0F : 0.0F) - (this.fluidStream ? 6.0F : 0.0F) + (this.active ? 8.0F : 0.0F);
        }

        private DigCandidate(List<BlockPos> items, BlockPos blockPos, float var3, boolean var4, float var5, float var6, float var7, boolean var8, boolean var9) {
            this.fluidStream = var9;
            this.blocks = items;
            this.base = blockPos;
            this.ticks = var3;
            this.active = var4;
            this.enemyDamage = var5;
            this.selfDamage = var6;
            this.score = var7;
            this.hasOccluder = var8;
        }

        private boolean isBetterThan(CrystalPlacementEvaluator.DigCandidate var1) {
            return var1 == null || this.score() > var1.score();
        }

        private CrystalPlacementEvaluator.DigPreview toPreview(boolean var1) {
            return new CrystalPlacementEvaluator.DigPreview(this.blocks, this.base, this.ticks, this.active, this.enemyDamage, this.selfDamage, var1);
        }

        private CrystalPlacementEvaluator.Decision toDecision() {
            return new CrystalPlacementEvaluator.Decision(CrystalPlacementEvaluator.Action.DIG, this.blocks.get(0), this.base, null, this.enemyDamage, this.selfDamage, null);
        }

        private boolean costsLessSelfDamageThan(CrystalPlacementEvaluator.Decision var1) {
            return this.selfDamage + 2.0F < var1.selfDamage();
        }
    }

    public enum Verdict {
        CHOSEN("выбрано"),
        CANDIDATE("кандидат"),
        AWAIT("ждём дроп"),
        OUT_OF_REACH("вне дальности взрыва"),
        LOW_CEILING("потолок урона"),
        NO_SUPPORT("нет опоры"),
        CELL_NOT_AIR("клетка занята блоком"),
        BLOCKED("сущность мешает"),
        NOT_VISIBLE("не видно"),
        NO_FACE("нет грани"),
        CANT_DIG("не выкопать"),
        STALLED("постановка не удалась"),
        UNCONFIRMED("ждём сервер"),
        TARGET_MOVING("цель убежит"),
        ENTITY_ABOVE("кто-то стоит"),
        LOW_DAMAGE("мало урона"),
        SELF_DAMAGE("много себе"),
        FRIEND_DAMAGE("заденет друга"),
        RESERVED("занято другим модулем"),
        FLOODS("зальёт водой");

        public final String label;

        Verdict(String var3) {
            this.label = var3;
        }

        public boolean isRejected() {
            return this != CHOSEN && this != CANDIDATE && this != AWAIT;
        }
    }

    private static final class PlacementSearch {
        private final LivingEntity target;
        private final AABB targetBounds;
        private final AABB playerBounds;
        private final Vec3 eyePosition;
        private final CrystalPlacementEvaluator.Settings settings;
        private final CrystalPlacementEvaluator.MiningContext mining;
        private final List<CrystalPlacementEvaluator.Evaluation> evaluations;
        private final int targetFloorY;
        private final double placeRangeSquared;
        private final double explodeRangeSquared;
        private final CrystalPlacementEvaluator.ActionSlot preferredCrystalSlot = new CrystalPlacementEvaluator.ActionSlot();
        private final CrystalPlacementEvaluator.ActionSlot preferredObsidianSlot = new CrystalPlacementEvaluator.ActionSlot();
        private final CrystalPlacementEvaluator.ActionSlot crystalSlot = new CrystalPlacementEvaluator.ActionSlot();
        private final CrystalPlacementEvaluator.ActionSlot obsidianSlot = new CrystalPlacementEvaluator.ActionSlot();
        private final CrystalPlacementEvaluator.ActionSlot explodeSlot = new CrystalPlacementEvaluator.ActionSlot();
        private final CrystalPlacementEvaluator.ItemClearCandidates itemClearCandidates = new CrystalPlacementEvaluator.ItemClearCandidates();
        private ItemEntity blockingItem;
        private BlockPos blockingItemPos;
        private float blockingItemScore;
        private boolean blockingItemPreferred;
        private float bestScore;
        private CrystalPlacementEvaluator.DigCandidate preferredDigCandidate;
        private CrystalPlacementEvaluator.DigCandidate fallbackDigCandidate;
        private final List<CrystalPlacementEvaluator.DigCandidate> digCandidates;

        private void evaluateObsidianPlacement(BlockPos blockPos, Vec3 vector, boolean var3) {
            if (this.settings.canPlaceObsidian()) {
                BlockPos blockPos2 = blockPos.above();
                if (!MinecraftAccess.mc.level.getBlockState(blockPos2).isAir()) {
                    this.addEvaluation(blockPos, CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, CrystalPlacementEvaluator.Verdict.CELL_NOT_AIR, 0.0F, 0.0F, 0.0F);
                } else if (ExplosionDamageUtils.isObstructed(blockPos)) {
                    this.addEvaluation(blockPos, CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, CrystalPlacementEvaluator.Verdict.BLOCKED, 0.0F, 0.0F, 0.0F);
                } else {
                    CrystalPlacementEvaluator.SpawnState sx = CrystalPlacementEvaluator.spawnStateAt(blockPos2);
                    if (sx == CrystalPlacementEvaluator.SpawnState.BLOCKED) {
                        this.considerBlockingItem(blockPos, CrystalPlacementEvaluator.settledItemAt(blockPos2), CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, vector);
                    } else {
                        ExplosionDamageUtils.Visibility sxx = this.visibilityOf(blockPos);
                        if (!sxx.visible()) {
                            this.addEvaluation(blockPos, CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, CrystalPlacementEvaluator.Verdict.NOT_VISIBLE, 0.0F, 0.0F, 0.0F);
                            if (var3) {
                                this.evaluateDigThroughOccluder(blockPos, vector, true, sxx);
                            }
                        } else if (ExplosionDamageUtils.findFace(blockPos, this.eyePosition, this.settings.placeRange(), this.settings.placeThroughWalls()) == null) {
                            this.addEvaluation(blockPos, CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, CrystalPlacementEvaluator.Verdict.NO_FACE, 0.0F, 0.0F, 0.0F);
                            if (var3) {
                                this.evaluateDigThroughOccluder(blockPos, vector, true, sxx);
                            }
                        } else {
                            float sxxx = ExplosionDamageUtils.damageWithBlockChanges(vector, this.target, this.targetBounds, blockPos, null);
                            this.submitPlacement(CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN, blockPos, vector, sxxx, ExplosionDamageUtils.damageWithBlockChanges(vector, MinecraftAccess.mc.player, this.playerBounds, blockPos, null), sx, var3);
                        }
                    }
                }
            }
        }

        private CrystalPlacementEvaluator.ActionSlot slotFor(CrystalPlacementEvaluator.Action var1, boolean var2) {
            if (var1 == CrystalPlacementEvaluator.Action.PLACE_CRYSTAL) {
                return var2 ? this.preferredCrystalSlot : this.crystalSlot;
            } else {
                return var2 ? this.preferredObsidianSlot : this.obsidianSlot;
            }
        }

        private void markChosen(CrystalPlacementEvaluator.Decision var1) {
            if (var1 != null && var1.pos() != null && this.evaluations != null) {
                for (int var2 = 0; var2 < this.evaluations.size(); var2++) {
                    CrystalPlacementEvaluator.Evaluation var3 = this.evaluations.get(var2);
                    if (!var3.verdict().isRejected() && var3.action() == var1.action() && var3.pos().equals(var1.pos())) {
                        this.evaluations.set(var2, new CrystalPlacementEvaluator.Evaluation(var3.pos(), var3.action(), CrystalPlacementEvaluator.Verdict.CHOSEN, var3.enemyDamage(), var3.selfDamage(), var3.score()));
                        return;
                    }
                }
            }
        }

        private boolean shouldClearItem(CrystalPlacementEvaluator.Decision var1) {
            if (this.blockingItem == null) {
                return false;
            } else if (!this.blockingItemPreferred && var1 != null) {
                return var1.action() != CrystalPlacementEvaluator.Action.DIG && var1.action() != CrystalPlacementEvaluator.Action.AWAIT ? this.blockingItemScore > this.bestScore + 5.0F : true;
            } else {
                return true;
            }
        }

        private boolean isReservedByOther(BlockPos blockPos) {
            return this.settings.reservationOwner() != null && BlockPlacementTracker.isReservedByAnother(this.settings.reservationOwner(), blockPos);
        }

        private CrystalPlacementEvaluator.Decision finish() {
            CrystalPlacementEvaluator.Decision var1 = this.bestDecision();
            if (var1 == null) {
                var1 = this.crystalSlot.decision();
            }
            if (var1 == null) {
                var1 = this.obsidianSlot.decision();
            }
            if (this.shouldClearItem(var1)) {
                CrystalPlacementEvaluator.Decision var2 = this.clearingDecision();
                if (var2 != null) {
                    var1 = var2;
                }
            }
            this.markChosen(var1);
            if (this.digCandidates != null) {
                var var5 = new ArrayList(this.digCandidates.size());
                for (int var3 = 0; var3 < this.digCandidates.size(); var3++) {
                    CrystalPlacementEvaluator.DigCandidate var4 = this.digCandidates.get(var3);
                    var5.add(var4.toPreview(var1 != null && var1.action() == CrystalPlacementEvaluator.Action.DIG && var4.blocks.get(0).equals(var1.pos()) && var4.base.equals(var1.base())));
                }
                CrystalPlacementEvaluator.lastDigPreviews = var5;
            }
            return var1;
        }

        private boolean rejectByDamage(CrystalPlacementEvaluator.Action var1, BlockPos blockPos, BlockPos blockPos2, Vec3 vector, float var5, float var6, float var7) {
            if (var5 < this.settings.minEnemyDamage()) {
                this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.LOW_DAMAGE, var5, var6, var7);
                return true;
            } else if (!CrystalPlacementEvaluator.isSelfDamageSafe(var6, var5, this.settings, blockPos2)) {
                this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.SELF_DAMAGE, var5, var6, var7);
                return true;
            } else if (ExplosionDamageUtils.maxDamage(vector, this.settings.friends()) > 0.0F) {
                this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.FRIEND_DAMAGE, var5, var6, var7);
                return true;
            } else {
                return false;
            }
        }

        private void evaluateBlock(MutableBlockPos blockPos) {
            if (this.settings.canPlaceCrystal()) {
                if (!(ExplosionDamageUtils.distanceSquaredToBlock(this.eyePosition, blockPos) > this.placeRangeSquared)) {
                    if (this.mining != null && this.mining.blacklist() != null && this.mining.blacklist().contains(blockPos)) {
                        this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.STALLED, 0.0F, 0.0F, 0.0F);
                    } else if (this.mining == null || this.mining.pending() == null || !this.mining.pending().contains(blockPos) && !this.mining.pending().contains(blockPos.above())) {
                        if (!this.isReservedByOther(blockPos) && !this.isReservedByOther(blockPos.above())) {
                            Vec3 vector = ExplosionDamageUtils.crystalBaseTop(blockPos);
                            float var3 = ExplosionDamageUtils.explosionDamage(vector, this.target, this.targetBounds, 1.0F);
                            if (var3 < this.settings.minEnemyDamage()) {
                                this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.LOW_CEILING, var3, 0.0F, 0.0F);
                            } else {
                                boolean var4 = ExplosionDamageUtils.distanceSquaredToCrystal(this.eyePosition, vector) <= this.explodeRangeSquared && (!this.settings.matrixSearch() || BlockPlacementManager.isWithinInteractionRange(vector));
                                boolean var5 = var4 && blockPos.getY() < this.targetFloorY;
                                BlockState blockState = MinecraftAccess.mc.level.getBlockState(blockPos);
                                if (ExplosionDamageUtils.isBlastResistant(blockState)) {
                                    BlockPos blockPos2 = blockPos.immutable();
                                    this.evaluateCrystalPlacement(blockPos2, vector, var4);
                                    if (var5) {
                                        this.evaluateDigBase(blockPos2, blockState, vector);
                                    }
                                } else if (blockState.canBeReplaced()) {
                                    BlockPos blockPos3 = blockPos.immutable();
                                    this.evaluateObsidianPlacement(blockPos3, vector, var4);
                                    if (var5) {
                                        this.evaluateDigBase(blockPos3, blockState, vector);
                                    }
                                } else if (!var4) {
                                    this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.OUT_OF_REACH, var3, 0.0F, 0.0F);
                                } else {
                                    if (!var5) {
                                        return;
                                    }
                                    if (this.settings.canPlaceObsidian() && ExplosionDamageUtils.isMineable(blockState)) {
                                        this.evaluateDigBase(blockPos.immutable(), blockState, vector);
                                    } else {
                                        this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.NO_SUPPORT, var3, 0.0F, 0.0F);
                                    }
                                }
                            }
                        } else {
                            this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.RESERVED, 0.0F, 0.0F, 0.0F);
                        }
                    } else {
                        this.addEvaluation(blockPos, null, CrystalPlacementEvaluator.Verdict.UNCONFIRMED, 0.0F, 0.0F, 0.0F);
                    }
                }
            }
        }

        private CrystalPlacementEvaluator.Decision bestDirectDecision() {
            float var1 = this.explodeSlot.bestScore();
            if (var1 != -Float.MAX_VALUE) {
                this.bestScore = var1;
                return this.explodeSlot.decision();
            }
            float var2 = this.preferredCrystalSlot.bestScore();
            float var3 = this.preferredObsidianSlot.bestScore();
            if (var2 == -Float.MAX_VALUE && var3 == -Float.MAX_VALUE) {
                return null;
            }
            float var4 = var3 - 3.0F;
            this.bestScore = Math.max(var2, var4);
            return var2 >= var4 ? this.preferredCrystalSlot.decision() : this.preferredObsidianSlot.decision();
        }

        private boolean isPreferredBase(BlockPos blockPos) {
            return this.mining != null && blockPos.equals(this.mining.preferredBase());
        }

        private void evaluateDigCandidate(List<BlockPos> items, BlockPos blockPos, float var3, boolean var4, Vec3 vector, BlockPos blockPos2, BlockPos blockPos3, boolean var8) {
            BlockPos blockPos4 = (BlockPos) items.get(0);
            if (var3 * this.mining.targetStep() > 1.0F) {
                this.addEvaluation(blockPos4, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.TARGET_MOVING, 0.0F, 0.0F, 0.0F);
            } else {
                float var10 = ExplosionDamageUtils.damageWithBlockChanges(vector, this.target, this.targetBounds, blockPos2, blockPos3);
                float var11 = ExplosionDamageUtils.damageWithBlockChanges(vector, MinecraftAccess.mc.player, this.playerBounds, blockPos2, blockPos3);
                float var12 = CrystalPlacementEvaluator.scorePlacement(var10, var11, this.target, this.targetBounds, vector);
                if (!this.rejectByDamage(CrystalPlacementEvaluator.Action.DIG, blockPos4, blockPos, vector, var10, var11, var12)) {
                    CrystalPlacementEvaluator.DigCandidate var13 = new CrystalPlacementEvaluator.DigCandidate(items, blockPos, var3, var4, var10, var11, var12, blockPos2 != null, var8);
                    this.addEvaluation(blockPos4, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.CANDIDATE, var10, var11, var13.score());
                    if (this.digCandidates != null) {
                        this.digCandidates.add(var13);
                    }
                    if (this.isPreferredBase(blockPos)) {
                        if (var13.isBetterThan(this.preferredDigCandidate)) {
                            this.preferredDigCandidate = var13;
                        }
                    } else {
                        if (var13.isBetterThan(this.fallbackDigCandidate)) {
                            this.fallbackDigCandidate = var13;
                        }
                    }
                }
            }
        }

        private CrystalPlacementEvaluator.Verdict digVerdict(BlockPos blockPos, BlockState blockState, List<BlockPos> items) {
            if (this.mining != null && this.mining.allowed()) {
                if (this.mining.pending() != null && this.mining.pending().contains(blockPos)) {
                    return CrystalPlacementEvaluator.Verdict.UNCONFIRMED;
                } else if (this.isReservedByOther(blockPos)) {
                    return CrystalPlacementEvaluator.Verdict.RESERVED;
                } else if (!ExplosionDamageUtils.isMineable(blockState)) {
                    return CrystalPlacementEvaluator.Verdict.CANT_DIG;
                } else if (ExplosionDamageUtils.isUnsupportedFalling(blockPos, blockState)) {
                    return CrystalPlacementEvaluator.Verdict.CANT_DIG;
                } else if (this.mining.blacklist() != null && this.mining.blacklist().contains(blockPos)) {
                    return CrystalPlacementEvaluator.Verdict.CANT_DIG;
                } else if (this.mining.keep() != null && this.mining.keep().contains(blockPos)) {
                    return CrystalPlacementEvaluator.Verdict.CANT_DIG;
                } else if (BlockBreakSpeedUtils.breakTicks(blockState, blockPos) > 400.0F) {
                    return CrystalPlacementEvaluator.Verdict.CANT_DIG;
                } else if (this.settings.digThroughWalls()) {
                    return ExplosionDamageUtils.distanceSquaredToBlock(this.eyePosition, blockPos) <= this.placeRangeSquared ? null : CrystalPlacementEvaluator.Verdict.OUT_OF_REACH;
                } else {
                    return ExplosionDamageUtils.hasVisibleAim(blockPos, this.eyePosition, this.settings.placeRange(), items) ? null : CrystalPlacementEvaluator.Verdict.NOT_VISIBLE;
                }
            } else {
                return CrystalPlacementEvaluator.Verdict.CANT_DIG;
            }
        }

        private boolean isDigPathVisible(BlockPos blockPos, List<BlockPos> items) {
            return !this.settings.matrixSearch() || ExplosionDamageUtils.isBaseVisible(blockPos, this.eyePosition, this.settings.placeRange(), items);
        }

        private void evaluateCrystalPlacement(BlockPos blockPos, Vec3 vector, boolean var3) {
            BlockPos blockPos2 = blockPos.above();
            if (MinecraftAccess.mc.level.getBlockState(blockPos2).isAir()) {
                CrystalPlacementEvaluator.SpawnState var5 = CrystalPlacementEvaluator.spawnStateAt(blockPos2);
                if (var5 == CrystalPlacementEvaluator.SpawnState.BLOCKED) {
                    this.considerBlockingItem(blockPos, CrystalPlacementEvaluator.settledItemAt(blockPos2), CrystalPlacementEvaluator.Action.PLACE_CRYSTAL, vector);
                } else {
                    boolean var6 = !this.settings.placeThroughWalls() && !ExplosionDamageUtils.hasVisibleAim(blockPos, this.eyePosition, this.settings.placeRange());
                    ExplosionDamageUtils.Visibility var7 = var6 ? null : this.visibilityOf(blockPos);
                    if (var7 != null && var7.visible()) {
                        float var8 = ExplosionDamageUtils.damageTo(vector, this.target, this.targetBounds);
                        this.submitPlacement(CrystalPlacementEvaluator.Action.PLACE_CRYSTAL, blockPos, vector, var8, ExplosionDamageUtils.damageTo(vector, MinecraftAccess.mc.player, this.playerBounds), var5, var3);
                    } else {
                        this.addEvaluation(blockPos, CrystalPlacementEvaluator.Action.PLACE_CRYSTAL, CrystalPlacementEvaluator.Verdict.NOT_VISIBLE, 0.0F, 0.0F, 0.0F);
                        if (var3) {
                            this.evaluateDigThroughOccluder(blockPos, vector, false, var7);
                        }
                    }
                }
            }
        }

        private void considerBlockingItem(BlockPos blockPos, ItemEntity itemEntity, CrystalPlacementEvaluator.Action var3, Vec3 vector) {
            this.addEvaluation(blockPos, var3, CrystalPlacementEvaluator.Verdict.BLOCKED, 0.0F, 0.0F, 0.0F);
            if (itemEntity != null) {
                float var5 = ExplosionDamageUtils.damageTo(vector, this.target, this.targetBounds);
                float var6 = ExplosionDamageUtils.damageTo(vector, MinecraftAccess.mc.player, this.playerBounds);
                if (CrystalPlacementEvaluator.isSelfDamageAcceptable(var6, var5, this.settings) && !(var5 < this.settings.minEnemyDamage())) {
                    float var7 = CrystalPlacementEvaluator.scorePlacement(var5, var6, this.target, this.targetBounds, vector);
                    boolean var8 = this.isPreferredBase(blockPos);
                    if (this.blockingItem == null || !this.blockingItemPreferred || var8) {
                        if (this.blockingItem == null || this.blockingItemPreferred != var8 || !(var7 <= this.blockingItemScore)) {
                            this.blockingItem = itemEntity;
                            this.blockingItemPos = blockPos;
                            this.blockingItemScore = var7;
                            this.blockingItemPreferred = var8;
                        }
                    }
                }
            }
        }

        private void evaluateDigBase(BlockPos blockPos, BlockState blockState, Vec3 vector) {
            BlockPos blockPos2 = blockPos.above();
            BlockState blockState2 = MinecraftAccess.mc.level.getBlockState(blockPos2);
            boolean var6 = !ExplosionDamageUtils.isBlastResistant(blockState);
            if (!var6 || this.settings.canPlaceObsidian()) {
                var var7 = new ArrayList(2);
                if (!blockState2.isAir()) {
                    var7.add(blockPos2);
                }
                if (var6 && !blockState.canBeReplaced()) {
                    var7.add(blockPos);
                }
                if (!var7.isEmpty()) {
                    BlockPos blockPos3 = (BlockPos) var7.get(0);
                    if (CrystalPlacementEvaluator.hasBlockingEntity(blockPos2)) {
                        this.addEvaluation(blockPos3, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.ENTITY_ABOVE, 0.0F, 0.0F, 0.0F);
                    } else {
                        float var9 = 0.0F;
                        boolean var10 = false;
                        for (int var11 = 0; var11 < var7.size(); var11++) {
                            BlockPos blockPos4 = (BlockPos) var7.get(var11);
                            BlockState blockState3 = blockPos4.equals(blockPos2) ? blockState2 : blockState;
                            CrystalPlacementEvaluator.Verdict var14 = this.digVerdict(blockPos4, blockState3, var7.subList(0, var11));
                            if (var14 != null) {
                                this.addEvaluation(blockPos3, CrystalPlacementEvaluator.Action.DIG, var14, 0.0F, 0.0F, 0.0F);
                                return;
                            }
                            var9 += this.remainingBreakTicks(blockPos4, blockState3);
                            if (blockPos4.equals(this.mining.activeBlock())) {
                                var10 = true;
                            }
                        }
                        CrystalPlacementEvaluator.DigShape var15 = CrystalPlacementEvaluator.classifyDigShape(blockPos, var7);
                        if (var15 == CrystalPlacementEvaluator.DigShape.CELL) {
                            this.addEvaluation(blockPos3, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.FLOODS, 0.0F, 0.0F, 0.0F);
                        } else if (var6 && ExplosionDamageUtils.findFace(blockPos, this.eyePosition, this.settings.placeRange(), this.settings.placeThroughWalls(), var7) == null) {
                            this.addEvaluation(blockPos3, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.NO_FACE, 0.0F, 0.0F, 0.0F);
                        } else {
                            boolean var16 = !var6 && !this.settings.placeThroughWalls() && !ExplosionDamageUtils.hasVisibleAim(blockPos, this.eyePosition, this.settings.placeRange(), var7);
                            if (!var16 && this.isDigPathVisible(blockPos, var7)) {
                                BlockPos blockPos5 = var6 ? blockPos : null;
                                BlockPos blockPos6 = blockState2.isAir() ? null : blockPos2;
                                this.evaluateDigCandidate(var7, blockPos, var9, var10, vector, blockPos5, blockPos6, var15 == CrystalPlacementEvaluator.DigShape.STREAM);
                            } else {
                                this.addEvaluation(blockPos3, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.NOT_VISIBLE, 0.0F, 0.0F, 0.0F);
                            }
                        }
                    }
                }
            }
        }

        private void evaluateDigThroughOccluder(BlockPos blockPos, Vec3 vector, boolean var3, ExplosionDamageUtils.Visibility var4) {
            if (this.settings.matrixSearch() && this.mining != null && this.mining.allowed()) {
                float var5 = this.settings.placeRange();
                BlockPos blockPos2 = null;
                if (var3) {
                    blockPos2 = ExplosionDamageUtils.faceOccluder(blockPos, this.eyePosition, var5, this.settings.placeThroughWalls());
                } else if (!this.settings.placeThroughWalls()) {
                    blockPos2 = ExplosionDamageUtils.occluderOf(blockPos, this.eyePosition, var5);
                }
                if (blockPos2 == null) {
                    blockPos2 = (var4 != null ? var4 : this.visibilityOf(blockPos)).occluder();
                }
                if (blockPos2 != null && !blockPos2.equals(blockPos) && !blockPos2.equals(blockPos.above())) {
                    BlockState blockState = MinecraftAccess.mc.level.getBlockState(blockPos2);
                    CrystalPlacementEvaluator.Verdict var8 = this.digVerdict(blockPos2, blockState, List.of());
                    if (var8 != null) {
                        this.addEvaluation(blockPos2, CrystalPlacementEvaluator.Action.DIG, var8, 0.0F, 0.0F, 0.0F);
                    } else {
                        var var9 = List.of(blockPos2);
                        CrystalPlacementEvaluator.DigShape var10 = CrystalPlacementEvaluator.classifyDigShape(blockPos, var9);
                        if (var10 == CrystalPlacementEvaluator.DigShape.CELL) {
                            this.addEvaluation(blockPos2, CrystalPlacementEvaluator.Action.DIG, CrystalPlacementEvaluator.Verdict.FLOODS, 0.0F, 0.0F, 0.0F);
                        } else {
                            boolean var11 = ExplosionDamageUtils.isBaseVisible(blockPos, this.eyePosition, var5, var9) && (var3 ? ExplosionDamageUtils.findFace(blockPos, this.eyePosition, var5, this.settings.placeThroughWalls(), var9) != null : this.settings.placeThroughWalls() || ExplosionDamageUtils.hasVisibleAim(blockPos, this.eyePosition, var5, var9));
                            if (!var11) {
                                this.addEvaluation(blockPos2, CrystalPlacementEvaluator.Action.DIG, var3 ? CrystalPlacementEvaluator.Verdict.NO_FACE : CrystalPlacementEvaluator.Verdict.NOT_VISIBLE, 0.0F, 0.0F, 0.0F);
                            } else {
                                this.evaluateDigCandidate(var9, blockPos, this.remainingBreakTicks(blockPos2, blockState), blockPos2.equals(this.mining.activeBlock()), vector, var3 ? blockPos : null, blockPos2, var10 == CrystalPlacementEvaluator.DigShape.STREAM);
                            }
                        }
                    }
                }
            }
        }

        private CrystalPlacementEvaluator.Decision bestDecision() {
            CrystalPlacementEvaluator.Decision var1 = this.bestDirectDecision();
            CrystalPlacementEvaluator.DigCandidate var2 = this.preferredDigCandidate != null ? this.preferredDigCandidate : this.fallbackDigCandidate;
            if (var2 == null) {
                return var1;
            }
            if (var1 != null && !var2.costsLessSelfDamageThan(var1)) {
                return var1;
            }
            this.bestScore = var2.score();
            return var2.toDecision();
        }

        private ExplosionDamageUtils.Visibility visibilityOf(BlockPos blockPos) {
            return this.settings.matrixSearch() ? ExplosionDamageUtils.crystalVisibility(blockPos, this.eyePosition, this.settings.placeRange(), List.of()) : CrystalPlacementEvaluator.ALWAYS_VISIBLE;
        }

        private CrystalPlacementEvaluator.Decision clearingDecision() {
            CrystalPlacementEvaluator.Decision var1 = this.itemClearCandidates.decisionAfterItem(this.blockingItem, this.blockingItemPos);
            return var1 == null ? null : new CrystalPlacementEvaluator.Decision(var1.action(), var1.pos(), var1.base(), null, var1.enemyDamage(), var1.selfDamage(), this.blockingItemPos);
        }

        private float remainingBreakTicks(BlockPos blockPos, BlockState blockState) {
            float var3 = BlockBreakSpeedUtils.breakTicks(blockState, blockPos);
            if (blockPos.equals(this.mining.activeBlock())) {
                var3 *= 1.0F - Mth.clamp(this.mining.activeProgress(), 0.0F, 1.0F);
            }
            return var3;
        }

        private void addEvaluation(BlockPos blockPos, CrystalPlacementEvaluator.Action var2, CrystalPlacementEvaluator.Verdict var3, float var4, float var5, float var6) {
            if (this.evaluations != null) {
                this.evaluations.add(new CrystalPlacementEvaluator.Evaluation(blockPos.immutable(), var2, var3, var4, var5, var6));
            }
        }

        private PlacementSearch(LivingEntity entity, AABB bounds, Vec3 vector, CrystalPlacementEvaluator.Settings var4, CrystalPlacementEvaluator.MiningContext var5, List<CrystalPlacementEvaluator.Evaluation> items) {
            this.blockingItemScore = -Float.MAX_VALUE;
            this.bestScore = -Float.MAX_VALUE;
            this.target = entity;
            this.targetBounds = bounds;
            this.playerBounds = MinecraftAccess.mc.player.getBoundingBox();
            this.eyePosition = vector;
            this.settings = var4;
            this.mining = var5;
            this.evaluations = items;
            this.targetFloorY = Mth.floor(bounds.minY);
            this.placeRangeSquared = var4.placeRange() * var4.placeRange();
            this.explodeRangeSquared = var4.explodeRange() * var4.explodeRange();
            this.digCandidates = items == null ? null : new ArrayList<>();
        }

        private void submitPlacement(CrystalPlacementEvaluator.Action var1, BlockPos blockPos, Vec3 vector, float var4, float var5, CrystalPlacementEvaluator.SpawnState var6, boolean var7) {
            float var8 = CrystalPlacementEvaluator.scorePlacement(var4, var5, this.target, this.targetBounds, vector);
            CrystalPlacementEvaluator.Decision var9 = new CrystalPlacementEvaluator.Decision(var1, blockPos, blockPos, null, var4, var5, null);
            boolean var10 = var7 && var6 == CrystalPlacementEvaluator.SpawnState.FREE;
            if (!this.rejectByDamage(var1, blockPos, blockPos, vector, var4, var5, var8)) {
                CrystalPlacementEvaluator.ActionSlot var11 = this.isPreferredBase(blockPos) ? this.explodeSlot : this.slotFor(var1, var7);
                if (var6 == CrystalPlacementEvaluator.SpawnState.FALLING_ITEM) {
                    if (this.mining != null && this.mining.awaitTicks() >= 2) {
                        this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.BLOCKED, var4, var5, var8);
                    } else {
                        this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.AWAIT, var4, var5, var8);
                        var11.offerAwaitScore(var8);
                    }
                } else {
                    this.addEvaluation(blockPos, var1, CrystalPlacementEvaluator.Verdict.CANDIDATE, var4, var5, var8);
                    if (var10) {
                        this.itemClearCandidates.add(var9, vector, var8);
                    }
                    var11.offer(var9, var8);
                }
            }
        }
    }

    public record DigPreview(List<BlockPos> blocks, BlockPos base, float ticks, boolean active, float enemyDamage, float selfDamage, boolean chosen) {
    }

    public record Evaluation(BlockPos pos, CrystalPlacementEvaluator.Action action, CrystalPlacementEvaluator.Verdict verdict, float enemyDamage, float selfDamage, float score) {
    }

    public record MiningContext(boolean allowed, Set<BlockPos> blacklist, Set<BlockPos> keep, BlockPos activeBlock, float activeProgress, float targetStep, BlockPos preferredBase, Set<BlockPos> pending, int awaitTicks) {
    }

    private static final class ActionSlot {
        private CrystalPlacementEvaluator.Decision decision;
        private float decisionScore;
        private float awaitScore;

        private ActionSlot() {
            this.decisionScore = -Float.MAX_VALUE;
            this.awaitScore = -Float.MAX_VALUE;
        }

        private void offerAwaitScore(float var1) {
            if (var1 > this.awaitScore) {
                this.awaitScore = var1;
            }
        }

        private float bestScore() {
            return Math.max(this.decisionScore, this.awaitScore);
        }

        private CrystalPlacementEvaluator.Decision decision() {
            return this.awaitScore > this.decisionScore ? CrystalPlacementEvaluator.AWAIT_DECISION : this.decision;
        }

        private void offer(CrystalPlacementEvaluator.Decision var1, float var2) {
            if (!(var2 <= this.decisionScore)) {
                this.decisionScore = var2;
                this.decision = var1;
            }
        }
    }

    private enum SpawnState {
        FREE, FALLING_ITEM, BLOCKED
    }

    private static final class ItemClearCandidates {
        private final List<CrystalPlacementEvaluator.Decision> decisions = new ArrayList<>(9);
        private final List<Vec3> aimPositions = new ArrayList<>(9);
        private final List<Float> scores = new ArrayList<>(9);

        private CrystalPlacementEvaluator.Decision decisionAfterItem(ItemEntity itemEntity, BlockPos blockPos) {
            CrystalPlacementEvaluator.Decision var3 = this.findDecisionAfterItem(itemEntity, blockPos, CrystalPlacementEvaluator.Action.PLACE_CRYSTAL);
            return var3 != null ? var3 : this.findDecisionAfterItem(itemEntity, blockPos, CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN);
        }

        private CrystalPlacementEvaluator.Decision findDecisionAfterItem(ItemEntity itemEntity, BlockPos blockPos, CrystalPlacementEvaluator.Action var3) {
            AABB boundingBox = itemEntity.getBoundingBox();
            for (int var5 = 0; var5 < this.decisions.size(); var5++) {
                CrystalPlacementEvaluator.Decision var6 = this.decisions.get(var5);
                if (var6.action() == var3) {
                    BlockPos blockPos2 = var6.pos();
                    if (blockPos2.getX() != blockPos.getX() || blockPos2.getZ() != blockPos.getZ() || Math.abs(blockPos2.getY() - blockPos.getY()) > 2) {
                        AABB bounds = new AABB(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(), blockPos2.getX() + 1.0, blockPos2.getY() + 3.0, blockPos2.getZ() + 1.0);
                        if (!bounds.intersects(boundingBox)) {
                            BlockPos blockPos3 = var3 == CrystalPlacementEvaluator.Action.PLACE_OBSIDIAN ? blockPos2 : null;
                            if (ExplosionDamageUtils.damageWithRemovedBlock(this.aimPositions.get(var5), itemEntity, blockPos3) >= 5.0F) {
                                return var6;
                            }
                        }
                    }
                }
            }
            return null;
        }

        private void add(CrystalPlacementEvaluator.Decision var1, Vec3 vector, float var3) {
            int var4 = 0;
            while (var4 < this.scores.size() && this.scores.get(var4) >= var3) {
                var4++;
            }
            if (var4 < 8) {
                this.decisions.add(var4, var1);
                this.aimPositions.add(var4, vector);
                this.scores.add(var4, var3);
                if (this.decisions.size() > 8) {
                    this.decisions.remove(8);
                    this.aimPositions.remove(8);
                    this.scores.remove(8);
                }
            }
        }
    }

    public enum Action {
        EXPLODE, PLACE_CRYSTAL, PLACE_OBSIDIAN, DIG, AWAIT
    }

    public record Decision(CrystalPlacementEvaluator.Action action, BlockPos pos, BlockPos base, EndCrystal crystal, float enemyDamage, float selfDamage, BlockPos clears) {
    }

    public record Settings(float placeRange, float explodeRange, float minEnemyDamage, float maxSelfDamage, float selfHealthLimit, boolean canPlaceCrystal, boolean canPlaceObsidian, boolean placeThroughWalls, boolean digThroughWalls, boolean explodeThroughWalls, List<? extends LivingEntity> friends, boolean matrixSearch, BlockPos committedBase, Object reservationOwner) {
    }
}
