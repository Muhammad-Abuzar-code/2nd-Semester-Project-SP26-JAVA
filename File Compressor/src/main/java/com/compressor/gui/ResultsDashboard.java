package com.compressor.gui;

import com.compressor.core.CompressionResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class ResultsDashboard {
    private List<CompressionResult> results;

    public ResultsDashboard(List<CompressionResult> results) {
        this.results = results;
    }

    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Full Benchmark Analysis");

        TableView<CompressionResult> table = new TableView<>();

        // Column: Algorithm Name
        TableColumn<CompressionResult, String> nameCol = new TableColumn<>("Algorithm");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPipelineName()));

        // Column: Compression Ratio
        TableColumn<CompressionResult, String> ratioCol = new TableColumn<>("Ratio");
        ratioCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.2f%%", d.getValue().getCompressionRatio())));

        // Column: Compression Time
        TableColumn<CompressionResult, String> cTimeCol = new TableColumn<>("Comp. Time");
        cTimeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCompressionTimeMs() + " ms"));


        // Column: Integrity Check
        TableColumn<CompressionResult, String> integrityCol = new TableColumn<>("Integrity");
        integrityCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isIntegrityPassed() ? "✅ OK" : "❌ FAIL"));

        table.getColumns().addAll(nameCol, ratioCol, cTimeCol,integrityCol);
        table.getItems().addAll(results);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox layout = new VBox(15, new Label("Benchmark Comparison Results"), table);
        layout.setPadding(new Insets(20));

        stage.setScene(new Scene(layout, 700, 400));
        stage.show();
    }
}