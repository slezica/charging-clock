package io.salezica.chclock.ambient;

public class StatusItem {

    public enum Tone { OK, WARN, NEUTRAL }

    public final String label;
    public final String value;
    public final Tone tone;
    public final String hint;   // instructions shown when something needs attention
    public final Runnable fix;  // optional action that takes the user to a fix

    public StatusItem(String label, String value, Tone tone, String hint, Runnable fix) {
        this.label = label;
        this.value = value;
        this.tone = tone;
        this.hint = hint;
        this.fix = fix;
    }

    public static StatusItem ok(String label, String value) {
        return new StatusItem(label, value, Tone.OK, null, null);
    }

    public static StatusItem neutral(String label, String value) {
        return new StatusItem(label, value, Tone.NEUTRAL, null, null);
    }

    public static StatusItem warn(String label, String value, String hint, Runnable fix) {
        return new StatusItem(label, value, Tone.WARN, hint, fix);
    }
}
