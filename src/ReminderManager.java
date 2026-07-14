import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Duration;
import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

public class ReminderManager {

    private static TrayIcon trayIcon;
    private static Timeline reminderTimeline;
    // 🌟 ذاكرة مؤقتة لمنع تكرار التنبيه للمهمة الواحدة داخل نفس الدقيقة
    private static final Set<String> notifiedTasksCache = new HashSet<>();

    public static void initialize() {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported on this device.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // 🌟 إصلاح الأيقونة: رسم مربع ملون أزرق بحجم 16x16 لكي يتعرف عليه نظام التشغيل كأيقونة صالحة
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(new java.awt.Color(52, 152, 219)); // لون أزرق متناسق
            g2d.fillRect(0, 0, 16, 16);
            g2d.dispose();

            trayIcon = new TrayIcon(image, "Everything-on-track");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("Everything-on-track Reminders");

            tray.add(trayIcon);
        } catch (Exception e) {
            System.out.println("Could not initialize System Tray safely. Notifications will fall back to in-app alerts.");
            e.printStackTrace();
            trayIcon = null;
        }
    }

    public static void showNotification(String taskTitle, String durationText) {
        System.out.println("⏰ NOTIFICATION TRIGGERED: \"" + taskTitle + "\" (" + durationText + ")");

        // 1. Laptop Native Notification
        if (trayIcon != null) {
            String title = "⏰ Task Starting Now!";
            String message = "Your task: \"" + taskTitle + "\" is starting (" + durationText + ").";

            // Run on a separate thread so it doesn't freeze the JavaFX UI
            new Thread(() -> trayIcon.displayMessage(title, message, MessageType.INFO)).start();
        } else {
            System.out.println("Tray icon is null, cannot send laptop notification.");
        }

        // 2. In-App JavaFX Popup
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Task Reminder ⏰");
            alert.setHeaderText("Task is starting now!");
            alert.setContentText("Your task: \"" + taskTitle + "\" (" + durationText + ") is starting.");
            alert.show();
        });
    }

    public static void startReminderCheck(TaskList taskList) {
        if (reminderTimeline != null) {
            reminderTimeline.stop();
        }

        // 🌟 جعل الفحص يعمل كل 20 ثانية لتجنب ثغرة تخطي الدقائق بسبب انزلاق الـ Thread
        reminderTimeline = new Timeline(new KeyFrame(Duration.seconds(20), event -> {
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // تفريغ الذاكرة المؤقتة للتنبيهات عند منتصف الليل لبدء يوم جديد
            if (now.getHour() == 0 && now.getMinute() == 0) {
                notifiedTasksCache.clear();
            }

            if (taskList == null || taskList.getTasksForDay(today) == null) return;

            for (Task task : taskList.getTasksForDay(today)) {
                if (task.getStartTime() == null || task.getEndTime() == null || task.isComplete()) continue;

                LocalTime taskStartTime = task.getStartTime().toLocalTime();

                // التحقق من تطابق الساعة والدقيقة الحالية
                if (taskStartTime.getHour() == now.getHour() && taskStartTime.getMinute() == now.getMinute()) {

                    // إنشاء مفتاح فريد للمهمة بالوقت والتاريخ لمنع تكرار إطلاق التنبيه في نفس الدقيقة
                    String taskKey = task.getTitle() + "_" + taskStartTime.toString() + "_" + today.toString();

                    if (!notifiedTasksCache.contains(taskKey)) {
                        notifiedTasksCache.add(taskKey); // وضعها في الذاكرة لمنع التكرار

                        String durationText = String.format("%02d:%02d - %02d:%02d",
                                task.getStartTime().getHour(), task.getStartTime().getMinute(),
                                task.getEndTime().getHour(), task.getEndTime().getMinute());

                        showNotification(task.getTitle(), durationText);
                    }
                }
            }
        }));

        reminderTimeline.setCycleCount(Animation.INDEFINITE);
        reminderTimeline.play();
    }


}