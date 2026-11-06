package com.beamremote.app;

import org.json.*;
import java.util.*;

public final class Profile {
    public String id = UUID.randomUUID().toString();
    public String name, brand, model, kind, protocol;
    public int temperature = 24, mode = 0, fan = 5, acCommand = 2;
    public boolean swing, sleep, turbo, eco;
    public String libraryId = "", libraryEntry = "", libraryModeCode = "C", libraryFanCode = "1";
    public final LinkedHashMap<String, Signal> commands = new LinkedHashMap<>();
    public boolean isAc() { return "AC".equals(kind); }
    public boolean isLibrary() { return "LIBRARY".equals(protocol); }
    public boolean isWhirlpool() { return "WHIRLPOOL3A".equals(protocol) || "WHIRLPOOL91".equals(protocol); }
    public boolean isVoltas() { return "VOLTAS183V".equals(protocol); }
    public boolean hasAcEncoder() { return "LG28".equals(protocol) || "LG2".equals(protocol) || isWhirlpool() || isVoltas() || isLibrary(); }
    public int minTemperature() { return isVoltas() ? 17 : "WHIRLPOOL3A".equals(protocol) ? 18 : 16; }
    public int maxTemperature() { return isVoltas() ? 30 : "WHIRLPOOL3A".equals(protocol) ? 32 : 30; }
    public Signal acSignal(boolean powerAction) {
        if (isLibrary()) throw new IllegalArgumentException("Library remotes use their imported command dictionary.");
        if (isVoltas()) return VoltasEncoder.signal(powerAction, temperature, mode, fan);
        if (isWhirlpool()) return WhirlpoolEncoder.signal("WHIRLPOOL91".equals(protocol), powerAction, temperature, mode, fan, acCommand, swing, sleep, turbo);
        return Encoders.lg("LG2".equals(protocol), powerAction, temperature, mode, fan);
    }
    public JSONObject json() throws JSONException {
        JSONObject root = new JSONObject().put("schemaVersion", 1).put("id", id)
            .put("name", name).put("brand", brand).put("model", model).put("kind", kind)
            .put("protocol", protocol).put("temperature", temperature).put("mode", mode).put("fan", fan).put("acCommand", acCommand)
            .put("swing", swing).put("sleep", sleep).put("turbo", turbo).put("eco", eco);
        if (isLibrary()) root.put("libraryId", libraryId).put("libraryEntry", libraryEntry)
            .put("libraryModeCode", libraryModeCode).put("libraryFanCode", libraryFanCode);
        JSONObject codes = new JSONObject();
        for (Map.Entry<String, Signal> entry : commands.entrySet()) {
            JSONArray pattern = new JSONArray();
            for (int value : entry.getValue().pattern()) pattern.put(value);
            codes.put(entry.getKey(), new JSONObject().put("frequency", entry.getValue().frequency).put("pattern", pattern));
        }
        return root.put("commands", codes);
    }
    public static Profile parse(JSONObject root) throws JSONException {
        if (integer(root.get("schemaVersion")) != 1) throw new IllegalArgumentException("Unsupported profile version.");
        Profile p = new Profile();
        p.name = text(root, "name", 60); p.brand = text(root, "brand", 60);
        p.model = text(root, "model", 100); p.kind = text(root, "kind", 8);
        p.protocol = text(root, "protocol", 16);
        if (!(p.kind.equals("TV") || p.kind.equals("AC"))) throw new IllegalArgumentException("Device type must be TV or AC.");
        if (!(p.protocol.equals("RAW") || p.protocol.equals("LG28") || p.protocol.equals("LG2") || p.isWhirlpool() || p.isVoltas() || p.isLibrary()))
            throw new IllegalArgumentException("Unknown profile protocol.");
        if (p.hasAcEncoder() && !p.isAc()) throw new IllegalArgumentException("AC encoders require an AC profile.");
        p.id = root.has("id") ? text(root, "id", 80) : p.id;
        p.temperature = root.has("temperature") ? integer(root.get("temperature")) : 24;
        p.mode = root.has("mode") ? integer(root.get("mode")) : 0;
        p.fan = root.has("fan") ? integer(root.get("fan")) : 5;
        p.acCommand = root.has("acCommand") ? integer(root.get("acCommand")) : 2;
        p.swing = bool(root, "swing"); p.sleep = bool(root, "sleep"); p.turbo = bool(root, "turbo"); p.eco = bool(root, "eco");
        if (p.isLibrary()) {
            p.libraryId = text(root, "libraryId", 64); p.libraryEntry = text(root, "libraryEntry", 400);
            p.libraryModeCode = text(root, "libraryModeCode", 2); p.libraryFanCode = text(root, "libraryFanCode", 2);
            if (!p.libraryId.matches("[a-f0-9]{64}") || !p.libraryEntry.startsWith("profiles/") ||
                p.libraryEntry.contains("..") || p.libraryEntry.contains("\\") || !p.libraryEntry.endsWith(".json") ||
                !p.libraryModeCode.matches("C|H|W|D|A|AI|F") || !p.libraryFanCode.matches("[0-9]|A") ||
                p.temperature < 0 || p.temperature > 100)
                throw new IllegalArgumentException("Invalid saved library remote.");
        }
        else if (p.isVoltas()) VoltasEncoder.signal(true, p.temperature, p.mode, p.fan);
        else if (p.isWhirlpool()) p.acSignal(false);
        else Encoders.lgCode(true, p.temperature, p.mode, p.fan);
        JSONObject codes = root.getJSONObject("commands");
        if (codes.length() > 64) throw new IllegalArgumentException("A profile can contain at most 64 commands.");
        Iterator<String> keys = codes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.trim().isEmpty() || key.length() > 60) throw new IllegalArgumentException("Command names must contain 1–60 characters.");
            JSONObject code = codes.getJSONObject(key);
            JSONArray values = code.getJSONArray("pattern");
            if (values.length() > 4096) throw new IllegalArgumentException("Signal has too many durations.");
            int[] pattern = new int[values.length()];
            for (int i = 0; i < values.length(); i++) pattern[i] = integer(values.get(i));
            p.commands.put(key, new Signal(integer(code.get("frequency")), pattern));
        }
        return p;
    }
    public void whirlpoolMode(int next) {
        mode = next; turbo = false; acCommand = next == 3 ? 23 : 6;
        if (next == 3) { temperature = 23; fan = 5; sleep = false; }
    }
    public void whirlpoolSleep(boolean on) {
        sleep = on; turbo = false; acCommand = 3;
        if (on) { if (mode == 3) mode = 0; fan = 1; }
    }
    public void whirlpoolTurbo(boolean on) {
        turbo = on; acCommand = 4;
        if (on) {
            sleep = false;
            if (mode != 4) mode = 0;
            fan = 4; temperature = mode == 4 ? maxTemperature() : minTemperature();
        }
    }
    private static boolean bool(JSONObject root, String key) throws JSONException {
        if (!root.has(key)) return false;
        Object value = root.get(key);
        if (!(value instanceof Boolean)) throw new IllegalArgumentException(key + " must be true or false.");
        return (Boolean) value;
    }
    private static String text(JSONObject object, String key, int limit) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty() || ((String) value).length() > limit)
            throw new IllegalArgumentException(key + " must contain 1–" + limit + " characters.");
        return ((String) value).trim();
    }
    private static int integer(Object value) {
        if (!(value instanceof Number) || ((Number) value).doubleValue() != ((Number) value).intValue())
            throw new IllegalArgumentException("Expected a whole number within the 32-bit range.");
        return ((Number) value).intValue();
    }
}
