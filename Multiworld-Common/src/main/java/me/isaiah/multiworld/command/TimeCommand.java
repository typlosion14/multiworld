package me.isaiah.multiworld.command;

import java.util.HashMap;

import me.isaiah.multiworld.MultiworldMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class TimeCommand implements Command {

    /**
     * "/mw time <set|add|query> <time> [world id]"
     *
     * Sets/adds/queries the time of day of the world the player is currently in, or of the named
     * world if a world id is supplied. Mirrors {@link DifficultyCommand}.
     *
     * Unlike vanilla {@code /time} (which applies to every world at once), this only affects the
     * targeted dimension.
     */
    public static int run(MinecraftServer mc, ServerPlayerEntity plr, String[] args) {
        ServerWorld w = Command.getWorldFor(plr);

        if (args.length < 3) {
            MultiworldMod.message(plr, "&cUsage: /mw time <set|add|query> <time> [world id]");
            return 1;
        }

        String action = args[1];
        String value = args[2];

        // Optional world id (args[3]) — same resolution as DifficultyCommand.
        if (args.length >= 4) {
            String a3 = args[3];

            HashMap<String, ServerWorld> worlds = new HashMap<>();
            mc.getWorldRegistryKeys().forEach(r -> {
                ServerWorld world = mc.getWorld(r);
                worlds.put(r.getValue().toString(), world);
            });

            if (a3.indexOf(':') == -1) a3 = "multiworld:" + a3;

            if (worlds.containsKey(a3)) {
                w = worlds.get(a3);
            } else {
                MultiworldMod.message(plr, "&cWorld not found: " + a3);
                return 1;
            }
        }

        String id = w.getRegistryKey().getValue().toString();

        if (action.equalsIgnoreCase("query")) {
            long result;
            if (value.equalsIgnoreCase("daytime")) {
                result = w.getTimeOfDay() % 24000L;
            } else if (value.equalsIgnoreCase("gametime")) {
                result = w.getTime();
            } else if (value.equalsIgnoreCase("day")) {
                result = (w.getTimeOfDay() / 24000L) % 2147483647L;
            } else {
                MultiworldMod.message(plr, "&cInvalid query: " + value + " (daytime, gametime, day)");
                return 1;
            }
            MultiworldMod.message(plr, "[&cMultiworld&r]: Time (" + value + ") of world '" + id + "' is: " + result);
            return 1;
        }

        // set / add need a numeric (or named) tick value
        long ticks = parseTime(value);
        if (ticks < 0) {
            MultiworldMod.message(plr, "&cInvalid time: " + value + " (a number, or day/noon/night/midnight)");
            return 1;
        }

        if (action.equalsIgnoreCase("set")) {
            w.setTimeOfDay(ticks);
            MultiworldMod.message(plr, "[&cMultiworld&r]: Time of world '" + id + "' set to: " + ticks);
        } else if (action.equalsIgnoreCase("add")) {
            w.setTimeOfDay(w.getTimeOfDay() + ticks);
            MultiworldMod.message(plr, "[&cMultiworld&r]: Added " + ticks + " ticks to world '" + id + "' (now " + w.getTimeOfDay() + ")");
        } else {
            MultiworldMod.message(plr, "&cInvalid action: " + action + " (set, add, query)");
            return 1;
        }

        return 1;
    }

    /**
     * Parse a time value: a raw tick number, or one of the vanilla named times.
     * Returns -1 if the value cannot be parsed.
     */
    private static long parseTime(String value) {
        switch (value.toLowerCase()) {
            case "day":      return 1000L;
            case "noon":     return 6000L;
            case "night":    return 13000L;
            case "midnight": return 18000L;
            case "sunrise":  return 23000L;
            case "sunset":   return 12000L;
            default:
                try {
                    long t = Long.parseLong(value);
                    return t < 0 ? -1 : t;
                } catch (NumberFormatException e) {
                    return -1;
                }
        }
    }

}
