import com.almasb.fxgl.entity.component.Component;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundComponent extends Component {
    //While I don't really use the FXML file to its full utility in this project, it's still a good thing
    //Makes it easier in the future when I want to do things
    private Canvas canvas1 = new Canvas(Game.getScreenWidth(), Game.getScreenHeight());

    /**
     * Passes in all of the nodes from the background.fxml file
     */
    @Override
    public void onAdded(){
        try {
            Parent root = FXMLLoader.load(getClass().getResource("assets/background.fxml"));
            ArrayList<Node> nodes = new ArrayList<>();
            Game.addAllDescendents(root, nodes);
            for (Node n:nodes) {
                entity.getViewComponent().addChild(n);
                if(n.getId() != null){
                    //I wanted to do something with this, but I'm lazy
                    if(n.getId().equals("canvas1")){
                        canvas1 = (Canvas) n;
                    }
                }
            }
            new GridRenderThread().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copied from the Geometry Wars code by AlmasB, renders a line on the canvas every time that the thread runs
     */
    private class GridRenderThread extends Thread{
        AtomicBoolean renderFinished = new AtomicBoolean(false);
        GridRenderThread(){
            super("GridRenderThread");
            setDaemon(true);
        }
        @Override
        public void run(){
            while (true) {
                if (renderFinished.get()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                canvas1.getGraphicsContext2D().clearRect(0, 0, Game.getScreenWidth(), Game.getScreenHeight());
                GraphicsContext g = canvas1.getGraphicsContext2D();
                g.setStroke(Color.BLACK);
                g.setLineWidth(25);
                g.strokeLine(0, 187, Game.getScreenWidth(), 187);
                g.strokeLine(0, 287, Game.getScreenWidth(), 287);
                renderFinished.set(true);
            }
        }
    }
}