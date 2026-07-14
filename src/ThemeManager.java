import java.util.prefs.Preferences;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Scene;

public class ThemeManager {
    private static Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    // Initialize with standard fallbacks instead of reading globally at class-load time
    public static StringProperty appThemeProperty = new SimpleStringProperty("Default (Pink)");
    public static StringProperty appFontProperty = new SimpleStringProperty("Arial");
    private static String currentImagePath = "default";
    private static String username = "guest";

    // --- CRITICAL ADDITION: Run this immediately after login succeeds! ---
    public static void loadUserPreferences(String currentUser, Scene scene) {
        if (currentUser == null || currentUser.trim().isEmpty()) return;

        // Update the current working username representation
        username = currentUser;

        // Load isolated properties using the username as a key prefix
        String savedTheme = prefs.get(username + "_theme", "Default (Pink)");
        String savedFont = prefs.get(username + "_font", "Arial");

        appThemeProperty.set(savedTheme);
        appFontProperty.set(savedFont);
        currentImagePath = prefs.get(username + "_imagePath", "default");

        // Immediately paint the window with the user's explicit preference context
        if (scene != null) {
            applyTheme(scene, savedTheme);
        }
    }

    public static void saveProfileData() {
        // Prevent saving if no valid user context exists
        if (username == null || username.equals("guest")) return;

        // Save into isolated user-prefixed spaces inside Java Preferences
        prefs.put(username + "_theme", appThemeProperty.get());
        prefs.put(username + "_font", appFontProperty.get());
        prefs.put(username + "_imagePath", currentImagePath);
        prefs.put(username + "_username", username);
    }


    public static String getUsername() {
        return username;
    }

    public static void setCurrentImagePath(String path) {
        currentImagePath = path;
        saveProfileData();
    }

    public static String getCurrentImagePath() {
        return currentImagePath;
    }

    public static void applyTheme(Scene scene, String themeName) {
        if (scene == null) return;
        String cssFile = "";
        switch (themeName) {
            case "Red":    cssFile = "Color/red-theme.css"; break;
            case "Purple": cssFile = "Color/purple-theme.css"; break;
            case "Black":  cssFile = "Color/black-theme.css"; break;
            case "Blue":   cssFile = "Color/blue-theme.css"; break;
            case "Yellow": cssFile = "Color/yellow-theme.css"; break;
            case "Green":  cssFile = "Color/green-theme.css"; break;
            default:       cssFile = "Color/style.css"; break;
        }
        try {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(ThemeManager.class.getResource(cssFile).toExternalForm());
            applyFont(scene, appFontProperty.get());
        } catch (Exception e) {
            System.out.println("تأكدي من وجود ملف الـ CSS: " + cssFile);
        }
    }

    public static void applyFont(Scene scene, String fontName) {
        if (scene != null && scene.getRoot() != null)
            scene.getRoot().setStyle("-fx-font-family: '" + fontName + "';");
    }
}