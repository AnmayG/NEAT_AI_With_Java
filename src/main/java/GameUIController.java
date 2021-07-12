import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;

import java.util.ArrayList;

public class GameUIController{
    @FXML
    Slider slider1;
    @FXML
    LineChart<Number, Number> chart1;
    @FXML
    NumberAxis xAxis, yAxis;

    private static double sliderValue = 0;

    /**
     * Initializes the UI
     */
    @FXML
    void initialize(){
        //https://www.tutorialspoint.com/how-to-set-action-to-a-slider-using-javafx
        slider1.valueProperty().addListener((observable, oldValue, newValue) -> sliderValue = slider1.getValue());
        chart1.setAnimated(false);
        xAxis.setLabel("Generation");
        yAxis.setLabel("Score");
    }

    public static double getSlider1Value(){
        return sliderValue;
    }
}
