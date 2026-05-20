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

    private final MultiLevelCompressor compressor = new MultiLevelCompressor();

    private final Label statusLabel = new Label();
    private final Button downloadButton = new Button("Download Result");
    private final Button statsButton = new Button("View Compression Stats");
    private final ImageView fileIconView = new ImageView();

    private File selectedInputFile;
    private boolean selectedIsFolder;
    private File processedTempFile;
    private File processedTempFolder;
    private boolean isCurrentlyCompressing;
    private boolean resultIsFolder;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RAZ Archiver");

        // HEADER SECTION
        StackPane headerPane = new StackPane();
        headerPane.setPadding(new Insets(15, 0, 15, 0));
        headerPane.setStyle("-fx-background-color: #ffffff; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-border-color: #dcdde1; -fx-border-width: 0 0 1 0;");

        ImageView logoView = new ImageView();
        try {
            File logoFile = new File("src/RAZ Archiver LOGO.jpg");
            if (!logoFile.exists()) logoFile = new File("RAZ Archiver LOGO.jpg");
            if (logoFile.exists()) {
                logoView.setImage(new Image(logoFile.toURI().toString()));
                logoView.setFitHeight(100);
                logoView.setPreserveRatio(true);
            }
        } catch (Exception e) {
            System.out.println("Logo error: " + e.getMessage());
        }
        headerPane.getChildren().add(logoView);

        // MAIN CONTENT AREA
        VBox mainContent = new VBox(30);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40, 50, 50, 50));
        mainContent.setStyle("-fx-background-color: #f0f2f5;");

        //Compress and Decompress section with drag and drop, and also select folder or file
        HBox splitContent = new HBox(60);
        splitContent.setAlignment(Pos.CENTER);

        VBox compressBox = createActionColumn(primaryStage, "Compress File or Folder", "COMPRESS", true);
        VBox decompressBox = createActionColumn(primaryStage, "Decompress Archive", "DECOMPRESS", false);
        splitContent.getChildren().addAll(compressBox, decompressBox);

        VBox feedbackArea = new VBox(15);
        feedbackArea.setAlignment(Pos.CENTER);


        HBox resultDisplay = new HBox(12);
        resultDisplay.setAlignment(Pos.CENTER);
        fileIconView.setFitWidth(40);
        fileIconView.setFitHeight(40);
        fileIconView.setPreserveRatio(true);
        statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        resultDisplay.getChildren().addAll(fileIconView, statusLabel);

        downloadButton.setVisible(false);
        downloadButton.setStyle("-fx-background-color: #000080;-fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        downloadButton.setOnAction(event -> {
            try {
                handleDownload(primaryStage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        statsButton.setVisible(false);
        statsButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 5;");
        statsButton.setOnAction(event -> showStatsWindow());

        feedbackArea.getChildren().addAll( resultDisplay, downloadButton, statsButton);
        mainContent.getChildren().addAll(splitContent, feedbackArea);

        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(headerPane);
        rootLayout.setCenter(mainContent);

        primaryStage.setScene(new Scene(rootLayout));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private VBox createActionColumn(Stage stage, String header, String btnText, boolean isCompress) {
        VBox col = new VBox(20);
        col.setAlignment(Pos.TOP_CENTER);
        col.setPrefWidth(450);

        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        VBox dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefSize(400, 250);
        dropZone.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b2bec3; -fx-border-style: dashed; "
                + "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-cursor: hand;");

        Label t = new Label("Drag and drop files here");
        t.setStyle("-fx-font-size: 18px; -fx-text-fill: #636e72;");
        dropZone.getChildren().add(t);

        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            if (event.getDragboard().hasFiles()) {
                File dropped = event.getDragboard().getFiles().get(0);
                setSelection(dropped, isCompress);
                event.setDropCompleted(true);
            }
            event.consume();
        });

        HBox selectButtons = new HBox(10);
        selectButtons.setAlignment(Pos.CENTER);

        if (isCompress) {
            Button fileBtn = new Button("Select File");
            fileBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 5;");
            fileBtn.setOnAction(event -> {
                FileChooser fc = new FileChooser();
                File file = fc.showOpenDialog(stage);
                if (file != null) setSelection(file, true);
            });

            Button folderBtn = new Button("Select Folder");
            folderBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 5;");
            folderBtn.setOnAction(event -> {
                DirectoryChooser dc = new DirectoryChooser();
                File folder = dc.showDialog(stage);
                if (folder != null) setSelection(folder, true);
            });
            selectButtons.getChildren().addAll(fileBtn, folderBtn);
        } else {
            Button fileBtn = new Button("Select .raz / .raz.zip");
            fileBtn.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 5;");
            fileBtn.setOnAction(event -> {
                FileChooser fc = new FileChooser();
                File file = fc.showOpenDialog(stage);
                if (file != null) setSelection(file, false);
            });
            selectButtons.getChildren().add(fileBtn);
        }

        Button actionBtn = new Button(btnText);
        actionBtn.setStyle("-fx-background-color: " + (isCompress ? "#007bff" : "#28a745")
                + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 12 45; -fx-background-radius: 5; -fx-cursor: hand;");
        actionBtn.setOnAction(event -> {
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

    private void setSelection(File f, boolean isCompress) {
        selectedInputFile = f;
        selectedIsFolder = f.isDirectory();

        // Load the provided File Icon
        try {
            File iconFile = new File("src/File Icon.png"); // Ensure filename matches exactly
            if (iconFile.exists()) {
                fileIconView.setImage(new Image(iconFile.toURI().toString()));
            } else {
                // Fallback if not in src
                iconFile = new File("File Icon.png");
                if (iconFile.exists()) {
                    fileIconView.setImage(new Image(iconFile.toURI().toString()));
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load selection icon.");
        }

        statsButton.setVisible(false);
        statusLabel.setText("Selected: " + f.getName());
    }

    private void runBackendProcess() {

        downloadButton.setVisible(false);
        statsButton.setVisible(false);
        fileIconView.setImage(null);
        resultIsFolder = false;

        statusLabel.setText(isCurrentlyCompressing ? "Compressing..." : "Restoring...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (isCurrentlyCompressing) {
                    if (selectedIsFolder) {
                        processedTempFile = File.createTempFile(selectedInputFile.getName() + "_", ".raz.zip");
                        compressor.compressFolder(selectedInputFile.getAbsolutePath(), processedTempFile.getAbsolutePath());
                    } else {
                        processedTempFile = File.createTempFile("raz_output", ".raz");
                        compressor.compressFile(selectedInputFile.getAbsolutePath(), processedTempFile.getAbsolutePath());
                    }
                } else {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"));
                    if (selectedInputFile.getName().endsWith(".zip")) {
                        resultIsFolder = true;
                        String path = compressor.decompressFolder(selectedInputFile.getAbsolutePath(), tempDir.getAbsolutePath());
                        processedTempFolder = new File(path);
                    } else {
                        String path = compressor.decompressFile(selectedInputFile.getAbsolutePath(), tempDir.getAbsolutePath());
                        processedTempFile = new File(path);
                    }
                }


                return null;
            }
        };

        //Platfor.runLater() switch back safely to main ui thread after task process completed in background thread
        task.setOnSucceeded(event -> Platform.runLater(() -> {
            statusLabel.setText(isCurrentlyCompressing ? "Compression Complete!" : "Restoration Complete!");
            downloadButton.setVisible(true);
            if (isCurrentlyCompressing) statsButton.setVisible(true);
            tryLoadIcon();
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            statusLabel.setText("Error occurred during processing.");
        }));

        new Thread(task).start();
    }

    private void showStatsWindow() {
        MultiLevelCompressor.CompressionStats stats = compressor.getLastStats();
        if (stats == null) return;

        Stage statsStage = new Stage();
        statsStage.setTitle("Compression Analysis");

        double ratio = (1.0 - (double) stats.compressedSize / stats.originalSize) * 100;
        String percentage = String.format("%.2f%%", ratio);

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f0f2f5;");

        // Changed alignment to Pos.CENTER to center all card contents
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(30));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: #ffffff; " +
                "-fx-background-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        Label heading = new Label("Compression Status");
        // Added center text alignment for the heading
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                "-fx-border-color: #007bff; -fx-border-width: 0 0 2 0; -fx-padding: 0 0 10 0; -fx-alignment: center;");
        heading.setMaxWidth(Double.MAX_VALUE);

        VBox details = new VBox(12);
        details.setAlignment(Pos.CENTER); // Center the stack of rows

        details.getChildren().addAll(
                createStatRow("Original Size   :", stats.originalSize + " bytes"),
                createStatRow("Compressed Size :", stats.compressedSize + " bytes"),
                createStatRow("Algorithm Used  :", stats.winnerAlgo),
                createStatRow("Compression %   :", percentage)
        );

        card.getChildren().addAll(heading, details);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 450, 400);
        statsStage.setScene(scene);
        statsStage.setResizable(false);
        statsStage.show();
    }

    private HBox createStatRow(String label, String value) {
        // Changed alignment to Pos.CENTER to center the key-value pair horizontally
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER);

        Label lblKey = new Label(label);
        lblKey.setStyle("-fx-font-weight: bold; -fx-text-fill: #636e72; -fx-font-size: 14px;");

        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #2d3436; -fx-font-size: 14px; -fx-font-weight: 500;");

        row.getChildren().addAll(lblKey, lblValue);
        return row;
    }

    private void handleDownload(Stage stage) throws IOException {
        if (selectedInputFile == null) return;

        if (resultIsFolder && processedTempFolder != null) {
            DirectoryChooser dc = new DirectoryChooser();
            File dest = dc.showDialog(stage);

            if (dest != null) {

                File finalFolder = new File(dest, processedTempFolder.getName());

                copyFolder(processedTempFolder.toPath(), finalFolder.toPath());

                statusLabel.setText("Folder saved successfully!");
            }
        } else if (processedTempFile != null) {
            FileChooser fc = new FileChooser();
            fc.setInitialFileName(processedTempFile.getName());
            File dest = fc.showSaveDialog(stage);
            if (dest != null) {
                try {
                    Files.copy(processedTempFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    statusLabel.setText("File saved successfully!");
                } catch (IOException e) {
                    statusLabel.setText("Save failed.");
                }
            }
        }
    }

    private void copyFolder(java.nio.file.Path source,
                            java.nio.file.Path target) throws IOException {

        Files.walk(source).forEach(path -> {
            try {

                java.nio.file.Path relative = source.relativize(path);
                java.nio.file.Path destination = target.resolve(relative);

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination,
                            StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void tryLoadIcon() {
        try {
            File iconFile = new File("src/raz_icon.png");
            if (iconFile.exists()) {
                fileIconView.setImage(new Image(iconFile.toURI().toString()));
            }
        } catch (Exception e) {
            System.out.println("Icon load error.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}