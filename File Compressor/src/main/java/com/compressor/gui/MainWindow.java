package com.compressor.gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class MainWindow extends Application {

    private Button selectedButton = null;
    private final String IDLE_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #dcdde1; -fx-border-width: 1; -fx-text-fill: #2f3640; -fx-cursor: hand;";
    private final String SELECTED_STYLE = "-fx-background-color: #3498db; -fx-border-color: #2980b9; -fx-border-width: 1; -fx-text-fill: #ffffff; -fx-cursor: hand;";

    @Override
    public void start(Stage primaryStage) {
        //HeaderSection
        Label headerLabel = new Label("File Compression Tool");
        headerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        headerLabel.setTextFill(Color.web("#2c3e50"));

        StackPane headerBar = new StackPane(headerLabel);
        headerBar.setStyle("-fx-background-color: #ffffff; -fx-padding: 40 0 40 0;");

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
            btn.setOnAction(e -> {
                if (selectedButton != null) {
                    selectedButton.setStyle(IDLE_STYLE);
                }
                selectedButton = btn;
                selectedButton.setStyle(SELECTED_STYLE);
            });

            algorithmContainer.getChildren().add(btn);
        }

        //Making Algorithm center using HBox
        HBox mainContent = new HBox(50);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setStyle("-fx-padding: 20;");
        mainContent.getChildren().add(algorithmContainer);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: #f5f6fa;");
        root.getChildren().addAll(headerBar, mainContent);

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setTitle("File Compression Tool");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}