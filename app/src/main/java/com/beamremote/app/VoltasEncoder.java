package com.beamremote.app;

/** Independent encoder for the published Voltas 183V DZU2 48-bit IR protocol candidate. */
public final class VoltasEncoder {
    private static final int COOL = 0, DRY = 1, FAN_ONLY = 2, HEAT = 4;
    private VoltasEncoder() {}

    /** mode uses the shared profile values: 0 cool, 1 dry, 2 fan, 4 heat. */
    public static Signal signal(boolean powerOn, int temperature, int mode, int fan) {
        if (!powerOn) return bytes(0xB2, 0x4D, 0x7B, 0x84, 0xE0, 0x1F);
        if (temperature < 17 || temperature > 30)
            throw new IllegalArgumentException("Voltas temperature must be 17–30°C.");
        int temp = temperatureNibble(temperature);
        int fanByte = fanByte(fan);
        int command, data;
        switch (mode) {
            case COOL: command = fanByte; data = (temp << 4); break;
            case DRY: command = 0x1F; data = (temp << 4) | 0x04; break; // Dry forces Auto fan.
            case FAN_ONLY: command = fanByte; data = 0xE4; break;
            case HEAT: command = fanByte; data = (temp << 4) | 0x0C; break;
            default: throw new IllegalArgumentException("Unsupported Voltas mode.");
        }
        return standard(command, data, -1);
    }

    public static Signal swing(boolean on) {
        return standard(on ? 0x6B : 0x0F, 0xE0, 0x1F);
    }

    public static Signal sleep(boolean on, int temperature) {
        if (on) return standard(0xE0, 0x03, 0xFC);
        int data = (temperatureNibble(temperature) << 4) | 0x01;
        return standard(0xAB, data, 0xFF);
    }

    /** The remote protocol defines Turbo as a toggle command. */
    public static Signal turboToggle() { return special(0xA2, 0x5D); }

    public static Signal eco(boolean on) { return special(on ? 0x82 : 0x83, 0x90); }

    /** The remote protocol defines display control as a toggle command. */
    public static Signal displayToggle() { return special(0xA5, 0x2D); }

    private static Signal standard(int command, int data, int checksumData) {
        int b3 = command ^ 0xFF;
        int b5 = checksumData < 0 ? data ^ 0xFF : checksumData;
        return bytes(0xB2, 0x4D, command, b3, data, b5);
    }

    private static Signal special(int data, int checksum) {
        return bytes(0xB5, 0x4A, 0xF5, 0x0A, data, checksum);
    }

    private static Signal bytes(int b0, int b1, int b2, int b3, int b4, int b5) {
        int[] pattern = new int[199];
        int index = 0;
        int[] frame = {b0, b1, b2, b3, b4, b5};
        for (int repeat = 0; repeat < 2; repeat++) {
            pattern[index++] = 4450; // header mark
            pattern[index++] = 4450; // header space
            for (int value : frame) {
                for (int bit = 0; bit < 8; bit++) {
                    pattern[index++] = 530;
                    pattern[index++] = ((value >>> bit) & 1) != 0 ? 1650 : 530;
                }
            }
            pattern[index++] = 530; // trailing mark
            if (repeat == 0) pattern[index++] = 5200; // inter-frame space
        }
        return new Signal(38000, pattern);
    }

    private static int fanByte(int fan) {
        switch (fan) {
            case 4: return 0x3F; // High
            case 2: return 0x5F; // Medium
            case 1: return 0x9F; // Low
            case 5: return 0xBF; // Auto
            default: throw new IllegalArgumentException("Unsupported Voltas fan speed.");
        }
    }

    // Voltas' table is Gray-coded, with a documented deviation at 27–28°C.
    private static int temperatureNibble(int temperature) {
        switch (temperature) {
            case 17: return 0x0; case 18: return 0x1; case 19: return 0x3; case 20: return 0x2;
            case 21: return 0x6; case 22: return 0x7; case 23: return 0x5; case 24: return 0x4;
            case 25: return 0xC; case 26: return 0xD; case 27: return 0x9; case 28: return 0x8;
            case 29: return 0xA; case 30: return 0xB;
            default: throw new IllegalArgumentException("Voltas temperature must be 17–30°C.");
        }
    }
}
