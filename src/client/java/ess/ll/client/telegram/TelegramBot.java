package ess.ll.client.telegram;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ess.ll.client.bots.Bot;
import ess.ll.client.bots.Bots;
import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Telegram: control the client from a Telegram bot. You make a bot with @BotFather, give its token to the client, start
 * it and send the bot the code the client shows: from then on that chat (and only it) drives the game. Plain text is
 * chat from the chosen account, "!home" a server command, ".toggle Aura" a client command, "#goto 0 0" Baritone; the
 * menu buttons give the status, a screenshot, the inventory, the modules and the accounts (your own and the bots).
 *
 * The token and the linked chat live in config/ess-best/telegram.json, on this computer only.
 */
public final class TelegramBot {
    public static final BooleanSetting FORWARD_CHAT=new BooleanSetting("Пересылать чат",false);
    public static final BooleanSetting NOTIFY=new BooleanSetting("Уведомления (смерть, кик)",true);
    public static final ActionSetting MENU=new ActionSetting("Открыть меню",TelegramBot::openMenu);
    public static final Setting[] SETTINGS={MENU,FORWARD_CHAT,NOTIFY};

    public enum State { STOPPED, STARTING, RUNNING, ERROR }

    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("ess-best").resolve("telegram.json");
    private static final Gson GSON=new Gson();
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    /** Replies go out on their own thread, in order, so the game never waits on Telegram. */
    private static final ExecutorService OUT=Executors.newSingleThreadExecutor(r->{ Thread t=new Thread(r,"ess-telegram-out"); t.setDaemon(true); return t; });
    private static String token="",linkedName="";
    private static long chatId;
    private static boolean loaded;
    private static volatile State state=State.STOPPED;
    private static volatile String error="",botName="",code="";
    private static volatile Thread poller;
    /** The account plain text and commands go to: a bot's name, or "" for the one being played. */
    private static String target="";
    private static final StringBuilder CHAT=new StringBuilder();
    private static long chatFlush;
    private static boolean wasDead,wasConnected;
    private static String lastScreen="";

    private TelegramBot() {}

    public static boolean enabled() { return ClientModules.TELEGRAM!=null && ClientModules.TELEGRAM.enabled(); }

    // ---- what the menu shows and sets ------------------------------------------------------------------------
    public static State state() { return state; }
    public static String error() { return error; }
    /** The bot's @name once it has started. */
    public static String botName() { return botName; }
    public static String code() { return code; }
    public static boolean hasToken() { load(); return !token.isBlank(); }
    /** The token with only its ends shown. */
    public static String maskedToken() {
        load();
        if(token.length()<12) return token.isEmpty() ? "" : "••••";
        return token.substring(0,6)+"••••••"+token.substring(token.length()-4);
    }
    public static void setToken(String value) {
        load();
        String t=value==null ? "" : value.trim();
        if(t.equals(token)) return;
        token=t; chatId=0; linkedName="";
        save();
        if(state==State.RUNNING || state==State.STARTING) { stop(); start(); }
    }
    public static boolean linked() { load(); return chatId!=0; }
    public static String linkedName() { return linkedName; }
    public static void unlink() { load(); chatId=0; linkedName=""; code=newCode(); save(); }
    public static void openMenu() {
        var mc=Minecraft.getInstance();
        mc.execute(()->mc.gui.setScreen(new TelegramScreen(mc.gui.screen())));
    }

