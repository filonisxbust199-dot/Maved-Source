package ess.ll.client.module.player;

import ess.ll.client.module.ClientModules;
import ess.ll.client.render.HudNotifications;
import ess.ll.client.render.Icons.Icon;
import ess.ll.client.render.Theme;
import ess.ll.client.setting.Settings.*;
import ess.ll.client.util.Rotation;
import ess.ll.client.util.RotationManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * AutoBrewing (from Zenith): brews the chosen potion in every brewing stand within reach. It counts water
 * bottles, blaze powder and the recipe's ingredients in the inventory; when they run short it takes them from the
 * chests and barrels around first. Then it fills the stands (three bottles and fuel each), adds one ingredient per
 * phase to all of them, waits for the phase to brew, takes the potions out and puts them into the nearby chests.
 * It aims at each block itself (silently) and turns off when done or when something is missing.
 */
public final class AutoBrewing {
    private enum Recipe {
        FIRE_RESISTANCE("Огнестойкость",Items.MAGMA_CREAM,Items.REDSTONE),
        INVISIBILITY("Невидимость",Items.GOLDEN_CARROT,Items.FERMENTED_SPIDER_EYE,Items.REDSTONE),
        STRENGTH("Сила",Items.BLAZE_POWDER,Items.GLOWSTONE_DUST),
        SPEED("Скорость",Items.SUGAR,Items.GLOWSTONE_DUST),
        JUMP_BOOST("Прыгучесть",Items.RABBIT_FOOT,Items.REDSTONE);
        final String title;
        final List<Item> ingredients;
        Recipe(String title,Item... items) {
            this.title=title;
            List<Item> all=new ArrayList<>(List.of(Items.NETHER_WART));
            all.addAll(List.of(items));
            ingredients=List.copyOf(all);
        }
    }
    public static final ModeSettings POTION=new ModeSettings("Зелье",java.util.Arrays.stream(Recipe.values()).map(r->r.title).toArray(String[]::new));
    public static final Setting[] SETTINGS={POTION};
    private static final long CLICK=50,BOTTLE_CLICK=75,PHASE=20_000,OPEN_TIMEOUT=1500;
    private static final int[] BOTTLE_SLOTS={0,1,2};
    private static final int INGREDIENT_SLOT=3,FUEL_SLOT=4,ROTATION_PRIORITY=40;
    private static final double SCAN_RADIUS=5,SCAN_HEIGHT=3;

    private enum State { IDLE, SCAN_CHESTS, OPEN_CHEST, LOOT_CHEST, SCAN_OUTPUT, OPEN_OUTPUT, STORE, SCAN_STANDS, OPEN_STAND,
        LOAD_BOTTLES, LOAD_FUEL, LOAD_INGREDIENT, WAIT_PHASE, TAKE_RESULT }
    /** Which pass over the stands is running: filling them, adding the next ingredient, or collecting. */
    private enum Pass { SETUP, INGREDIENT_LOAD, INGREDIENT_WAIT, COLLECT }
    /** Moving one item onto a menu slot: pick the stack, drop one, put the rest back. */
    private enum Move { IDLE, PICKED, PLACED }

    private static State state=State.IDLE;
    private static Pass pass=Pass.SETUP;
    private static Move move=Move.IDLE;
    private static boolean wasEnabled,looted,lootedAny;
    private static long timer,lastUse;
    private static Recipe recipe;
    private static Map<Item,Integer> perBrew=Map.of();
    private static final List<BlockPos> stands=new ArrayList<>(),chests=new ArrayList<>();
    private static BlockPos stand,chest;
    private static Rotation aim;
    /** Brews left, stands used this round, stand index, ingredient index, bottle slot index, taken slot index. */
    private static int brews,used,standIndex,ingredientIndex,bottleIndex,takeIndex;
    private static int moveFrom=-1,moveTo=-1;

    private AutoBrewing() {}

    private static boolean enabled() { return ClientModules.AUTO_BREWING!=null && ClientModules.AUTO_BREWING.enabled(); }
    private static boolean passed(long ms) { return System.currentTimeMillis()-timer>=ms; }
    private static void reset() { timer=System.currentTimeMillis(); }

