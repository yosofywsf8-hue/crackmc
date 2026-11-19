package dev.crackmc.mod.mixin;

import dev.crackmc.mod.CrackMCMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("HEAD"), cancellable = true, require = 0)
    private void onConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        PlayerManager pm = (PlayerManager) (Object) this;
        String name = player.getName().getString().toLowerCase(Locale.ROOT);

        if (CrackMCMod.bannedNames.contains(name)) {
            player.networkHandler.disconnect(Text.literal("Banned from this CrackMC world."));
            ci.cancel();
            return;
        }

        if (pm.getCurrentPlayerCount() >= CrackMCMod.maxPlayers) {
            player.networkHandler.disconnect(Text.literal("CrackMC world is full (" + CrackMCMod.maxPlayers + " max)."));
            ci.cancel();
            return;
        }

        if (CrackMCMod.pinCode != null && !CrackMCMod.verifiedPinPlayers.contains(name)) {
            player.sendMessage(Text.literal("[CrackMC] This world is PIN-protected. Use /pin <code> to stay connected."), false);
        }
    }
}