    // ---- start and stop ---------------------------------------------------------------------------------------
    /** Client tick: the module switch starts and stops the bot; notifications and the chat feed. */
    public static void tick(Minecraft mc) {
        boolean on=enabled();
        if(on && (state==State.STOPPED)) start();
        if(!on && state!=State.STOPPED && state!=State.ERROR) stop();
        if(!on && state==State.ERROR) state=State.STOPPED;
        if(state!=State.RUNNING || chatId==0) return;
        if(NOTIFY.get()) notifications(mc);
        if(FORWARD_CHAT.get() && System.currentTimeMillis()>chatFlush) {
            String lines;
            synchronized(CHAT) { lines=CHAT.toString(); CHAT.setLength(0); }
            if(!lines.isBlank()) send(lines.length()>3800 ? lines.substring(lines.length()-3800) : lines,null);
            chatFlush=System.currentTimeMillis()+2500;
        }
    }
    public static synchronized void start() {
        load();
        if(poller!=null) return;
        if(token.isBlank()) { state=State.ERROR; error="Нет токена: создай бота у @BotFather и вставь токен"; return; }
        if(chatId==0 && code.isEmpty()) code=newCode();
        state=State.STARTING; error="";
        Thread t=new Thread(TelegramBot::poll,"ess-telegram");
        t.setDaemon(true);
        poller=t;
        t.start();
    }
    public static synchronized void stop() {
        Thread t=poller;
        poller=null;
        if(t!=null) t.interrupt();
        state=State.STOPPED;
    }
    private static String newCode() { return String.format(Locale.ROOT,"%06d",RANDOM.nextInt(1_000_000)); }

    private static void poll() {
        long offset=0;
        int failures=0;
        try {
            JsonObject me=call("getMe",new JsonObject(),20);
            if(me==null || !me.get("ok").getAsBoolean()) { fail("Telegram не принял токен — проверь его"); return; }
            botName=me.getAsJsonObject("result").get("username").getAsString();
            state=State.RUNNING;
            setCommands();
            if(chatId!=0) send("Клиент запущен и на связи.",keyboard());
            while(poller==Thread.currentThread()) {
                JsonObject body=new JsonObject();
                body.addProperty("offset",offset);
                body.addProperty("timeout",25);
                JsonObject res=call("getUpdates",body,40);
                if(res==null || !res.get("ok").getAsBoolean()) {
                    if(++failures>=5) { state=State.ERROR; error="Нет связи с Telegram, пробую снова"; }
                    Thread.sleep(Math.min(30_000,2000L*failures));
                    continue;
                }
                failures=0;
                state=State.RUNNING; error="";
                for(var e:res.getAsJsonArray("result")) {
                    JsonObject update=e.getAsJsonObject();
                    offset=update.get("update_id").getAsLong()+1;
                    try { update(update); } catch(Exception ex) { System.err.println("[ESS] telegram update: "+ex); }
                }
            }
        } catch(InterruptedException ignored) {
        } catch(Exception e) {
            fail("Ошибка Telegram: "+e.getClass().getSimpleName());
        }
    }
    private static void fail(String message) {
        error=message; state=State.ERROR;
        synchronized(TelegramBot.class) { if(poller==Thread.currentThread()) poller=null; }
    }

    // ---- incoming ---------------------------------------------------------------------------------------------
    private static void update(JsonObject update) {
        if(update.has("callback_query")) {
            JsonObject q=update.getAsJsonObject("callback_query");
            long from=q.getAsJsonObject("message").getAsJsonObject("chat").get("id").getAsLong();
            JsonObject answer=new JsonObject();
            answer.addProperty("callback_query_id",q.get("id").getAsString());
            OUT.execute(()->call("answerCallbackQuery",answer,15));
            if(from!=chatId) return;
            String data=q.get("data").getAsString();
            int message=q.getAsJsonObject("message").get("message_id").getAsInt();
            Minecraft.getInstance().execute(()->button(data,message));
            return;
        }
        if(!update.has("message")) return;
        JsonObject m=update.getAsJsonObject("message");
        long from=m.getAsJsonObject("chat").get("id").getAsLong();
        String text=m.has("text") ? m.get("text").getAsString().trim() : "";
        if(chatId==0) {
            // Not linked yet: only the code shown in the client links a chat.
            if(!code.isEmpty() && text.replace("/start","").trim().equals(code)) {
                chatId=from;
                JsonObject user=m.getAsJsonObject("from");
                linkedName=user!=null && user.has("username") ? "@"+user.get("username").getAsString() : user!=null ? user.get("first_name").getAsString() : "";
                code="";
                save();
                send("Готово, чат привязан к клиенту. Жми кнопки снизу или /help.",keyboard());
            } else reply(from,"Этот бот ещё не привязан. Отправь сюда 6-значный код из меню Telegram в клиенте.");
            return;
        }
        if(from!=chatId) { reply(from,"Этот бот привязан к другому чату."); return; }
        if(!text.isEmpty()) Minecraft.getInstance().execute(()->command(text));
    }