    public static void tick(Minecraft mc) {
        boolean on=enabled();
        if(on && !wasEnabled) start(mc);
        if(!on && wasEnabled) stop();
        wasEnabled=on;
        if(!on || state==State.IDLE) return;
        if(mc.player==null || mc.level==null || mc.gameMode==null) { disable(); return; }
        if(aim!=null) RotationManager.rotate(aim,180f,ROTATION_PRIORITY,false);
        switch (state) {
            case SCAN_CHESTS -> { scanChests(mc); if(chests.isEmpty()) { reportMissing(mc); disable(); } else nextChest(mc); }
            case OPEN_CHEST -> open(mc,State.LOOT_CHEST,()->nextChest(mc));
            case LOOT_CHEST -> loot(mc);
            case SCAN_OUTPUT -> { scanChests(mc); nextOutput(mc); }
            case OPEN_OUTPUT -> open(mc,State.STORE,()->nextOutput(mc));
            case STORE -> store(mc);
            case SCAN_STANDS -> scanStands(mc);
            case OPEN_STAND -> openStand(mc);
            case LOAD_BOTTLES -> loadBottles(mc);
            case LOAD_FUEL -> loadFuel(mc);
            case LOAD_INGREDIENT -> loadIngredient(mc);
            case WAIT_PHASE -> waitPhase(mc);
            case TAKE_RESULT -> takeResult(mc);
            case IDLE -> {}
        }
    }

    private static void start(Minecraft mc) {
        if(mc.player==null) { ClientModules.AUTO_BREWING.setEnabled(false); return; }
        recipe=Recipe.values()[Math.max(0,POTION.options.indexOf(POTION.get()))];
        perBrew=new LinkedHashMap<>();
        perBrew.merge(Items.BLAZE_POWDER,1,Integer::sum);
        for(Item item:recipe.ingredients) perBrew.merge(item,1,Integer::sum);
        brews=used=standIndex=ingredientIndex=0;
        looted=lootedAny=false;
        pass=Pass.SETUP;
        stand=chest=null; aim=null;
        stands.clear(); chests.clear();
        clearMove();
        reset();
        plan(mc);
    }
    private static void stop() {
        state=State.IDLE; aim=null; stand=chest=null;
        stands.clear(); chests.clear();
        pass=Pass.SETUP;
        RotationManager.releasePriority(ROTATION_PRIORITY);
    }
    private static void disable() { ClientModules.AUTO_BREWING.setEnabled(false); stop(); wasEnabled=false; }

    /** Counts what the inventory can brew; with nothing, the chests around are looted once. */
    private static void plan(Minecraft mc) {
        brews=possibleBrews(mc);
        if(brews<=0) {
            if(!looted) { looted=true; lootedAny=false; state=State.SCAN_CHESTS; }
            else { reportMissing(mc); disable(); }
            return;
        }
        info("Ресурсов хватает на "+brews*3+" зелий ("+brews+" варки)");
        state=State.SCAN_STANDS;
    }
    private static int possibleBrews(Minecraft mc) {
        int n=count(mc,AutoBrewing::isWater)/3;
        for(var e:perBrew.entrySet()) n=Math.min(n,count(mc,s->s.is(e.getKey()))/e.getValue());
        return n;
    }
    private static void reportMissing(Minecraft mc) {
        List<String> missing=new ArrayList<>();
        int water=count(mc,AutoBrewing::isWater);
        if(water<3) missing.add(Items.POTION.getName(new ItemStack(Items.POTION)).getString()+" "+water+"/3");
        for(var e:perBrew.entrySet()) {
            int have=count(mc,s->s.is(e.getKey()));
            if(have<e.getValue()) missing.add(e.getKey().getName(new ItemStack(e.getKey())).getString()+" "+have+"/"+e.getValue());
        }
        if(missing.isEmpty()) info("Для зелья "+recipe.title+" ресурсов хватает");
        else error("Для зелья "+recipe.title+" не хватает: "+String.join(", ",missing));
    }

