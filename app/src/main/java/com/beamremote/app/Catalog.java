package com.beamremote.app;

public final class Catalog {
    public static final String[] OPTIONS = {
        "Samsung TV · BN59-01180A family", "LG TV · NEC address 04 (candidate)",
        "LG AC · 6711A20083V / LG28", "LG AC · AKB75215403 / LG2",
        "Other TV · custom codes", "Other AC · custom scenes",
        "Whirlpool AC · DG11J1-3A / 04 · 18–32°C", "Whirlpool AC · DG11J1-91 · 16–30°C",
        "Voltas AC · 183V DZU2 · protocol candidate · 17–30°C"
    };
    public static Profile create(int choice, String name) {
        Profile p = new Profile(); p.name = name;
        p.kind = choice == 2 || choice == 3 || choice == 5 || choice == 8 ? "AC" : "TV";
        p.brand = choice == 0 ? "Samsung" : choice < 4 ? "LG" : choice == 8 ? "Voltas" : "Custom";
        p.model = choice == 0 ? "BN59-01180A family" : choice == 1 ? "NEC address 04 candidate" :
            choice == 2 ? "6711A20083V" : choice == 3 ? "AKB75215403" : "Unspecified";
        p.protocol = choice == 2 ? "LG28" : choice == 3 ? "LG2" : "RAW";
        if (choice == 6 || choice == 7) {
            p.kind = "AC"; p.brand = "Whirlpool";
            p.model = choice == 6 ? "DG11J1-3A / DG11J1-04" : "DG11J1-91";
            p.protocol = choice == 6 ? "WHIRLPOOL3A" : "WHIRLPOOL91";
        }
        if (choice == 8) {
            p.kind = "AC"; p.brand = "Voltas"; p.model = "183V DZU2 · protocol candidate"; p.protocol = "VOLTAS183V";
        }
        if (choice < 2) {
            String[] labels = {"Power", "Volume +", "Volume −", "Mute", "Channel +", "Channel −", "Input", "Up", "Down", "Left", "Right", "Back", "Menu"};
            int[] samsung = {2, 7, 11, 15, 18, 16, 1, 96, 97, 101, 98, 88, 26};
            int[] lg = {8, 2, 3, 9, 0, 1, 11, 64, 65, 7, 6, 40, 67};
            for (int i = 0; i < labels.length; i++) p.commands.put(labels[i], choice == 0 ? Encoders.samsung(samsung[i]) : Encoders.nec(4, lg[i]));
            // Samsung ENTER is not included until verified for this specific family.
            if (choice == 1) p.commands.put("OK", Encoders.nec(4, 68));
        }
        return p;
    }
}
