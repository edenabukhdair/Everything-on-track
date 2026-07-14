import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskList {


    private List<Task> allTasks = new ArrayList<>();


    public void addTask(Task task) {
        if (task != null) {
            this.allTasks.add(task);
        }
    }


    public void deleteTask(Task task) {
        if (task != null) {
            this.allTasks.remove(task);
        }
    }


    public List<Task> getTasksForDay(LocalDate date) {
        List<Task> dayTasks = new ArrayList<>();
        for (Task task : allTasks) {

            if (task.getStartTime().toLocalDate().equals(date)) {
                dayTasks.add(task);
            }
        }
        return dayTasks;
    }



    public List<Task> getTasks() {
        return allTasks;
    }
}


