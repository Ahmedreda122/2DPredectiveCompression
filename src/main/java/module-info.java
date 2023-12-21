module com.example.predictive {
  requires javafx.controls;
  requires javafx.fxml;

  requires com.dlsc.formsfx;
  requires java.desktop;

  opens com.example.predictive to javafx.fxml;
  exports com.example.predictive;
}