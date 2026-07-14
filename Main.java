import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {

    // المتغيرات الأساسية للمشروع
    private TaskList taskList = new TaskList();
    private TimelineView timelineView;
    private DiaryView diaryView;
    private PriorityView priorityView;
    private MyBackpackView backpackView; // 🌟 قمنا بإضافته هنا كمتغير ثابت للتمكن من مناداته
    private StackPane rootLayer;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Everything-on-track");

        // تشغيل التذكيرات
        ReminderManager.initialize();
        ReminderManager.startReminderCheck(taskList);

        // إظهار شاشة تسجيل الدخول أولاً
        LoginView loginView = new LoginView(() -> showMainApp(primaryStage));
        primaryStage.setScene(new Scene(loginView.getView(), 550, 720));
        primaryStage.show();
    }

    private void showMainApp(Stage primaryStage) {
        // 1. تعريف واجهات العرض (إنشاء الكائنات فقط بدون تحميل عشوائي)
        timelineView = new TimelineView(taskList);
        diaryView = new DiaryView();
        priorityView = new PriorityView(taskList);
        backpackView = new MyBackpackView(); // 🌟 إنشاء الكائن هنا

        // 2. التبويبات (Schedule مع التبويبات الفرعية)
        TabPane scheduleSubTabs = new TabPane();
        scheduleSubTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab timelineTab = new Tab("Timeline", timelineView.getView());
        Tab priorityTab = new Tab("Priority", priorityView.getView());

        // تحديث المصفوفة فور اختيار تبويب الـ Priority لضمان ظهور المهام مرتبة
        priorityTab.setOnSelectionChanged(e -> {
            if (priorityTab.isSelected()) priorityView.refreshMatrix();
        });
        scheduleSubTabs.getTabs().addAll(timelineTab, priorityTab);

        TabPane mainTabs = new TabPane();
        mainTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mainTabs.setTabMinWidth(160);
        mainTabs.setStyle("-fx-tab-min-width: 160px; -fx-tab-max-width: 160px;");

        Tab scheduleMainTab = new Tab("Schedule", scheduleSubTabs);
        Tab diaryTab = new Tab("Diary", diaryView.getView());
        Tab backpackTab = new Tab("My Backpack", backpackView.getView()); // 🌟 تمرير الواجهة المحفوظة
        mainTabs.getTabs().addAll(scheduleMainTab, diaryTab, backpackTab);

        // 3. إعدادات الـ Settings
        Button settingsButton = new Button("⚙️ Settings");
        settingsButton.setStyle("-fx-background-radius: 5; -fx-padding: 5 15; -fx-cursor: hand;");

        // 4. الحاوية الأساسية
        rootLayer = new StackPane();
        BorderPane mainContent = new BorderPane();
        mainContent.setTop(new HBox(settingsButton));
        mainContent.setCenter(mainTabs);

        rootLayer.getChildren().add(mainContent);

        // 5. إنشاء المشهد (Scene)
        Scene mainScene = new Scene(rootLayer, 550, 720);

        // 🌟 الخطوة السحرية: تحميل بيانات الثيم والملفات الخاصة بالمستخدم الفعلي الآن!
        ThemeManager.loadUserPreferences(LoginView.currentUser, mainScene);

        // استدعاء دوال الـ refresh المحدثة لكل واجهة بناءً على المستخدم الحالي
        timelineView.refreshView();
        diaryView.refreshView();
        backpackView.refreshView(); // 🌟 تحميل حقيبة المستخدم الحالي ونصوصها وصورها

        // 6. زر الإعدادات في ملف Main.java
        settingsButton.setOnAction(e -> {
            // 🌟 قمنا بإضافة taskList هنا كمتغير ثالث ليتوافق مع بنية الكلاس
            SettingsView settings = new SettingsView(mainScene, rootLayer, taskList);
            rootLayer.getChildren().add(settings.getView());
        });
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }
}