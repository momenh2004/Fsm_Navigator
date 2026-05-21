package com.fsm.navigator.auth;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * TtsManager — Singleton Text-to-Speech en français.
 * Actif uniquement si le profil PMR courant autorise le TTS.
 */
public class TtsManager {

    private static TextToSpeech tts;
    private static boolean      ready = false;

    public static void init(Context context) {
        if (tts != null) return;
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.FRENCH);
                ready = (result != TextToSpeech.LANG_MISSING_DATA
                      && result != TextToSpeech.LANG_NOT_SUPPORTED);
            }
        });
    }

    /** Parle immédiatement (interrompt ce qui est en cours). */
    public static void speak(String text) {
        if (!ready || !PmrManager.ttsEnabled()) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fsm_tts");
    }

    /** Ajoute à la file sans interrompre. */
    public static void speakQueue(String text) {
        if (!ready || !PmrManager.ttsEnabled()) return;
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, "fsm_tts");
    }

    /** Parle même si le profil n'autorise pas le TTS (ex: dialog système). */
    public static void speakForce(String text) {
        if (!ready) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "fsm_tts_force");
    }

    public static void stop() {
        if (tts != null) tts.stop();
    }

    public static void release() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts   = null;
            ready = false;
        }
    }

    public static boolean isReady() { return ready; }
}