    // Chests: looting ingredients and storing finished potions.
    private static void scanChests(Minecraft mc) {
        chests.clear();
        for(BlockPos pos:around(mc)) if(isChest(mc.level.getBlockState(pos)) && hit(mc,pos)!=null) chests.add(pos);
        sortByDistance(mc,chests);
    }
    private static void nextChest(Minecraft mc) {
        if(chests.isEmpty()) {
            close(mc);
            if(!lootedAny) { reportMissing(mc); disable(); }
            else plan(mc);
            return;
        }
        chest=chests.removeFirst();
        state=State.OPEN_CHEST;
        reset();
    }
    private static void loot(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof ChestMenu menu)) { open(mc,State.LOOT_CHEST,()->nextChest(mc)); return; }
        if(!passed(CLICK)) return;
        Slot slot=null;
        int size=menu.getContainer().getContainerSize();
        for(int i=0;i<size;i++) {
            ItemStack stack=menu.getSlot(i).getItem();
            if(isWater(stack) || perBrew.containsKey(stack.getItem())) { slot=menu.getSlot(i); break; }
        }
        if(slot==null || !roomInInventory(mc,slot.getItem())) { close(mc); chest=null; nextChest(mc); return; }
        click(mc,menu,slot.index,0,ContainerInput.QUICK_MOVE);
        lootedAny=true;
        reset();
    }
    private static void nextOutput(Minecraft mc) {
        if(chests.isEmpty()) { finishRound(mc); return; }
        chest=chests.removeFirst();
        state=State.OPEN_OUTPUT;
        reset();
    }
    private static void store(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof ChestMenu menu)) { open(mc,State.STORE,()->nextOutput(mc)); return; }
        if(!passed(CLICK)) return;
        Slot slot=playerSlot(menu,AutoBrewing::isBrewed);
        if(slot==null) { finishRound(mc); return; }
        if(!roomInChest(menu,slot.getItem())) { close(mc); chest=null; nextOutput(mc); return; }
        click(mc,menu,slot.index,0,ContainerInput.QUICK_MOVE);
        reset();
    }
    private static void finishRound(Minecraft mc) {
        close(mc);
        chest=null;
        if(brews<=0) { info("Автозельеварение завершено"); disable(); }
        else beginPass();
    }
    /** Walks up to a chest (or barrel) and opens it; a menu that does not show up in time skips it. */
    private static void open(Minecraft mc,State next,Runnable skip) {
        if(chest==null) { skip.run(); return; }
        if(mc.player.containerMenu instanceof ChestMenu) { state=next; aim=null; reset(); return; }
        if(passed(OPEN_TIMEOUT)) { chest=null; aim=null; skip.run(); return; }
        BlockHitResult hit=hit(mc,chest);
        if(hit==null) { chest=null; aim=null; skip.run(); return; }
        use(mc,hit);
    }

    // Brewing stands.
    private static void scanStands(Minecraft mc) {
        stands.clear();
        for(BlockPos pos:around(mc)) if(mc.level.getBlockState(pos).is(Blocks.BREWING_STAND) && hit(mc,pos)!=null) stands.add(pos);
        sortByDistance(mc,stands);
        if(stands.isEmpty()) { error("Рядом нет доступных зельеварок"); disable(); return; }
        beginPass();
    }
    private static void beginPass() {
        used=Math.min(brews,stands.size());
        standIndex=0; ingredientIndex=0;
        pass=Pass.SETUP;
        nextStand();
    }
    private static void nextStand() {
        if(standIndex>=used) { passDone(); return; }
        stand=stands.get(standIndex);
        bottleIndex=0; takeIndex=0;
        clearMove();
        state=State.OPEN_STAND;
        reset();
    }
    private static void standDone(Minecraft mc) { close(mc); standIndex++; nextStand(); }
    private static void passDone() {
        var mc=Minecraft.getInstance();
        close(mc);
        standIndex=0;
        clearMove();
        if(pass==Pass.SETUP || pass==Pass.INGREDIENT_LOAD) {
            pass=Pass.INGREDIENT_WAIT;
            stand=null;
            state=State.WAIT_PHASE;
            reset();
        } else {
            brews-=used;
            state=State.SCAN_OUTPUT;
            reset();
        }
    }
    private static void openStand(Minecraft mc) {
        if(stand==null) { state=State.SCAN_STANDS; return; }
        if(mc.player.containerMenu instanceof BrewingStandMenu) {
            state=switch (pass) {
                case SETUP -> State.LOAD_BOTTLES;
                case INGREDIENT_LOAD,INGREDIENT_WAIT -> State.LOAD_INGREDIENT;
                case COLLECT -> State.TAKE_RESULT;
            };
            bottleIndex=0; aim=null; reset();
            return;
        }
        BlockHitResult hit=passed(OPEN_TIMEOUT) ? null : hit(mc,stand);
        if(hit==null) { stand=null; aim=null; state=State.SCAN_STANDS; return; }
        use(mc,hit);
    }
    private static void loadBottles(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof BrewingStandMenu menu)) { reopen(); return; }
        if(!passed(BOTTLE_CLICK)) return;
        if(move==Move.IDLE && !menu.getCarried().isEmpty()) { returnCarried(mc,menu); reset(); return; }
        if(bottleIndex>=BOTTLE_SLOTS.length) {
            int missing=missingBottle(menu);
            if(missing!=-1) bottleIndex=missing;
            else state=State.LOAD_FUEL;
            reset();
            return;
        }
        int slot=BOTTLE_SLOTS[bottleIndex];
        if(move!=Move.IDLE) { moveOne(mc,menu,AutoBrewing::isWater,slot); reset(); return; }
        ItemStack inSlot=menu.getSlot(slot).getItem();
        if(isWater(inSlot)) { clearMove(); bottleIndex++; reset(); }
        else if(!inSlot.isEmpty()) { takeIndex=0; state=State.TAKE_RESULT; reset(); }
        else if(!moveOne(mc,menu,AutoBrewing::isWater,slot)) { error("Не найдены бутылочки с водой"); quit(mc); }
        else reset();
    }
    private static void loadFuel(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof BrewingStandMenu menu)) { reopen(); return; }
        if(!passed(CLICK)) return;
        Predicate<ItemStack> powder=s->s.is(Items.BLAZE_POWDER);
        if(move==Move.IDLE && !menu.getCarried().isEmpty()) { returnCarried(mc,menu); reset(); }
        else if(move!=Move.IDLE) { moveOne(mc,menu,powder,FUEL_SLOT); reset(); }
        else if(!menu.getSlot(FUEL_SLOT).getItem().isEmpty()) {
            int missing=missingBottle(menu);
            if(missing!=-1) { bottleIndex=missing; state=State.LOAD_BOTTLES; reset(); }
            else { clearMove(); state=State.LOAD_INGREDIENT; }
        }
        else if(!moveOne(mc,menu,powder,FUEL_SLOT)) { error("Не найдено топливо для зельеварки"); quit(mc); }
        else reset();
    }
    private static void loadIngredient(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof BrewingStandMenu menu)) { reopen(); return; }
        if(!passed(CLICK)) return;
        if(move==Move.IDLE && !menu.getCarried().isEmpty()) { returnCarried(mc,menu); reset(); return; }
        if(ingredientIndex>=recipe.ingredients.size()) { standDone(mc); return; }
        Item item=recipe.ingredients.get(ingredientIndex);
        Predicate<ItemStack> wanted=s->s.is(item);
        if(move!=Move.IDLE) { moveOne(mc,menu,wanted,INGREDIENT_SLOT); reset(); }
        else if(!menu.getSlot(INGREDIENT_SLOT).getItem().isEmpty()) { clearMove(); standDone(mc); }
        else if(!moveOne(mc,menu,wanted,INGREDIENT_SLOT)) { error("Не найден ингредиент: "+item.getName(new ItemStack(item)).getString()); quit(mc); }
        else reset();
    }
    /** One brewing phase takes 20 s; then the next ingredient goes in, or the potions are collected. */
    private static void waitPhase(Minecraft mc) {
        if(!passed(PHASE)) return;
        ingredientIndex++;
        pass=ingredientIndex>=recipe.ingredients.size() ? Pass.COLLECT : Pass.INGREDIENT_LOAD;
        nextStand();
    }
    private static void takeResult(Minecraft mc) {
        if(!(mc.player.containerMenu instanceof BrewingStandMenu menu)) { reopen(); return; }
        if(!passed(CLICK)) return;
        if(takeIndex>=BOTTLE_SLOTS.length) { standDone(mc); return; }
        int slot=BOTTLE_SLOTS[takeIndex];
        if(menu.getSlot(slot).getItem().isEmpty()) takeIndex++;
        else click(mc,menu,slot,0,ContainerInput.QUICK_MOVE);
        reset();
    }
    private static void reopen() { state=State.OPEN_STAND; reset(); }
    private static void quit(Minecraft mc) { returnAndClose(mc); disable(); }
    private static int missingBottle(BrewingStandMenu menu) {
        if(!menu.getCarried().isEmpty() || move!=Move.IDLE) return 0;
        for(int i=0;i<BOTTLE_SLOTS.length;i++) if(!isWater(menu.getSlot(BOTTLE_SLOTS[i]).getItem())) return i;
        return -1;
    }

    // Moving single items.
    /** One step of putting one matching item from the inventory onto a menu slot; false when there is none. */
    private static boolean moveOne(Minecraft mc,AbstractContainerMenu menu,Predicate<ItemStack> wanted,int target) {
        if(move==Move.IDLE) {
            Slot source=playerSlot(menu,wanted);
            if(source==null) return false;
            moveFrom=source.index; moveTo=target;
            move=Move.PICKED;
            click(mc,menu,source.index,0,ContainerInput.PICKUP);
            return true;
        }
        if(moveTo!=target) { clearMove(); return true; }
        if(move==Move.PICKED) {
            if(menu.getCarried().isEmpty()) { clearMove(); return true; }
            click(mc,menu,moveTo,1,ContainerInput.PICKUP);
            move=Move.PLACED;
            return true;
        }
        if(menu.getCarried().isEmpty()) clearMove();
        else { returnCarried(mc,menu); clearMove(); }
        return true;
    }
    private static void clearMove() { move=Move.IDLE; moveFrom=moveTo=-1; }
    /** Puts what is on the cursor back where it came from, or into any free inventory slot. */
    private static void returnCarried(Minecraft mc,AbstractContainerMenu menu) {
        if(menu.getCarried().isEmpty()) { clearMove(); return; }
        Slot back=moveFrom>=0 && moveFrom<menu.slots.size() ? menu.getSlot(moveFrom) : null;
        if(back!=null && back.container instanceof Inventory && (back.getItem().isEmpty()
                || ItemStack.isSameItemSameComponents(back.getItem(),menu.getCarried()) && back.getItem().getCount()<back.getItem().getMaxStackSize())) {
            click(mc,menu,back.index,0,ContainerInput.PICKUP);
            return;
        }
        Slot free=playerSlot(menu,ItemStack::isEmpty);
        if(free!=null) click(mc,menu,free.index,0,ContainerInput.PICKUP);
        else error("Нет свободного слота, чтобы вернуть предмет с курсора");
    }
    private static void returnAndClose(Minecraft mc) {
        var menu=mc.player.containerMenu;
        if(menu!=null && !menu.getCarried().isEmpty()) {
            returnCarried(mc,menu);
            if(!menu.getCarried().isEmpty()) return;
        }
        close(mc);
    }
    private static void close(Minecraft mc) {
        if(mc.player.containerMenu!=mc.player.inventoryMenu) mc.player.closeContainer();
    }
    private static void click(Minecraft mc,AbstractContainerMenu menu,int slot,int button,ContainerInput input) {
        mc.gameMode.handleContainerInput(menu.containerId,slot,button,input,mc.player);
    }
    private static Slot playerSlot(AbstractContainerMenu menu,Predicate<ItemStack> wanted) {
        for(Slot slot:menu.slots) if(slot.container instanceof Inventory && wanted.test(slot.getItem())) return slot;
        return null;
    }
    private static boolean roomInInventory(Minecraft mc,ItemStack stack) {
        var inventory=mc.player.getInventory();
        for(int i=0;i<36;i++) {
            ItemStack s=inventory.getItem(i);
            if(s.isEmpty() || ItemStack.isSameItemSameComponents(s,stack) && s.getCount()<s.getMaxStackSize()) return true;
        }
        return false;
    }
    private static boolean roomInChest(ChestMenu menu,ItemStack stack) {
        int size=menu.getContainer().getContainerSize();
        for(int i=0;i<size;i++) {
            ItemStack s=menu.getSlot(i).getItem();
            if(s.isEmpty() || ItemStack.isSameItemSameComponents(s,stack) && s.getCount()<s.getMaxStackSize()) return true;
        }
        return false;
    }

    // Items.
    private static boolean isWater(ItemStack stack) {
        if(!stack.is(Items.POTION)) return false;
        PotionContents contents=stack.get(DataComponents.POTION_CONTENTS);
        return contents!=null && contents.is(Potions.WATER);
    }
    private static boolean isBrewed(ItemStack stack) {
        if(!stack.is(Items.POTION)) return false;
        PotionContents contents=stack.get(DataComponents.POTION_CONTENTS);
        return contents!=null && contents.potion().isPresent() && !contents.is(Potions.WATER);
    }
    private static int count(Minecraft mc,Predicate<ItemStack> wanted) {
        int n=0;
        var inventory=mc.player.getInventory();
        for(int i=0;i<36;i++) if(wanted.test(inventory.getItem(i))) n+=inventory.getItem(i).getCount();
        return n;
    }

    // Blocks and aiming.
    private static boolean isChest(BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.BARREL);
    }
    private static List<BlockPos> around(Minecraft mc) {
        List<BlockPos> out=new ArrayList<>();
        BlockPos at=mc.player.blockPosition();
        int r=(int)Math.ceil(SCAN_RADIUS),h=(int)Math.ceil(SCAN_HEIGHT);
        for(BlockPos pos:BlockPos.betweenClosed(at.offset(-r,-h,-r),at.offset(r,h,r)))
            if(pos.distSqr(at)<=SCAN_RADIUS*SCAN_RADIUS+SCAN_HEIGHT*SCAN_HEIGHT) out.add(pos.immutable());
        return out;
    }
    private static void sortByDistance(Minecraft mc,List<BlockPos> list) {
        Vec3 eye=mc.player.getEyePosition();
        list.sort(Comparator.comparingDouble(p->Vec3.atCenterOf(p).distanceToSqr(eye)));
    }
    /** A point of the block that the eye sees directly (centre or a face centre) within reach, or null. */
    private static BlockHitResult hit(Minecraft mc,BlockPos pos) {
        BlockState state=mc.level.getBlockState(pos);
        if(!state.is(Blocks.BREWING_STAND) && !isChest(state)) return null;
        Vec3 eye=mc.player.getEyePosition(),centre=Vec3.atCenterOf(pos);
        double reach=mc.player.blockInteractionRange();
        List<Vec3> points=new ArrayList<>(List.of(centre));
        for(Direction d:Direction.values()) points.add(centre.add(Vec3.atLowerCornerOf(d.getUnitVec3i()).scale(.5)));
        for(Vec3 point:points) {
            if(point.distanceTo(eye)>reach) continue;
            Vec3 end=eye.add(point.subtract(eye).normalize().scale(reach));
            BlockHitResult result=mc.level.clip(new ClipContext(eye,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player));
            if(result.getType()!=HitResult.Type.MISS && result.getBlockPos().equals(pos)) return result;
        }
        return null;
    }
    /** Aims at the block and clicks it once the server-side rotation looks at it. */
    private static void use(Minecraft mc,BlockHitResult hit) {
        Vec3 eye=mc.player.getEyePosition();
        aim=Rotation.fromDirection(hit.getLocation().subtract(eye));
        Rotation server=new Rotation(RotationManager.getServerYaw(),RotationManager.getServerPitch());
        long now=System.currentTimeMillis();
        if(server.distanceTo(aim)>4 || now-lastUse<CLICK*4) return;
        mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);
        mc.player.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
        lastUse=now;
    }

    private static void info(String text) { HudNotifications.queue(Icon.FLASK,Theme.accent(),"AutoBrewing",text); }
    private static void error(String text) { HudNotifications.queue(Icon.CLOSE,0xffff6b5e,"AutoBrewing",text); }
}
