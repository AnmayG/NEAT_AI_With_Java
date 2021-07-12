import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class GameMenu extends FXGLMenu {
    /**
     * Creates a new game menu based on the hard-coded FXML nodes
     */
    public GameMenu() {
        super(MenuType.GAME_MENU);
        Label l = new Label("Paused");
        l.setLayoutX(10);
        l.setLayoutY(Game.getScreenHeight() - 40);
        l.setFont(new Font(25));
        l.setTextFill(Color.WHITE);
        getContentRoot().getChildren().add(l);

        Button b = new Button("Main Menu");
        b.setOnMouseClicked(mouseEvent->{
            Game.setFirstInit(true);
            fireExitToMainMenu();
        });
        b.setLayoutX(100);
        b.setLayoutY(Game.getScreenHeight()-40);
        b.setPrefWidth(100);
        b.setFont(new Font(15));
        getContentRoot().getChildren().add(b);
    }
}
