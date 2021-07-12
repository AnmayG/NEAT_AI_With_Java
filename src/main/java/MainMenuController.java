import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class MainMenuController {
    @FXML
    Pane aPane;
    @FXML
    Slider slider1, slider2;

    //Yeah I'm not documenting this, there's no point
    @FXML
    void initialize(){
        aPane.setBackground(new Background(new BackgroundFill(Color.GRAY, null, null)));
    }

    @FXML
    private void b1Click(){
        Config.setMode("AI");
    }

    @FXML
    private void b2Click(){
        Config.setMode("Player");
    }

    @FXML
    private void b4Click(){
        Config.setMode("Loaded");
    }
}