package ess.ll.client.features;

import com.mojang.authlib.GameProfile;
import ess.ll.client.integration.EntityExtensionAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

/**
 * .fakeplayer: client-side players to test combat on. A fake player stands where you were, wears a copy of your
 * armour and holds a totem that never runs out. Nothing about it reaches the server: your hits on it are not sent,
 * and it takes their damage here instead (attack cooldown, crits, Sharpness, its armour and Protection), as well as
 * the damage of explosions near it (crystals, anchors). Below zero health it pops the totem like a real player:
 * health 1, absorption, regeneration, and the pop event every module sees.
 *
 * To the modules it is an ordinary player, so Aura, TargetESP, CrystalAura and the HUD target it; AntiBot is told
 * it is real.
 */
public final class FakePlayers {
    /** Entity ids far below anything the server hands out. */
    private static final int FIRST_ID=-1_000_000;
    private static final List<Fake> FAKES=new ArrayList<>();
    private static ClientLevel level;
    private static int nextId=FIRST_ID;

    private FakePlayers() {}

    private static final class Fake {
        final RemotePlayer player;
        /** Ticks left of the hurt window and the damage that opened it, as on a server. */
        int invulnerable;
        float lastDamage;
        int regenTicks, pops;
        Fake(RemotePlayer player) { this.player=player; }
    }

    public static boolean isFake(Entity entity) { return find(entity)!=null; }
    /** Names of the fake players, for command hints. */
    public static List<String> names() { return FAKES.stream().map(f->f.player.getName().getString()).toList(); }
    private static Fake find(Entity entity) {
        for(Fake fake:FAKES) if(fake.player==entity) return fake;
        return null;
    }

