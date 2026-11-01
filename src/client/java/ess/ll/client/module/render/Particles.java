package ess.ll.client.module.render;

import ess.ll.client.core.MutableVector4d;
import ess.ll.client.event.AttackEntityEvent;
import ess.ll.client.event.Subscribe;
import ess.ll.client.event.TickEvent;
import ess.ll.client.event.TotemPopEvent;
import ess.ll.client.module.ClientModule;
import ess.ll.client.module.ModuleCategory;
import ess.ll.client.module.ModuleSubCategory;
import ess.ll.client.network.PreMovementPacketEvent;
import ess.ll.client.render.ClientParticle;
import ess.ll.client.render.ClientParticleManager;
import ess.ll.client.render.ColorUtils;
import ess.ll.client.render.ParticleCollisionMode;
import ess.ll.client.render.ParticleInterpolationMode;
import ess.ll.client.setting.ModeSetting;
import ess.ll.client.setting.MultiToggleSetting;
import ess.ll.client.setting.NamedOption;
import ess.ll.client.setting.NumberSetting;
import ess.ll.client.setting.ToggleSetting;
import ess.ll.client.util.MovementUtils;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class Particles extends ClientModule {
    private final MultiToggleSetting spawnTriggers;
    private final ModeSetting colorMode;
    private final ModeSetting hitPhysics;
    private final NumberSetting<Integer> hitParticleCount;
    private final NumberSetting<Integer> worldParticleCount;
    private final MultiToggleSetting textureOptions;
    private final ToggleSetting glow;
    private final ArrayDeque<Entity> pendingEntities;
    private final Random random;
    private final Set<Block> ignoredBlocks;
    private int tickCounter;
    private boolean rainbowColors;

    @Override
    public void onDisable() {
        super.onDisable();
        this.resetState();
    }

    @Subscribe
    private void onPreMovementPacket(PreMovementPacketEvent event) {
        if (this.spawnTriggers.isSelected("Ходьбе")) {
            if (mc.player != null) {
                if (event.getLastX() != event.getX() || event.getLastY() != event.getY() || event.getLastZ() != event.getZ()) {
                    for (int var2 = 0; var2 < 2; var2++) {
                        float var3 = mc.player.getBbHeight() * this.random.nextFloat();
                        this.emitBrightSquare(new MutableVector4d(event.getLastX() + this.random.nextFloat((-0.5F), 0.5F), event.getLastY() + var3, event.getLastZ() + this.random.nextFloat((-0.5F), 0.5F)), new MutableVector4d(mc.player.getDeltaMovement().x + this.random.nextFloat((-0.1F), 0.1F), 0.0, mc.player.getDeltaMovement().z + this.random.nextFloat((-0.1F), 0.1F)).multiply(2.0F * (1.0F + this.random.nextFloat())), 8.0E-6F, 800, this.particleColor(ClientParticleManager.INSTANCE.size() * 50), this.random.nextFloat(0.1F, 0.225F), true);
                    }
                }
            }
        }
    }

    public ToggleSetting getGlow() {
        return this.glow;
    }

    private static int msToTicks(int var0) {
        return Math.max(1, Math.round(var0 / 50.0F));
    }

    public NumberSetting<Integer> getHitParticleCount() {
        return this.hitParticleCount;
    }

    private void spawnHitEffect(LivingEntity entity) {
        int var2 = msToTicks(3000);
        int value = this.hitParticleCount.getValue();
        for (int var4 = 0; var4 < value; var4++) {
            ClientParticle.SpawnOptions spawnOptions = new ClientParticle.SpawnOptions(entity.getX() + (this.random.nextDouble() - 0.5) * 0.5, entity.getY() + this.random.nextDouble() * entity.getBbHeight(), entity.getZ() + (this.random.nextDouble() - 0.5) * 0.5).withVelocity((this.random.nextDouble() * 0.5 - 0.25) * 0.9, this.random.nextDouble() * 0.25 * 0.01, (this.random.nextDouble() * 0.5 - 0.25) * 0.9).withCollisionMode(ParticleCollisionMode.RICOCHET).withInterpolationMode(ParticleInterpolationMode.LINEAR).withGravity(1.0F).withCollisionEnabled(true).withSize(0.6F, 0.6F).withLifetime(var2).withMaxRenderDistance(15.0);
            ClientParticleManager.INSTANCE.spawn(spawnOptions, this.particleColor(var4 * 30), 0.15F, var2);
        }
    }

    private void spawnAmbient() {
        Vec3 position2 = mc.gameRenderer.mainCamera().position();
        double var2 = position2.x();
        double var4 = position2.y();
        double var6 = position2.z();
        float var8 = 3.0F;
        if (this.tickCounter % 15 == 0) {
            int var9 = this.worldParticleCount.getValue() * 10;
            for (int var10 = 0; var10 < var9; var10++) {
                double var11 = this.random.nextFloat(1.0F, 30.0F);
                double var13 = Math.toRadians(this.random.nextFloat(0.0F, 360.0F));
                double var15 = -Math.sin(var13) * var11;
                double var17 = Math.cos(var13) * var11;
                this.emitBrightSquare(new MutableVector4d(var2 + var15, var4 + this.random.nextFloat((-15.0F), 15.0F), var6 + var17), new MutableVector4d(this.random.nextFloat(-var8, var8), this.random.nextFloat((-1.0F), 1.0F), this.random.nextFloat(-var8, var8)), 8.0E-6F, 2500, this.particleColor(ClientParticleManager.INSTANCE.size() * 50), this.random.nextFloat(0.1F, 0.225F), true);
            }
            this.tickCounter = 0;
        }
    }

    private void spawnTrail(Entity entity) {
        if (!(MovementUtils.speedPerSecond(entity) < 0.35F)) {
            Vec3 position2 = entity.position();
            Vec3 deltaMovement = entity.getDeltaMovement();
            double var4 = position2.x();
            double var6 = position2.y();
            double var8 = position2.z();
            double var10 = deltaMovement.x();
            double var12 = deltaMovement.y();
            double var14 = deltaMovement.z();
            for (int var16 = 0; var16 < 5; var16++) {
                float var17 = this.random.nextFloat();
                this.emitBrightSquare(new MutableVector4d(var4, var6, var8), new MutableVector4d(this.random.nextFloat(-var17, var17) + var10, this.random.nextFloat(-var17, var17) + var12, this.random.nextFloat(-var17, var17) + var14), 8.0E-6F, 1000, this.particleColor(ClientParticleManager.INSTANCE.size() * 50), this.random.nextFloat(0.1F, 0.225F), true);
            }
        }
    }

    public ModeSetting getHitPhysics() {
        return this.hitPhysics;
    }

    public Random getRandom() {
        return this.random;
    }

    private void emitSquare(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6, float var7, boolean var8, int var9) {
        this.emitParticle(mutableVector4d, mutableVector4d2, var3, var5, 300, var6, var7, var8, var9);
    }

    public boolean isRainbowColors() {
        return this.rainbowColors;
    }

    public ModeSetting getColorMode() {
        return this.colorMode;
    }

    private void emitQuad(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6, int var7, float var8, boolean var9) {
        this.emitParticle(mutableVector4d, mutableVector4d2, var3, var5, var6, var7, var8, var9, 0);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        this.rainbowColors = this.colorMode.is("Радужные");
        if (mc.level == null) {
            this.resetState();
        } else {
            this.tickCounter++;
            boolean selected = this.spawnTriggers.isSelected("Бездействие");
            boolean selected2 = this.spawnTriggers.isSelected("Эндер жемчуг");
            boolean selected3 = this.spawnTriggers.isSelected("Стрела");
            boolean selected4 = this.spawnTriggers.isSelected("Поп тотема");
            this.flushPending(selected4);
            if (selected) {
                this.spawnAmbient();
            }
            this.refreshEntities(selected2, selected3);
        }
    }

    public Particles() {
        super(ModuleCategory.Render, ModuleSubCategory.World);
        this.spawnTriggers = new MultiToggleSetting("Спавнить при").addOption(NamedOption.of("Ударе", true)).addOption(NamedOption.of("Поп тотема", true)).addOption(NamedOption.of("Бездействие", false)).addOption(NamedOption.of("Ходьбе", true)).addOption(NamedOption.of("Эндер жемчуг", false)).addOption(NamedOption.of("Стрела", false)).withDescription("Выбор условий спавна").withContainer(this);
        this.colorMode = new ModeSetting("Цвет").addModes("От темы", "Радужные").withDefault("Радужные").withContainer(this);
        this.hitPhysics = new ModeSetting("Физика удара").addModes("Отскок", "Рикошет").withDefault("Рикошет").withDescription("Отскок — частицы упираются в блоки и докатываются; Рикошет — отражаются от граней и не тормозят").withVisibility(() -> this.spawnTriggers.isSelected("Ударе")).withContainer(this);
        this.hitParticleCount = new NumberSetting<Integer>("Спавнить при ударе").withMin(5).withMax(50).withStep(1).withDefault(15).withVisibility(() -> this.spawnTriggers.isSelected("Ударе")).withContainer(this);
        this.worldParticleCount = new NumberSetting<Integer>("Спавнить в мире").withMin(3).withMax(50).withStep(1).withDefault(20).withVisibility(() -> this.spawnTriggers.isSelected("Бездействие")).withContainer(this);
        this.textureOptions = new MultiToggleSetting("Элементы").addOption(NamedOption.of("Орбиз", true)).addOption(NamedOption.of("Звезды", false)).addOption(NamedOption.of("Сердечки", false)).addOption(NamedOption.of("Доллары", false)).addOption(NamedOption.of("Снежинки", false)).addOption(NamedOption.of("Короны", false)).addOption(NamedOption.of("Молнии", false)).addOption(NamedOption.of("Линии", false)).addOption(NamedOption.of("Кругляши", false)).addOption(NamedOption.of("Ромбы", false)).addOption(NamedOption.of("Искры", false)).withMinSelected(1).withContainer(this);
        this.glow = new ToggleSetting("Свечение").setValue(false).withContainer(this);
        this.pendingEntities = new ArrayDeque<>();
        this.random = new Random();
        this.ignoredBlocks = Collections.newSetFromMap(new IdentityHashMap<>());
        this.rainbowColors = false;
        this.setName("Particles");
        this.initIgnoredBlocks();
    }

    private void initIgnoredBlocks() {
        this.ignoredBlocks.add(Blocks.AIR);
        this.ignoredBlocks.add(Blocks.CAVE_AIR);
        this.ignoredBlocks.add(Blocks.VOID_AIR);
        this.ignoredBlocks.add(Blocks.WATER);
        this.ignoredBlocks.add(Blocks.LAVA);
        this.ignoredBlocks.add(Blocks.GRASS_BLOCK);
        this.ignoredBlocks.add(Blocks.TALL_GRASS);
        this.ignoredBlocks.add(Blocks.FERN);
        this.ignoredBlocks.add(Blocks.LARGE_FERN);
        this.ignoredBlocks.add(Blocks.DEAD_BUSH);
        this.ignoredBlocks.add(Blocks.SEAGRASS);
        this.ignoredBlocks.add(Blocks.TALL_SEAGRASS);
        this.ignoredBlocks.add(Blocks.VINE);
        this.ignoredBlocks.add(Blocks.GLOW_LICHEN);
        this.ignoredBlocks.add(Blocks.SUGAR_CANE);
        this.ignoredBlocks.add(Blocks.CACTUS);
        this.ignoredBlocks.add(Blocks.SNOW);
        this.ignoredBlocks.add(Blocks.COBWEB);
        this.ignoredBlocks.add(Blocks.ICE);
        this.ignoredBlocks.add(Blocks.PACKED_ICE);
        this.ignoredBlocks.add(Blocks.BLUE_ICE);
        this.ignoredBlocks.add(Blocks.FROSTED_ICE);
        this.ignoredBlocks.add(Blocks.SLIME_BLOCK);
        this.ignoredBlocks.add(Blocks.HONEY_BLOCK);
    }

    public MultiToggleSetting getSpawnTriggers() {
        return this.spawnTriggers;
    }

    @Subscribe
    private void onAttackEntity(AttackEntityEvent event) {
        if (event.getPhase() != AttackEntityEvent.Phase.POST) {
            if (mc.player != null && mc.level != null) {
                if (this.spawnTriggers.isSelected("Ударе")) {
                    if (event.getEntity() instanceof LivingEntity var2) {
                        if (this.hitPhysics.is("Рикошет")) {
                            this.spawnHitEffect(var2);
                        } else {
                            this.spawnHitBurst(var2);
                        }
                    }
                }
            }
        }
    }

    private void flushPending(boolean var1) {
        if (!this.pendingEntities.isEmpty()) {
            if (!var1) {
                this.pendingEntities.clear();
            } else {
                Entity entity;
                while ((entity = this.pendingEntities.pollFirst()) != null) {
                    ClientParticleManager.INSTANCE.emitBurst(entity, 30);
                }
            }
        }
    }

    private void onArrowTrail(Arrow arrow) {
        this.spawnTrail(arrow);
    }

    private void emitDefaultQuad(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6) {
        this.emitQuad(mutableVector4d, mutableVector4d2, var3, var5, 300, var6, this.random.nextFloat(0.1F, 0.225F), false);
    }

    public MultiToggleSetting getTextureOptions() {
        return this.textureOptions;
    }

    private void onPearlTrail(ThrownEnderpearl thrownEnderpearl) {
        this.spawnTrail(thrownEnderpearl);
    }

    public int particleColor(int var1) {
        return this.rainbowColors ? ColorUtils.rainbow(1, this.random.nextInt(), 1.0F, 1.0F, 1.0F) : ColorUtils.Theme.animatedThemeGradient(var1);
    }

    public NumberSetting<Integer> getWorldParticleCount() {
        return this.worldParticleCount;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.resetState();
    }

    public ArrayDeque<Entity> getPendingEntities() {
        return this.pendingEntities;
    }

    private void emitColoredQuad(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6, int var7) {
        this.emitQuad(mutableVector4d, mutableVector4d2, var3, var5, var6, var7, this.random.nextFloat(0.1F, 0.225F), false);
    }

    private void spawnHitBurst(LivingEntity entity) {
        float var2 = 0.5F;
        Vec3 deltaMovement = entity.getDeltaMovement();
        for (int var4 = 0; var4 < this.hitParticleCount.getValue(); var4++) {
            float var5 = this.random.nextFloat(0.1F, 0.15F);
            int var6 = this.random.nextInt(40, 60);
            int var7 = Math.max(12, var6 / 2);
            double var8 = this.random.nextFloat(-var2, var2) + deltaMovement.x * 0.35;
            double var10 = this.random.nextFloat((-0.2F), 0.15F) + Math.max(0.0, deltaMovement.y) * 0.25;
            double var12 = this.random.nextFloat(-var2, var2) + deltaMovement.z * 0.35;
            ClientParticle.SpawnOptions spawnOptions = new ClientParticle.SpawnOptions(entity.getX() + this.random.nextDouble((-0.25), 0.25), entity.getY() + this.random.nextDouble(0.1, entity.getBbHeight()), entity.getZ() + this.random.nextDouble((-0.25), 0.25)).withVelocity(var8, var10, var12).withGravity(0.2F).withAccelerateWhenBlocked(true).withFriction(0.9F).withRestitution(2.0F).withCollisionEnabled(true).withSize(var5, var5).withLifetime(var6).withMotionDelay(this.random.nextInt(1, 3));
            ClientParticleManager.INSTANCE.spawn(spawnOptions, this.particleColor(this.random.nextInt()), var5, var7);
        }
    }

    public Set<Block> getIgnoredBlocks() {
        return this.ignoredBlocks;
    }

    private void resetState() {
        this.pendingEntities.clear();
        this.tickCounter = 0;
    }

    private void emitParticle(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6, int var7, float var8, boolean var9, int var10) {
        if (mc.level != null) {
            Block block = mc.level.getBlockState(BlockPos.containing(mutableVector4d.x, mutableVector4d.y, mutableVector4d.z)).getBlock();
            if (this.ignoredBlocks.contains(block)) {
                int var12 = msToTicks(var5);
                int var13 = msToTicks(var6);
                float var14 = (float) Math.max(0.0, var3 * 175.0);
                float var15 = Math.max(0.02F, var8);
                ClientParticle.SpawnOptions spawnOptions = new ClientParticle.SpawnOptions(mutableVector4d.x, mutableVector4d.y, mutableVector4d.z).withVelocity(mutableVector4d2.x * 0.07, mutableVector4d2.y * 0.07, mutableVector4d2.z * 0.07).withGravity(var14).withLifetime(var12).withFriction(0.98F).withCollisionEnabled(!var9).withSize(var15, var15).withMotionDelay(var10);
                ClientParticleManager.INSTANCE.spawn(spawnOptions, var7, var8, var13);
            }
        }
    }

    private void refreshEntities(boolean var1, boolean var2) {
        if (mc.level != null) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (var1 && entity instanceof ThrownEnderpearl var5) {
                    this.onPearlTrail(var5);
                }
                if (var2 && entity instanceof Arrow var6) {
                    this.onArrowTrail(var6);
                }
            }
        }
    }

    private void emitBrightSquare(MutableVector4d mutableVector4d, MutableVector4d mutableVector4d2, double var3, int var5, int var6, float var7, boolean var8) {
        this.emitQuad(mutableVector4d, mutableVector4d2, var3, var5, 300, var6, var7, var8);
    }

    public int getTickCounter() {
        return this.tickCounter;
    }

    @Subscribe
    private void onTotemPop(TotemPopEvent event) {
        Entity entity = event.getEntity();
        if (entity != null) {
            this.pendingEntities.addLast(entity);
        }
    }
}
