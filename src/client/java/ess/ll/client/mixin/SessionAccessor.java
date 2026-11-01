package ess.ll.client.mixin;

import com.mojang.authlib.services.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.CompletableFuture;

/** The session pieces the account switcher replaces: who is logged in, their profile, chat keys and reporting. */
@Mixin(Minecraft.class)
public interface SessionAccessor {
    @Mutable @Accessor("user") void ess$setUser(User user);
    @Accessor("profileFuture") CompletableFuture<ProfileResult> ess$profileFuture();
    @Mutable @Accessor("profileFuture") void ess$setProfileFuture(CompletableFuture<ProfileResult> future);
    @Mutable @Accessor("profileKeyPairManager") void ess$setProfileKeys(ProfileKeyPairManager keys);
    @Accessor("reportingContext") void ess$setReportingContext(ReportingContext context);
}
