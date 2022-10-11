package dev.lone.GriefPreventionStickFix;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import dev.lone.LoneLibs.nbt.nbtapi.utils.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Pattern;

public final class Main extends JavaPlugin implements Listener
{
    private static Pattern REGEX_NO_ONE_CLAIMED;
    private static Pattern REGEX_SIZE;
    private static Pattern REGEX_LAST_LOGIN;
    private static Pattern REGEX_CLAIMED_BY;
    private static Material MATERIAL;

    @SuppressWarnings("ConstantConditions")
    private void loadConfig()
    {
        MATERIAL = Material.valueOf(getConfig().getString("material"));
        REGEX_NO_ONE_CLAIMED = Pattern.compile(getConfig().getString("regex.no-one-claimed-block"));
        REGEX_SIZE = Pattern.compile(getConfig().getString("regex.size"));
        REGEX_LAST_LOGIN = Pattern.compile(getConfig().getString("regex.last-login"));
        REGEX_CLAIMED_BY = Pattern.compile(getConfig().getString("regex.claimed-by"));
    }

    @Override
    public void onEnable()
    {
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, this);

        if(!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_19_R1))
        {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.CHAT)
            {
                @Override
                public void onPacketSending(PacketEvent e)
                {
                    PacketContainer packet = e.getPacket();

                    WrappedChatComponent wrappedChatComponent = packet.getChatComponents().getValues().get(0);
                    if (wrappedChatComponent == null)
                        return;
                    String json = wrappedChatComponent.getJson();
                    if (json == null)
                        return;
                    if (e.getPlayer().getInventory().getItemInMainHand().getType() != MATERIAL)
                        return;

                    handleSuppression(e, json);
                }
            });
        }
        else
        {
            ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.SYSTEM_CHAT)
            {
                @Override
                public void onPacketSending(PacketEvent e)
                {
                    PacketContainer packet = e.getPacket();
                    if(!isActionBar(packet))
                    {
                        String json = packet.getStrings().readSafely(0);
                        handleSuppression(e, json);
                    }
                }
            });
        }
        Bukkit.getConsoleSender().sendMessage("[GriefPreventionStickFix] " + ChatColor.GREEN + "Enabled fix messages on interact with " + MATERIAL + " with ItemMeta");
    }

    private void handleSuppression(PacketEvent e, String json)
    {
        if (REGEX_NO_ONE_CLAIMED.matcher(json).find()
                || REGEX_SIZE.matcher(json).find()
                || REGEX_LAST_LOGIN.matcher(json).find()
                || REGEX_CLAIMED_BY.matcher(json).find()
        )
        {
            if (e.getPlayer().getInventory().getItemInMainHand().hasItemMeta())
                e.setCancelled(true);
        }
    }

    @Override
    public void onDisable()
    {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        Bukkit.getConsoleSender().sendMessage("[GriefPreventionStickFix] " + ChatColor.GRAY + "Disabled fix messages on interact with " + MATERIAL + " with ItemMeta");
    }

    private boolean isActionBar(PacketContainer packet)
    {
        Integer typeId = packet.getIntegers().readSafely(0);
        if(typeId != null) // <= 1.19
            return typeId != 2; // != GAME_INFO, not actionbar
        Boolean overlay = packet.getBooleans().readSafely(0); // >= 1.19.1
        if(overlay != null)
            return overlay;
        return false;
    }
}