    private static void command(String text) {
        String lower=text.toLowerCase(Locale.ROOT);
        String[] parts=text.split("\\s+",2);
        String head=parts[0].toLowerCase(Locale.ROOT).replaceAll("@.*$",""),arg=parts.length>1 ? parts[1].trim() : "";
        switch (head) {
            case "/start", "/help", "❓" -> send(help(),keyboard());
            case "/status", "📊" -> send(status(),null);
            case "/screen", "📸" -> screenshot();
            case "/inv", "🎒" -> send(inventory(),null);
            case "/count" -> send(arg.isEmpty() ? "Напиши: /count алмаз" : count(arg),null);
            case "/modules", "🧩" -> modules(0,0);
            case "/on", "/off", "/toggle" -> send(toggle(arg,head),null);
            case "/accounts", "👥" -> accounts(0);
            case "/use" -> send(use(arg),null);
            case "/alts" -> alts();
            case "/join" -> send(join(arg),null);
            case "/leave" -> send(leave(),null);
            case "/stop", "⛔" -> run("#stop");
            case "/b" -> run("#"+arg);
            case "/cmd" -> run("/"+arg);
            case "/say" -> run(arg);
            case "/chat" -> {
                boolean want=!lower.contains("off") && (lower.contains("on") || !FORWARD_CHAT.get());
                if(want!=FORWARD_CHAT.get()) FORWARD_CHAT.toggle();
                send("Пересылка чата: "+(FORWARD_CHAT.get() ? "включена" : "выключена"),null); }
            default -> {
                // A Telegram "/" command we do not know is a server command: "!" is the safe way to write one.
                if(text.startsWith("!")) run("/"+text.substring(1));
                else if(text.startsWith("/")) run(text);
                else run(text);
            }
        }
    }
    private static void button(String data,int message) {
        String[] p=data.split(":",3);
        switch (p[0]) {
            case "mod" -> { var m=find(p[2]); if(m!=null) m.toggle(); modules(Integer.parseInt(p[1]),message); }
            case "page" -> modules(Integer.parseInt(p[1]),message);
            case "acc" -> { target=p[1].equals("@main") ? "" : p[1]; accounts(message); }
            case "play" -> {
                Bot bot=bot(p[1]);
                if(bot!=null && Bots.canSwitch(bot)) { Bots.switchTo(bot); target=""; send("Играешь за "+bot.name,null); }
                else send("Сейчас нельзя переключиться на этот аккаунт: он должен быть в сети",null);
                accounts(message);
            }
            case "alt" -> {
                var account=ess.ll.client.screen.AltAccounts.all().stream().filter(a->a.name.equals(p[1])).findFirst().orElse(null);
                if(account==null || !ess.ll.client.screen.AltAccounts.canUse(account)) { send("Этот аккаунт недоступен",null); return; }
                ess.ll.client.screen.AltAccounts.use(account);
                send("Аккаунт сменён на "+account.name+". Он заработает при следующем входе на сервер: /join адрес",null);
            }
            default -> {}
        }
    }

