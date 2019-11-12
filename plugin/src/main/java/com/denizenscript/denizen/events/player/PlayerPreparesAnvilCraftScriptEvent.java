package com.denizenscript.denizen.events.player;

import com.denizenscript.denizen.objects.EntityTag;
import com.denizenscript.denizen.objects.InventoryTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.PlayerTag;
import com.denizenscript.denizen.utilities.implementation.BukkitScriptEntryData;
import com.denizenscript.denizen.events.BukkitScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;

public class PlayerPreparesAnvilCraftScriptEvent extends BukkitScriptEvent implements Listener {

    // <--[event]
    // @Events
    // player prepares anvil craft item
    // player prepares anvil craft <item>
    //
    // @Regex ^on player prepares anvil craft [^\s]+$
    //
    // @Triggers when a player prepares an anvil to craft an item.
    //
    // @Warning The player doing the crafting is estimated and may be inaccurate.
    //
    // @Context
    // <context.inventory> returns the InventoryTag of the anvil inventory.
    // <context.item> returns the ItemTag to be crafted.
    // <context.repair_cost> returns an ElementTag(Number) of the repair cost.
    // <context.new_name> returns an ElementTag of the new name.
    //
    // @Determine
    // ElementTag(Number) to set the repair cost.
    // ItemTag to change the item that is crafted.
    //
    // @Player Always.
    //
    // -->

    public PlayerPreparesAnvilCraftScriptEvent() {
        instance = this;
    }

    public static PlayerPreparesAnvilCraftScriptEvent instance;
    public PrepareAnvilEvent event;
    public ItemTag result;
    public PlayerTag player;

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("player prepares anvil craft");
    }

    @Override
    public boolean matches(ScriptPath path) {
        String eItem = path.eventArgLowerAt(4);

        if (!tryItem(result, eItem)) {
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "PlayerPreparesAnvilCraft";
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determinationObj) {
        if (determinationObj instanceof ElementTag && ((ElementTag) determinationObj).isInt()) {
            event.getInventory().setRepairCost(((ElementTag) determinationObj).asInt());
            return true;
        }
        String determination = determinationObj.toString();
        if (ItemTag.matches(determination)) {
            result = ItemTag.valueOf(determination, path.container);
            event.setResult(result.getItemStack());
            return true;
        }
        return super.applyDetermination(path, determinationObj);
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return new BukkitScriptEntryData(player, null);
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("item")) {
            return result;
        }
        else if (name.equals("repair_cost")) {
            return new ElementTag(event.getInventory().getRepairCost());
        }
        else if (name.equals("new_name")) {
            return new ElementTag(event.getInventory().getRenameText());
        }
        else if (name.equals("inventory")) {
            return InventoryTag.mirrorBukkitInventory(event.getInventory());
        }
        return super.getContext(name);
    }

    @EventHandler
    public void onCraftItem(PrepareAnvilEvent event) {
        if (event.getInventory().getViewers().size() == 0) {
            return;
        }
        HumanEntity humanEntity = event.getInventory().getViewers().get(0);
        if (EntityTag.isNPC(humanEntity)) {
            return;
        }
        this.event = event;
        result = new ItemTag(event.getResult());
        this.player = EntityTag.getPlayerFrom(humanEntity);
        this.cancelled = false;
        fire(event);
    }
}
