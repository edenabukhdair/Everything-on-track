import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TimelineView {

    private TaskList taskList;
   // private GridPane timelineGrid;
    private LocalDate selectedDate = LocalDate.now();
    private VBox viewContainer;
    private Pane tasksLayer;

    private static final double ROW_HEIGHT = 55.0;
    private static final double HOUR_LABEL_WIDTH = 75.0;
    private static final double TASK_WIDTH = 190.0;
    private static final double TASK_GAP = 8.0;

    public TimelineView(TaskList taskList) {
        this.taskList = taskList;
        buildView();
        ReminderManager.startReminderCheck(this.taskList);

        // Update the view if the theme changes (CSS handles the colors, this just redraws)
        if (ThemeManager.appThemeProperty != null) {
            ThemeManager.appThemeProperty.addListener((obs, oldVal, newVal) -> Platform.runLater(this::populateTimelineUI));
        }
        if (ThemeManager.appFontProperty != null) {
            ThemeManager.appFontProperty.addListener((obs, oldVal, newVal) -> Platform.runLater(this::populateTimelineUI));
        }
    }

    public void refreshView() {
        if (taskList != null && taskList.getTasks() != null) {
            taskList.getTasks().clear();
        }
        loadTasksFromFile();
        populateTimelineUI();
    }

    private void buildView() {
        viewContainer = new VBox(15);
        HBox.setHgrow(viewContainer, Priority.ALWAYS);
        VBox.setVgrow(viewContainer, Priority.ALWAYS);
        populateTimelineUI();
    }

    private void populateTimelineUI() {
        viewContainer.getChildren().clear();
        viewContainer.setStyle("-fx-padding: 20;"); // Stripped background color lock

        DatePicker datePicker = new DatePicker(selectedDate);
        datePicker.setOnAction(e -> {
            selectedDate = datePicker.getValue();
            refreshTimeline();
        });

        Pane container = new Pane();
        container.setPrefHeight(24 * ROW_HEIGHT);
        container.setMinWidth(1200);
        // Stripped background color lock so CSS takes over

        for (int hour = 0; hour < 24; hour++) {
            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, hour * ROW_HEIGHT, 5000, hour * ROW_HEIGHT);
            line.setStroke(Color.web("#555555")); // Set grid lines to a neutral grey
            container.getChildren().add(line);

            Label hourLabel = new Label(String.format("%02d:00", hour));
            hourLabel.setLayoutX(8);
            hourLabel.setLayoutY(hour * ROW_HEIGHT + 4);
            hourLabel.setStyle("-fx-font-weight: bold;"); // Stripped font and color lock
            container.getChildren().add(hourLabel);
        }

        tasksLayer = new Pane();
        tasksLayer.setPrefHeight(24 * ROW_HEIGHT);
        container.getChildren().add(tasksLayer);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefSize(800, 600);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setPannable(true);
        scrollPane.setContent(container);
        // Stripped the massive inline CSS block here. Your black-theme.css .scroll-pane class will handle this now.
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Button addButton = new Button("+");
        addButton.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-background-radius: 30; -fx-min-width: 60; -fx-min-height: 60; -fx-cursor: hand;");
        // Stripped background and text colors so your CSS .button class applies properly.
        addButton.setOnAction(e -> showAddTaskDialog());

        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(scrollPane, addButton);
        StackPane.setAlignment(addButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(addButton, new Insets(0, 20, 20, 0));
        VBox.setVgrow(stackPane, Priority.ALWAYS);

        viewContainer.getChildren().addAll(datePicker, stackPane);
        refreshTimeline();
    }

    private void refreshTimeline() {
        if (tasksLayer == null) return;
        tasksLayer.getChildren().clear();

        List<Task> dayTasks = taskList.getTasksForDay(selectedDate);
        List<List<Task>> columns = new ArrayList<>();

        for (Task task : dayTasks) {
            int taskStart = task.getStartTime().getHour() * 60 + task.getStartTime().getMinute();
            int taskEnd = task.getEndTime().getHour() * 60 + task.getEndTime().getMinute();
           // if (taskEnd <= taskStart) taskEnd = taskStart + 30;

            int colIndex = -1;
            for (int i = 0; i < columns.size(); i++) {
                boolean conflict = false;
                for (Task other : columns.get(i)) {
                    int oStart = other.getStartTime().getHour() * 60 + other.getStartTime().getMinute();
                    int oEnd = other.getEndTime().getHour() * 60 + other.getEndTime().getMinute();
                    if (taskStart < oEnd && oStart < taskEnd) {
                        conflict = true;
                        break;
                    }
                }
                if (!conflict) {
                    colIndex = i;
                    break;
                }
            }
            if (colIndex == -1) {
                columns.add(new ArrayList<>());
                colIndex = columns.size() - 1;
            }
            columns.get(colIndex).add(task);

            double yPosition = (taskStart / 60.0) * ROW_HEIGHT;
            double taskHeight = ((taskEnd - taskStart) / 60.0) * ROW_HEIGHT;
            double xPosition = HOUR_LABEL_WIDTH + colIndex * (TASK_WIDTH + TASK_GAP);

            HBox taskBox = createTaskBox(task);
            taskBox.setLayoutX(xPosition);
            taskBox.setLayoutY(yPosition);
            taskBox.setPrefWidth(TASK_WIDTH);
            taskBox.setPrefHeight(taskHeight);

            tasksLayer.getChildren().add(taskBox);
        }

        double requiredWidth = HOUR_LABEL_WIDTH + (columns.size() * (TASK_WIDTH + TASK_GAP)) + 100;
        tasksLayer.setPrefWidth(requiredWidth);
        if (tasksLayer.getParent() != null) {
            ((Pane) tasksLayer.getParent()).setPrefWidth(requiredWidth);
        }
    }

    private HBox createTaskBox(Task task) {
        HBox taskBox = new HBox(6);
        taskBox.setAlignment(Pos.TOP_LEFT);

        // Only keeping the task's specific color chosen by the user in the ColorPicker
        String taskColor = (task.getColor() != null) ? task.getColor() : "#555555";
        taskBox.setStyle("-fx-background-color: " + taskColor + "; -fx-background-radius: 6; -fx-padding: 8 10; -fx-cursor: hand;");

        javafx.scene.shape.Circle checkCircle = new javafx.scene.shape.Circle(6);
        checkCircle.setStroke(Color.WHITE);
        checkCircle.setStrokeWidth(2.0);

        String displayIcon = (task.getIcon() != null) ? task.getIcon() : "📌";
        Label taskLabel = new Label(displayIcon + " " + task.getTitle());
        taskLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-font-weight: bold;");
        taskLabel.setWrapText(true);

        if (task.isComplete()) {
            checkCircle.setFill(Color.WHITE);
            taskBox.setOpacity(0.5);
        } else {
            checkCircle.setFill(Color.TRANSPARENT);
            taskBox.setOpacity(1.0);
        }

        checkCircle.setOnMouseClicked(e -> {
            e.consume();
            task.setComplete(!task.isComplete());
            saveTasksToFile();
            refreshTimeline();
        });

        taskBox.setOnMouseClicked(e -> showEditDeleteTaskDialog(task));
        taskBox.getChildren().addAll(checkCircle, taskLabel);
        return taskBox;
    }

    private void showAddTaskDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Add New Task");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        // Background color lock stripped - dialog will use CSS root styling

        Label titleLabel = new Label("Task Title:");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");

        Label descLabel = new Label("Task Description:");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Enter task description...");
        descArea.setPrefHeight(80);

        ComboBox<Integer> startHourBox = new ComboBox<>();
        ComboBox<Integer> startMinBox = new ComboBox<>();
        ComboBox<Integer> endHourBox = new ComboBox<>();
        ComboBox<Integer> endMinBox = new ComboBox<>();

        for (int i = 0; i < 24; i++) {
            startHourBox.getItems().add(i);
            endHourBox.getItems().add(i);
        }
        for (int i = 0; i < 60; i ++) {
            startMinBox.getItems().add(i);
            endMinBox.getItems().add(i);
        }
        startHourBox.setValue(9);
        startMinBox.setValue(0);
        endHourBox.setValue(10);
        endMinBox.setValue(0);

        Label colon1 = new Label(":");
        Label colon2 = new Label(":");

        HBox startTimeBox = new HBox(5, startHourBox, colon1, startMinBox);
        HBox endTimeBox = new HBox(5, endHourBox, colon2, endMinBox);

        ColorPicker colorPicker = new ColorPicker(Color.valueOf("#555555"));
        colorPicker.getStyleClass().add("split-button");
        colorPicker.setStyle("-fx-color-label-visible: false;");
        ComboBox<String> priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll("Do Now", "Schedule", "Delegate", "Eliminate");
        priorityComboBox.setValue("Do Now");

        HBox colorAndPriorityRow = new HBox(15);
        Label cLbl = new Label("Choose Color:");
        Label pLbl = new Label("Choose Priority:");
        VBox colorSection = new VBox(5, cLbl, colorPicker);
        VBox prioritySection = new VBox(5, pLbl, priorityComboBox);
        colorAndPriorityRow.getChildren().addAll(colorSection, prioritySection);

        ComboBox<String> iconComboBox = new ComboBox<>();
        iconComboBox.getItems().addAll("📌", "💻", "📚", "🏋️", "🍕", "⏰", "💡");
        iconComboBox.setValue("📌");

        Button saveButton = new Button("Add Task");

        saveButton.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Cannot save a task without a title!", ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }

            LocalTime startTime = LocalTime.of(startHourBox.getValue(), startMinBox.getValue());
            LocalTime endTime = LocalTime.of(endHourBox.getValue(), endMinBox.getValue());

            if (endTime.isBefore(startTime)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "End time cannot be before start time!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            Task newTask = new Task(titleField.getText(), LocalDateTime.of(selectedDate, startTime), LocalDateTime.of(selectedDate, endTime));
            newTask.setDescription(descArea.getText());
            newTask.setIcon(iconComboBox.getValue());
            newTask.setPriority(priorityComboBox.getValue());

            String webColor = "#" + colorPicker.getValue().toString().substring(2, 8).toUpperCase();
            newTask.setColor(webColor);

            taskList.addTask(newTask);

            saveTasksToFile();
            refreshTimeline();
            dialog.close();
        });

        Label sTimeLbl = new Label("Start Time:");
        Label eTimeLbl = new Label("End Time:");
        Label iconLbl = new Label("Choose Icon:");

        layout.getChildren().addAll(
                titleLabel, titleField,
                descLabel, descArea,
                sTimeLbl, startTimeBox,
                eTimeLbl, endTimeBox,
                colorAndPriorityRow,
                iconLbl, iconComboBox,
                saveButton
        );

        Scene scene = new Scene(layout, 340, 510);

        // Ensure the dialog loads the user's selected CSS file
        ThemeManager.applyTheme(scene, ThemeManager.appThemeProperty.get());

        dialog.setScene(scene);
        dialog.show();
    }

    private void showEditDeleteTaskDialog(Task task) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Edit / Delete Task");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label titleLabel = new Label("Task Title:");
        TextField titleField = new TextField(task.getTitle());

        Label descLabel = new Label("Task Description:");
        TextArea descArea = new TextArea(task.getDescription());
        descArea.setPrefHeight(80);

        ComboBox<Integer> editStartHourBox = new ComboBox<>();
        ComboBox<Integer> editStartMinBox = new ComboBox<>();
        ComboBox<Integer> editEndHourBox = new ComboBox<>();
        ComboBox<Integer> editEndMinBox = new ComboBox<>();

        for (int i = 0; i < 24; i++) {
            editStartHourBox.getItems().add(i);
            editEndHourBox.getItems().add(i);
        }
        for (int i = 0; i < 60; i ++) {
            editStartMinBox.getItems().add(i);
            editEndMinBox.getItems().add(i);
        }

        editStartHourBox.setValue(task.getStartTime().getHour());
        editStartMinBox.setValue(task.getStartTime().getMinute());
        editEndHourBox.setValue(task.getEndTime().getHour());
        editEndMinBox.setValue(task.getEndTime().getMinute());

        Label colon1 = new Label(":");
        Label colon2 = new Label(":");

        HBox editStartTimeBox = new HBox(5, editStartHourBox, colon1, editStartMinBox);
        HBox editEndTimeBox = new HBox(5, editEndHourBox, colon2, editEndMinBox);

        ColorPicker editcolor = new ColorPicker(Color.valueOf(task.getColor() != null ? task.getColor() : "#555555"));
        editcolor.getStyleClass().add("split-button");
        editcolor.setStyle("-fx-color-label-visible: false;");
        ComboBox<String> editPriorityComboBox = new ComboBox<>();
        editPriorityComboBox.getItems().addAll("Do Now", "Do Later", "Delegate", "Eliminate");
        editPriorityComboBox.setValue(task.getPriority() != null ? task.getPriority() : "Do Now");

        HBox editColorAndPriorityRow = new HBox(15);
        Label ecLbl = new Label("Edit Color:");
        Label epLbl = new Label("Edit Priority:");
        VBox editColorSection = new VBox(5, ecLbl, editcolor);
        VBox editPrioritySection = new VBox(5, epLbl, editPriorityComboBox);
        editColorAndPriorityRow.getChildren().addAll(editColorSection, editPrioritySection);

        ComboBox<String> editIconComboBox = new ComboBox<>();
        editIconComboBox.getItems().addAll("📌", "💻", "📚", "🏋️", "🍕", "⏰", "💡");
        editIconComboBox.setValue(task.getIcon() != null ? task.getIcon() : "📌");

        Button saveChangesButton = new Button("Save Changes");
        Button deleteTaskButton = new Button("Delete Task");
        deleteTaskButton.setStyle("-fx-background-color: #CD5C5C; -fx-text-fill: white; -fx-cursor: hand;"); // Kept delete red so it stands out as dangerous

        saveChangesButton.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) return;

            LocalTime newStartTime = LocalTime.of(editStartHourBox.getValue(), editStartMinBox.getValue());
            LocalTime newEndTime = LocalTime.of(editEndHourBox.getValue(), editEndMinBox.getValue());

            if (newEndTime.isBefore(newStartTime)) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "End time cannot be before start time!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            task.setTitle(titleField.getText());
            task.setDescription(descArea.getText());
            task.setStartTime(LocalDateTime.of(task.getStartTime().toLocalDate(), newStartTime));
            task.setEndTime(LocalDateTime.of(task.getEndTime().toLocalDate(), newEndTime));
            task.setIcon(editIconComboBox.getValue());
            task.setPriority(editPriorityComboBox.getValue());

            String webColor = "#" + editcolor.getValue().toString().substring(2, 8).toUpperCase();
            task.setColor(webColor);

            saveTasksToFile();
            refreshTimeline();
            dialog.close();
        });

        deleteTaskButton.setOnAction(e -> {
            taskList.deleteTask(task);
            saveTasksToFile();
            refreshTimeline();
            dialog.close();
        });

        HBox buttonBox = new HBox(15, saveChangesButton, deleteTaskButton);
        buttonBox.setAlignment(Pos.CENTER);

        Label sTimeLbl = new Label("Edit Start Time:");
        Label eTimeLbl = new Label("Edit End Time:");
        Label iconLbl = new Label("Edit Icon:");

        layout.getChildren().addAll(
                titleLabel, titleField,
                descLabel, descArea,
                sTimeLbl, editStartTimeBox,
                eTimeLbl, editEndTimeBox,
                editColorAndPriorityRow,
                iconLbl, editIconComboBox,
                buttonBox
        );

        Scene scene = new Scene(layout, 360, 520);

        // Ensure the dialog loads the user's selected CSS file
        ThemeManager.applyTheme(scene, ThemeManager.appThemeProperty.get());

        dialog.setScene(scene);
        dialog.show();
    }

    public VBox getView() {
        return viewContainer;
    }

    public void saveTasksToFile() {
        if (LoginView.currentUser == null) return;
        String userFile = LoginView.currentUser + "_tasks.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userFile))) {
            for (Task task : taskList.getTasks()) {
                String title = task.getTitle();
                String start = task.getStartTime().toString();
                String end = task.getEndTime().toString();
                String color = (task.getColor() != null) ? task.getColor() : "#555555";
                String desc = (task.getDescription() != null && !task.getDescription().isEmpty()) ? task.getDescription().replace("\n", "[NEWLINE]") : "null";
                String icon = (task.getIcon() != null) ? task.getIcon() : "📌";
                String priority = (task.getPriority() != null) ? task.getPriority() : "Do Now";
                writer.write(title + "|" + start + "|" + end + "|" + color + "|" + desc + "|" + icon + "|" + priority + "|" + task.isComplete());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving tasks: " + e.getMessage());
        }
    }

    public void loadTasksFromFile() {
        if (LoginView.currentUser == null) return;
        String userFile = LoginView.currentUser + "_tasks.txt";
        File file = new File(userFile);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    String title = parts[0];
                    LocalDateTime start = LocalDateTime.parse(parts[1]);
                    LocalDateTime end = LocalDateTime.parse(parts[2]);
                    String color = parts[3];
                    String desc = parts[4].equals("null") ? "" : parts[4].replace("[NEWLINE]", "\n");
                    String icon = parts[5];
                    String priority = (parts.length == 7) ? parts[6] : "Do Now";
                    boolean isComplete = (parts.length == 8) && Boolean.parseBoolean(parts[7]);
                    Task task = new Task(title, start, end);
                    task.setColor(color);
                    task.setDescription(desc);
                    task.setIcon(icon);
                    task.setPriority(priority);
                    task.setComplete(isComplete);
                    taskList.addTask(task);
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }
}