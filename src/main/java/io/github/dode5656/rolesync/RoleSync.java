package io.github.dode5656.rolesync;

import ch.jalu.configme.resource.YamlFileResource;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.service.yaml.YamlFileResourceProvider;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import io.github.dode5656.rolesync.commands.ReloadCommand;
import io.github.dode5656.rolesync.commands.SyncCommand;
import io.github.dode5656.rolesync.commands.UnSyncCommand;
import io.github.dode5656.rolesync.events.AuthMeLoginEvent;
import io.github.dode5656.rolesync.events.AuthMeRegisterEvent;
import io.github.dode5656.rolesync.events.JoinEvent;
import io.github.dode5656.rolesync.storage.FileStorage;
import io.github.dode5656.rolesync.utilities.ConfigChecker;
import io.github.dode5656.rolesync.utilities.MessageManager;
import io.github.dode5656.rolesync.utilities.PluginStatus;
import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.logging.Level;

public final class RoleSync extends JavaPlugin {

    private FileStorage playerCache;
    private FileStorage messages;
    private MessageManager messageManager;
    private JDA jda;
    private PluginStatus pluginStatus;
    private ConfigChecker configChecker;

    @Override
    public void onEnable() {
        pluginStatus = PluginStatus.ENABLED;

        saveDefaultConfig();

        playerCache = new FileStorage("playerCache.yml", new File(getDataFolder().getPath(), "cache"));

        messages = new FileStorage("messages.yml", new File(getDataFolder().getPath()));
        messages.saveDefaults(this);
        messageManager = new MessageManager(this);

        configChecker = new ConfigChecker(this);
        configChecker.checkDefaults();

        startBot();

        getCommand("sync").setExecutor(new SyncCommand(this));
        getCommand("syncreload").setExecutor(new ReloadCommand(this));
        getCommand("unsync").setExecutor(new UnSyncCommand(this));

        if (getServer().getPluginManager().getPlugin(AuthMe.getPluginName()) != null) {
            Settings authMeSettings = null;
            try {
                authMeSettings = (Settings) FieldUtils.readDeclaredField(Bukkit.getPluginManager().getPlugin("AuthMe"), "settings", true);
            } catch (IllegalAccessException e) {
                getLogger().log(Level.SEVERE, "Couldn't get AuthMe Settings", e);
            }

            if (authMeSettings != null && authMeSettings.getProperty(RegistrationSettings.FORCE_LOGIN_AFTER_REGISTER)) {
                getServer().getPluginManager().registerEvents(new AuthMeLoginEvent(this),this);
            } else {
                getServer().getPluginManager().registerEvents(new AuthMeRegisterEvent(this), this);
            }

        } else {
            getServer().getPluginManager().registerEvents(new JoinEvent(this), this);
        }

        if (!getConfig().getBoolean("opt-out-bstats", false)) {
            int pluginId = 6790;
            Metrics metrics = new Metrics(this, pluginId);
        }

    }

    @Override
    public void onDisable() {
        if (jda != null && jda.getStatus() == JDA.Status.CONNECTED) jda.shutdown();
    }

    public final FileStorage getMessages() {
        return messages;
    }

    public final FileStorage getPlayerCache() {
        return playerCache;
    }

    public final MessageManager getMessageManager() {
        return messageManager;
    }

    public final JDA getJDA() {
        return jda;
    }

    public PluginStatus getPluginStatus() {
        return pluginStatus;
    }

    public void setPluginStatus(PluginStatus pluginStatus) {
        this.pluginStatus = pluginStatus;
    }

    public ConfigChecker getConfigChecker() {
        return configChecker;
    }

    public void startBot() {
        try {
            this.jda = new JDABuilder(AccountType.BOT).setToken(getConfig().getString("bot-token")).build();
        } catch (LoginException e) {
            getLogger().log(Level.SEVERE, "Error when logging in!");
            disablePlugin();
        }
    }

    public void disablePlugin() {
        pluginStatus = PluginStatus.DISABLED;
    }
}
