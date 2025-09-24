package org.haemin.guildPlus.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.haemin.guildPlus.GuildPlus;

import java.lang.reflect.Method;

public class EconHook {
    private final boolean vaultPresent;
    private Object provider;
    private Method mHas;
    private Method mWithdraw;
    private Method mTxnSuccess;

    public EconHook(GuildPlus plugin) {
        this.vaultPresent = Bukkit.getPluginManager().getPlugin("Vault") != null;
        try {
            if (vaultPresent) {
                Class<?> econClazz = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(econClazz);
                if (rsp != null) {
                    provider = rsp.getProvider();
                    mHas = econClazz.getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
                    mWithdraw = econClazz.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
                    Class<?> respClazz = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
                    mTxnSuccess = respClazz.getMethod("transactionSuccess");
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[GuildPlus] Vault economy hook failed: " + t.getMessage());
            provider = null;
        }
        if (provider == null) plugin.getLogger().warning("[GuildPlus] Vault economy not available. Costs will be ignored.");
    }

    public boolean isReady() { return provider != null; }

    public boolean has(Player p, double amount) {
        try { return provider != null && (boolean) mHas.invoke(provider, p, amount); }
        catch (Throwable ignored) { return false; }
    }

    public boolean withdraw(Player p, double amount) {
        try {
            if (provider == null) return false;
            Object resp = mWithdraw.invoke(provider, p, amount);
            return (boolean) mTxnSuccess.invoke(resp);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
