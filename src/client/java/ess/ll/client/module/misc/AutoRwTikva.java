package ess.ll.client.module.misc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalGetToBlock;
import ess.ll.client.bots.Bot;
import ess.ll.client.bots.Bots;
import ess.ll.client.command.DotCommands;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * AutoRwTikva: two accounts of this client on one server collect the heads at a row of homes. The teleporting
 * account goes home by home (/home t1, t2...: the homes whose names start with the prefix, read from /homes or a set
 * count), the other one comes after it with /tpa every so often (the server's cooldown, 30 s by default), the
 * teleporting one accepts, and the one that came finds the player head near it, walks up and breaks it. After the
 * last home it stops. Either account can be the one being played; the other plays in the background as a bot.
 */
public final class AutoRwTikva {
    public static final ModeSettings ROLE=new ModeSettings("Телепортируется","Основной аккаунт","Второй аккаунт");
    public static final SliderSetting COUNT=new SliderSetting("Хомов (0 — из /homes)",0,50,1,0);
    public static final SliderSetting TPA_DELAY=new SliderSetting("Задержка /tpa, сек",3,60,1,30);
    public static final SliderSetting RADIUS=new SliderSetting("Радиус поиска головы",2,8,.5f,5);
    public static final Setting[] SETTINGS={ROLE,COUNT,TPA_DELAY,RADIUS};
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("rwtikva.txt");
    /** How long to wait for each step before giving up on it, in ticks. */
    private static final int LIST_WAIT=60, HOME_WAIT=160, ACCEPT_AFTER=20, TP_WAIT=240, DIG_WAIT=200;
    /** A player head breaks in 30 ticks by hand; the server takes the dig once 70% of that has passed. */
    private static final int HEAD_DIG_TICKS=32;

    private enum Step { START, LIST, HOME, WAIT_HOME, TPA, ACCEPT, WAIT_TP, FIND, WALK, DIG, CHECK }

    private static String prefix=load();
    private static volatile Step step=Step.START;
    private static int timer;
    private static long lastTpa;
    /** Filled from the network thread when a bot hears /homes, read on the client thread. */
    private static final List<String> homes=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static int index;
    private static volatile Account teleporter, sender;
    private static Vec3 homeFrom;
    private static BlockPos head;
    private static boolean speaking;
    private static int digTick;

    private AutoRwTikva() {}

    public static boolean enabled() { return ClientModules.AUTO_RW_TIKVA!=null && ClientModules.AUTO_RW_TIKVA.enabled(); }

    // ---------------------------------------------------------------- the two accounts

    /**
     * One of the two accounts: a bot of this client, or (bot null) the player's own session when it is not one of
     * the bots, as in a world opened to LAN. The played account acts through the game itself, a bot by its packets.
     */
    private record Account(Bot bot) {
        String name() { return bot!=null ? bot.name : Minecraft.getInstance().getUser().getName(); }
        boolean played() { return bot==null || bot.foreground(); }
        void chat(String text) {
            if(bot!=null) { bot.chat(text); return; }
            var listener=Minecraft.getInstance().getConnection();
            if(listener!=null) { if(text.startsWith("/")) listener.sendCommand(text.substring(1)); else listener.sendChat(text); }
        }
        Vec3 position() {
            var me=Minecraft.getInstance().player;
            if(played()) return me==null ? null : me.position();
            return bot.positioned() ? new Vec3(bot.x(),bot.y(),bot.z()) : null;
        }
        BlockState block(BlockPos pos) {
            var level=Minecraft.getInstance().level;
            if(played()) return level==null ? null : level.getBlockState(pos);
            return bot.block(pos);
        }
        /** Whether this chat line came to this account (null: the game chat of a session that is not a bot). */
        boolean hears(Bot from) { return bot==from || from!=null && from.foreground() && played(); }
    }

    /** The two accounts online on the same server: the teleporting one first. Null when there are not two. */
    private static Account[] accounts() {
        var mc=Minecraft.getInstance();
        Bot main=Bots.main();
        Bot other=null;
        for(Bot bot:Bots.all()) {
            if(bot==main || bot.state()!=Bot.State.ONLINE) continue;
            if(main!=null && !bot.address.equalsIgnoreCase(main.address)) continue;
            other=bot; break;
        }
        if(other==null) return null;
        Account first;
        if(main!=null && main.state()==Bot.State.ONLINE) first=new Account(main);
        else if(main==null && mc.player!=null && !other.foreground()) first=new Account(null);
        else return null;
        Account second=new Account(other);
        return ROLE.is("Основной аккаунт") ? new Account[]{first,second} : new Account[]{second,first};
    }
    private static boolean isHead(BlockState state) {
        return state!=null && (state.is(Blocks.PLAYER_HEAD) || state.is(Blocks.PLAYER_WALL_HEAD));
    }

    // ---------------------------------------------------------------- chat

    /** Every chat line an account receives: the played one through the game chat, a bot through its log. */
    public static void heard(Bot bot,String text) {
        Account listening=teleporter;
        if(!enabled() || speaking || listening==null || !listening.hears(bot) || step!=Step.LIST || text.startsWith("> ")) return;
        Matcher m=Pattern.compile("(?iu)(?<![\\p{L}\\d_])"+Pattern.quote(prefix)+"(\\d+)(?![\\p{L}\\d_])").matcher(text);
        while(m.find()) {
            String name=prefix+m.group(1);
            if(!homes.contains(name)) homes.add(name);
        }
    }
    private static void say(String text) {
        speaking=true;
        try { DotCommands.say("AutoRwTikva: "+text); } finally { speaking=false; }
    }
    private static void stop(String why) {
        say(why);
        if(ClientModules.AUTO_RW_TIKVA!=null) ClientModules.AUTO_RW_TIKVA.setEnabled(false);
    }

    // ---------------------------------------------------------------- the cycle

    public static void tick(Minecraft mc) {
        if(!enabled()) { if(step!=Step.START) reset(mc); return; }
        if(mc.player==null) return;
        Account[] pair=accounts();
        if(pair==null) { stop("нужны два аккаунта в сети на одном сервере (второй — бот в меню аккаунтов)"); return; }
        if(step!=Step.START && (!pair[0].equals(teleporter) || !pair[1].equals(sender))) { stop("аккаунты сменились, остановлено"); return; }
        timer++;
        switch (step) {
            case START -> {
                teleporter=pair[0]; sender=pair[1]; homes.clear(); index=0; timer=0;
                int count=Math.round(COUNT.get());
                if(count>0) {
                    for(int i=1;i<=count;i++) homes.add(prefix+i);
                    say(teleporter.name()+" идёт по "+count+" хомам «"+prefix+"N», "+sender.name()+" ломает головы");
                    go(Step.HOME);
                } else {
                    teleporter.chat("/homes");
                    go(Step.LIST);
                }
            }
            case LIST -> {
                if(timer<LIST_WAIT) return;
                if(homes.isEmpty()) { stop("в ответе на /homes нет хомов «"+prefix+"N»; задай префикс .tikva home <начало> или число хомов в настройках"); return; }
                homes.sort((a,b)->Integer.compare(number(a),number(b)));
                say("хомы: "+String.join(", ",homes)+" — "+teleporter.name()+" телепортируется, "+sender.name()+" ломает головы");
                go(Step.HOME);
            }
            case HOME -> {
                if(index>=homes.size()) { stop("все хомы пройдены"); return; }
                homeFrom=teleporter.position();
                teleporter.chat("/home "+homes.get(index));
                go(Step.WAIT_HOME);
            }
            case WAIT_HOME -> {
                // The home is reached when the account jumps away; a home it already stands at times out instead.
                Vec3 now=teleporter.position();
                if(now!=null && homeFrom!=null && now.distanceTo(homeFrom)>3 || timer>=HOME_WAIT) go(Step.TPA);
            }
            case TPA -> {
                Vec3 a=teleporter.position(),b=sender.position();
                if(a!=null && b!=null && a.distanceTo(b)<4) { go(Step.FIND); return; }
                long wait=Math.round(TPA_DELAY.get()*1000)-(System.currentTimeMillis()-lastTpa);
                if(wait>0) return;
                sender.chat("/tpa "+teleporter.name());
                lastTpa=System.currentTimeMillis();
                go(Step.ACCEPT);
            }
            case ACCEPT -> {
                if(timer<ACCEPT_AFTER) return;
                teleporter.chat("/tpaccept");
                go(Step.WAIT_TP);
            }
            case WAIT_TP -> {
                Vec3 a=teleporter.position(),b=sender.position();
                if(a!=null && b!=null && a.distanceTo(b)<4) go(Step.FIND);
                else if(timer>=TP_WAIT) { say("/tpa не сработал у "+homes.get(index)+", пробую ещё раз"); go(Step.TPA); }
            }
            case FIND -> {
                // The chunk around the new spot may still be on its way after the teleport.
                head=findHead();
                if(head!=null) { walk(mc); go(Step.WALK); }
                else if(timer>=40) { say("у "+homes.get(index)+" нет головы рядом, дальше"); next(); }
            }
            case WALK -> {
                Vec3 at=sender.position();
                if(at!=null && at.add(0,1.62,0).distanceTo(Vec3.atCenterOf(head))<=4.2) { stopWalking(); startDig(mc); go(Step.DIG); }
                else if(timer>=DIG_WAIT) { stopWalking(); say("не дошёл до головы у "+homes.get(index)+", дальше"); next(); }
                else if(timer%40==0) walk(mc);
            }
            case DIG -> {
                if(sender.played()) digForeground(mc);
                if(!isHead(sender.block(head)) || !sender.played() && !sender.bot().digging()) go(Step.CHECK);
                else if(timer>=DIG_WAIT) { say("голова у "+homes.get(index)+" не ломается, дальше"); next(); }
            }
            case CHECK -> {
                if(!isHead(sender.block(head))) { say("голова у "+homes.get(index)+" сломана ("+(index+1)+"/"+homes.size()+")"); next(); }
                else if(timer>=40) { startDig(mc); go(Step.DIG); }
            }
        }
    }
    private static void go(Step next) { step=next; timer=0; }
    private static void next() { index++; head=null; go(Step.HOME); }
    private static int number(String home) {
        try { return Integer.parseInt(home.substring(prefix.length())); } catch(RuntimeException e) { return Integer.MAX_VALUE; }
    }
    private static void reset(Minecraft mc) {
        if(sender!=null && sender.played() && mc.gameMode!=null) mc.gameMode.stopDestroyBlock();
        stopWalking();
        step=Step.START; timer=0; teleporter=sender=null; head=null; homes.clear();
    }

    /** The nearest player head around the account that came. */
    private static BlockPos findHead() {
        Vec3 at=sender.position();
        if(at==null) return null;
        Vec3 eye=at.add(0,1.62,0);
        double radius=RADIUS.get(), best=Double.MAX_VALUE;
        BlockPos found=null;
        int r=(int)Math.ceil(radius);
        BlockPos centre=BlockPos.containing(at);
        for(BlockPos pos:BlockPos.betweenClosed(centre.offset(-r,-r,-r),centre.offset(r,r,r))) {
            double d=Vec3.atCenterOf(pos).distanceTo(eye);
            if(d>radius+1 || d>=best || !isHead(sender.block(pos))) continue;
            best=d; found=pos.immutable();
        }
        return found;
    }
    /** Up to the head: Baritone for the played account, a straight walk for a bot. */
    private static void walk(Minecraft mc) {
        Vec3 at=sender.position();
        if(at==null || head==null || at.add(0,1.62,0).distanceTo(Vec3.atCenterOf(head))<=4.2) return;
        if(sender.played()) {
            try { BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(head)); } catch(Throwable ignored) {}
        } else {
            Vec3 target=Vec3.atBottomCenterOf(head);
            Vec3 away=new Vec3(at.x-target.x,0,at.z-target.z);
            double len=away.length();
            sender.bot().walkTo(len<1e-3 ? target : target.add(away.scale(2.5/len)).with(Direction.Axis.Y,at.y));
        }
    }
    private static void stopWalking() {
        if(sender==null) return;
        if(sender.played()) {
            try { BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything(); } catch(Throwable ignored) {}
        } else sender.bot().walkTo(null);
    }
    private static void startDig(Minecraft mc) {
        digTick=0;
        if(!sender.played()) sender.bot().dig(head,HEAD_DIG_TICKS);
    }
    /** The played account digs like the player would: looks at the head and holds the attack on it. */
    private static void digForeground(Minecraft mc) {
        var p=mc.player;
        if(p==null || mc.gameMode==null) return;
        Vec3 eye=p.getEyePosition(), centre=Vec3.atCenterOf(head);
        double dx=centre.x-eye.x,dy=centre.y-eye.y,dz=centre.z-eye.z;
        p.setYRot((float)Math.toDegrees(Math.atan2(-dx,dz)));
        p.setXRot((float)Math.toDegrees(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))));
        Direction face=Direction.getApproximateNearest(-dx,-dy,-dz);
        if(digTick++==0) mc.gameMode.startDestroyBlock(head,face);
        else mc.gameMode.continueDestroyBlock(head,face);
        p.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
    }

    // ---------------------------------------------------------------- .tikva

    public static List<String> command(List<String> a) {
        String sub=a.size()>1 ? a.get(1).toLowerCase(Locale.ROOT) : "status";
        return switch (sub) {
            case "home", "prefix" -> {
                if(a.size()<3) yield List.of("Использование: .tikva home <начало имени хома>, например .tikva home t");
                prefix=a.get(2);
                save();
                yield List.of("AutoRwTikva: хомы «"+prefix+"1», «"+prefix+"2»...");
            }
            case "start", "on" -> { if(!enabled()) ClientModules.AUTO_RW_TIKVA.setEnabled(true); yield List.of("AutoRwTikva: запущено"); }
            case "stop", "off" -> { if(enabled()) ClientModules.AUTO_RW_TIKVA.setEnabled(false); yield List.of("AutoRwTikva: остановлено"); }
            default -> {
                Account[] pair=accounts();
                yield List.of("AutoRwTikva: "+(enabled() ? "работает, шаг "+step+(homes.isEmpty() ? "" : ", хом "+Math.min(index+1,homes.size())+"/"+homes.size()) : "выключено"),
                        "Хомы: «"+prefix+"N»"+(COUNT.get()>0 ? ", "+Math.round(COUNT.get())+" шт." : ", список из /homes"),
                        pair==null ? "Аккаунты: нужен второй аккаунт в сети на этом сервере" : "Телепортируется "+pair[0].name()+", ломает "+pair[1].name());
            }
        };
    }
    private static String load() {
        try { String s=Files.readString(FILE,StandardCharsets.UTF_8).trim(); return s.isEmpty() ? "t" : s; } catch(Exception e) { return "t"; }
    }
    private static void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE,prefix,StandardCharsets.UTF_8); } catch(Exception ignored) {}
    }

    /** Dev check: the step and the progress. */
    public static String stateForTest() { return step+" "+index+"/"+homes.size()+" "+homes; }
}
