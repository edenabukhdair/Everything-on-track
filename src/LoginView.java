import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class LoginView {

    private VBox layout;
    private TextField usernameField;
    private PasswordField passwordField;
    private Button actionButton;
    private Label statusLabel;

    private Map<String, String> userDatabase = new HashMap<>();
    private final String USERS_FILE = "app_users.txt";

    public static String currentUser = null;

    public LoginView(Runnable onLoginSuccess) {
        loadUsersFromFile();

        layout = new VBox(15);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #F4F7F6;");

        Label titleLabel = new Label("Welcome to Everything on Track ✨");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label creatorLabel = new Label("Designed by Eden");
        creatorLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic; -fx-text-fill: #16A085;");

        Label infoLabel = new Label("Sign In, or enter a new username to Sign Up.");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D; -fx-text-alignment: center;");

        Label usernameLabel = new Label("Username");
        usernameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setStyle("-fx-background-radius: 5; -fx-padding: 8;");

        Label passwordLabel = new Label("Password");
        passwordLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-background-radius: 5; -fx-padding: 8;");

        actionButton = new Button("Sign In / Sign Up 🚀");
        actionButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        actionButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please fill in all fields!");
                statusLabel.setTextFill(Color.RED);
                return;
            }

            if (userDatabase.containsKey(username)) {
                // --- LOGIN FLOW ---
                if (userDatabase.get(username).equals(password)) {
                    currentUser = username;
                    onLoginSuccess.run();
                } else {
                    statusLabel.setText("Incorrect password!");
                    statusLabel.setTextFill(Color.RED);
                }
            } else {
                // --- SIGN UP FLOW ---
                if (!isValidPassword(password)) {
                    // Changed this line to show the specific requirements!
                    statusLabel.setText("Password must be 8+ chars, 1 uppercase, and 1 number!");
                    statusLabel.setTextFill(Color.RED);
                    return;
                }

                userDatabase.put(username, password);
                saveUserToFile(username, password);
                currentUser = username;
                onLoginSuccess.run();
            }
        });

        layout.getChildren().addAll(
                titleLabel,
                creatorLabel,
                infoLabel,
                usernameLabel,
                usernameField,
                passwordLabel,
                passwordField,
                actionButton,
                statusLabel
        );
    }

    public VBox getView() {
        return layout;
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Z])(?=.*\\d).{8,}$");
    }

    private void saveUserToFile(String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(username + "|" + password);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Error saving user: " + e.getMessage());
        }
    }

    private void loadUsersFromFile() {
        File file = new File(USERS_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2) userDatabase.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }
}