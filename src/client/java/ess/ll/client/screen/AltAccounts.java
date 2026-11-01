package ess.ll.client.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.services.ProfileResult;
import ess.ll.client.mixin.SessionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Saved accounts for the main menu's Accounts page, in config/ess-accounts.json. Accounts without a password
 * are switched to in place; a Microsoft account can only be the one the game was launched with, since its
 * token comes from the launcher and is never written to disk.
 */
public final class AltAccounts {
    private AltAccounts() {}
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("ess-accounts.json");
    private static final Pattern NAME=Pattern.compile("[A-Za-z0-9_]{3,16}");

    public static final class Account {
        public String name;
        public UUID uuid;
        public boolean microsoft, favorite;
        public String serverName="", serverIp="";
        public long serverAt, addedAt;
        transient Supplier<PlayerSkin> skin;
        @SuppressWarnings("unused") private Account() {}
        Account(String name,UUID uuid,boolean microsoft) { this.name=name; this.uuid=uuid; this.microsoft=microsoft; addedAt=System.currentTimeMillis(); }
    }

    private static List<Account> accounts;
    // The launcher's session, kept so switching back to it restores chat signing and the real profile.
    private static User launchUser;
    private static CompletableFuture<ProfileResult> launchProfile;
    private static ProfileKeyPairManager launchKeys;
    private static ReportingContext launchReporting;

    public static List<Account> all() { load(); return accounts; }
    public static Account current() {
        UUID id=Minecraft.getInstance().getUser().getProfileId();
        for(var account:all()) if(account.uuid.equals(id)) return account;
        return null;
    }
    public static boolean isCurrent(Account account) { return account.uuid.equals(Minecraft.getInstance().getUser().getProfileId()); }
    /** Only the launch account's Microsoft session is available; other Microsoft entries need the launcher. */
    public static boolean canUse(Account account) { load(); return !account.microsoft || account.uuid.equals(launchUser.getProfileId()); }
    public static boolean validName(String name) { return NAME.matcher(name).matches(); }

    /** Adds an account without a password; null when the nick is taken. */
    public static Account addOffline(String name) {
        for(var account:all()) if(account.name.equalsIgnoreCase(name)) return null;
        var account=new Account(name,UUIDUtil.createOfflinePlayerUUID(name),false);
        accounts.add(account);
        save();
        return account;
    }
    public static void remove(Account account) { all().remove(account); save(); }
    public static void favorite(Account account) { account.favorite=!account.favorite; save(); }

    /** Logs in as the account without restarting: the session, profile, chat keys and reporting are swapped. */
    public static void use(Account account) {
        load();
        var mc=Minecraft.getInstance();
        var session=(SessionAccessor)mc;
        if(account.uuid.equals(launchUser.getProfileId())) {
            session.ess$setUser(launchUser);
            session.ess$setProfileFuture(launchProfile);
            session.ess$setProfileKeys(launchKeys);
            session.ess$setReportingContext(launchReporting);
            return;
        }
        if(account.microsoft) return;
        session.ess$setUser(new User(account.name,account.uuid,"",Optional.empty(),Optional.empty()));
        // No profile result: the game falls back to a plain profile with this name and id.
        session.ess$setProfileFuture(CompletableFuture.completedFuture(null));
        session.ess$setProfileKeys(ProfileKeyPairManager.EMPTY_KEY_MANAGER);
        session.ess$setReportingContext(ReportingContext.create(ReportEnvironment.local(),UserApiService.OFFLINE));
    }

    /** From ConnectScreenMixin: the server joined becomes the current account's last server. */
    public static void played(ServerData data) {
        var account=current();
        if(account==null) return;
        account.serverName=data.name;
        account.serverIp=data.ip;
        account.serverAt=System.currentTimeMillis();
        save();
    }

    public static PlayerSkin skin(Account account) {
        if(account.skin!=null) return account.skin.get();
        load();
        if(account.uuid.equals(launchUser.getProfileId()) && account.microsoft) {
            // The launch profile carries the skin textures once its lookup has finished.
            ProfileResult result=launchProfile.getNow(null);
            if(!launchProfile.isDone()) return DefaultPlayerSkin.get(account.uuid);
            GameProfile profile=result!=null ? result.profile() : new GameProfile(account.uuid,account.name);
            account.skin=Minecraft.getInstance().getSkinManager().createLookup(profile,false);
        } else {
            PlayerSkin fallback=DefaultPlayerSkin.get(account.uuid);
            account.skin=()->fallback;
        }
        return account.skin.get();
    }

    private static final String[] PARTS={"ess","nova","dark","frost","neo","pix","kai","zen","vex","lux","rex","sky","ash","volt","myst","rio"};
    public static String randomName() {
        var random=new Random();
        String name;
        do {
            String a=PARTS[random.nextInt(PARTS.length)], b=PARTS[random.nextInt(PARTS.length)];
            name=Character.toUpperCase(a.charAt(0))+a.substring(1)+(random.nextBoolean() ? "_" : "")+Character.toUpperCase(b.charAt(0))+b.substring(1);
            if(random.nextInt(3)==0) name+=random.nextInt(10,1000);
        } while(!validName(name));
        return name;
    }

    private static void load() {
        if(accounts!=null) return;
        var mc=Minecraft.getInstance();
        launchUser=mc.getUser();
        launchProfile=((SessionAccessor)mc).ess$profileFuture();
        launchKeys=mc.getProfileKeyPairManager();
        launchReporting=mc.getReportingContext();
        accounts=new ArrayList<>();
        try {
            if(Files.exists(FILE)) {
                List<Account> stored=GSON.fromJson(Files.readString(FILE),new TypeToken<List<Account>>(){}.getType());
                if(stored!=null) for(var account:stored) if(account!=null && account.name!=null && account.uuid!=null) accounts.add(account);
            }
        } catch(Exception ignored) {}
        // The launch account is always listed; an id that is not the offline one for its name came from Microsoft.
        boolean microsoft=!launchUser.getProfileId().equals(UUIDUtil.createOfflinePlayerUUID(launchUser.getName()));
        Account launch=null;
        for(var account:accounts) if(account.uuid.equals(launchUser.getProfileId())) launch=account;
        if(launch==null) { launch=new Account(launchUser.getName(),launchUser.getProfileId(),microsoft); accounts.addFirst(launch); }
        launch.name=launchUser.getName();
        launch.microsoft=microsoft;
        save();
    }
    private static void save() {
        if(accounts==null) return;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE,GSON.toJson(accounts));
        } catch(IOException ignored) {}
    }
}
