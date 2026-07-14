import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiaryView {

    private TextField monthGoalField;
    private TextField weekGoalField;
    private VBox monthGoalBox;
    private VBox weekGoalBox;
    private VBox viewContainer;
    private TextArea diaryTextArea;
    private ComboBox<Integer> ratingComboBox;

    private HBox imagesHorizontalBox;
    private ScrollPane imagesScrollPane;
    private Button uploadPhotoButton;
    private List<String> currentImagePathsList = new ArrayList<>();

    private LocalDate selectedDate = LocalDate.now();
    private DatePicker datePicker;

    private Button prevButton;
    private Button nextButton;

    private Map<LocalDate, DiaryEntry> diaryStorage = new HashMap<>();
    private boolean isLoading = false;

    private String getUserDir() {
        String user = (LoginView.currentUser != null ? LoginView.currentUser : "guest");
        return "src/Packables/" + user + "/";
    }

    public DiaryView() {
        buildView();
    }

    private void buildView() {
        viewContainer = new VBox(15);
        viewContainer.setStyle("-fx-padding: 20;");
        HBox.setHgrow(viewContainer, Priority.ALWAYS);

        monthGoalField = new TextField();
        monthGoalField.setPromptText("Enter this month goal...");
        monthGoalBox = new VBox(5, new Label("My Month Goal:"), monthGoalField);
        monthGoalField.focusedProperty().addListener((obs, oldVal, hasFocus) -> {
            if (!hasFocus) saveGoalsToFile();
        });

        weekGoalField = new TextField();
        weekGoalField.setPromptText("Enter this week goal...");
        weekGoalBox = new VBox(5, new Label("My Week Goal:"), weekGoalField);
        weekGoalField.focusedProperty().addListener((obs, oldVal, hasFocus) -> {
            if (!hasFocus) saveGoalsToFile();
        });

        datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("-fx-border-radius: 5;");
        datePicker.setOnAction(e -> {
            saveDiaryToFile();
            selectedDate = datePicker.getValue();
            loadEntryForDate();
            updateGoalVisibility();
        });

        Label titleLabel = new Label("My Diary Entry");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        diaryTextArea = new TextArea();
        diaryTextArea.setPromptText("How was your day? Write it down here...");
        diaryTextArea.setStyle("-fx-border-radius: 5;");
        diaryTextArea.setPrefHeight(180);


        // Save instantly to RAM memory safely
        diaryTextArea.textProperty().addListener((obs, oldVal, newVal) -> {
            // 1. If the app is loading a file, don't do anything
            if (isLoading) return;

            // 2. Safely get the rating. If the ComboBox isn't ready yet, default to 5 instead of crashing
            int currentRating = 5;
            if (ratingComboBox != null && ratingComboBox.getValue() != null) {
                currentRating = ratingComboBox.getValue();
            }

            // 3. Update RAM safely
            DiaryEntry entry = new DiaryEntry(selectedDate, newVal, currentRating, new ArrayList<>(currentImagePathsList));
            diaryStorage.put(selectedDate, entry);
        });

// Save to file on your hard drive only when user switches fields/focus away
        diaryTextArea.focusedProperty().addListener((obs, oldVal, hasFocus) -> {
            if (!hasFocus) {
                saveDiaryToFile();
            }
        });

        prevButton = new Button("< Previous Day");
        prevButton.setStyle("-fx-font-weight: bold; -fx-background-radius: 10;");
        prevButton.setOnAction(e -> {
            saveDiaryToFile();
            selectedDate = selectedDate.minusDays(1);
            datePicker.setValue(selectedDate);
            loadEntryForDate();
            updateGoalVisibility();
        });

        nextButton = new Button("Next Day >");
        nextButton.setStyle("-fx-font-weight: bold; -fx-background-radius: 10;");
        nextButton.setOnAction(e -> {
            saveDiaryToFile();
            selectedDate = selectedDate.plusDays(1);
            datePicker.setValue(selectedDate);
            loadEntryForDate();
            updateGoalVisibility();
        });

        HBox navigationBox = new HBox(20, prevButton, nextButton);
        navigationBox.setAlignment(Pos.CENTER);

        imagesHorizontalBox = new HBox(10);
        imagesHorizontalBox.setAlignment(Pos.CENTER_LEFT);
        imagesScrollPane = new ScrollPane(imagesHorizontalBox);
        imagesScrollPane.setFitToHeight(true);
        imagesScrollPane.setPrefHeight(135);
        imagesScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-vbar-policy: never;");

        uploadPhotoButton = new Button("📸 Add Photo");
        uploadPhotoButton.setStyle("-fx-font-weight: bold; -fx-background-radius: 10;");
        uploadPhotoButton.setOnAction(e -> {
            if (currentImagePathsList.size() >= 10) {
                new Alert(Alert.AlertType.WARNING, "Max 10 photos!", ButtonType.OK).showAndWait();
                return;
            }
            FileChooser fileChooser = new FileChooser();
            File selectedFile = fileChooser.showOpenDialog(new Stage());
            if (selectedFile != null) {
                currentImagePathsList.add(selectedFile.toURI().toString());
                renderPhotosView();

                DiaryEntry entry = new DiaryEntry(selectedDate, diaryTextArea.getText(), ratingComboBox.getValue(), new ArrayList<>(currentImagePathsList));
                diaryStorage.put(selectedDate, entry);
                saveDiaryToFile();
            }
        });

        HBox photoLayoutContainer = new HBox(15, uploadPhotoButton, imagesScrollPane);
        photoLayoutContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imagesScrollPane, Priority.ALWAYS);

        Label ratingLabel = new Label("Rate your day (1-5):");
        ratingLabel.setStyle("-fx-font-weight: bold;");
        ratingComboBox = new ComboBox<>();
        ratingComboBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingComboBox.setValue(5);
        ratingComboBox.setStyle("-fx-border-radius: 5;");

        ratingComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!isLoading && newVal != null) {
                DiaryEntry entry = new DiaryEntry(selectedDate, diaryTextArea.getText(), newVal, new ArrayList<>(currentImagePathsList));
                diaryStorage.put(selectedDate, entry);
                saveDiaryToFile();
            }
        });

        HBox ratingBox = new HBox(10, ratingLabel, ratingComboBox);
        ratingBox.setAlignment(Pos.CENTER_LEFT);

        viewContainer.getChildren().addAll(datePicker, titleLabel, diaryTextArea, navigationBox, photoLayoutContainer, ratingBox);
        updateGoalVisibility();
    }

    private void updateGoalVisibility() {
        viewContainer.getChildren().removeAll(monthGoalBox, weekGoalBox);
        if (selectedDate.getDayOfMonth() == 1) viewContainer.getChildren().add(1, monthGoalBox);

        if (selectedDate.getDayOfWeek().getValue() == 7) {
            int idx = viewContainer.getChildren().contains(monthGoalBox) ? 2 : 1;
            viewContainer.getChildren().add(idx, weekGoalBox);
        }
    }

    private void saveGoalsToFile() {
        if (isLoading || LoginView.currentUser == null) return;
        File file = new File(getUserDir() + "goals.txt");
        file.getParentFile().mkdirs();
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println(monthGoalField.getText());
            out.println(weekGoalField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadGoalsFromFile() {
        if (LoginView.currentUser == null) return;
        File file = new File(getUserDir() + "goals.txt");
        isLoading = true;
        try {
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    monthGoalField.setText(reader.readLine());
                    weekGoalField.setText(reader.readLine());
                }
            } else {
                monthGoalField.clear();
                weekGoalField.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            isLoading = false;
        }
    }

    private void renderPhotosView() {
        imagesHorizontalBox.getChildren().clear();
        for (int i = 0; i < currentImagePathsList.size(); i++) {
            String path = currentImagePathsList.get(i);
            ImageView imgView = new ImageView(new Image(path));
            imgView.setFitWidth(100);
            imgView.setFitHeight(100);
            imgView.setPreserveRatio(true);

            Button xBtn = new Button("×");
            xBtn.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-background-radius: 10; -fx-padding: 1 5;");

            final int index = i;
            xBtn.setOnAction(e -> {
                currentImagePathsList.remove(index);
                renderPhotosView();

                DiaryEntry entry = new DiaryEntry(selectedDate, diaryTextArea.getText(), ratingComboBox.getValue(), new ArrayList<>(currentImagePathsList));
                diaryStorage.put(selectedDate, entry);
                saveDiaryToFile();
            });

            StackPane singleImageContainer = new StackPane(imgView, xBtn);
            StackPane.setAlignment(xBtn, Pos.TOP_RIGHT);
            singleImageContainer.setStyle("-fx-border-radius: 5; -fx-padding: 2; -fx-background-color: white;");
            imagesHorizontalBox.getChildren().add(singleImageContainer);
        }
        uploadPhotoButton.setDisable(currentImagePathsList.size() >= 10);
    }

    private void loadEntryForDate() {
        isLoading = true;
        try {
            if (diaryStorage.containsKey(selectedDate)) {
                DiaryEntry entry = diaryStorage.get(selectedDate);
                diaryTextArea.setText(entry.getText());
                ratingComboBox.setValue(entry.getRating());
                currentImagePathsList = new ArrayList<>(entry.getImagePaths());
                renderPhotosView();
            } else {
                clearFields();
            }
        } finally {
            isLoading = false;
        }
    }

    private void clearFields() {
        diaryTextArea.clear();
        ratingComboBox.setValue(5);
        currentImagePathsList.clear();
        renderPhotosView();
    }

    public void refreshView() {
        loadDiaryFromFile();
        loadGoalsFromFile();
        loadEntryForDate();
        updateGoalVisibility();
    }

    public VBox getView() {
        return viewContainer;
    }

    public void saveDiaryToFile() {
        if (LoginView.currentUser == null || isLoading) return;
        File file = new File(getUserDir() + "diary.txt");
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<LocalDate, DiaryEntry> entry : diaryStorage.entrySet()) {
                String joinedPaths = String.join(",", entry.getValue().getImagePaths());
                String safeText = entry.getValue().getText().replace("\n", "[NEWLINE]");
                writer.write(entry.getKey() + "|" + safeText + "|" + entry.getValue().getRating() + "|" + (joinedPaths.isEmpty() ? "null" : joinedPaths));
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving diary: " + e.getMessage());
        }
    }

    public void loadDiaryFromFile() {
        if (LoginView.currentUser == null) return;
        File file = new File(getUserDir() + "diary.txt");
        diaryStorage.clear();
        isLoading = true;
        clearFields();

        if (!file.exists()) {
            isLoading = false;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;

                List<String> paths = new ArrayList<>();
                if (!parts[3].equals("null")) {
                    for (String p : parts[3].split(",")) paths.add(p);
                }

                LocalDate parsedDate = LocalDate.parse(parts[0]);
                String parsedText = parts[1].replace("[NEWLINE]", "\n");
                int parsedRating = Integer.parseInt(parts[2]);

                diaryStorage.put(parsedDate, new DiaryEntry(parsedDate, parsedText, parsedRating, paths));
            }
        } catch (Exception e) {
            System.out.println("Error loading diary: " + e.getMessage());
        } finally {
            isLoading = false;
        }
    }
}