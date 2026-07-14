import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.stage.FileChooser;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class SettingsView {
    private VBox settingsLayout;
    private Circle profileCircle;
    private Label avatarLabel;
    private ComboBox<String> themeComboBox, fontComboBox;
    private Scene scene;

    private Button backBtn;
    private Button editPicBtn;
    private Button myImprovementBtn;
    private TextField nicknameField;

    public SettingsView(Scene scene, StackPane parentRoot, TaskList taskList) {
        this.scene = scene;
        settingsLayout = new VBox(20);
        settingsLayout.setPadding(new Insets(30));
        settingsLayout.setAlignment(Pos.TOP_CENTER);

        // 🌟 ربط الحاوية بكلاس .root لالتقاط خلفية الـ CSS المحددة
        settingsLayout.getStyleClass().add("root");

        // 1. زر الرجوع
        backBtn = new Button("⬅ Back");
        backBtn.setOnAction(e -> parentRoot.getChildren().remove(settingsLayout));

        // 2. البروفايل مع القلم
        StackPane profileStack = new StackPane();
        profileStack.setMaxWidth(110);

        profileCircle = new Circle(55, Color.web("#EAE2DF"));
        avatarLabel = new Label("Add Photo");
        avatarLabel.getStyleClass().add("label"); // ربطه بتنسيق النص في الـ CSS

        editPicBtn = new Button("✏️");
        editPicBtn.setOnAction(e -> handleImageUpload());
        // ستايل مباشر بسيط للحفاظ على دائرية زر القلم دون تداخل مع ملف الـ CSS العام
        editPicBtn.setStyle("-fx-background-radius: 50; -fx-background-color: #E0E0E0; -fx-cursor: hand;");

        profileStack.getChildren().addAll(profileCircle, avatarLabel, editPicBtn);
        StackPane.setAlignment(editPicBtn, Pos.BOTTOM_RIGHT);

        // 3. الاسم
        nicknameField = new TextField(ThemeManager.getUsername());
        nicknameField.setEditable(false);
        nicknameField.setMaxWidth(160);
        nicknameField.setAlignment(Pos.CENTER);
        // جعل حقل النص شفافاً ومناسباً لعرض الاسم داخل البروفايل
        nicknameField.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 16px;");

        // 4. الثيم والخط
        themeComboBox = new ComboBox<>();
        themeComboBox.getItems().addAll("Default (Pink)", "Purple", "Black", "Blue", "Yellow", "Green", "Red");
        themeComboBox.setValue(ThemeManager.appThemeProperty.get());

        fontComboBox = new ComboBox<>();
        fontComboBox.getItems().addAll("Arial", "Verdana", "Helvetica", "Courier New");
        fontComboBox.setValue(ThemeManager.appFontProperty.get());

        // الربط بالـ ThemeManager (الـ CSS يتكفل كلياً بالألوان تلقائياً)
        themeComboBox.setOnAction(e -> {
            ThemeManager.appThemeProperty.set(themeComboBox.getValue());
            ThemeManager.applyTheme(this.scene, themeComboBox.getValue());
            ThemeManager.saveProfileData();
        });

        fontComboBox.setOnAction(e -> {
            ThemeManager.appFontProperty.set(fontComboBox.getValue());
            ThemeManager.applyFont(this.scene, fontComboBox.getValue());
            ThemeManager.saveProfileData();
        });

        // 5. زر الإحصائيات
        myImprovementBtn = new Button("📈 My Improvement");
        myImprovementBtn.setOnAction(e -> {
            VBox statsLayer = createImprovementLayer(taskList, parentRoot);
            parentRoot.getChildren().add(statsLayer);
        });

        // 🌟 مستمع لمراقبة تغيير الخط وتطبيقه لحظياً
        ThemeManager.appFontProperty.addListener((observable, oldValue, newValue) -> refreshFonts());

        // استرجاع الصورة المحفوظة
        updateProfileImage(ThemeManager.getCurrentImagePath());

        // إضافة المكونات للحاوية
        settingsLayout.getChildren().addAll(backBtn, profileStack, nicknameField, themeComboBox, fontComboBox, myImprovementBtn);

        // تطبيق الخطوط المحددة عند تحميل الشاشة لأول مرة
        refreshFonts();
    }

    /**
     * دالة رشيقة لتحديث نوع الخط فقط عبر المكونات (الألوان تُدار كلياً بملف الـ CSS)
     */
    private void refreshFonts() {
        String currentFont = ThemeManager.appFontProperty.get();
        String fontStyle = "-fx-font-family: '" + currentFont + "';";

        backBtn.setStyle(fontStyle);
        myImprovementBtn.setStyle(fontStyle);
        editPicBtn.setStyle("-fx-background-radius: 50; -fx-background-color: #E0E0E0; -fx-cursor: hand; " + fontStyle);
        avatarLabel.setStyle(fontStyle + " -fx-font-size: 12px;");
        nicknameField.setStyle("-fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 16px; " + fontStyle);
        themeComboBox.setStyle(fontStyle);
        fontComboBox.setStyle(fontStyle);
    }

    // بناء طبقة المخطط البياني للإحصائيات
    private VBox createImprovementLayer(TaskList taskList, StackPane parentRoot) {
        String currentFont = ThemeManager.appFontProperty.get();
        String fontStyle = "-fx-font-family: '" + currentFont + "';";

        VBox viewContainer = new VBox(20);
        viewContainer.setAlignment(Pos.CENTER);
        viewContainer.setPadding(new Insets(30));
        viewContainer.getStyleClass().add("root"); // تلتقط الخلفية المناسبة من الـ CSS للثيم

        Label title = new Label("My Improvement 📈");
        title.getStyleClass().add("title-label");
        title.setStyle(fontStyle);

        // إعداد المحاور
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Timeline");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Tasks");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        // إنشاء المخطط البياني الأعمدة
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Weekly Task Statistics");
        barChart.setAnimated(true);

        XYChart.Series<String, Number> addedSeries = new XYChart.Series<>();
        addedSeries.setName("Tasks Added");

        XYChart.Series<String, Number> completedSeries = new XYChart.Series<>();
        completedSeries.setName("Tasks Completed");

        // معالجة البيانات لآخر 4 أسابيع
        int[] addedCounts = new int[4];
        int[] completedCounts = new int[4];

        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int currentWeek = today.get(weekFields.weekOfWeekBasedYear());
        int currentYear = today.getYear();

        if (taskList != null && taskList.getTasks() != null) {
            for (Task task : taskList.getTasks()) {
                if (task.getStartTime() == null) continue;

                LocalDate taskDate = task.getStartTime().toLocalDate();
                int taskWeek = taskDate.get(weekFields.weekOfWeekBasedYear());
                int taskYear = taskDate.getYear();

                if (taskYear == currentYear) {
                    int weekDiff = currentWeek - taskWeek;
                    if (weekDiff >= 0 && weekDiff < 4) {
                        addedCounts[weekDiff]++;
                        if (task.isComplete()) {
                            completedCounts[weekDiff]++;
                        }
                    }
                }
            }
        }

        // تفريغ البيانات داخل المخطط بترتيب زمني صحيح
        String[] weekLabels = {"This Week", "Last Week", "2 Weeks Ago", "3 Weeks Ago"};
        for (int i = 3; i >= 0; i--) {
            addedSeries.getData().add(new XYChart.Data<>(weekLabels[i], addedCounts[i]));
            completedSeries.getData().add(new XYChart.Data<>(weekLabels[i], completedCounts[i]));
        }

        barChart.getData().addAll(addedSeries, completedSeries);

        // زر الرجوع لشاشة الإعدادات
        Button backButton = new Button("Back to Settings");
        backButton.setStyle(fontStyle);
        backButton.setOnAction(e -> parentRoot.getChildren().remove(viewContainer));

        viewContainer.getChildren().addAll(title, barChart, backButton);

        return viewContainer;
    }

    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String path = selectedFile.toURI().toString();
            ThemeManager.setCurrentImagePath(path);
            updateProfileImage(path);
        }
    }

    private void updateProfileImage(String path) {
        if (path != null && !path.equals("default")) {
            try {
                Image img = new Image(path);
                profileCircle.setFill(new ImagePattern(img));
                avatarLabel.setVisible(false);
            } catch (Exception e) {
                avatarLabel.setVisible(true);
            }
        } else {
            avatarLabel.setVisible(true);
        }
    }

    public VBox getView() { return settingsLayout; }
}