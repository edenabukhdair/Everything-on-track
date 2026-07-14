import java.time.LocalDateTime;

public class Task {
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String color;
    private String description;
    private String icon;
    private boolean isComplete = false;
    private String priority = "Do Now";


    public Task(String title, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;

    }



    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isComplete() {return isComplete;}
    public void setComplete(boolean complete) {this.isComplete = complete;}

    public String getPriority() {return priority;}
    public void setPriority(String priority) {this.priority = priority;}

}

