package com.example.predictive;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class HelloController {

  @FXML
  protected void onCompressButtonClick() throws IOException {

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select a file to compress");
    // Set the initial directory to the current directory
    fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    File file = fileChooser.showOpenDialog(new Stage());
    BufferedImage img = Predict.loadImage(Paths.get(file.getAbsolutePath()));
    Predict.ImageArr3D quantized = Predict.getQuantizedDiff(img);
    Predict.compressToFile(quantized);
  }
  @FXML
  protected void onDecompressButtonClick(){
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select a file to decompress");
    // Set the initial directory to the current directory
    fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
    File file = fileChooser.showOpenDialog(new Stage());
    Decompress.ImageArr3D _QuantizedFromFile = Decompress.loadQuantizedDiff(file.getAbsolutePath());
    BufferedImage predicted = Decompress.decompressImage(_QuantizedFromFile);
    Decompress.saveImage(Paths.get("decode.bmp"), predicted, "bmp");
  }
}