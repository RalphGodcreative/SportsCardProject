package RGcards.SportsCardProject.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * Gemini text models the admin can pick, limited to those with free-tier quota.
 * Limits are the free-tier figures from the AI Studio rate-limits page (Nov 2026).
 * Only the 2.5 models get free Google Search grounding; for the rest the search
 * tool is left off the request, since a grounded call would be refused.
 */
public enum GeminiModel {

    GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash", 5, 20, true),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite", 10, 20, true),
    GEMINI_3_5_FLASH_LITE("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite", 15, 500, false),
    GEMINI_3_1_FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite", 15, 500, false),
    GEMINI_3_8_FLASH("gemini-3.8-flash", "Gemini 3.8 Flash", 5, 20, false),
    GEMINI_3_6_FLASH("gemini-3.6-flash", "Gemini 3.6 Flash", 5, 20, false),
    GEMINI_3_FLASH("gemini-3-flash-preview", "Gemini 3 Flash Preview", 5, 20, false),
    GEMINI_4_HYDROGEN("gemini-4-hydrogen", "Gemini 4 Hydrogen", 5, 20, false);

    public static final GeminiModel DEFAULT = GEMINI_2_5_FLASH;

    private final String id;
    private final String label;
    private final int freeRpm;
    private final int freeRpd;
    private final boolean freeSearchGrounding;

    GeminiModel(String id, String label, int freeRpm, int freeRpd, boolean freeSearchGrounding) {
        this.id = id;
        this.label = label;
        this.freeRpm = freeRpm;
        this.freeRpd = freeRpd;
        this.freeSearchGrounding = freeSearchGrounding;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getFreeRpm() {
        return freeRpm;
    }

    public int getFreeRpd() {
        return freeRpd;
    }

    public boolean isFreeSearchGrounding() {
        return freeSearchGrounding;
    }

    public static Optional<GeminiModel> fromId(String id) {
        return Arrays.stream(values()).filter(m -> m.id.equals(id)).findFirst();
    }
}
