package ess.ll.client.command;

import ess.ll.client.module.ClientModules;
import ess.ll.client.setting.Settings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import java.util.*;

public final class DotCommands {
    private DotCommands() {}
    public static boolean allowChat(String message) {
        if(!message.startsWith(".")) return true;
        execute(message); return false;
    }
    public static void execute(String message) {
        try {
            List<String> a=CommandLine.parse(message.substring(1));
            if(a.isEmpty()) { help(); return; }
            switch (a.getFirst().toLowerCase(Locale.ROOT)) {
                case "help", "?" -> { if(a.size()>1) section(a.get(1)); else help(); }
                case "modules" -> say(String.join(", ",ClientModules.all().stream().map(m->m.name+(m.enabled()?" [ON]":" [OFF]")).toList()));
                case "toggle", "t" -> {
                    require(a,2,".toggle <модуль>"); var m=ClientModules.find(a.get(1)); m.toggle(); say(m.name+": "+(m.enabled()?"ON":"OFF"));
                }
                case "bind" -> {
                    require(a,3,".bind <модуль> <клавиша|MOUSE4|NONE> [toggle|hold]"); var m=ClientModules.find(a.get(1)); m.bind=Keys.parse(a.get(2));
                    if(a.size()>3) {
                        String mode=a.get(3).toLowerCase(Locale.ROOT);
                        if(!mode.equals("hold") && !mode.equals("toggle")) throw new IllegalArgumentException("Режим: toggle или hold");
                        m.hold=mode.equals("hold");
                    }
                    say(m.name+" → "+Keys.name(m.bind)+(m.hold?" (hold)":" (toggle)"));
                }
                case "config", "cfg", "configs" -> config(a);
                case "friend", "friends" -> friend(a);
                case "macro" -> macro(a);
                case "blockesp" -> blockEsp(a);
                case "way", "waypoint", "wp" -> way(a);
                case "emote", "e" -> emote(a);
                case "set" -> setting(a);
                case "search", "ah" -> {
                    var mc=net.minecraft.client.Minecraft.getInstance();
                    var held=mc.player==null ? net.minecraft.world.item.ItemStack.EMPTY : mc.player.getMainHandItem();
                    if(held.isEmpty()) error("возьми предмет в руку");
                    else { String name=ess.ll.client.module.misc.AucHelper.searchName(held); mc.player.connection.sendCommand("ah search "+name); say("Аукцион: ищу "+name); }
                }
                case "tg", "telegram" -> { for(String line:ess.ll.client.telegram.TelegramBot.dotCommand(a)) say(line); }
                case "sort", "sorter" -> { for(String line:ess.ll.client.module.misc.AutoSorter.command(a)) say(line); }
                case "tikva", "rwtikva" -> { for(String line:ess.ll.client.module.misc.AutoRwTikva.command(a)) say(line); }
                case "fakeplayer", "fake", "fp" -> { for(String line:ess.ll.client.features.FakePlayers.command(a)) say(line); }
                default -> send(prefix().append(text("Неизвестная команда ",RED)).append(text("."+a.getFirst(),WHITE).withStyle(ChatFormatting.BOLD))
                        .append(text(" · ",DARK)).append(link("открыть .help",".help","Список команд")));
            }
            ClientData.flush();
        } catch(Exception e) { error(e.getMessage()==null ? e.getClass().getSimpleName() : e.getMessage()); }
    }
    private static void blockEsp(List<String> a) {
        require(a,2,".blockesp add <блок> <#RRGGBB|цвет> | remove <блок> | clear | list");
        var esp=ess.ll.client.module.render.BlockESP.INSTANCE;
        switch (a.get(1).toLowerCase(Locale.ROOT)) {
            case "add" -> {
                require(a,4,".blockesp add <блок> <#RRGGBB|цвет|R G B>");
                int color;
                if(a.size()==6) {
                    int r=Integer.parseInt(a.get(3)),g=Integer.parseInt(a.get(4)),b=Integer.parseInt(a.get(5));
                    if(r<0 || r>255 || g<0 || g>255 || b<0 || b>255)throw new IllegalArgumentException("RGB: 0–255");
                    color=0xff000000|r<<16|g<<8|b;
                } else {
                    if(a.size()!=4)throw new IllegalArgumentException("Цвет: #RRGGBB, имя или R G B");
                    String value=switch (a.get(3).toLowerCase(Locale.ROOT)) {
                        case "white" -> "FFFFFF"; case "black" -> "000000";
                        case "red" -> "FF3030"; case "green" -> "30FF60"; case "blue" -> "4080FF";
                        case "cyan" -> "40DFFF"; case "yellow" -> "FFFF30"; case "orange" -> "FF9930";
                        case "pink" -> "FF80C0"; case "purple" -> "B060FF"; default -> a.get(3);
                    }; color=ColorSetting.parse(value);
                }
                esp.add(a.get(2),color); say("BlockESP: "+a.get(2)+" → "+String.format("#%06X",color&0xffffff));
            }
            case "remove", "del", "delete" -> { require(a,3,".blockesp remove <блок>"); say(esp.remove(a.get(2))?"Блок удалён":"Блока нет в списке"); }
            case "clear" -> { esp.clear(); say("Список BlockESP очищен"); }
            case "list" -> {
                if(esp.entries().isEmpty())say("Список BlockESP пуст");
                esp.entries().forEach((name,color)->say(name+" → "+String.format("#%06X",color&0xffffff)));
            }
            case "on", "off" -> { ClientModules.find("BlockESP").setEnabled(a.get(1).equalsIgnoreCase("on")); say("BlockESP: "+a.get(1)); }
            default -> throw new IllegalArgumentException("Действия: add, remove, clear, list, on, off");
        }
    }
    private static void config(List<String> a) throws Exception {
        require(a,2,".config save/load/delete/list <имя>");
        String op=a.get(1).toLowerCase(Locale.ROOT);
        if(op.equals("list")) { say("Конфиги: "+(ClientData.profiles().isEmpty() ? "пока нет" : String.join(", ",ClientData.profiles()))); return; }
        require(a,3,".config "+op+" <имя>");
        switch (op) {
            case "save" -> ClientData.saveProfile(a.get(2));
            case "load" -> ClientData.loadProfile(a.get(2));
            case "delete", "remove" -> ClientData.deleteProfile(a.get(2));
            default -> throw new IllegalArgumentException("Действия: save, load, delete, list");
        }
        say("Конфиг "+a.get(2)+": "+op);
    }
    /** .way add <x> <z> [имя] | remove <имя|номер> | go <имя|номер> | list | clear | off; "~" is your own coordinate. */
    private static void way(List<String> a) {
        require(a,2,".way add <x> <z> [имя] | remove | go | list | clear | off");
        switch (a.get(1).toLowerCase(Locale.ROOT)) {
            case "add" -> {
                require(a,4,".way add <x> <z> [имя]");
                var p=ess.ll.client.features.Waypoints.add(a.size()>4 ? String.join(" ",a.subList(4,a.size())) : null,coordinate(a.get(2),true),coordinate(a.get(3),false));
                say("Метка «"+p.name()+"» на "+p.x()+" "+p.z());
            }
            case "remove", "del", "delete" -> {
                require(a,3,".way remove <имя|номер>");
                var p=ess.ll.client.features.Waypoints.remove(String.join(" ",a.subList(2,a.size())));
                say(p==null ? "Такой метки нет" : "Метка «"+p.name()+"» удалена");
            }
            case "go", "set" -> {
                require(a,3,".way go <имя|номер>");
                var p=ess.ll.client.features.Waypoints.go(String.join(" ",a.subList(2,a.size())));
                say(p==null ? "Такой метки нет" : "Ведём к «"+p.name()+"»");
            }
            case "list" -> {
                var here=ess.ll.client.features.Waypoints.here();
                if(here.isEmpty()) say("Меток здесь нет · .way add <x> <z> [имя]");
                for(int i=0;i<here.size();i++) say((i+1)+". "+here.get(i).name()+" · "+here.get(i).x()+" "+here.get(i).z());
            }
            case "clear" -> say("Удалено меток: "+ess.ll.client.features.Waypoints.clear());
            case "off", "hide" -> { ess.ll.client.features.Waypoints.hide(); say("Метка скрыта · .way go <имя>, чтобы вернуть"); }
            default -> throw new IllegalArgumentException("Действия: add, remove, go, list, clear, off");
        }
    }
    private static void emote(List<String> a) {
        require(a,2,".emote <id> | stop | list");
        String arg=a.get(1).toLowerCase(Locale.ROOT);
        switch (arg) {
            case "stop" -> { ess.ll.client.features.emote.Emotes.stop(); say("Эмоция остановлена"); }
            case "list" -> say("Эмоции: "+String.join(", ",ess.ll.client.features.emote.Emotes.all().stream().map(e->e.id()).toList()));
            default -> {
                var e=ess.ll.client.features.emote.Emotes.find(String.join("_",a.subList(1,a.size())));
                if(e==null || !ess.ll.client.module.misc.EmotesModule.play(e.id())) throw new IllegalArgumentException("Неизвестная эмоция: "+arg+" · .emote list");
                say("Эмоция «"+e.name()+"»");
            }
        }
    }
    /** A whole block coordinate; "~" or "~5" is relative to the player. */
    private static int coordinate(String value,boolean x) {
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(value.startsWith("~")) {
            if(player==null) throw new IllegalArgumentException("~ работает только в мире");
            double base=x ? player.getX() : player.getZ();
            return (int)Math.floor(base+(value.length()>1 ? Double.parseDouble(value.substring(1)) : 0));
        }
        try { return (int)Math.floor(Double.parseDouble(value)); }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Координата — число или ~: "+value); }
    }
    private static void friend(List<String> a) {
        require(a,2,".friend add/remove/list/clear <ник>");
        String op=a.get(1).toLowerCase(Locale.ROOT);
        if(op.equals("list")) { say("Друзья: "+String.join(", ",ClientData.friends())); return; }
        if(op.equals("clear")) { ClientData.clearFriends(); say("Список друзей очищен"); return; }
        require(a,3,".friend "+op+" <ник>");
        if(op.equals("add")) ClientData.addFriend(a.get(2));
        else if(op.equals("remove") || op.equals("del")) ClientData.removeFriend(a.get(2));
        else throw new IllegalArgumentException("Действия: add, remove, list, clear");
        say("Друг "+a.get(2)+": "+op);
    }
    private static void macro(List<String> a) {
        require(a,2,".macro add <имя> <клавиша> <текст> | remove/list/clear");
        switch (a.get(1).toLowerCase(Locale.ROOT)) {
            case "list" -> { for(var m:ClientData.macros()) say(m.name()+" ["+Keys.name(m.key())+"] → "+m.text()); }
            case "clear" -> { ClientData.clearMacros(); say("Макросы очищены"); }
            case "remove", "del" -> { require(a,3,".macro remove <имя>"); ClientData.removeMacro(a.get(2)); say("Макрос удалён"); }
            case "add" -> {
                require(a,5,".macro add <имя> <клавиша> <текст>");
                int key=Keys.parse(a.get(3));
                ClientData.putMacro(a.get(2),key,String.join(" ",a.subList(4,a.size()))); say("Макрос сохранён: "+a.get(2));
            }
            default -> throw new IllegalArgumentException("Действия: add, remove, list, clear");
        }
    }
    private static void setting(List<String> a) {
        require(a,4,".set <модуль> \"настройка\" <значение>");
        var module=ClientModules.find(a.get(1));
        Setting setting=module.settings.stream().filter(s->s.name().equalsIgnoreCase(a.get(2))).findFirst().orElseThrow(()->new IllegalArgumentException("Настройки: "+String.join(", ",module.settings.stream().map(Setting::name).toList())));
        String value=String.join(" ",a.subList(3,a.size()));
        if(setting instanceof BooleanSetting b) {
            if(!Set.of("true","false","on","off").contains(value.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Значение: on/off");
            boolean enabled=value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true"); if(b.get()!=enabled)b.toggle();
        } else if(setting instanceof SliderSetting slider) {
            float v=Float.parseFloat(value); if(!Float.isFinite(v) || v<slider.min || v>slider.max) throw new IllegalArgumentException("Диапазон: "+slider.min+"–"+slider.max); slider.set(v);
        } else if(setting instanceof ModeSettings mode) {
            int selected=-1; for(int i=0;i<mode.options.size();i++) if(mode.options.get(i).equalsIgnoreCase(value)) selected=i;
            if(selected<0) throw new IllegalArgumentException("Режимы: "+String.join(", ",mode.options)); mode.select(selected);
        } else if(setting instanceof MultiBooleanSettings multi) {
            Set<String> selected=new HashSet<>();
            if(!value.equalsIgnoreCase("none")) for(String choice:value.split(",")) {
                String actual=multi.options.stream().filter(o->o.equalsIgnoreCase(choice.trim())).findFirst().orElseThrow(()->new IllegalArgumentException("Пункты: "+String.join(",",multi.options))); selected.add(actual);
            }
            for(int i=0;i<multi.options.size();i++) if(multi.selected(i)!=selected.contains(multi.options.get(i)))multi.toggle(i);
        }
        say(module.name+" / "+setting.name()+" = "+value);
    }
    private static void require(List<String> args,int size,String usage) { if(args.size()<size) throw new IllegalArgumentException(usage); }
    // ---------------------------------------------------------------- output

    private static final int WHITE=0xf4f8ff,GRAY=0xaab3c2,DARK=0x6f7888,RED=0xff6b5e;
    /** A line of a help section: the command, its arguments and what it does. */
    private record Line(String command,String args,String about) {}
    private record Section(String key,String title,String about,List<Line> lines) {}
    private static final List<Section> SECTIONS=List.of(
        new Section("modules","Модули","включение, бинды и настройки",List.of(
            new Line(".modules","","все модули и их состояние"),
            new Line(".toggle","<модуль>","включить или выключить модуль"),
            new Line(".bind","<модуль> <клавиша|MOUSE4|NONE> [toggle|hold]","повесить модуль на клавишу"),
            new Line(".set","<модуль> \"настройка\" <значение>","поменять настройку: on/off, число, режим или список"))),
        new Section("configs","Configs","система конфигов",List.of(
            new Line(".cfg save","<имя>","сохранить текущие настройки"),
            new Line(".cfg load","<имя>","загрузить конфиг"),
            new Line(".cfg delete","<имя>","удалить конфиг"),
            new Line(".cfg list","","все сохранённые конфиги"))),
        new Section("friends","Друзья","список друзей",List.of(
            new Line(".friend add","<ник>","добавить в друзья"),
            new Line(".friend remove","<ник>","убрать из друзей"),
            new Line(".friend list","","все друзья"),
            new Line(".friend clear","","очистить список"))),
        new Section("macros","Макросы","текст или команда на клавишу",List.of(
            new Line(".macro add","<имя> <клавиша> <текст>","сообщение, /команда или .команда по нажатию"),
            new Line(".macro remove","<имя>","удалить макрос"),
            new Line(".macro list","","все макросы"),
            new Line(".macro clear","","удалить все макросы"))),
        new Section("blockesp","BlockESP","подсветка блоков",List.of(
            new Line(".blockesp add","<блок> <#RRGGBB|цвет|R G B>","подсвечивать блок цветом"),
            new Line(".blockesp remove","<блок>","перестать подсвечивать"),
            new Line(".blockesp list","","подсвечиваемые блоки"),
            new Line(".blockesp on/off","","включить или выключить модуль"))),
        new Section("way","Метки","точки на карте со стрелкой",List.of(
            new Line(".way add","<x> <z> [имя]","поставить метку, ~ — твоя координата"),
            new Line(".way go","<имя|номер>","вести к этой метке"),
            new Line(".way remove","<имя|номер>","удалить метку"),
            new Line(".way list","","метки этого сервера"),
            new Line(".way clear / off","","удалить все / скрыть метку"))),
        new Section("tg","Telegram","управление клиентом из Telegram",List.of(
            new Line(".tg token","<токен>","токен бота от @BotFather"),
            new Line(".tg start / stop","","запустить или остановить бота"),
            new Line(".tg code","","код, который нужно отправить боту"),
            new Line(".tg unlink","","отвязать чат"))),
        new Section("search","Аукцион","поиск предмета из руки",List.of(
            new Line(".search","","/ah search с названием предмета в руке"))),
        new Section("tikva","AutoRwTikva","два аккаунта собирают головы на хомах",List.of(
            new Line(".tikva home","<начало>","начало имени хомов: t — это /home t1, t2..."),
            new Line(".tikva start / stop","","запустить или остановить; нужен второй аккаунт в сети на этом сервере"),
            new Line(".tikva","","что делает сейчас"))),
        new Section("fakeplayer","FakePlayer","фейковый игрок для теста PvP",List.of(
            new Line(".fakeplayer add","[имя]","игрок с тотемом на твоём месте: Aura, TargetESP и CrystalAura бьют его, он получает урон"),
            new Line(".fakeplayer del","[имя|all]","убрать одного или всех"),
            new Line(".fakeplayer list","","здоровье и сколько тотемов он снёс"))),
        new Section("sort","AutoSorter","раскладка из сундука по сундукам",List.of(
            new Line(".sort source","","сундук, на который смотришь, — откуда раскладывать"),
            new Line(".sort add","[предмет]","добавить сундуку предмет: из руки или по id"),
            new Line(".sort learn","","сундук запомнит свои предметы, когда его откроешь"),
            new Line(".sort remove / list / clear","","снять отметку, показать, снять все"),
            new Line(".sort start / stop","","запустить или остановить сортировку"))),
        new Section("emote","Эмоции","анимации персонажа",List.of(
            new Line(".emote","<id>","сыграть эмоцию"),
            new Line(".emote stop","","остановить"),
            new Line(".emote list","","все эмоции"))));

    /** .help: the sections; each opens with a click. */
    private static void help() {
        send(prefix().append(text("Команды клиента",WHITE).withStyle(ChatFormatting.BOLD)).append(text(" · нажми на раздел",DARK)));
        for(var section:SECTIONS)
            send(Component.literal("  ").append(link("▸ "+section.title(),".help "+section.key(),"Нажми, чтобы увидеть подробнее").withStyle(ChatFormatting.BOLD))
                    .append(text(" — "+section.about()+" ",GRAY)).append(text("(подробнее)",DARK)));
    }
    /** .help <раздел>: the section's commands in a column; a click puts the command into the chat field. */
    private static void section(String key) {
        var section=SECTIONS.stream().filter(s->s.key().equalsIgnoreCase(key) || s.title().equalsIgnoreCase(key)).findFirst().orElse(null);
        if(section==null) { error("Нет раздела "+key); help(); return; }
        send(prefix().append(text(section.title(),WHITE).withStyle(ChatFormatting.BOLD)).append(text(" — "+section.about(),GRAY)));
        for(var line:section.lines()) {
            var entry=Component.literal("  ").append(text(line.command(),accent()).withStyle(ChatFormatting.BOLD));
            if(!line.args().isEmpty()) entry.append(text(" "+line.args(),GRAY));
            entry.append(text("  "+line.about(),DARK));
            String insert=line.command().replace("/off","")+(line.args().isEmpty() ? "" : " ");
            entry.withStyle(style->style.withClickEvent(new ClickEvent.SuggestCommand(insert))
                    .withHoverEvent(new HoverEvent.ShowText(text("Нажми, чтобы вставить в чат",GRAY))));
            send(entry);
        }
        send(Component.literal("  ").append(link("« все разделы",".help","Назад к списку разделов")));
    }

    private static int accent() { return ess.ll.client.render.Theme.accent()&0xffffff; }
    private static MutableComponent text(String text,int color) { return Component.literal(text).withStyle(style->style.withColor(TextColor.fromRgb(color))); }
    /** Accent text that runs a client command when clicked (ScreenClickMixin keeps it off the server). */
    private static MutableComponent link(String label,String command,String hover) {
        return Component.literal(label).withStyle(style->style.withColor(TextColor.fromRgb(accent()))
                .withClickEvent(new ClickEvent.RunCommand(command)).withHoverEvent(new HoverEvent.ShowText(text(hover,GRAY))));
    }
    private static MutableComponent prefix() {
        return text("Maved",accent()).withStyle(ChatFormatting.BOLD).append(text(" » ",DARK).withStyle(style->style.withBold(false)));
    }
    private static void send(Component message) {
        if(output!=null) { output.accept(message.getString()); return; }
        Minecraft.getInstance().gui.chatListener().handleSystemMessage(message,false);
    }
    /** A reply: the part before the first ": " or " → " stands out in bold, the rest is plain. */
    public static void say(String text) {
        int cut=text.indexOf(": ");
        if(cut<0) cut=text.indexOf(" → ");
        MutableComponent body=cut<0 ? text(text,WHITE)
                : text(text.substring(0,cut),WHITE).withStyle(ChatFormatting.BOLD).append(text(text.substring(cut),GRAY).withStyle(style->style.withBold(false)));
        send(prefix().append(body));
    }
    private static void error(String text) {
        boolean usage=text.startsWith(".");
        send(prefix().append(text(usage ? "Использование: " : "Ошибка: ",RED).withStyle(ChatFormatting.BOLD))
                .append(text(text,usage ? GRAY : WHITE).withStyle(style->style.withBold(false))));
    }

    /** Where replies go instead of the chat while a command typed elsewhere (the bot console) runs. */
    private static java.util.function.Consumer<String> output;
    public static void execute(String message,java.util.function.Consumer<String> replies) {
        var saved=output;
        output=replies;
        try { execute(message); } finally { output=saved; }
    }
}
