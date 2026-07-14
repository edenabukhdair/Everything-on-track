import java.time.LocalDate;
import java.util.List;

class DiaryEntry {
    private LocalDate date;
    private String text;
    private int rating;
    private List<String> imagePaths;

    public DiaryEntry( LocalDate date, String text, int rating, List<String> imagePaths) {
        this.date = date;
        this.text = text;
        this.rating = rating;
        this.imagePaths = imagePaths;
    }

    //public LocalDate getDate() {
      //  return date;
    //}


    public String getText() {
        return text;
    }

    public int getRating() {
        return rating;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }
}
