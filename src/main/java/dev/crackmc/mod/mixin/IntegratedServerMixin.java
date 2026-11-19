package dev.crackmc.mod.mixin;

import dev.crackmc.mod.CrackMCMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(targets = "net.minecraft.server.integrated.IntegratedServer", remap = false)
public abstract class IntegratedServerMixin {

    @Inject(method = "*", at = @At("RETURN"), require = 0)
    private void onOpenToLan(GameMode gameMode, boolean cheatsAllowed, int port, CallbackInfoReturnable<Boolean> cir) {
        if (CrackMCMod.crackedModeEnabled && Boolean.TRUE.equals(cir.getReturnValue())) {
            Object self = this;
            if (self instanceof MinecraftServer server) {
                try {
                    Method setOnlineMode = server.getClass().getMethod("setOnlineMode", boolean.class);
                    setOnlineMode.invoke(server, false);
                    CrackMCMod.LOGGER.info("[CrackMC] Forced offline-mode=false for cracked players.");
                } catch (Exception e) {
                    CrackMCMod.LOGGER.debug("[CrackMC] setOnlineMode reflection fallback: {}", e.getMessage());
                }
            }
        }
    }
}
