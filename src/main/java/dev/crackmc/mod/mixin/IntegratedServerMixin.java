package dev.crackmc.mod.mixin;

import dev.crackmc.mod.CrackMCMod;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @Inject(method = "openToLan", at = @At("RETURN"), require = 0)
    private void onOpenToLan(CallbackInfoReturnable<Boolean> cir) {
        if (CrackMCMod.crackedModeEnabled && Boolean.TRUE.equals(cir.getReturnValue())) {
            IntegratedServer server = (IntegratedServer) (Object) this;
            server.setOnlineMode(false);
            CrackMCMod.LOGGER.info("[CrackMC] LAN opened with cracked (offline) mode enabled on 1.21.11.");
        }
    }
}