    // ---- actions ----------------------------------------------------------------------------------------------
    /** The chosen account: a background bot, or null for the one being played. */
    private static Bot chosen() {
        if(target.isEmpty()) return null;
        Bot b=bot(target);
        if(b==null || b.foreground()) { target=""; return null; }
        return b;
    }
    private static Bot bot(String name) {
        for(Bot b:Bots.all()) if(b.name.equalsIgnoreCase(name)) return b;
        return null;
    }
    /** Chat, "/" server commands, "." client commands and "#" Baritone, as the chosen account. */
    private static void run(String text) {
        var mc=Minecraft.getInstance();
        Bot b=chosen();
        if(b==null && Bots.main()!=null && Bots.main().foreground()) b=Bots.main();
        if(b==null) b=Bots.foreground();
        if(b!=null) {
            int before=b.log().size();
            Bot bot=b;
            bot.chat(text);
            // The bot's console gets the replies of "." and "#" lines; pass back what appeared.
            OUT.execute(()->{
                try { Thread.sleep(700); } catch(InterruptedException ignored) {}
                List<String> log=bot.log();
                List<String> fresh=new ArrayList<>();
                for(int i=Math.min(before,log.size());i<log.size();i++) if(!log.get(i).startsWith("> ")) fresh.add(log.get(i));
                send((bot.foreground() ? "" : "["+bot.name+"] ")+(fresh.isEmpty() ? "Отправлено: "+text : String.join("\n",fresh)),null);
            });
            return;
        }
        // No Bots tab session (singleplayer or menus): the client itself.
        if(text.startsWith(".")) {
            List<String> out=new ArrayList<>();
            ess.ll.client.command.DotCommands.execute(text,out::add);
            send(out.isEmpty() ? "Выполнено: "+text : String.join("\n",out),null);
        } else if(text.startsWith("#")) {
            try {
                boolean ok=baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(text.substring(1).trim());
                send(ok ? "Baritone: "+text.substring(1) : "Baritone не знает такой команды",null);
            } catch(Throwable e) { send("Baritone недоступен",null); }
        } else if(mc.getConnection()!=null) {
            if(text.startsWith("/")) mc.getConnection().sendCommand(text.substring(1)); else mc.getConnection().sendChat(text);
            send("Отправлено: "+text,null);
        } else send("Ты не в игре: /join адрес",null);
    }

