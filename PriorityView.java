import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.time.LocalDate;
import java.util.List;

public class PriorityView {

    private TaskList taskList;
    private LocalDate selectedDate = LocalDate.now();
    private VBox viewContainer;

    private VBox doNowContainer;
    private VBox scheduleContainer;
    private VBox delegateContainer;
    private VBox eliminateContainer;

    // 🌟 Added to track which task is currently being dragged
    private Task currentDraggedTask = null;

    public PriorityView(TaskList taskList) {
        this.taskList = taskList;
        buildView();
    }

    private void buildView() {
        viewContainer = new VBox(20);
        viewContainer.setStyle(" -fx-padding: 25;");
        HBox.setHgrow(viewContainer, Priority.ALWAYS);

        // شريط التحكم العلوي
        HBox headerRow = new HBox(15);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("Task Priority Matrix");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; ");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setStyle("  -fx-border-radius: 5;");
        datePicker.setMinWidth(130);
        datePicker.setPrefWidth(140);

        datePicker.setOnAction(e -> {
            selectedDate = datePicker.getValue();
            refreshMatrix();
        });

        headerRow.getChildren().addAll(titleLabel, datePicker);

        // شبكة مصفوفة الأولويات الرباعية
        GridPane matrixGrid = new GridPane();
        matrixGrid.setHgap(15);
        matrixGrid.setVgap(15);
        VBox.setVgrow(matrixGrid, Priority.ALWAYS);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        matrixGrid.getColumnConstraints().addAll(col1, col2);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(50);
        matrixGrid.getRowConstraints().addAll(row1, row2);

        // 🌟 Updated to pass the priority key string so the drop zone knows what priority to assign
        doNowContainer = createMatrixQuadrant("🔴 Do Now (Urgent & Important)", "#F4E3E1", "#D38E83", "Do Now");
        scheduleContainer = createMatrixQuadrant("📅 Do Later (Important & Not Urgent)", "#F5EDE4", "#C8B1A6", "Do Later");
        delegateContainer = createMatrixQuadrant("🤝 Delegate (Urgent & Not Important)", "#EFEBE4", "#A89F91", "Delegate");
        eliminateContainer = createMatrixQuadrant("🗑️ Eliminate (Not Urgent & Not Important)", "#F1F1ED", "#B0B0AC", "Eliminate");

        matrixGrid.add(doNowContainer, 0, 0);
        matrixGrid.add(scheduleContainer, 1, 0);
        matrixGrid.add(delegateContainer, 0, 1);
        matrixGrid.add(eliminateContainer, 1, 1);

        viewContainer.getChildren().addAll(headerRow, matrixGrid);
        refreshMatrix();
    }

