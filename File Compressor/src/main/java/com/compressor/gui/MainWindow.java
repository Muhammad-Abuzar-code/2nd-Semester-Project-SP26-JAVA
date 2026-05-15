package com.compressor.gui;

import com.compressor.core.MultiLevelCompressor;
import com.compressor.io.FileHandler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class MainWindow extends Application {

    private MultiLevelCompressor compressor = new MultiLevelCompressor();
    private FileHandler fileHandler = new FileHandler();

    private ProgressBar progressBar  = new ProgressBar(0);
    private Label       statusLabel  = new Label();
    private Button      downloadButton = new Button("Download Result");
    private ImageView   fileIconView = new ImageView();

    private File    selectedInputFile;      // file or folder the user picked
    private boolean selectedIsFolder;       // true when selection is a folder
    private File    processedTempFile;      // single-file result (temp .raz or restored file)
    private File    processedTempFolder;    // folder result (restored folder, for decompress)
    private boolean isCurrentlyCompressing;
    private boolean resultIsFolder;         // true when the output is a folder (decompress folder)

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("File Compressor");

        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        root.setStyle("-fx-background-color: white;");

        HBox splitContent = new HBox(60);
        splitContent.setAlignment(Pos.CENTER);

        VBox compressBox   = createActionColumn(primaryStage, "Compress File or Folder", "COMPRESS",   true);
        VBox decompressBox = createActionColumn(primaryStage, "Decompress Archive",       "DECOMPRESS", false);
        splitContent.getChildren().addAll(compressBox, decompressBox);

        // ── Feedback area ──
        VBox feedbackArea = new VBox(15);
        feedbackArea.setAlignment(Pos.CENTER);

        progressBar.setPrefWidth(500);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #28a745;");

        HBox resultDisplay = new HBox(12);
        resultDisplay.setAlignment(Pos.CENTER);

        fileIconView.setFitWidth(40);
        fileIconView.setFitHeight(40);
        fileIconView.setPreserveRatio(true);

        statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333333;");
        resultDisplay.getChildren().addAll(fileIconView, statusLabel);

        downloadButton.setVisible(false);
        downloadButton.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ced4da; "
                + "-fx-padding: 10 30; -fx-cursor: hand; -fx-font-weight: bold;");
        downloadButton.setOnAction(e -> handleDownload(primaryStage));

        feedbackArea.getChildren().addAll(progressBar, resultDisplay, downloadButton);
        root.getChildren().addAll(splitContent, feedbackArea);

        primaryStage.setScene(new Scene(root));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    // ─────────────────────────────────────────────────────────────
    //  BUILD ONE SIDE COLUMN
    // ─────────────────────────────────────────────────────────────

    private VBox createActionColumn(Stage stage, String header, String btnText, boolean isCompress) {
        VBox col = new VBox(20);
        col.setAlignment(Pos.TOP_CENTER);
        col.setPrefWidth(450);

        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // ── Drop zone ──
        VBox dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefSize(400, 200);
        dropZone.setStyle("-fx-border-color: #cccccc; -fx-border-style: dashed; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-cursor: hand;");

        Label t  = new Label(isCompress ? "File or Folder" : "Decompress");
        t.setStyle("-fx-font-size: 20px;");
        Label st = new Label("Drag & Drop here, or use the buttons below");
        dropZone.getChildren().addAll(t, st);

        // Drag-over
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        // Drop — detect file vs folder automatically
        dropZone.setOnDragDropped(event -> {
            if (event.getDragboard().hasFiles()) {
                File dropped = event.getDragboard().getFiles().get(0);
                setSelection(dropped, isCompress);
                event.setDropCompleted(true);
            }
            event.consume();
        });

        // ── Select buttons ──
        HBox selectButtons = new HBox(10);
        selectButtons.setAlignment(Pos.CENTER);

        if (isCompress) {
            // Compress side: separate "File" and "Folder" buttons
            Button fileBtn = new Button("Select File");
            fileBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-padding: 8 20; -fx-background-radius: 5;");
            fileBtn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                File file = fc.showOpenDialog(stage);
                if (file != null) setSelection(file, true);
            });

            Button folderBtn = new Button("Select Folder");
            folderBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-padding: 8 20; -fx-background-radius: 5;");
            folderBtn.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                File folder = dc.showDialog(stage);
                if (folder != null) setSelection(folder, true);
            });

            selectButtons.getChildren().addAll(fileBtn, folderBtn);
        } else {
            // Decompress side: user picks either a .raz file or a .raz.zip
            Button fileBtn = new Button("Select .raz / .raz.zip");
            fileBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-padding: 8 20; -fx-background-radius: 5;");
            fileBtn.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("RAZ Archives", "*.raz", "*.zip"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                File file = fc.showOpenDialog(stage);
                if (file != null) setSelection(file, false);
            });
            selectButtons.getChildren().add(fileBtn);
        }

        // ── Action button ──
        Button actionBtn = new Button(btnText);
        actionBtn.setStyle("-fx-background-color: " + (isCompress ? "#0099ff" : "#28a745")
                + "; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 12 45; -fx-background-radius: 5;");
        actionBtn.setOnAction(e -> {
            if (selectedInputFile != null) {
                isCurrentlyCompressing = isCompress;
                runBackendProcess();
            } else {
                statusLabel.setText("Please select a file or folder first.");
            }
        });

        col.getChildren().addAll(headerLabel, dropZone, selectButtons, actionBtn);
        return col;
    }

    /** Set the current selection and update status label. */
    private void setSelection(File f, boolean isCompress) {
        selectedInputFile = f;
        selectedIsFolder  = f.isDirectory();
        fileIconView.setImage(null);

        if (selectedIsFolder) {
            statusLabel.setText("Selected folder: " + f.getName());
        } else {
            statusLabel.setText("Selected: " + f.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  BACKGROUND TASK
    // ─────────────────────────────────────────────────────────────

    private void runBackendProcess() {
        progressBar.setVisible(true);
        downloadButton.setVisible(false);
        fileIconView.setImage(null);
        resultIsFolder = false;

        String statusMsg = isCurrentlyCompressing
                ? (selectedIsFolder ? "Compressing folder..." : "Compressing file...")
                : "Restoring...";
        statusLabel.setText(statusMsg);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                if (isCurrentlyCompressing) {

                    if (selectedIsFolder) {
                        // ── Compress folder → .raz.zip ──
                        processedTempFile = File.createTempFile(
                                selectedInputFile.getName() + "_", ".raz.zip");
                        compressor.compressFolder(
                                selectedInputFile.getAbsolutePath(),
                                processedTempFile.getAbsolutePath());

                    } else {
                        // ── Compress single file → .raz ──
                        processedTempFile = File.createTempFile("raz_output_", ".raz");
                        compressor.compressFile(
                                selectedInputFile.getAbsolutePath(),
                                processedTempFile.getAbsolutePath());
                    }

                } else {
                    // ── Decompress ──
                    String name = selectedInputFile.getName();
                    File tempDir = new File(System.getProperty("java.io.tmpdir"));

                    if (name.endsWith(".raz.zip") || name.endsWith(".zip")) {
                        // ── Decompress folder archive → restored folder ──
                        resultIsFolder = true;
                        String restoredPath = compressor.decompressFolder(
                                selectedInputFile.getAbsolutePath(),
                                tempDir.getAbsolutePath());
                        processedTempFolder = new File(restoredPath);

                    } else {
                        // ── Decompress single .raz file ──
                        String originalExt = compressor.getOriginalExtensionFromHeader(
                                selectedInputFile.getAbsolutePath());

                        // Build the exact filename decompressFile() will write
                        String rawName     = name;
                        String withoutRaz  = rawName.endsWith(".raz")
                                ? rawName.substring(0, rawName.length() - 4) : rawName;
                        String baseName    = (!originalExt.isEmpty() && withoutRaz.endsWith(originalExt))
                                ? withoutRaz.substring(0, withoutRaz.length() - originalExt.length())
                                : withoutRaz;
                        String restoredFileName = "restored_" + baseName + originalExt;

                        compressor.decompressFile(
                                selectedInputFile.getAbsolutePath(),
                                tempDir.getAbsolutePath());

                        processedTempFile = new File(tempDir, restoredFileName);
                    }
                }

                // Progress animation
                for (int i = 1; i <= 100; i++) {
                    updateProgress(i, 100);
                    Thread.sleep(15);
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            progressBar.setVisible(false);

            String resultName;
            if (isCurrentlyCompressing) {
                resultName = selectedIsFolder
                        ? selectedInputFile.getName() + ".raz.zip"
                        : fileHandler.getFileName(selectedInputFile.getAbsolutePath()) + ".raz";
            } else {
                resultName = resultIsFolder
                        ? processedTempFolder.getName()
                        : processedTempFile.getName();
            }

            // Try to show icon
            tryLoadIcon();

            statusLabel.setText((isCurrentlyCompressing ? "Compressed: " : "Restored: ") + resultName);
            downloadButton.setVisible(true);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            progressBar.setVisible(false);
            Throwable ex = task.getException();
            statusLabel.setText("Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        }));

        new Thread(task).start();
    }

    // ─────────────────────────────────────────────────────────────
    //  DOWNLOAD
    // ─────────────────────────────────────────────────────────────

    private void handleDownload(Stage stage) {

        if (resultIsFolder) {
            // For a restored folder: ask the user where to put it, then copy the whole tree
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Save Restored Folder To...");
            File destination = dc.showDialog(stage);
            if (destination != null) {
                try {
                    File finalDest = new File(destination, processedTempFolder.getName());
                    copyFolder(processedTempFolder, finalDest);
                    statusLabel.setText("Saved folder: " + finalDest.getAbsolutePath());
                    downloadButton.setVisible(false);
                } catch (IOException ex) {
                    statusLabel.setText("Save failed: " + ex.getMessage());
                }
            }
        } else {
            // Single file (compressed .raz, compressed .raz.zip, or restored file)
            FileChooser fc = new FileChooser();
            fc.setTitle("Save File");
            fc.setInitialFileName(processedTempFile.getName());
            File destination = fc.showSaveDialog(stage);
            if (destination != null) {
                try {
                    Files.copy(processedTempFile.toPath(), destination.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    statusLabel.setText("Saved: " + destination.getName());
                    downloadButton.setVisible(false);
                } catch (IOException ex) {
                    statusLabel.setText("Save failed: " + ex.getMessage());
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UTILITIES
    // ─────────────────────────────────────────────────────────────

    /** Recursively copy a folder and all its contents to a destination. */
    private void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            dest.mkdirs();
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyFolder(child, new File(dest, child.getName()));
                }
            }
        } else {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void tryLoadIcon() {
        try {
            var stream = getClass().getResourceAsStream("/raz_icon.png");
            if (stream != null) {
                fileIconView.setImage(new Image(stream));
            } else {
                for (File f : new File[]{new File("src/raz_icon.png"), new File("raz_icon.png")}) {
                    if (f.exists()) { fileIconView.setImage(new Image(f.toURI().toString())); break; }
                }
            }
        } catch (Exception ex) {
            System.out.println("Icon error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}