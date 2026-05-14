package com.compressor.gui;

import com.compressor.core.CompressionResult;
import com.compressor.core.MultiLevelCompressor;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends Application {

    private Button selectedButton = null;
    private final String IDLE_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #dcdde1; -fx-border-width: 1; -fx-text-fill: #2f3640; -fx-cursor: hand;";
    private final String SELECTED_STYLE = "-fx-background-color: #3498db; -fx-border-color: #2980b9; -fx-border-width: 1; -fx-text-fill: #ffffff; -fx-cursor: hand;";

    private final String DROP_ZONE_IDLE = "-fx-background-color: #ffffff; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 15; -fx-background-radius: 15;";
    private final String DROP_ZONE_ACTIVE = "-fx-background-color: #ebf5fb; -fx-border-color: #2980b9; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 15; -fx-background-radius: 15;";
    @Override
    public void start(Stage primaryStage) {

        //HeaderSection
        Label headerLabel = new Label("File Compression Tool");
        headerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        headerLabel.setTextFill(Color.web("#2c3e50"));

        StackPane headerBar = new StackPane(headerLabel);
        headerBar.setStyle("-fx-background-color: #ffffff; -fx-padding: 40 0 40 0;");

        //Drag and Drop Section
        VBox fileContainer = new VBox(25);
        fileContainer.setAlignment(Pos.TOP_CENTER);
        fileContainer.setPrefWidth(300);

        VBox dropZone = new VBox(10);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.setPrefHeight(350);
        dropZone.setStyle(DROP_ZONE_IDLE);

        Label dropLabel = new Label("Drag and Drop File\nor Click to Browse");
        dropLabel.setStyle("-fx-text-alignment: center; -fx-text-fill: #7f8c8d;");
        dropLabel.setFont(Font.font("Arial", 16));

        Label iconLabel = new Label("📁");
        iconLabel.setFont(Font.font(50));

        dropZone.getChildren().addAll(iconLabel, dropLabel);

        dropZone.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                dropLabel.setText("Selected: " + file.getName());
            }
        });

        // --- Drag and Drop Logic ---
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                dropZone.setStyle(DROP_ZONE_ACTIVE);
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> dropZone.setStyle(DROP_ZONE_IDLE));

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File selectedFile = db.getFiles().get(0);
                dropLabel.setText("Selected: " + selectedFile.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Label fileHeader = new Label("1. SOURCE FILE");
        fileHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        fileHeader.setTextFill(Color.web("#7f8c8d"));

        fileContainer.getChildren().addAll(fileHeader,dropZone);

        //AlgorithmSection
        VBox algorithmContainer = new VBox(15);
        algorithmContainer.setAlignment(Pos.TOP_CENTER);
        algorithmContainer.setPrefWidth(300);

        Label algoHeader = new Label("2. SELECT ALGORITHM");
        algoHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        algoHeader.setTextFill(Color.web("#7f8c8d"));
        algoHeader.setStyle("-fx-padding: 0 0 10 0;");

        algorithmContainer.getChildren().add(algoHeader);

        //Setting Algorithm names
        String[] algorithms = {"RLE", "LZW", "Huffman", "RLE → Huffman", "LZW → Huffman", "RLE → LZW → Huffman"};

        for (String algo : algorithms) {
            Button btn = new Button(algo);
            btn.setMaxWidth(Double.MAX_VALUE); // Make button fill width
            btn.setPrefHeight(45);
            btn.setFont(Font.font("Arial", 14));
            btn.setStyle(IDLE_STYLE);

            //Making color change when selected
            btn.setOnMouseEntered(e -> {
                if (btn != selectedButton) {
                    btn.setOpacity(0.7);
                }
            });
            btn.setOnMouseExited(e -> btn.setOpacity(1.0));
            btn.setOnAction(e -> {
                if (selectedButton != null) {
                    selectedButton.setStyle(IDLE_STYLE);
                }
                selectedButton = btn;
                selectedButton.setStyle(SELECTED_STYLE);
                btn.setOpacity(1.0);
            });

            algorithmContainer.getChildren().add(btn);
        }

        //Execution Section
        VBox executeContainer = new VBox(25);
        executeContainer.setAlignment(Pos.TOP_CENTER);
        executeContainer.setPrefWidth(300);

        Label execHeader = new Label("3. EXECUTION");
        execHeader.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        execHeader.setTextFill(Color.web("#7f8c8d"));

        Button startBtn = createActionButton("START COMPRESSION", "#27ae60");
        Button decompressBtn = createActionButton("DECOMPRESS FILE", "#2c3e50");
        Button benchmarkBtn = createActionButton("RUN BENCHMARK", "#3498db");


        executeContainer.getChildren().addAll(execHeader,startBtn,decompressBtn,benchmarkBtn);

        HBox mainContent = new HBox(50);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setStyle("-fx-padding: 20;");
        mainContent.getChildren().addAll(fileContainer,algorithmContainer,executeContainer);


        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f5f6fa;");
        root.getChildren().addAll(headerBar, mainContent);

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("File Compression Tool");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

    }
    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(70);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;");

        // Simple hover effect
        btn.setOnMouseEntered(e -> btn.setOpacity(0.9));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));

        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}