    /** .fakeplayer add / del / list; returns the lines to show. */
    public static List<String> command(List<String> a) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) return List.of("Зайди в мир");
        String sub=a.size()>1 ? a.get(1).toLowerCase(Locale.ROOT) : "add";
        switch (sub) {
            case "add", "spawn" -> {
                String name=a.size()>2 ? a.get(2) : "FakePlayer";
                if(name.length()>16) return List.of("Имя длиннее 16 символов");
                add(name);
                return List.of("FakePlayer: "+name+" стоит на твоём месте, с тотемом");
            }
            case "del", "remove", "rm" -> {
                if(FAKES.isEmpty()) return List.of("FakePlayer: фейковых игроков нет");
                String name=a.size()>2 ? a.get(2) : "all";
                int removed=remove(name);
                return List.of(removed==0 ? "FakePlayer: нет игрока "+name : "FakePlayer: убрано "+removed);
            }
            case "list" -> {
                if(FAKES.isEmpty()) return List.of("FakePlayer: фейковых игроков нет");
                List<String> lines=new ArrayList<>();
                for(Fake fake:FAKES) lines.add(fake.player.getName().getString()+" · "+String.format(Locale.ROOT,"%.1f",fake.player.getHealth()+fake.player.getAbsorptionAmount())
                        +" HP · тотемов: "+fake.pops);
                return lines;
            }
            default -> { return List.of(".fakeplayer add [имя] · .fakeplayer del [имя|all] · .fakeplayer list"); }
        }
    }

    public static void add(String name) {
        var mc=Minecraft.getInstance();
        var me=mc.player;
        if(level!=mc.level) { FAKES.clear(); level=mc.level; }
        var fake=new RemotePlayer(mc.level,new GameProfile(UUID.randomUUID(),name));
        fake.setId(nextId--);
        fake.getInventory().replaceWith(me.getInventory());
        for(EquipmentSlot slot:EquipmentSlot.values()) {
            ItemStack stack=me.getItemBySlot(slot).copy();
            fake.setItemSlot(slot,stack);
            // The client never applies equipment attributes itself; the armour has to count for every module.
            if(slot!=EquipmentSlot.MAINHAND && slot!=EquipmentSlot.OFFHAND)
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY).forEach(slot,(attribute,modifier)->{
                    var instance=fake.getAttribute(attribute);
                    if(instance!=null) instance.addOrUpdateTransientModifier(modifier);
                });
        }
        fake.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        fake.snapTo(me.getX(),me.getY(),me.getZ(),me.getYRot(),me.getXRot());
        fake.setYHeadRot(me.getYHeadRot());
        fake.yBodyRot=me.yBodyRot;
        fake.setOnGround(true);
        fake.setHealth(fake.getMaxHealth());
        ((EntityExtensionAccess)fake).setBotEntity(true);
        mc.level.addEntity(fake);
        FAKES.add(new Fake(fake));
    }
    /** Removes the fake players with this name (any case), or all of them for "all". */
    public static int remove(String name) {
        int removed=0;
        for(var it=FAKES.iterator();it.hasNext();) {
            Fake fake=it.next();
            if(!name.equalsIgnoreCase("all") && !fake.player.getName().getString().equalsIgnoreCase(name)) continue;
            if(fake.player.level() instanceof ClientLevel world) world.removeEntity(fake.player.getId(),Entity.RemovalReason.DISCARDED);
            it.remove();
            removed++;
        }
        return removed;
    }

    public static void tick() {
        var mc=Minecraft.getInstance();
        if(FAKES.isEmpty()) return;
        // A new world or dimension drops its entities, the fakes with them.
        if(mc.level!=level || mc.player==null) { FAKES.clear(); level=mc.level; return; }
        for(Fake fake:FAKES) {
            var p=fake.player;
            if(fake.invulnerable>0) fake.invulnerable--;
            // Regeneration II after a pop: half a heart every 12 ticks.
            if(fake.regenTicks>0) {
                fake.regenTicks--;
                if(fake.regenTicks%12==0) p.setHealth(Math.min(p.getMaxHealth(),p.getHealth()+1));
            }
            if(!p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TOTEM_OF_UNDYING));
        }
    }

    /**
     * Called instead of sending the attack packet when the target is a fake player; returns false for real ones.
     * Runs before the attack resets the cooldown, so the cooldown still counts.
     */
    public static boolean attack(Player attacker,Entity target) {
        Fake fake=find(target);
        if(fake==null) return false;
        // The weapon's damage from the item: the client does not keep it in the player's attributes.
        ItemStack weapon=attacker.getMainHandItem();
        float damage=(float)weapon.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY)
                .compute(Attributes.ATTACK_DAMAGE,attacker.getAttributeBaseValue(Attributes.ATTACK_DAMAGE),EquipmentSlot.MAINHAND);
        float charge=attacker.getAttackStrengthScale(.5f);
        int sharpness=level(Enchantments.SHARPNESS,weapon);
        float bonus=sharpness>0 ? .5f*sharpness+.5f : 0;
        damage=damage*(.2f+charge*charge*.8f)+bonus*charge;
        boolean crit=charge>.9f && attacker.fallDistance>0 && !attacker.onGround() && !attacker.onClimbable() && !attacker.isInWater()
                && !attacker.hasEffect(MobEffects.BLINDNESS) && !attacker.isPassenger() && !attacker.isSprinting();
        if(crit) damage*=1.5f;
        DamageSource source=fake.player.damageSources().playerAttack(attacker);
        damage=CombatRules.getDamageAfterAbsorb(fake.player,damage,source,fake.player.getArmorValue(),(float)fake.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        damage=CombatRules.getDamageAfterMagicAbsorb(damage,protection(fake.player,false));
        hurt(fake,damage,source);
        return true;
    }

    /**
     * An explosion the server reported: crystals, anchors and TNT hurt the fake players near it, by the server's own
     * formula (how much of the body the blast sees, difficulty, armour, Protection and Blast Protection).
     */
    public static void explosion(Vec3 center,float radius) {
        var mc=Minecraft.getInstance();
        for(Fake fake:new ArrayList<>(FAKES)) {
            var p=fake.player;
            double reach=radius*2;
            double dist=Math.sqrt(p.distanceToSqr(center))/reach;
            if(dist>1) continue;
            double impact=(1-dist)*ServerExplosion.getSeenPercent(center,p);
            float damage=(float)((impact*impact+impact)/2*7*reach+1);
            damage=switch (mc.level.getDifficulty()) {
                case PEACEFUL -> 0;
                case EASY -> Math.min(damage/2+1,damage);
                case NORMAL -> damage;
                case HARD -> damage*1.5f;
            };
            DamageSource source=p.damageSources().explosion(null,null);
            damage=CombatRules.getDamageAfterAbsorb(p,damage,source,p.getArmorValue(),(float)p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
            damage=CombatRules.getDamageAfterMagicAbsorb(damage,protection(p,true));
            if(damage>0) hurt(fake,damage,source);
        }
    }

    private static void hurt(Fake fake,float damage,DamageSource source) {
        var mc=Minecraft.getInstance();
        var p=fake.player;
        // The hurt window: a weaker hit inside it does nothing, a stronger one only deals the difference.
        if(fake.invulnerable>10) {
            if(damage<=fake.lastDamage) return;
            float extra=damage-fake.lastDamage;
            fake.lastDamage=damage;
            damage=extra;
        } else {
            fake.lastDamage=damage;
            fake.invulnerable=20;
            mc.getConnection().handleDamageEvent(new ClientboundDamageEventPacket(p,source));
        }
        float absorbed=Math.min(p.getAbsorptionAmount(),damage);
        p.setAbsorptionAmount(p.getAbsorptionAmount()-absorbed);
        float health=p.getHealth()-(damage-absorbed);
        if(health>0) { p.setHealth(health); return; }
        // The totem: health 1, Absorption II and Regeneration II, and the pop every module hears about.
        fake.pops++;
        p.setHealth(1);
        p.setAbsorptionAmount(4);
        fake.regenTicks=45*20;
        mc.getConnection().handleEntityEvent(new ClientboundEntityEventPacket(p,(byte)35));
    }

    private static int level(ResourceKey<Enchantment> key,ItemStack stack) {
        var mc=Minecraft.getInstance();
        if(stack.isEmpty() || mc.level==null) return 0;
        Holder<Enchantment> holder=mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key).orElse(null);
        return holder==null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(holder,stack);
    }
    /** Enchantment protection points of the armour, as the server sums them (capped at 20). */
    private static float protection(Player player,boolean explosion) {
        int points=0;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}) {
            var stack=player.getItemBySlot(slot);
            points+=level(Enchantments.PROTECTION,stack);
            if(explosion) points+=2*level(Enchantments.BLAST_PROTECTION,stack);
        }
        return Math.min(20,points);
    }

    /** Dev check: the first fake player's health, absorption and pops. */
    public static String stateForTest() {
        if(FAKES.isEmpty()) return "none";
        var p=FAKES.getFirst().player;
        return String.format(Locale.ROOT,"hp=%.1f abs=%.1f pops=%d",p.getHealth(),p.getAbsorptionAmount(),FAKES.getFirst().pops);
    }
    public static Entity firstForTest() { return FAKES.isEmpty() ? null : FAKES.getFirst().player; }
}
