package net.aufdemrand.denizen.scripts.commands.world;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.nms.NMSHandler;
import net.aufdemrand.denizen.nms.NMSVersion;
import net.aufdemrand.denizen.objects.dChunk;
import net.aufdemrand.denizen.objects.dLocation;
import net.aufdemrand.denizen.utilities.DenizenAPI;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.depends.Depends;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Duration;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.HashMap;
import java.util.Map;

public class ChunkLoadCommand extends AbstractCommand implements Listener {

    // <--[command]
    // @Name ChunkLoad
    // @Syntax chunkload ({add}/remove/removeall) [<chunk>] (duration:<value>)
    // @Required 1
    // @Short Keeps a chunk actively loaded and allowing activity.
    // @Group world
    //
    // @Description
    // Forces a chunk to load and stay loaded in the world for the duration specified or until removed.
    // This will not over server restarts.
    // If no duration is specified it defaults to 0 (forever).
    // While a chunk is loaded all normal activity such as crop growth and npc activity continues,
    // other than activity that requires a nearby player.
    //
    // @Tags
    // <w@world.loaded_chunks>
    // <ch@chunk.is_loaded>
    //
    // @Usage
    // Use to load a chunk.
    // - chunkload ch@0,0,world
    //
    // @Usage
    // Use to temporarily load a chunk.
    // - chunkload ch@0,0,world duration:5m
    //
    // @Usage
    // Use to stop loading a chunk.
    // - chunkload remove ch@0,0,world
    //
    // @Usage
    // Use to stop loading all chunks.
    // - chunkload removeall
    // -->

    @Override
    public void onEnable() {
        Denizen denizen = DenizenAPI.getCurrentInstance();
        denizen.getServer().getPluginManager().registerEvents(this, denizen);
        if (Depends.citizens != null) {
            denizen.getServer().getPluginManager().registerEvents(new ChunkLoadCommandNPCEvents(), denizen);
        }
    }

    /*
     * Keeps a chunk loaded
     */

    private enum Action {ADD, REMOVE, REMOVEALL}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {
            if (arg.matchesEnum(Action.values())
                    && !scriptEntry.hasObject("action")) {
                scriptEntry.addObject("action", new Element(arg.getValue().toUpperCase()));
                if (arg.getValue().equalsIgnoreCase("removeall")) {
                    scriptEntry.addObject("location", new dLocation(Bukkit.getWorlds().get(0), 0, 0, 0));
                }
            }
            else if (arg.matchesArgumentType(dChunk.class)
                    && !scriptEntry.hasObject("location")) {
                scriptEntry.addObject("location", arg.asType(dChunk.class).getCenter());
            }
            else if (arg.matchesArgumentType(dLocation.class)
                    && !scriptEntry.hasObject("location")) {
                scriptEntry.addObject("location", arg.asType(dLocation.class));
            }
            else if (arg.matchesArgumentType(Duration.class)
                    && !scriptEntry.hasObject("duration")) {
                scriptEntry.addObject("duration", arg.asType(Duration.class));
            }
            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("location")) {
            throw new InvalidArgumentsException("Missing location argument!");
        }

        if (!scriptEntry.hasObject("action")) {
            scriptEntry.addObject("action", new Element("ADD"));
        }

        if (!scriptEntry.hasObject("duration")) {
            scriptEntry.addObject("duration", new Duration(0));
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        // Get objects
        Element action = scriptEntry.getElement("action");
        dLocation chunkloc = (dLocation) scriptEntry.getObject("location");
        Duration length = (Duration) scriptEntry.getObject("duration");

        if (scriptEntry.dbCallShouldDebug()) {

            dB.report(scriptEntry, getName(),
                    action.debug()
                            + chunkloc.debug()
                            + length.debug());

        }

        Chunk chunk = chunkloc.getChunk();
        String chunkString = chunk.getX() + ", " + chunk.getZ() + "," + chunkloc.getWorldName();

        switch (Action.valueOf(action.asString())) {
            case ADD:
                if (length.getSeconds() != 0) {
                    chunkDelays.put(chunkString, System.currentTimeMillis() + length.getMillis());
                }
                else {
                    chunkDelays.put(chunkString, (long) 0);
                }
                dB.echoDebug(scriptEntry, "...added chunk " + chunk.getX() + ", " + chunk.getZ() + " with a delay of " + length.getSeconds() + " seconds.");
                if (!chunk.isLoaded()) {
                    chunk.load();
                }
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14_R1)) {
                    chunk.setForceLoaded(true);
                    if (length.getSeconds() > 0) {
                        Bukkit.getScheduler().scheduleSyncDelayedTask(DenizenAPI.getCurrentInstance(), new Runnable() {
                            @Override
                            public void run() {
                                if (chunkDelays.containsKey(chunkString) && chunkDelays.get(chunkString) <= System.currentTimeMillis()) {
                                    chunk.setForceLoaded(false);
                                    chunkDelays.remove(chunkString);
                                }
                            }
                        }, length.getTicks() + 20);
                    }
                }
                break;
            case REMOVE:
                if (chunkDelays.containsKey(chunkString)) {
                    chunkDelays.remove(chunkString);
                    if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14_R1)) {
                        chunk.setForceLoaded(false);
                    }
                    dB.echoDebug(scriptEntry, "...allowing unloading of chunk " + chunk.getX() + ", " + chunk.getZ());
                }
                else {
                    dB.echoError("Chunk was not on the load list!");
                }
                break;
            case REMOVEALL:
                dB.echoDebug(scriptEntry, "...allowing unloading of all stored chunks");
                if (NMSHandler.getVersion().isAtLeast(NMSVersion.v1_14_R1)) {
                    for (String chunkStr : chunkDelays.keySet()) {
                        dChunk loopChunk = dChunk.valueOf(chunkStr);
                        loopChunk.getChunk().setForceLoaded(false);
                    }
                }
                chunkDelays.clear();
                break;
        }
    }

    // Map of chunks with delays
    Map<String, Long> chunkDelays = new HashMap<>();

    @EventHandler
    public void stopUnload(ChunkUnloadEvent e) {
        if (!(e instanceof Cancellable)) { // Not cancellable in 1.14
            return;
        }
        String chunkString = e.getChunk().getX() + ", " + e.getChunk().getZ();
        if (chunkDelays.containsKey(chunkString)) {
            if (chunkDelays.get(chunkString) == 0) {
                ((Cancellable) e).setCancelled(true);
            }
            else if (System.currentTimeMillis() < chunkDelays.get(chunkString)) {
                ((Cancellable) e).setCancelled(true);
            }
            else {
                chunkDelays.remove(chunkString);
            }
        }
    }

    public class ChunkLoadCommandNPCEvents implements Listener {
        @EventHandler
        public void stopDespawn(NPCDespawnEvent e) {
            if (e.getNPC() == null || !e.getNPC().isSpawned()) {
                return;
            }
            Chunk chnk = e.getNPC().getEntity().getLocation().getChunk();
            String chunkString = chnk.getX() + ", " + chnk.getZ();
            if (chunkDelays.containsKey(chunkString)) {
                if (chunkDelays.get(chunkString) == 0) {
                    e.setCancelled(true);
                }
                else if (System.currentTimeMillis() < chunkDelays.get(chunkString)) {
                    e.setCancelled(true);
                }
                else {
                    chunkDelays.remove(chunkString);
                }
            }
        }
    }
}