    // بناء المربع بحماية مطلقة للعناوين من الاختفاء والنقاط
    // 🌟 Added 'priorityKey' to identify drop zones
    private VBox createMatrixQuadrant(String titleText, String bgColor, String headerTextColor, String priorityKey) {
        VBox quadrantBox = new VBox(10);
        quadrantBox.setPadding(new Insets(15));

        String defaultStyle = "-fx-background-color: " + bgColor + "; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 8, 0, 0, 4);";
        String hoverStyle = "-fx-background-color: derive(" + bgColor + ", -5%); -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);";

        quadrantBox.setStyle(defaultStyle);
        quadrantBox.setMinWidth(120);

        // 🌟 الحل الجذري: استخدام TextFlow و Text بدلاً من Label لمنع النقاط (...) نهائياً وإجبار الالتفاف
        Text titleTextNode = new Text(titleText);
        titleTextNode.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-fill: " + headerTextColor + ";");

        TextFlow headerTextFlow = new TextFlow(titleTextNode);
        headerTextFlow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(headerTextFlow, Priority.ALWAYS);

        // حاوية قائمة المهام الداخلية
        VBox tasksListHolder = new VBox(8);
        tasksListHolder.setStyle("-fx-background-color: transparent;");

        // الـ ScrollPane يحتوي حصراً على المهام
        ScrollPane scrollPane = new ScrollPane(tasksListHolder);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        quadrantBox.getChildren().addAll(headerTextFlow, scrollPane);

        // حفظ المرجع للوصول السريع
        quadrantBox.setUserData(tasksListHolder);

        // =====================================================================
        // 🌟 DRAG TARGET LOGIC: Allow cards to be dropped into this quadrant
        // =====================================================================

        // Accept drag
        quadrantBox.setOnDragOver(event -> {
            if (event.getGestureSource() != quadrantBox && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        // Visual feedback when hovering over quadrant
        quadrantBox.setOnDragEntered(event -> {
            if (event.getGestureSource() != quadrantBox && event.getDragboard().hasString()) {
                quadrantBox.setStyle(hoverStyle);
            }
            event.consume();
        });

        // Remove visual feedback
        quadrantBox.setOnDragExited(event -> {
            quadrantBox.setStyle(defaultStyle);
            event.consume();
        });

        // Handle the drop
        quadrantBox.setOnDragDropped(event -> {
            boolean success = false;
            if (currentDraggedTask != null) {
                // Update the task priority based on the quadrant dropped into
                currentDraggedTask.setPriority(priorityKey);

                // Refresh to show the updated UI
                refreshMatrix();
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return quadrantBox;
    }

    public void refreshMatrix() {
        VBox doNowList = (VBox) doNowContainer.getUserData();
        VBox scheduleList = (VBox) scheduleContainer.getUserData();
        VBox delegateList = (VBox) delegateContainer.getUserData();
        VBox eliminateList = (VBox) eliminateContainer.getUserData();

        doNowList.getChildren().clear();
        scheduleList.getChildren().clear();
        delegateList.getChildren().clear();
        eliminateList.getChildren().clear();

        List<Task> dayTasks = taskList.getTasksForDay(selectedDate);

        for (Task task : dayTasks) {
            // تصميم بطاقات المهام بطريقة مرنة تمنع اختفاء النصوص والـ الوقت
            GridPane taskCard = new GridPane();
            taskCard.setPadding(new Insets(8, 10, 8, 10));
            taskCard.setHgap(5);
            taskCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 2, 0, 0, 1);");

            // 🌟 Added cursor styling so users know they can drag it
            taskCard.setOnMouseEntered(e -> taskCard.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 8; -fx-cursor: open_hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 3, 0, 0, 1);"));
            taskCard.setOnMouseExited(e -> taskCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: default; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 2, 0, 0, 1);"));

            // إعدادات مرونة الأعمدة للبطاقة: نص المهمة يمتد والوقت يأخذ مساحة ثابتة ومحمية
            ColumnConstraints textCol = new ColumnConstraints();
            textCol.setHgrow(Priority.ALWAYS);
            ColumnConstraints timeCol = new ColumnConstraints();
            timeCol.setHgrow(Priority.NEVER);
            timeCol.setMinWidth(38);
            taskCard.getColumnConstraints().addAll(textCol, timeCol);

            // عنوان المهمة باستخدام TextFlow لمنع ظهور أي نقاط في نص المهمة أيضاً!
            String icon = (task.getIcon() != null) ? task.getIcon() : "📌";
            Text taskTextNode = new Text(icon + " " + task.getTitle());
            taskTextNode.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-fill: #34495e;");

            TextFlow taskTextFlow = new TextFlow(taskTextNode);
            taskTextFlow.setMaxWidth(Double.MAX_VALUE);
            // Ignore mouse interactions on text so drag works perfectly on the card
            taskTextFlow.setMouseTransparent(true);

            // عرض الوقت على اليمين
            Label timeLabel = new Label(task.getStartTime().toLocalTime().toString());
            timeLabel.setStyle("-fx-font-size: 11px; ");
            timeLabel.setAlignment(Pos.CENTER_RIGHT);
            timeLabel.setMouseTransparent(true);

            if (task.isComplete()) {
                taskCard.setOpacity(0.5);
                taskTextNode.setStyle("-fx-font-size: 12px; -fx-fill: #7f8c8d; -fx-strikethrough: true;");
            }

            taskCard.add(taskTextFlow, 0, 0);
            taskCard.add(timeLabel, 1, 0);
            GridPane.setHalignment(timeLabel, javafx.geometry.HPos.RIGHT);

            // =====================================================================
            // 🌟 DRAG SOURCE LOGIC: Allow the card to be dragged
            // =====================================================================
            taskCard.setOnDragDetected(event -> {
                Dragboard db = taskCard.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                // JavaFX requires some string/data to trigger the drag
                content.putString(task.getTitle());
                db.setContent(content);

                // Track which object is moving
                currentDraggedTask = task;
                taskCard.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: closed_hand; -fx-opacity: 0.5;");
                event.consume();
            });

            taskCard.setOnDragDone(event -> {
                currentDraggedTask = null; // Clear when done
                event.consume();
            });

            String priority = (task.getPriority() != null) ? task.getPriority() : "Do Now";
            switch (priority) {
                case "Do Now":
                    doNowList.getChildren().add(taskCard);
                    break;
                case "Do Later":
                    scheduleList.getChildren().add(taskCard);
                    break;
                case "Delegate":
                    delegateList.getChildren().add(taskCard);
                    break;
                case "Eliminate":
                    eliminateList.getChildren().add(taskCard);
                    break;
                default:
                    doNowList.getChildren().add(taskCard);
                    break;
            }
        }
    }

    public VBox getView() {
        return viewContainer;
    }
}