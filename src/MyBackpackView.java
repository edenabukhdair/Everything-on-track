import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.stage.FileChooser;
import java.io.*;
import java.util.Scanner;

import javafx.scene.SnapshotParameters;
import javafx.scene.Node;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class MyBackpackView {
    private BorderPane mainLayout;
    private VBox pocketContent;
    private VBox pagesContainer;
    private TilePane filesGrid;
    private VBox addBtn;

    private String currentFont;
    private String activeSubView = "interface";
    private String currentEditingFile = null;

    private String getUserDir() {
        String user = (ThemeManager.getUsername() != null ? ThemeManager.getUsername() : "guest");
        return "src/Packables/" + user + "/";
    }

    private String getPocketDataFile() {
        return getUserDir() + "pocket_data.txt";
    }

    private String getBackpackFilesFile() {
        return getUserDir() + "backpack_files.txt";
    }

    private Color selectedColor = Color.BLACK;
    private boolean isPenActive = false;
    private boolean isEraserActive = false;
    private boolean isTextActive = false;

    private String currentPageStyle = "Blank";

    public MyBackpackView() {
        mainLayout = new BorderPane();
        mainLayout.getStyleClass().add("backpack-main-layout");

        syncFont();
        showBackpackInterface();

        if (ThemeManager.appFontProperty != null) {
            ThemeManager.appFontProperty.addListener((obs, oldVal, newVal) -> Platform.runLater(this::refreshView));
        }
    }

    private void syncFont() {
        currentFont = ThemeManager.appFontProperty.get();
        if (currentFont == null) currentFont = "Arial";
    }

    public void refreshView() {
        syncFont();

        if ("interface".equals(activeSubView)) {
            showBackpackInterface();
            filesGrid.getChildren().clear();
            filesGrid.getChildren().add(addBtn);
            pocketContent.getChildren().clear();
            loadFilesFromDisk();
            loadFromDisk();
        } else if ("editor".equals(activeSubView) && currentEditingFile != null) {
            savePageContents(currentEditingFile);
            showPackableEditor(currentEditingFile);
        }
    }

    private void showBackpackInterface() {
        activeSubView = "interface";
        currentEditingFile = null;

        filesGrid = new TilePane(30, 30);
        filesGrid.setPadding(new Insets(30));
        filesGrid.getStyleClass().add("backpack-grid");
        // Ensure the grid itself has a transparent background
        filesGrid.setStyle("-fx-background-color: transparent;");

        addBtn = new VBox(10);
        addBtn.setAlignment(Pos.CENTER);

        Button btn = new Button("+");
        btn.getStyleClass().add("backpack-add-button");
        btn.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-size: 50px; -fx-background-radius: 50; -fx-pref-width: 100; -fx-pref-height: 100; -fx-font-weight: bold; -fx-cursor: hand;");

        btn.setOnAction(e -> {
            TextInputDialog nameDialog = new TextInputDialog();
            nameDialog.setTitle("New Packable File");
            nameDialog.setHeaderText(null);
            nameDialog.setContentText("Enter File Name:");

            nameDialog.showAndWait().ifPresent(fileName -> {
                if (!fileName.trim().isEmpty()) {
                    createFileCard(fileName.trim());
                    saveFilesToDisk();
                    showPackableEditor(fileName.trim());
                }
            });
        });

        Label addNewFileLabel = new Label("Add New File");
        addNewFileLabel.getStyleClass().add("backpack-label");
        addNewFileLabel.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold;");
        addBtn.getChildren().addAll(btn, addNewFileLabel);

        pocketContent = new VBox(10);
        pocketContent.setPadding(new Insets(10));
        pocketContent.getStyleClass().add("backpack-pocket-content");
        pocketContent.setStyle("-fx-background-color: transparent;");

        ScrollPane pocketScroll = new ScrollPane(pocketContent);
        pocketScroll.setFitToWidth(true);
        pocketScroll.getStyleClass().add("backpack-scroll-pane");
        // Clear layout backgrounds on the sidebar scrollpane
        pocketScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        MenuButton addMenu = new MenuButton("Keep a Key 🔑");
        addMenu.setMaxWidth(Double.MAX_VALUE);
        addMenu.getStyleClass().add("backpack-menu-button");
        addMenu.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-background-radius: 5;");

        MenuItem addNoteItem = new MenuItem("Key: Note");
        MenuItem addImgItem = new MenuItem("Key: Photo");

        addNoteItem.setOnAction(e -> {
            HBox inputBox = new HBox(5);
            inputBox.setAlignment(Pos.CENTER_LEFT);

            TextField noteInput = new TextField();
            noteInput.setPromptText("Type your note...");
            noteInput.getStyleClass().add("backpack-text-input");
            noteInput.setStyle("-fx-font-family: '" + currentFont + "';");
            HBox.setHgrow(noteInput, Priority.ALWAYS);

            Button submitBtn = new Button("Add");
            submitBtn.getStyleClass().add("backpack-submit-button");
            submitBtn.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-cursor: hand;");

            Runnable saveNote = () -> {
                if (!noteInput.getText().isEmpty()) {
                    createPocketCard("Key: Note", noteInput.getText(), null);
                    saveToDisk();
                    pocketContent.getChildren().remove(inputBox);
                }
            };

            submitBtn.setOnAction(ae -> saveNote.run());
            noteInput.setOnAction(ae -> saveNote.run());

            inputBox.getChildren().addAll(noteInput, submitBtn);
            pocketContent.getChildren().add(0, inputBox);
        });

        addImgItem.setOnAction(e -> {
            File file = new FileChooser().showOpenDialog(null);
            if (file != null) {
                createPocketCard("Key: Photo", file.getAbsolutePath(), null);
                saveToDisk();
            }
        });
        addMenu.getItems().addAll(addNoteItem, addImgItem);

        Label pocketTitle = new Label("Pocket");
        pocketTitle.getStyleClass().add("backpack-title-label");
        pocketTitle.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-font-size: 16px;");

        VBox sidebar = new VBox(10, pocketTitle, addMenu, pocketScroll);
        sidebar.setPrefWidth(260);
        sidebar.getStyleClass().add("backpack-sidebar");
        sidebar.setStyle("-fx-padding: 15; -fx-border-width: 0 0 0 1;");

        ScrollPane filesScroll = new ScrollPane(filesGrid);
        filesScroll.setFitToWidth(true);
        filesScroll.getStyleClass().add("backpack-scroll-pane");

        // 🌟 FIX: Clean out the default white backgrounds from the ScrollPane container and inner viewport
        filesScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainLayout.setCenter(filesScroll);
        mainLayout.setRight(sidebar);
    }

    private void createFileCard(String fileName) {
        VBox fileCard = new VBox(8);
        fileCard.setAlignment(Pos.CENTER);
        fileCard.getStyleClass().add("backpack-file-card");

        // Left intact: The exact border rules, padding, and layout parameters for individual file entries stay completely valid
        fileCard.setStyle("-fx-padding: 12; -fx-background-radius: 10; -fx-pref-width: 125; -fx-pref-height: 145; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-border-color: " + (ThemeManager.appThemeProperty.get().equals("Black") ? "gray" : ThemeManager.appThemeProperty.get().contains("Pink") ? "pink" : ThemeManager.appThemeProperty.get().toLowerCase()) + ";");
        ThemeManager.appThemeProperty.addListener((obs, oldVal, newVal) -> {
            javafx.application.Platform.runLater(() -> {
                fileCard.setStyle("-fx-padding: 12; -fx-background-radius: 10; -fx-pref-width: 125; -fx-pref-height: 145; -fx-border-radius: 10; -fx-border-width: 1.5; -fx-border-color: " + (newVal.equals("Black") ? "gray" : newVal.contains("Pink") ? "pink" : newVal.toLowerCase()) + ";");
            });
        });
        Label nameLabel = new Label(fileName);
        nameLabel.getStyleClass().add("backpack-card-text");
        nameLabel.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);
        nameLabel.setAlignment(Pos.CENTER);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.TOP_RIGHT);

        Button renameBtn = new Button("✏️");
        renameBtn.getStyleClass().add("backpack-icon-button");
        renameBtn.setStyle("-fx-font-size: 12px; -fx-padding: 0; -fx-cursor: hand;");
        renameBtn.setOnAction(e -> {
            String oldName = nameLabel.getText();
            TextInputDialog renameDialog = new TextInputDialog(oldName);
            renameDialog.setTitle("Rename File");
            renameDialog.setHeaderText(null);
            renameDialog.setContentText("Enter new name:");

            renameDialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty() && !newName.equals(oldName)) {
                    nameLabel.setText(newName.trim());
                    renameFileOnDisk(oldName.trim(), newName.trim());
                    saveFilesToDisk();
                }
            });
        });

        Button deleteBtn = new Button("×");
        deleteBtn.getStyleClass().add("backpack-delete-button");
        deleteBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete File");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to delete \"" + nameLabel.getText() + "\"?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    deleteFileFromDisk(nameLabel.getText());
                    filesGrid.getChildren().remove(fileCard);
                    saveFilesToDisk();
                }
            });
        });

        topRow.getChildren().addAll(renameBtn, deleteBtn);

        Button iconBtn = new Button("📝");
        iconBtn.getStyleClass().add("backpack-card-icon-button");
        iconBtn.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-size: 32px; -fx-background-radius: 8; -fx-pref-width: 65; -fx-pref-height: 65; -fx-cursor: hand;");
        iconBtn.setOnAction(e -> showPackableEditor(nameLabel.getText()));

        fileCard.getChildren().addAll(topRow, iconBtn, nameLabel);
        filesGrid.getChildren().add(fileCard);
    }

    private void showPackableEditor(String fileName) {
        activeSubView = "editor";
        currentEditingFile = fileName;

        BorderPane editorRoot = new BorderPane();
        editorRoot.getStyleClass().add("backpack-editor-root");

        pagesContainer = new VBox(20);
        pagesContainer.setPadding(new Insets(20));
        pagesContainer.setAlignment(Pos.TOP_CENTER);

        isPenActive = false; isEraserActive = false; isTextActive = false;
        currentPageStyle = "Blank";

        loadPageContents(fileName);

        ColorPicker colorPicker = new ColorPicker(Color.BLACK);
        colorPicker.setPrefWidth(45); colorPicker.setPrefHeight(45);
        colorPicker.setStyle("-fx-background-radius: 8; -fx-padding: 2;");
        colorPicker.setOnAction(e -> selectedColor = colorPicker.getValue());

        String toolbarBtnStyle = "-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-pref-width: 45; -fx-pref-height: 45; -fx-background-radius: 8; -fx-padding: 0; -fx-cursor: hand;";

        Button backBtn = new Button("←");
        backBtn.getStyleClass().add("backpack-toolbar-btn");
        backBtn.setStyle(toolbarBtnStyle + " -fx-font-size: 18px;");
        backBtn.setOnAction(e -> {
            savePageContents(fileName);
            showBackpackInterface();
            refreshView();
        });

        Button addPageBtn = new Button("⊞");
        addPageBtn.getStyleClass().add("backpack-toolbar-btn");
        addPageBtn.setStyle(toolbarBtnStyle + " -fx-font-size: 20px;");
        addPageBtn.setOnAction(e -> addNewPage());

        Button penBtn = new Button("✏");
        penBtn.getStyleClass().add("backpack-toolbar-btn");
        penBtn.setStyle(toolbarBtnStyle + " -fx-font-size: 18px;");

        Button eraserBtn = new Button("⛝");
        eraserBtn.getStyleClass().add("backpack-toolbar-btn");
        eraserBtn.setStyle(toolbarBtnStyle + " -fx-font-size: 18px;");

        Button textBtn = new Button("Aa");
        textBtn.getStyleClass().add("backpack-toolbar-btn");
        textBtn.setStyle(toolbarBtnStyle + " -fx-font-size: 16px;");

        penBtn.setOnAction(e -> {
            isPenActive = true; isEraserActive = false; isTextActive = false;
            penBtn.getStyleClass().add("backpack-toolbar-btn-active");
            eraserBtn.getStyleClass().remove("backpack-toolbar-btn-active");
            textBtn.getStyleClass().remove("backpack-toolbar-btn-active");
        });

        eraserBtn.setOnAction(e -> {
            isPenActive = false; isEraserActive = true; isTextActive = false;
            eraserBtn.getStyleClass().add("backpack-toolbar-btn-active");
            penBtn.getStyleClass().remove("backpack-toolbar-btn-active");
            textBtn.getStyleClass().remove("backpack-toolbar-btn-active");
        });

        textBtn.setOnAction(e -> {
            isPenActive = false; isEraserActive = false; isTextActive = true;
            textBtn.getStyleClass().add("backpack-toolbar-btn-active");
            penBtn.getStyleClass().remove("backpack-toolbar-btn-active");
            eraserBtn.getStyleClass().remove("backpack-toolbar-btn-active");
        });

        Button addImgBtn = new Button("+");
        addImgBtn.getStyleClass().add("backpack-toolbar-add-img");
        addImgBtn.setStyle("-fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 24px; -fx-pref-width: 48; -fx-pref-height: 48; -fx-padding: 0; -fx-cursor: hand;");
        addImgBtn.setOnAction(e -> handleImport());

        Label titleLabel = new Label("📄 " + fileName);
        titleLabel.getStyleClass().add("backpack-toolbar-title");
        titleLabel.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold; -fx-font-size: 15px; -fx-padding: 0 15 0 0;");

        HBox toolbarBox = new HBox(12);
        toolbarBox.setAlignment(Pos.CENTER);
        toolbarBox.setPadding(new Insets(8, 20, 8, 20));
        toolbarBox.getStyleClass().add("backpack-toolbar-box");
        toolbarBox.setStyle("-fx-background-radius: 30;");
        toolbarBox.getChildren().addAll(backBtn, titleLabel, addPageBtn, penBtn, colorPicker, eraserBtn, textBtn, addImgBtn);

        HBox topWrapper = new HBox(toolbarBox);
        topWrapper.setAlignment(Pos.CENTER);
        topWrapper.setPadding(new Insets(15, 15, 5, 15));

        HBox backgroundStyleBar = new HBox(10);
        backgroundStyleBar.setAlignment(Pos.CENTER);
        backgroundStyleBar.setPadding(new Insets(0, 0, 10, 0));

        Button blankBtn = new Button("📄 Blank");
        Button ruledBtn = new Button("📝 Ruled");
        Button gridBtn = new Button("🌐 Grid");
        Button dotBtn = new Button("░ Dot");

        String subBtnStyle = "-fx-font-family: '" + currentFont + "'; -fx-font-size: 12px; -fx-background-radius: 15; -fx-padding: 5 15 5 15; -fx-font-weight: bold; -fx-cursor: hand;";
        blankBtn.setStyle(subBtnStyle); ruledBtn.setStyle(subBtnStyle); gridBtn.setStyle(subBtnStyle); dotBtn.setStyle(subBtnStyle);
        blankBtn.getStyleClass().add("backpack-sub-btn"); ruledBtn.getStyleClass().add("backpack-sub-btn"); gridBtn.getStyleClass().add("backpack-sub-btn"); dotBtn.getStyleClass().add("backpack-sub-btn");

        blankBtn.setOnAction(e -> changeCurrentPageBackground("Blank"));
        ruledBtn.setOnAction(e -> changeCurrentPageBackground("Ruled"));
        gridBtn.setOnAction(e -> changeCurrentPageBackground("Grid"));
        dotBtn.setOnAction(e -> changeCurrentPageBackground("Dot"));

        backgroundStyleBar.getChildren().addAll(blankBtn, ruledBtn, gridBtn, dotBtn);

        VBox combinedTopHeader = new VBox(topWrapper, backgroundStyleBar);

        ScrollPane scroll = new ScrollPane(pagesContainer);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("backpack-scroll-pane");
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        editorRoot.setTop(combinedTopHeader);
        editorRoot.setCenter(scroll);

        mainLayout.setCenter(editorRoot);
        mainLayout.setRight(null);
    }

    private void injectPage(StackPane page, Node... middleNodes) {
        page.setPrefSize(450, 580);
        page.setMaxSize(450, 580);
        page.getStyleClass().add("backpack-paper-page");
        page.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 5, 0, 0, 0);");

        Canvas bgCanvas = new Canvas(450, 580);
        Canvas drawingCanvas = new Canvas(450, 580);

        drawPageBackground(bgCanvas, currentPageStyle);
        setupDrawingEvents(drawingCanvas);

        page.getChildren().add(bgCanvas);
        for (Node n : middleNodes) {
            page.getChildren().add(n);
        }
        page.getChildren().add(drawingCanvas);

        Button delPageBtn = new Button("🗑 Delete Page");
        delPageBtn.getStyleClass().add("backpack-delete-page-btn");
        delPageBtn.setStyle("-fx-font-family: '" + currentFont + "'; -fx-background-color: transparent; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 0 5 0;");

        HBox pageToolbar = new HBox(delPageBtn);
        pageToolbar.setAlignment(Pos.CENTER_RIGHT);
        pageToolbar.setMaxWidth(450);

        VBox pageWrapper = new VBox(0, pageToolbar, page);
        pageWrapper.setAlignment(Pos.CENTER);

        delPageBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Page");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to delete this page? This cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    pagesContainer.getChildren().remove(pageWrapper);
                }
            });
        });

        pagesContainer.getChildren().add(pageWrapper);
    }

    private void addNewPage() {
        injectPage(new StackPane());
    }

    private void changeCurrentPageBackground(String style) {
        currentPageStyle = style;
        if (!pagesContainer.getChildren().isEmpty()) {
            VBox lastWrapper = (VBox) pagesContainer.getChildren().get(pagesContainer.getChildren().size() - 1);
            StackPane lastPage = (StackPane) lastWrapper.getChildren().get(1);
            Canvas bgCanvas = (Canvas) lastPage.getChildren().get(0);
            drawPageBackground(bgCanvas, style);
        }
    }

    private void drawPageBackground(Canvas canvas, String style) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (style.equals("Ruled")) {
            gc.setStroke(Color.web("#E0E0E0"));
            gc.setLineWidth(1.0);
            for (double y = 40; y < canvas.getHeight(); y += 28) {
                gc.strokeLine(0, y, canvas.getWidth(), y);
            }
        } else if (style.equals("Grid")) {
            gc.setStroke(Color.web("#E8E8E8"));
            gc.setLineWidth(1.0);
            for (double x = 20; x < canvas.getWidth(); x += 20) {
                gc.strokeLine(x, 0, x, canvas.getHeight());
            }
            for (double y = 20; y < canvas.getHeight(); y += 20) {
                gc.strokeLine(0, y, canvas.getWidth(), y);
            }
        } else if (style.equals("Dot")) {
            gc.setFill(Color.web("#B0B0B0"));
            for (double x = 20; x < canvas.getWidth(); x += 20) {
                for (double y = 20; y < canvas.getHeight(); y += 20) {
                    gc.fillOval(x - 1, y - 1, 2, 2);
                }
            }
        }
    }

    private void setupDrawingEvents(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(3.5);

        canvas.setOnMousePressed(e -> {
            if (isPenActive) {
                gc.setStroke(selectedColor);
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
                gc.stroke();
            } else if (isEraserActive) {
                gc.clearRect(e.getX() - 12, e.getY() - 12, 24, 24);
            } else if (isTextActive) {
                double x = e.getX();
                double y = e.getY();
                Platform.runLater(() -> {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Add Text");
                    dialog.setHeaderText(null);
                    dialog.setContentText("Enter your text:");
                    dialog.showAndWait().ifPresent(text -> {
                        if (!text.trim().isEmpty()) {
                            gc.setFill(selectedColor);
                            gc.setFont(javafx.scene.text.Font.font(currentFont, 16));
                            gc.fillText(text, x, y);
                        }
                    });
                });
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isPenActive) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
            } else if (isEraserActive) {
                gc.clearRect(e.getX() - 12, e.getY() - 12, 24, 24);
            }
        });
    }

    private void handleImport() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        File f = fc.showOpenDialog(null);
        if (f == null) return;

        String fileNameLower = f.getName().toLowerCase();

        if (fileNameLower.endsWith(".pdf")) {
            try {
                PDDocument document = PDDocument.load(f);
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    java.awt.image.BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 130);
                    javafx.scene.image.WritableImage fxImage = SwingFXUtils.toFXImage(bim, null);

                    Platform.runLater(() -> {
                        ImageView pdfPageView = new ImageView(fxImage);
                        pdfPageView.setFitWidth(430);
                        pdfPageView.setPreserveRatio(true);
                        injectPage(new StackPane(), pdfPageView);
                    });
                }
                document.close();
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        } else if (fileNameLower.matches(".*\\.(png|jpg|jpeg|gif)")) {
            if (!pagesContainer.getChildren().isEmpty()) {
                VBox activeWrapper = (VBox) pagesContainer.getChildren().get(pagesContainer.getChildren().size()-1);
                StackPane activePage = (StackPane) activeWrapper.getChildren().get(1);
                ImageView iv = new ImageView(new Image(f.toURI().toString()));
                iv.setFitWidth(300); iv.setPreserveRatio(true);
                activePage.getChildren().add(activePage.getChildren().size() - 1, iv);
            }
        }
    }

    private void createPocketCard(String type, String content, Image imgOverride) {
        VBox card = new VBox(5);
        card.getStyleClass().add("backpack-pocket-card");
        card.setStyle("-fx-padding: 10; -fx-background-radius: 5; -fx-border-radius: 5;");

        // 🌟 ENHANCED: Made the X delete button noticeably stand out with explicit sizes, bold design, and crimson color accentuation
        Button deleteBtn = new Button("×");
        deleteBtn.getStyleClass().add("backpack-pocket-delete-btn");
        deleteBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +              // Increased size from 14px to 18px
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #E63946;" +           // Added a striking crimson red color
                        "-fx-padding: 0 5 0 5;" +             // Better breathing click zone bounds
                        "-fx-cursor: hand;"
        );

        // Add a smooth hover effect to let users know it's a critical interactive target
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color: rgba(230, 57, 70, 0.15);" +
                        "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #D62828; -fx-background-radius: 4; -fx-padding: 0 5 0 5; -fx-cursor: hand;"
        ));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #E63946; -fx-padding: 0 5 0 5; -fx-cursor: hand;"
        ));

        deleteBtn.setOnAction(e -> { pocketContent.getChildren().remove(card); saveToDisk(); });

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("backpack-pocket-type-label");
        typeLabel.setStyle("-fx-font-family: '" + currentFont + "'; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(typeLabel, spacer, deleteBtn);
        topRow.setAlignment(Pos.CENTER);

        card.getChildren().add(topRow);

        if (type.equals("Key: Photo")) {
            ImageView iv = new ImageView(new Image(new File(content).toURI().toString()));
            iv.setFitWidth(200); iv.setPreserveRatio(true);
            card.getChildren().add(iv);
        } else {
            Label contentLabel = new Label(content);
            contentLabel.getStyleClass().add("backpack-pocket-content-label");
            contentLabel.setStyle("-fx-font-family: '" + currentFont + "';");
            contentLabel.setWrapText(true);
            card.getChildren().add(contentLabel);
        }

        pocketContent.getChildren().add(card);
    }

    private void savePageContents(String fileName) {
        if (ThemeManager.getUsername() == null) return;
        try {
            File metaFile = new File(getUserDir() + "backpack_meta_" + fileName + ".txt");
            metaFile.getParentFile().mkdirs();
            try (PrintWriter out = new PrintWriter(new FileWriter(metaFile))) {
                out.println(pagesContainer.getChildren().size());
                out.println(currentPageStyle);
            }

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);

            int i = 0;
            for (var node : pagesContainer.getChildren()) {
                if (node instanceof VBox wrapper && wrapper.getChildren().size() > 1) {

                    StackPane page = (StackPane) wrapper.getChildren().get(1);

                    String oldStyle = page.getStyle();
                    page.setStyle("-fx-background-color: transparent;");

                    Node bgCanvas = page.getChildren().get(0);
                    bgCanvas.setVisible(false);

                    WritableImage img = new WritableImage(450, 580);
                    page.snapshot(params, img);

                    bgCanvas.setVisible(true);
                    page.setStyle(oldStyle);

                    File pageFile = new File(getUserDir() + "backpack_page_" + fileName + "_" + i + ".png");
                    ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", pageFile);
                    i++;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadPageContents(String fileName) {
        pagesContainer.getChildren().clear();
        if (ThemeManager.getUsername() == null) {
            addNewPage();
            return;
        }

        File metaFile = new File(getUserDir() + "backpack_meta_" + fileName + ".txt");
        if (!metaFile.exists()) {
            addNewPage();
            return;
        }

        try (Scanner scanner = new Scanner(metaFile)) {
            if (scanner.hasNextLine()) {
                int numPages = Integer.parseInt(scanner.nextLine().trim());
                if (scanner.hasNextLine()) {
                    currentPageStyle = scanner.nextLine().trim();
                }

                for (int i = 0; i < numPages; i++) {
                    File pageFile = new File(getUserDir() + "backpack_page_" + fileName + "_" + i + ".png");

                    if (pageFile.exists()) {
                        Image savedImg = new Image(pageFile.toURI().toString());
                        ImageView iv = new ImageView(savedImg);
                        iv.setFitWidth(450); iv.setFitHeight(580);
                        injectPage(new StackPane(), iv);
                    } else {
                        injectPage(new StackPane());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            addNewPage();
        }
    }

    private void renameFileOnDisk(String oldName, String newName) {
        if (ThemeManager.getUsername() == null) return;

        File oldMeta = new File(getUserDir() + "backpack_meta_" + oldName + ".txt");
        File newMeta = new File(getUserDir() + "backpack_meta_" + newName + ".txt");
        if (oldMeta.exists()) oldMeta.renameTo(newMeta);

        int i = 0;
        while (true) {
            File oldPage = new File(getUserDir() + "backpack_page_" + oldName + "_" + i + ".png");
            File newPage = new File(getUserDir() + "backpack_page_" + newName + "_" + i + ".png");
            if (oldPage.exists()) {
                oldPage.renameTo(newPage);
                i++;
            } else {
                break;
            }
        }
    }

    private void deleteFileFromDisk(String fileName) {
        if (ThemeManager.getUsername() == null) return;

        File metaFile = new File(getUserDir() + "backpack_meta_" + fileName + ".txt");
        if (metaFile.exists()) metaFile.delete();

        int i = 0;
        while (true) {
            File pageFile = new File(getUserDir() + "backpack_page_" + fileName + "_" + i + ".png");
            if (pageFile.exists()) {
                pageFile.delete();
                i++;
            } else {
                break;
            }
        }
    }

    private void saveFilesToDisk() {
        File file = new File(getBackpackFilesFile());
        file.getParentFile().mkdirs();

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            for (var node : filesGrid.getChildren()) {
                if (node instanceof VBox card && card != addBtn) {
                    if (card.getChildren().size() == 3 && card.getChildren().get(2) instanceof Label nameLabel) {
                        out.println(nameLabel.getText());
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadFilesFromDisk() {
        File file = new File(getBackpackFilesFile());
        if (!file.exists()) return;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String name = scanner.nextLine().trim();
                if (!name.isEmpty()) {
                    createFileCard(name);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveToDisk() {
        File file = new File(getPocketDataFile());
        file.getParentFile().mkdirs();

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            for (var node : pocketContent.getChildren()) {
                if (node instanceof VBox card && card.getChildren().size() > 1) {
                    Label type = (Label) ((HBox) card.getChildren().get(0)).getChildren().get(0);
                    String val = "";

                    if (type.getText().equals("Key: Photo")) {
                        ImageView iv = (ImageView) card.getChildren().get(1);
                        val = new File(java.net.URI.create(iv.getImage().getUrl())).getAbsolutePath();
                    } else {
                        val = ((Label) card.getChildren().get(1)).getText();
                    }
                    out.println(type.getText() + "|" + val);
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadFromDisk() {
        File file = new File(getPocketDataFile());
        if (!file.exists()) return;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    createPocketCard(parts[0], parts[1], null);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public BorderPane getView() {
        return mainLayout;
    }
}