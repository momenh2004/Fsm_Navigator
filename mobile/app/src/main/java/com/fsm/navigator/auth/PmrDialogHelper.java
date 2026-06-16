package com.fsm.navigator.auth;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import com.fsm.navigator.model.PointInteret;

// Affiche les dialogs de sélection de profil PMR et gère l'accessibilité.
public class PmrDialogHelper {

    private static final String ADMIN_EMAIL = "administration@fsm.rnu.tn";

    // Montre le dialog de sélection du profil PMR (appelle onComplete après).
    public static void showProfileDialog(Context context, Runnable onComplete) {
        String[] options = {
            "♿  Fauteuil roulant",
            "👁  Malvoyant / non-voyant",
            "🦴  Béquilles / mobilité réduite",
            "🦻  Malentendant / sourd"
        };
        PmrManager.PmrProfile[] profiles = {
            PmrManager.PmrProfile.WHEELCHAIR,
            PmrManager.PmrProfile.VISUALLY_IMPAIRED,
            PmrManager.PmrProfile.CRUTCHES,
            PmrManager.PmrProfile.HEARING_IMPAIRED
        };

        new AlertDialog.Builder(context)
            .setTitle("Votre profil d'accessibilité")
            .setItems(options, (d, which) -> {
                PmrManager.setProfile(profiles[which]);
                String label = options[which].replaceAll("[^\\p{L}\\s/]", "").trim();
                TtsManager.speakForce("Profil " + label + " activé. Navigation adaptée.");
                if (profiles[which] == PmrManager.PmrProfile.VISUALLY_IMPAIRED) {
                    showTalkBackSuggestion(context, onComplete);
                } else {
                    if (onComplete != null) onComplete.run();
                }
            })
            .setNegativeButton("Annuler", (d, w) -> {
                PmrManager.reset();
                if (onComplete != null) onComplete.run();
            })
            .setCancelable(false)
            .show();
    }

    // Suggère d'activer TalkBack pour les utilisateurs malvoyants.
    public static void showTalkBackSuggestion(Context context, Runnable onComplete) {
        TtsManager.speakForce(
            "Conseil accessibilité. Pour une meilleure expérience, "
          + "activez TalkBack dans les paramètres d'accessibilité de votre téléphone.");

        new AlertDialog.Builder(context)
            .setTitle("👁 Conseil accessibilité")
            .setMessage(
                "Pour une expérience optimale en mode malvoyant, "
              + "activez TalkBack sur votre téléphone.\n\n"
              + "TalkBack lira automatiquement tout le contenu "
              + "de l'écran à votre place.\n\n"
              + "Voulez-vous ouvrir les paramètres maintenant ?")
            .setPositiveButton("Ouvrir les paramètres", (d, w) -> {
                context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                if (onComplete != null) onComplete.run();
            })
            .setNegativeButton("Non merci", (d, w) -> {
                if (onComplete != null) onComplete.run();
            })
            .setCancelable(false)
            .show();
    }

    // Vérifie l'accessibilité PMR du POI et affiche le dialog si inaccessible (retourne true si bloqué).
    public static boolean checkAndShow(Context context, PointInteret poi,
                                       Runnable onProceed) {
        if (!PmrManager.isEnabled()) return false;
        if (!PmrManager.avoidsStairs()) return false;
        if (poi.isAccessiblePmr()) return false;

        show(context, poi.getNom(), onProceed);
        return true;
    }

    // Surcharge : vérifie l'accessibilité PMR avec le nom de salle et le flag booléen.
    public static boolean checkAndShow(Context context, String salleNom,
                                       boolean accessiblePmr, Runnable onProceed) {
        if (!PmrManager.isEnabled()) return false;
        if (!PmrManager.avoidsStairs()) return false;
        if (accessiblePmr) return false;

        show(context, salleNom, onProceed);
        return true;
    }

    // Affiche l'alerte "salle non accessible" avec option de contact admin.
    private static void show(Context context, String salleNom, Runnable onProceed) {
        String message =
            "Cette salle n'est pas accessible aux personnes à mobilité réduite.\n\n" +
            "Veuillez contacter l'administration FSM pour une assistance.";

        TtsManager.speakForce(
            "Attention. " + salleNom + " n'est pas accessible "
          + "aux personnes à mobilité réduite. "
          + "Veuillez contacter l'administration.");

        new AlertDialog.Builder(context)
            .setTitle("♿ Salle non accessible")
            .setMessage(message)
            .setPositiveButton("Contacter →", (d, w) -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + ADMIN_EMAIL));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Assistance PMR — " + salleNom);
                context.startActivity(
                    Intent.createChooser(intent, "Contacter l'administration"));
            })
            .setNeutralButton("Continuer quand même", (d, w) -> {
                if (onProceed != null) onProceed.run();
            })
            .setNegativeButton("Annuler", null)
            .setCancelable(false)
            .show();
    }
}
