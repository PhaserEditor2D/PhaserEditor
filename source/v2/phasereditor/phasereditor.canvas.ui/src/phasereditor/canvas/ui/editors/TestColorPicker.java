package phasereditor.canvas.ui.editors;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class TestColorPicker extends Application {
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    Scene scene = new Scene(new HBox(20), 400, 100);
    HBox box = (HBox) scene.getRoot();
    final ColorPicker colorPicker = new ColorPicker();
    colorPicker.setValue(Color.RED);

    final Text text = new Text("Color picker:");
    text.setFill(colorPicker.getValue());

    colorPicker.setOnAction((ActionEvent t) -> {
      text.setFill(colorPicker.getValue());
    });

    box.getChildren().addAll(colorPicker, text);

    stage.setScene(scene);
    stage.show();
  }
}