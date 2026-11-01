package ess.ll.client.module.misc;

import ess.ll.client.command.Keys;
import ess.ll.client.module.ClientModules;
import ess.ll.client.render.GlowFx;
import ess.ll.client.setting.Settings.*;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

/**
 * ShulkerPreview (from Zenith): a shulker box's tooltip shows its 27 slots as a little chest window instead of a
 * text list, and shulkers lying on the ground show the same window above them. Either always or while a key is
 * held. Hooks: ShulkerPreviewMixin (tooltip image), ShulkerPreviewTooltipMixin (drawing it),
 * ShulkerPreviewLinesMixin (the vanilla content lines), and the HUD layer for the world.
 */
public final class ShulkerPreview {
    public static final BooleanSetting ALWAYS=new BooleanSetting("Всегда показывать",true);
    public static final KeybindSetting KEY=new KeybindSetting("Клавиша",-1).visibleWhen(()->!ALWAYS.get());
    public static final BooleanSetting WORLD=new BooleanSetting("В мире",true);
    public static final SliderSetting WORLD_SCALE=new SliderSetting("Размер в мире",.3f,1.5f,.05f,.7f);
    public static final Setting[] SETTINGS={ALWAYS,KEY,WORLD,WORLD_SCALE};
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("ess-best","textures/gui/shulker_preview.png");
    private static final int WIDTH=176,HEIGHT=67;

    private ShulkerPreview() {}

    /** The tooltip image of a shulker box: its 27 slots, copied so later changes do not leak into it. */
    public record Contents(NonNullList<ItemStack> items) implements TooltipComponent {}

    private static boolean shown() {
        if(ClientModules.SHULKER_PREVIEW==null || !ClientModules.SHULKER_PREVIEW.enabled()) return false;
        return ALWAYS.get() || KEY.get()>=0 && Keys.isDown(KEY.get());
    }
    public static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock;
    }
    private static NonNullList<ItemStack> items(ItemStack stack) {
        NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
        ItemContainerContents contents=stack.get(DataComponents.CONTAINER);
        if(contents!=null) contents.copyInto(items);
        return items;
    }
    /** Replaces the tooltip image of a shulker box; empty for everything else or when the preview is off. */
    public static Optional<TooltipComponent> image(ItemStack stack) {
        return shown() && isShulker(stack) ? Optional.of(new Contents(items(stack))) : Optional.empty();
    }
    /** The vanilla "contains ..." lines go away while the window shows the same thing. */
    public static boolean hidesContentLines() { return shown(); }

    public static ClientTooltipComponent client(Contents contents) {
        return new ClientTooltipComponent() {
            @Override public int getHeight(Font font) { return HEIGHT+2; }
            @Override public int getWidth(Font font) { return WIDTH; }
            @Override public void extractImage(Font font,int x,int y,int w,int h,GuiGraphicsExtractor g) { draw(g,font,contents.items(),x,y); }
        };
    }
    private static void draw(GuiGraphicsExtractor g,Font font,NonNullList<ItemStack> items,int x,int y) {
        g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,x,y,0,0,WIDTH,HEIGHT,WIDTH,HEIGHT);
        for(int i=0;i<27 && i<items.size();i++) {
            ItemStack stack=items.get(i);
            if(stack.isEmpty()) continue;
            int sx=x+8+i%9*18,sy=y+7+i/9*18;
            g.item(stack,sx,sy);
            g.itemDecorations(font,stack,sx,sy);
        }
    }

    /** HUD layer: the window above every shulker box item on the ground that is in view. */
    public static void renderHud(GuiGraphicsExtractor g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(!WORLD.get() || !shown() || mc.level==null || mc.player==null) return;
        float partial=delta.getGameTimeDeltaPartialTick(true),scale=WORLD_SCALE.get();
        for(var entity:mc.level.entitiesForRendering()) {
            if(!(entity instanceof ItemEntity item) || !isShulker(item.getItem())) continue;
            Vec3 at=item.getPosition(partial);
            AABB box=item.getBoundingBox().move(at.subtract(item.position()));
            float[] rect=project(box);
            if(rect==null) continue;
            float cx=(rect[0]+rect[2])/2,cy=rect[1]-4-HEIGHT*scale/2;
            g.pose().pushMatrix();
            g.pose().translate(cx,cy);
            g.pose().scale(scale,scale);
            draw(g,mc.font,items(item.getItem()),-WIDTH/2,-HEIGHT/2);
            g.pose().popMatrix();
        }
    }
    /** Screen rectangle (min x, min y, max x, max y) of the box's corners in front of the camera, or null. */
    private static float[] project(AABB box) {
        float[] rect=null;
        for(int i=0;i<8;i++) {
            Vector2f p=GlowFx.screen(new Vec3((i&1)==0 ? box.minX : box.maxX,(i&2)==0 ? box.minY : box.maxY,(i&4)==0 ? box.minZ : box.maxZ),true);
            if(p==null) continue;
            if(rect==null) rect=new float[]{p.x,p.y,p.x,p.y};
            else { rect[0]=Math.min(rect[0],p.x); rect[1]=Math.min(rect[1],p.y); rect[2]=Math.max(rect[2],p.x); rect[3]=Math.max(rect[3],p.y); }
        }
        return rect;
    }
}
