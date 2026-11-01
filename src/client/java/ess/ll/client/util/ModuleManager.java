package ess.ll.client.util;
import ess.ll.client.module.combat.*;
import ess.ll.client.module.player.ClickPearl;
import ess.ll.client.module.player.FreeCam;
import ess.ll.client.module.combat.AntiBot;
import ess.ll.client.module.combat.Criticals;
import ess.ll.client.module.movement.AirStake;
import ess.ll.client.module.combat.WebTrap;
import ess.ll.client.module.movement.NoWeb;
import ess.ll.client.module.player.AutoTool;
import ess.ll.client.module.player.InventoryMove;
import ess.ll.client.module.player.ElytraHelper;
import ess.ll.client.module.movement.ElytraMotyaon;
import ess.ll.client.module.movement.ElytraBooster;
import ess.ll.client.module.movement.AutoSprint;
import ess.ll.client.module.render.SwingAnimations;
import ess.ll.client.module.render.ViewModel;
import ess.ll.client.module.render.Particles;
import ess.ll.client.module.render.ProjectileHelper;
import ess.ll.client.module.render.TargetESP;
import ess.ll.client.module.render.EntityESP;
import ess.ll.client.module.render.Removals;
import ess.ll.client.integration.OptionalModules.*;
/** Narrow registry: the four ported modules share their real instances with ESS. */
public enum ModuleManager { INSTANCE;
 private final AttackAura aura=new AttackAura(); private final AutoTotem totem=new AutoTotem();
 private final CrystalAura crystal=new CrystalAura(); private final PearlTarget pearl=new PearlTarget();
 private final ClickPearl thrower=new ClickPearl();
 private final ess.ll.client.module.combat.CrystalAuraV2 crystalV2=new ess.ll.client.module.combat.CrystalAuraV2();
 private final ess.ll.client.module.combat.AntiCrystal antiCrystal=new ess.ll.client.module.combat.AntiCrystal();
 private final AutoTool autoTool=new AutoTool(); private final SwingAnimations swings=new SwingAnimations();
 private final ViewModel viewModel=new ViewModel();
 private final Particles particles=new Particles();
 private final ProjectileHelper predictions=new ProjectileHelper();
 private final TargetESP targetEsp=new TargetESP();
 private final EntityESP entityEsp=new EntityESP();
 private final Removals removals=new Removals();
 private final ElytraHelper elytraHelper=new ElytraHelper();
 private final ElytraMotyaon elytraMotyaon=new ElytraMotyaon();
 private final ElytraBooster elytraBooster=new ElytraBooster();
 private final Criticals criticals=new Criticals(); private final ElytraTarget elytra=new ElytraTarget();
 private final FreeCam camera=new FreeCam(); private final AntiBot bots=new AntiBot();
 private final InventoryMove inventory=new InventoryMove(); private final AirStake air=new AirStake();
 private final AutoSprint sprint=new AutoSprint();
 private final WebTrap webTrap=new WebTrap();
 private final AutoExplosion autoExplosion=new AutoExplosion();
 private final NoFriendDamage noFriendDamage=new NoFriendDamage();
 private final ess.ll.client.module.player.WebIgnore webIgnore=new ess.ll.client.module.player.WebIgnore();
 public NoFriendDamage noFriendDamage(){return noFriendDamage;}
 public ess.ll.client.module.player.WebIgnore webIgnore(){return webIgnore;}
 public AutoExplosion autoExplosion(){return autoExplosion;}
 private final NoWeb noWeb=new NoWeb();
 private final Absent absent=new Absent();
 public AttackAura aura(){return aura;} public AutoTotem autoTotem(){return totem;}
 public ess.ll.client.module.combat.CrystalAuraV2 crystalAuraV2(){return crystalV2;}
 public ess.ll.client.module.combat.AntiCrystal antiCrystal(){return antiCrystal;}
 public CrystalAura crystalAura(){return crystal;} public PearlTarget pearlTarget(){return pearl;}
 public ClickPearl clickPearl(){return thrower;} public Criticals criticals(){return criticals;}
 public AutoTool autoTool(){return autoTool;} public SwingAnimations swingAnimations(){return swings;}
 public ViewModel viewModel(){return viewModel;}
 public Particles particles(){return particles;}
 public ProjectileHelper predictions(){return predictions;}
 public TargetESP targetESP(){return targetEsp;}
 public EntityESP entityESP(){return entityEsp;}
 public Removals removals(){return removals;}
 public ElytraHelper elytraHelper(){return elytraHelper;}
 public ElytraMotyaon elytraMotyaon(){return elytraMotyaon;}
 public ElytraBooster elytraBooster(){return elytraBooster;}
 public ElytraTarget elytraTarget(){return elytra;} public FreeCam freeCam(){return camera;}
 public AntiBot antiBot(){return bots;} public MaceHelper maceHelper(){return null;}
 public WebTrap webTrap(){return webTrap;} public AutoAnchor autoAnchor(){return null;}
 public NoWeb noWeb(){return noWeb;}
 public InventoryMove inventoryMove(){return inventory;} public AirStake airStuck(){return air;}
 public AutoSprint autoSprint(){return sprint;}
 public Absent scoreboardHealth(){return absent;} public Absent absentModule(){return absent;}
}
