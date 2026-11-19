package dev.crackmc.mod.mixin;

import dev.crackmc.mod.CrackMCMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Locale;

@Mixin(targets = "net.minecraft.server.network.ServerLoginNetworkHandler", remap = false)
public class ServerLoginNetworkHandlerMixin {

    @Inject(method = "*", at = @At("HEAD"), require = 0)
    private void onLoginStart(CallbackInfo ci) {
        if (!CrackMCMod.crackedModeEnabled) return;
        try {
            String profileName = null;
            for (Field f : this.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(this);
                if (val != null && val.getClass().getName().contains("GameProfile")) {
                    try {
                        Object nameObj = val.getClass().getMethod("getName").invoke(val);
                        if (nameObj != null) profileName = nameObj.toString();
                    } catch (Exception ignored) {}
                }
            }
            if (profileName != null) {
                String lower = profileName.toLowerCase(Locale.ROOT);
                if (CrackMCMod.bannedNames.contains(lower)) {
                    disconnectSafely("You are banned from this CrackMC world.");
                }
            }
        } catch (Exception ignored) {}
    }

    private void disconnectSafely(String reason) {
        try {
            for (var m : this.getClass().getMethods()) {
                if (m.getName().equals("disconnect") && m.getParameterCount() == 1) {
                    Class<?> pType = m.getParameterTypes()[0];
                    if (pType.getName().contains("Text")) {
                        Object textObj = pType.getMethod("literal", String.class).invoke(null, reason);
                        m.invoke(this, textObj);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