    private static String status() {
        var mc=Minecraft.getInstance();
        StringBuilder s=new StringBuilder();
        var p=mc.player;
        var server=mc.getCurrentServer();
        s.append("Аккаунт: ").append(mc.getUser().getName()).append('\n');
        s.append("Сервер: ").append(server!=null ? server.ip : mc.getSingleplayerServer()!=null ? "одиночный мир" : "не в игре").append('\n');
        if(p!=null) {
            s.append(String.format(Locale.ROOT,"Здоровье: %.1f ❤  Еда: %d",p.getHealth(),p.getFoodData().getFoodLevel())).append('\n');
            s.append("Координаты: ").append(p.blockPosition().getX()).append(' ').append(p.blockPosition().getY()).append(' ').append(p.blockPosition().getZ())
                    .append(" (").append(mc.level.dimension().identifier().getPath()).append(")\n");
            var info=mc.getConnection()==null ? null : mc.getConnection().getPlayerInfo(p.getUUID());
            if(info!=null) s.append("Пинг: ").append(info.getLatency()).append(" мс\n");
        }
        long on=ClientModules.all().stream().filter(ClientModules.Module::enabled).count();
        s.append("Модулей включено: ").append(on).append('\n');
        if(!target.isEmpty()) s.append("Команды идут боту: ").append(target).append('\n');
        if(Bots.all().size()>1) s.append("Ботов: ").append(Bots.all().size()-1);
        return s.toString().trim();
    }
    private static String inventory() {
        var p=Minecraft.getInstance().player;
        if(p==null) return "Ты не в игре";
        Map<String,Integer> items=new LinkedHashMap<>();
        var inv=p.getInventory();
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack st=inv.getItem(i);
            if(!st.isEmpty()) items.merge(st.getHoverName().getString(),st.getCount(),Integer::sum);
        }
        if(items.isEmpty()) return "Инвентарь пуст";
        StringBuilder s=new StringBuilder("Инвентарь:\n");
        items.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue()).forEach(e->s.append("• ").append(e.getKey()).append(" × ").append(e.getValue()).append('\n'));
        return s.toString().trim();
    }
    /** Pieces of an item in your inventory, matched by id ("diamond") or by name ("алмаз"). */
    static String count(String query) {
        var p=Minecraft.getInstance().player;
        if(p==null) return "Ты не в игре";
        String q=query.toLowerCase(Locale.ROOT).trim(),id=q.replace(' ','_');
        Map<String,Integer> found=new LinkedHashMap<>();
        var inv=p.getInventory();
        // An exact id ("stone") counts only that item; otherwise every item whose id or name contains the words.
        boolean exact=false;
        for(int i=0;i<inv.getContainerSize();i++) if(BuiltInRegistries.ITEM.getKey(inv.getItem(i).getItem()).getPath().equals(id) && !inv.getItem(i).isEmpty()) exact=true;
        for(int i=0;i<inv.getContainerSize();i++) {
            ItemStack st=inv.getItem(i);
            if(st.isEmpty()) continue;
            String key=BuiltInRegistries.ITEM.getKey(st.getItem()).getPath(),name=st.getHoverName().getString();
            boolean match=exact ? key.equals(id) : key.contains(id) || name.toLowerCase(Locale.ROOT).contains(q);
            if(match) found.merge(name,st.getCount(),Integer::sum);
        }
        if(found.isEmpty()) return "«"+query+"» в инвентаре нет";
        int total=found.values().stream().mapToInt(Integer::intValue).sum();
        StringBuilder s=new StringBuilder("«"+query+"»: "+total+" шт.");
        if(found.size()>1) found.forEach((k,v)->s.append("\n• ").append(k).append(" × ").append(v));
        return s.toString();
    }
    private static ClientModules.Module find(String name) {
        for(var m:ClientModules.all()) if(m.name.equalsIgnoreCase(name)) return m;
        return null;
    }
    private static String toggle(String name,String how) {
        var m=find(name);
        if(m==null) return name.isEmpty() ? "Напиши имя модуля: "+how+" Aura" : "Нет модуля "+name;
        if(m==ClientModules.TELEGRAM) return "Telegram отсюда не выключается";
        if(how.equals("/on")) m.setEnabled(true); else if(how.equals("/off")) m.setEnabled(false); else m.toggle();
        return m.name+": "+(m.enabled() ? "включён" : "выключен");
    }
    /** The modules as buttons, 24 a page; tapping one toggles it. */
    private static void modules(int page,int edit) {
        List<ClientModules.Module> all=ClientModules.all().stream().filter(m->m!=ClientModules.TELEGRAM).toList();
        int per=24,pages=(all.size()+per-1)/per;
        page=Math.floorMod(page,Math.max(1,pages));
        JsonArray rows=new JsonArray();
        JsonArray row=null;
        for(int i=page*per;i<Math.min(all.size(),(page+1)*per);i++) {
            var m=all.get(i);
            if(row==null || row.size()==2) { row=new JsonArray(); rows.add(row); }
            row.add(button((m.enabled() ? "🟢 " : "⚪ ")+m.name,"mod:"+page+":"+m.name));
        }
        if(pages>1) {
            JsonArray nav=new JsonArray();
            nav.add(button("◀","page:"+(page-1)));
            nav.add(button((page+1)+" / "+pages,"page:"+page));
            nav.add(button("▶","page:"+(page+1)));
            rows.add(nav);
        }
        JsonObject markup=new JsonObject();
        markup.add("inline_keyboard",rows);
        if(edit!=0) editMarkup(edit,markup); else send("Модули — нажми, чтобы включить или выключить:",markup);
    }
    private static void accounts(int edit) {
        JsonArray rows=new JsonArray();
        StringBuilder s=new StringBuilder("Аккаунты. «Выбрать» — куда идут сообщения и команды, «Играть» — за кого играет клиент.\n");
        for(Bot b:Bots.all()) {
            boolean chosen=b.foreground() ? target.isEmpty() : b.name.equalsIgnoreCase(target);
            s.append('\n').append(chosen ? "▶ " : "• ").append(b.name).append(b.main ? " (твой)" : "").append(b.foreground() ? " — играешь" : "").append(" · ").append(state(b));
            JsonArray row=new JsonArray();
            row.add(button((chosen ? "✅ " : "")+b.name,"acc:"+(b.foreground() ? "@main" : b.name)));
            if(!b.foreground()) row.add(button("🎮 Играть","play:"+b.name));
            rows.add(row);
        }
        if(Bots.all().isEmpty()) s.append("\nБотов нет: добавь их во вкладке Bots в клиенте. Сейчас команды идут твоему аккаунту.");
        JsonObject markup=new JsonObject();
        markup.add("inline_keyboard",rows);
        if(edit!=0) { editText(edit,s.toString(),markup); } else send(s.toString(),markup);
    }
    private static String state(Bot b) {
        return switch (b.state()) {
            case ONLINE -> "в сети, ❤ "+Math.round(b.health());
            default -> b.state().name().toLowerCase(Locale.ROOT)+(b.reason()==null || b.reason().isEmpty() ? "" : " ("+b.reason()+")");
        };
    }
    private static String use(String name) {
        if(name.isEmpty() || name.equalsIgnoreCase("me")) { target=""; return "Команды идут аккаунту, за который играешь"; }
        Bot b=bot(name);
        if(b==null) return "Нет аккаунта "+name+" — /accounts";
        target=b.foreground() ? "" : b.name;
        return "Команды идут: "+b.name;
    }
    private static void alts() {
        var list=ess.ll.client.screen.AltAccounts.all();
        if(list.isEmpty()) { send("Сохранённых аккаунтов нет: добавь их в меню аккаунтов клиента",null); return; }
        JsonArray rows=new JsonArray();
        for(var a:list) {
            JsonArray row=new JsonArray();
            row.add(button((ess.ll.client.screen.AltAccounts.isCurrent(a) ? "✅ " : "")+a.name+(a.microsoft ? " (Microsoft)" : ""),"alt:"+a.name));
            rows.add(row);
        }
        JsonObject markup=new JsonObject();
        markup.add("inline_keyboard",rows);
        send("Сохранённые аккаунты — нажми, чтобы войти под ним при следующем заходе:",markup);
    }
    private static String join(String address) {
        if(address.isEmpty()) return "Напиши адрес: /join mc.server.ru";
        var mc=Minecraft.getInstance();
        if(mc.level!=null) leave();
        var data=new net.minecraft.client.multiplayer.ServerData(address,address,net.minecraft.client.multiplayer.ServerData.Type.OTHER);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new ess.ll.client.screen.MainMenuScreen(),mc,
                net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address),data,false,null);
        return "Захожу на "+address+"…";
    }
    private static String leave() {
        var mc=Minecraft.getInstance();
        if(mc.level==null) return "Ты и так не в игре";
        Bots.quit();
        mc.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
        return "Вышел с сервера";
    }
    private static void screenshot() {
        var mc=Minecraft.getInstance();
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{
            try {
                Path tmp=Files.createTempFile("ess-telegram",".png");
                try { image.writeToFile(tmp); } finally { image.close(); }
                byte[] png=Files.readAllBytes(tmp);
                Files.deleteIfExists(tmp);
                OUT.execute(()->photo(png,status()));
            } catch(Exception e) { send("Скриншот не получился: "+e.getClass().getSimpleName(),null); }
        });
    }

    // ---- notifications and the chat feed ------------------------------------------------------------------
    /** From ChatComponentMixin: a line shown in the game chat. */
    public static void chatLine(String line) {
        if(state!=State.RUNNING || chatId==0 || !FORWARD_CHAT.get() || line.isBlank()) return;
        synchronized(CHAT) {
            if(CHAT.length()<16_000) CHAT.append(line.replaceAll("§.","")).append('\n');
        }
    }
    private static void notifications(Minecraft mc) {
        boolean dead=mc.player!=null && mc.player.isDeadOrDying();
        if(dead && !wasDead) send("💀 Ты умер"+(mc.player.blockPosition()!=null ? " на "+mc.player.blockPosition().toShortString() : ""),null);
        wasDead=dead;
        boolean connected=mc.getConnection()!=null;
        if(wasConnected && !connected) {
            var screen=mc.gui.screen();
            String why=screen==null ? "" : screen.getTitle().getString();
            send("🔌 Отключился от сервера"+(why.isBlank() ? "" : ": "+why),null);
        }
        wasConnected=connected;
        lastScreen=mc.gui.screen()==null ? "" : mc.gui.screen().getClass().getSimpleName();
    }

    // ---- help and keyboard --------------------------------------------------------------------------------
    private static String help() {
        return """
                Управление клиентом Maved.

                Кнопки снизу: статус, скрин, инвентарь, модули, аккаунты, стоп Baritone.

                Текст — сообщение в чат игры.
                !команда — команда сервера (!home, !ah search алмаз).
                .команда — команда клиента (.toggle Aura, .sort start).
                #команда — Baritone (#goto 100 200, #mine diamond_ore, #stop).

                /count алмаз — сколько этого в инвентаре
                /on Aura, /off Aura — включить, выключить модуль
                /accounts — твой аккаунт и боты; /use имя — кому идут команды
                /alts — сохранённые аккаунты
                /join адрес, /leave — зайти на сервер, выйти
                /chat — пересылка чата игры сюда (вкл/выкл)""";
    }
    private static JsonObject keyboard() {
        String[][] rows={{"📊 Статус","📸 Скрин","🎒 Инвентарь"},{"🧩 Модули","👥 Аккаунты","⛔ Стоп"},{"❓ Помощь"}};
        JsonArray keyboard=new JsonArray();
        for(String[] r:rows) {
            JsonArray row=new JsonArray();
            for(String label:r) { JsonObject b=new JsonObject(); b.addProperty("text",label); row.add(b); }
            keyboard.add(row);
        }
        JsonObject markup=new JsonObject();
        markup.add("keyboard",keyboard);
        markup.addProperty("resize_keyboard",true);
        markup.addProperty("is_persistent",true);
        return markup;
    }
    private static JsonObject button(String text,String data) {
        JsonObject b=new JsonObject();
        b.addProperty("text",text); b.addProperty("callback_data",data.length()>64 ? data.substring(0,64) : data);
        return b;
    }
    private static void setCommands() {
        String[][] list={{"help","что умеет бот"},{"status","здоровье, координаты, сервер"},{"screen","скриншот экрана"},{"inv","инвентарь"},
                {"count","сколько предмета: /count алмаз"},{"modules","модули кнопками"},{"accounts","аккаунты и боты"},{"alts","сохранённые аккаунты"},
                {"join","зайти на сервер"},{"leave","выйти с сервера"},{"stop","остановить Baritone"},{"chat","пересылка чата"}};
        JsonArray commands=new JsonArray();
        for(String[] c:list) { JsonObject o=new JsonObject(); o.addProperty("command",c[0]); o.addProperty("description",c[1]); commands.add(o); }
        JsonObject body=new JsonObject();
        body.add("commands",commands);
        call("setMyCommands",body,15);
    }

    // ---- Telegram API -----------------------------------------------------------------------------------------
    private static String api() {
        return System.getProperty("ess.telegram.api","https://api.telegram.org")+"/bot"+token+"/";
    }
    private static JsonObject call(String method,JsonObject body,int timeoutSeconds) {
        try {
            HttpRequest request=HttpRequest.newBuilder(URI.create(api()+method)).timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body),StandardCharsets.UTF_8)).build();
            HttpResponse<String> response=HTTP.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch(Exception e) {
            // Never the URL: it holds the token.
            System.err.println("[ESS] telegram "+method+": "+e.getClass().getSimpleName());
            return null;
        }
    }
    private static void send(String text,JsonObject markup) { if(chatId!=0) reply(chatId,text,markup); }
    private static void reply(long chat,String text) { reply(chat,text,null); }
    private static void reply(long chat,String text,JsonObject markup) {
        JsonObject body=new JsonObject();
        body.addProperty("chat_id",chat);
        body.addProperty("text",text.length()>4000 ? text.substring(0,4000) : text);
        if(markup!=null) body.add("reply_markup",markup);
        OUT.execute(()->call("sendMessage",body,20));
    }
    private static void editMarkup(int message,JsonObject markup) {
        JsonObject body=new JsonObject();
        body.addProperty("chat_id",chatId); body.addProperty("message_id",message); body.add("reply_markup",markup);
        OUT.execute(()->call("editMessageReplyMarkup",body,20));
    }
    private static void editText(int message,String text,JsonObject markup) {
        JsonObject body=new JsonObject();
        body.addProperty("chat_id",chatId); body.addProperty("message_id",message); body.addProperty("text",text); body.add("reply_markup",markup);
        OUT.execute(()->call("editMessageText",body,20));
    }
    private static void photo(byte[] png,String caption) {
        try {
            String boundary="----ess"+Long.toHexString(RANDOM.nextLong());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            part(out,boundary,"chat_id",String.valueOf(chatId));
            part(out,boundary,"caption",caption.length()>1000 ? caption.substring(0,1000) : caption);
            out.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"screen.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(png);
            out.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request=HttpRequest.newBuilder(URI.create(api()+"sendPhoto")).timeout(Duration.ofSeconds(60))
                    .header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray())).build();
            HTTP.send(request,HttpResponse.BodyHandlers.discarding());
        } catch(Exception e) { System.err.println("[ESS] telegram sendPhoto: "+e.getClass().getSimpleName()); }
    }
    private static void part(ByteArrayOutputStream out,String boundary,String name,String value) {
        out.writeBytes(("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n").getBytes(StandardCharsets.UTF_8));
    }

    // ---- storage ----------------------------------------------------------------------------------------------
    private static void load() {
        if(loaded) return;
        loaded=true;
        try {
            if(!Files.exists(FILE)) return;
            JsonObject o=JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonObject();
            token=o.has("token") ? o.get("token").getAsString() : "";
            chatId=o.has("chat") ? o.get("chat").getAsLong() : 0;
            linkedName=o.has("chatName") ? o.get("chatName").getAsString() : "";
        } catch(Exception e) { System.err.println("[ESS] telegram.json not read: "+e.getClass().getSimpleName()); }
    }
    private static void save() {
        try {
            JsonObject o=new JsonObject();
            o.addProperty("token",token); o.addProperty("chat",chatId); o.addProperty("chatName",linkedName);
            Files.createDirectories(FILE.getParent());
            Path tmp=FILE.resolveSibling("telegram.json.tmp");
            Files.writeString(tmp,GSON.toJson(o),StandardCharsets.UTF_8);
            Files.move(tmp,FILE,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        } catch(Exception e) { System.err.println("[ESS] telegram.json not saved: "+e.getClass().getSimpleName()); }
    }

    /** .tg: the same settings from the client chat, for when the menu is not at hand. */
    public static List<String> dotCommand(List<String> a) {
        String sub=a.size()>1 ? a.get(1).toLowerCase(Locale.ROOT) : "";
        return switch (sub) {
            case "token" -> { if(a.size()<3) yield List.of("Использование: .tg token <токен от @BotFather>"); setToken(a.get(2)); yield List.of("Токен сохранён: "+maskedToken()); }
            case "start" -> { ClientModules.TELEGRAM.setEnabled(true); start(); yield List.of("Telegram запускается…"+(linked() ? "" : " Отправь боту код: "+code)); }
            case "stop" -> { ClientModules.TELEGRAM.setEnabled(false); yield List.of("Telegram остановлен"); }
            case "unlink" -> { unlink(); yield List.of("Чат отвязан. Новый код: "+code); }
            case "menu" -> { openMenu(); yield List.of(); }
            case "code" -> List.of(linked() ? "Уже привязан к "+linkedName : "Код для бота: "+(code.isEmpty() ? "запусти бота" : code));
            default -> List.of("Состояние: "+state+(error.isEmpty() ? "" : " — "+error)+(botName.isEmpty() ? "" : " · @"+botName),
                    linked() ? "Чат: "+linkedName : "Чат не привязан"+(code.isEmpty() ? "" : " · код "+code),
                    ".tg token <токен> · .tg start · .tg stop · .tg code · .tg unlink");
        };
    }
}
