import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.PhysicsWorld;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static com.almasb.fxgl.dsl.FXGL.*;

public class Game extends GameApplication{
    private static final int screenWidth = 1000;
    private static final int screenHeight = 600;
    private Entity player = null;
    private static ArrayList<Entity> targets = new ArrayList<>();

    //This might not be the smartest way, but it's the way that involves the least objects and as such the least memory
    //At least it should be. Maybe. At the very least, I can do it and it doesn't screw too much up.
    //It may be better to use a class with all of this information already, but that just brings up new problems
    //Plus since I'll have to iterate through these things often it's better to do something like this
    //It's more efficient than showing everything like an entity would at least.
    private static ArrayList<ArrayList<Rectangle>> quasiEnemies = new ArrayList<>();
    private static ArrayList<int[]> quasiSpeeds = new ArrayList<>();
    private static ArrayList<Rectangle> quasiBullets = new ArrayList<>();
    private static ArrayList<Point2D> quasiDirections = new ArrayList<>();
    private static ArrayList<int[]> numTicksLasted = new ArrayList<>();

    private static boolean firstInit = true;
    private static final ArrayList<Integer> fIdArr = new ArrayList<>();
    private static final ArrayList<double[]> angleArr = new ArrayList<>();
    private final int generationSize = 1000;
    private int numRuns = 0;
    private static Overall overallPopulation;
    private double lastHigh = 0;
    private double lastAvg = 0;
    private ArrayList<Double> scores = new ArrayList<>();
    private static final ArrayList<ArrayList<Double>> values = new ArrayList<>();
    private final ArrayList<XYChart.Series<Number, Number>> seriesArrayList;
    {
        //this is a cool new thing that I'm just using so that the line doesn't exceed the limit
        seriesArrayList = new ArrayList<>(Arrays.asList(new XYChart.Series<>(), new XYChart.Series<>()));
    }
    private int showCounter = 0;

    /**
     * This is where you should declare any UI controls that you want to reference.
     * For example, if you want to modify canvas1, add "private Canvas canvas1;"
     */
    public static class GameUIMenu {
        //These fields need to be public if I want this to work
        //Essentially, I'm declaring all of my fields in here.
        //Think about it like adding an @FXML tag, but instead we're putting it inside of a class
        //So you can just copy in all of this code and you can make a new UI
        //It also needs to be in a different class otherwise it doesn't work :(
        //So just declare everything in here.
        //It makes everything easier later on as well.

        public static Canvas canvas1;
        public static Label label1;
        public static LineChart<Number, Number> chart1;
        public static ListView<String> list1;
        public static Canvas backgroundCanvas;
    }

    /**
     * Returns the height of the GameApp
     * @return the height of the Game
     */
    public static int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Returns the width of the GameApp
     * @return the width of the Game
     */
    public static int getScreenWidth() {
        return screenWidth;
    }

    /**
     * The type declarations
     */
    public enum Type {
        PLAYER, BULLET, ENEMY, BACKGROUND
    }

    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setWidth(screenWidth);
        gameSettings.setHeight(screenHeight);
        gameSettings.setMainMenuEnabled(true);
        gameSettings.setApplicationMode(ApplicationMode.DEVELOPER);
        gameSettings.setDeveloperMenuEnabled(true);
        gameSettings.setTitle("NEAT Guns");
        gameSettings.setSceneFactory(new SceneFactory() {
            @NotNull
            @Override
            public FXGLMenu newMainMenu() {
                return new MainMenu();
            }

            @NotNull
            @Override
            public FXGLMenu newGameMenu() {
                return new GameMenu();
            }
        });
    }

    /**
     * Loads the nodes from the gameUI.fxml file into the UI.
     * Also assigns values to the GameUIMenu class variables so that they can be accessed elsewhere
     * This is so that you can just copy-paste these two functions and nothing will change
     */
    @Override
    protected void initUI(){
        //I got bored so here's a new expansion to FXGL! You can now make UIs in SceneBuilder!
        //this is actually way more useful than you'd think, I don't know why I didn't think of this sooner
        //It's genuinely useful.
        Parent root = null;
        try {
            root = FXMLLoader.load(getClass().getResource("assets/gameUI.fxml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert root != null;
        ArrayList<Node> nodes = new ArrayList<>();
        addAllDescendents(root, nodes);
        //This finds the nodes with the right ID and sets their values correctly
        //Because I'm using the field thing I don't have to deal with all the other junk
        try {
            Field[] fields = GameUIMenu.class.getFields();
            for (Node n : nodes) {
                if (n.getId() != null) {
                    for (Field field : fields) {
                        if (n.getId().equals(field.getName())) {
                            field.set(GameUIMenu.class, n);
                        }
                    }
                }
            }
        }catch(IllegalAccessException e){
            e.printStackTrace();
        }

        while(root.getChildrenUnmodifiable().size() > 0){
            //I don't know what the heck happened here but root.getChildrenUnmodifiable() is getting smaller.
            //Shouldn't be happening. Kind of scary.
            Node n = root.getChildrenUnmodifiable().get(0);
            if(Config.mode.equals("Player") || Config.mode.equals("Slider")){
                n.setVisible(false);
            }
            getGameScene().addUINode(n);
        }
    }

    /**
     * Adds all child nodes from the parent FXML node into the array list
     * @param parent The parent FXML node
     * @param nodes The array list to pass all child nodes, including the parent node, into
     */
    //https://stackoverflow.com/questions/24986776/how-do-i-get-all-nodes-in-a-parent-in-javafx
    public static void addAllDescendents(Parent parent, ArrayList<Node> nodes) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            nodes.add(node);
            if (node instanceof Parent)
                addAllDescendents((Parent)node, nodes);
        }
    }

    /**
     * Passes a new target with a random start coordinate if there are not enough targets in the targets array list
     * Otherwise sets the position of the last target in the arraylist to a random starting coordinate
     * @return The index of the created target
     */
    public static int createTarget(){
        if(targets.size() < Config.numTargets) {
            targets.add(spawn("Enemy", new SpawnData(Config.randInt(-100, 0), Math.random() < 0.5 ? 0 : 100)
                    .put("speed", Config.variableSpeeds ? Config.randInt(1, 5) : 5)));
            targets.sort(Comparator.comparingDouble(Entity::getX).reversed());
            return targets.size() - 1;
        }else{
            targets.sort(Comparator.comparingDouble(Entity::getX).reversed());
            targets.get(Config.numTargets-1).setPosition(Config.randInt(-100, 0), Math.random() < 0.5 ? 0 : 100);
            for (int i = 0; i < Config.numTargets-1; i++) {
                targets.get(i).getComponent(TargetComponent.class).setSpeed(Config.variableSpeeds?Config.randInt(1, 5):5);
            }
            return 0;
        }
    }

    /**
     * Removes a target from the array list targets
     * @param target the target entity to be removed
     */
    public static void removeTarget(Entity target){
        targets.remove(target);
    }

    /**
     * Starts the simulation for the players that don't have their targets showing.
     * Essentially, since the other players don't have targets (to cut down on lag and make the display more simple),
     * we use rectangle objects to take the place of those targets. This creates those rectangles.
     */
    public void initSimEnemies(){
        //Create the rectangles for each player
        quasiEnemies = new ArrayList<>();
        quasiSpeeds = new ArrayList<>();
        quasiBullets = new ArrayList<>();
        quasiDirections = new ArrayList<>();
        numTicksLasted = new ArrayList<>();
        for (int i = 0; i < generationSize; i++) {
            ArrayList<Rectangle> playerEnemies = new ArrayList<>();
            for (int j = 0; j < Config.numTargets; j++) {
                playerEnemies.add(new Rectangle(Config.randInt(-100, 0), Math.random() < 0.5 ? 0 : 100, 100, 200));
            }
            quasiEnemies.add(playerEnemies);
            int[] enemySpeeds = new int[Config.numTargets];
            for (int j = 0; j < enemySpeeds.length; j++) {
                enemySpeeds[j] = Config.variableSpeeds?Config.randInt(1, 5):5;
            }
            quasiSpeeds.add(enemySpeeds);
            quasiBullets.add(null);
            quasiDirections.add(new Point2D(0, 0));
            numTicksLasted.add(new int[Config.numTargets]);
        }
    }

    /**
     * This updates the rectangles that we use as substitutes for the targets.
     */
    public void simulateEnemies(){
        //Essentially, I'm only going to be displaying the best brain in order to reduce lag.
        //But since the other things still need to have something that they can hit, this does it for me
        //So I can just use rectangles and move them around the screen or whatever
        /*TODO:
         *  x Create the rectangles for each player
         *  x Have each player calculate their move and everything
         *  x Then calculate the bullet's trajectory and move it every tick (do this in onUpdate)
         *  x At the same time move these rectangles around
         *  x When the bullet hits a rectangle, remove the rectangle and add a new one like you do in createTarget
         *  x Make the rectangles correct when they collide with each other
         */
        for (int i = 0; i < quasiEnemies.size(); i++){
            quasiEnemies.get(i).sort(Comparator.comparingDouble(Rectangle::getX).reversed());
            boolean champ = overallPopulation.getPlayers().get(i).getComponent(PlayerComponent.class).getStudent().isChamp();
            for (int j = 0; j < quasiEnemies.get(i).size(); j++) {
                //move the rectangles
                Rectangle r = quasiEnemies.get(i).get(j);
                r.setLocation((int) r.getX() + quasiSpeeds.get(i)[j], (int) r.getY());
                //check if any of the rectangles are colliding and if so change their position and speed
                for (int k = 0; k < quasiEnemies.get(i).size(); k++) {
                    Rectangle r2 = quasiEnemies.get(i).get(k);
                    if(r.equals(r2)) continue;
                    if(r.intersects(r2)) {
                        if (r.getX() >= r2.getX()) {
                            r2.setLocation((int) (r.getX() - r2.getWidth() - 10), (int) r2.getY());
                            quasiSpeeds.get(i)[k] = quasiSpeeds.get(i)[j];
                        } else {
                            r.setLocation((int) (r2.getX() - r.getWidth() - 10), (int) r.getY());
                            quasiSpeeds.get(i)[j] = quasiSpeeds.get(i)[k];
                        }
                    }
                }

                //if(a.getX() >= b.getX()) {
                //   b.setX(a.getX() - b.getWidth() - 5);
                //   b.getComponent(TargetComponent.class).setSpeed(a.getComponent(TargetComponent.class).getSpeed());
                //}else{
                //   a.setX(b.getX() - a.getWidth() - 5);
                //   a.getComponent(TargetComponent.class).setSpeed(b.getComponent(TargetComponent.class).getSpeed());
                //}

                //Update the number of ticks that each target lasted
                numTicksLasted.get(i)[j] = numRuns;
                int thresholdShowCounter = 0;
                if(showCounter > thresholdShowCounter) {
                    if (champ) {
                        targets.get(j).setX(r.getX());
                        targets.get(j).setY(r.getY());
                        targets.get(j).getComponent(TargetComponent.class).setSpeed(quasiSpeeds.get(i)[j]);
                        if(j == quasiEnemies.get(i).size() - 1) showCounter = 0;
                    }
                }
                if(r.getX() > screenWidth){
                    //If the target reaches the end you die
                    overallPopulation.getPlayers().get(i).getComponent(PlayerComponent.class).setDead(true);
                    numTicksLasted.get(i)[j] = 0;
                    r.setLocation(Config.randInt(-100, 0), Math.random() < 0.5 ? 0 : 100);
                    if(champ) {
                        overallPopulation.getPlayers().get(i).getComponent(PlayerComponent.class).getStudent().setChamp(false);
                        targets.get(j).setX(r.getX());
                        targets.get(j).setY(r.getY());
                    }
                }
            }
        }
        for (int i = 0; i < quasiBullets.size(); i++) {
            PlayerComponent honorsStudent = overallPopulation.getPlayers().get(i).getComponent(PlayerComponent.class);
            if(quasiBullets.get(i)==null)continue;
            if(honorsStudent.getStudent().isChamp() && honorsStudent.getBullet() != null) {
                quasiBullets.get(i).setLocation((int) honorsStudent.getBullet().getPosition().getX(),
                        (int) honorsStudent.getBullet().getPosition().getY());
                honorsStudent.getBullet().setPosition(quasiBullets.get(i).getX(), quasiBullets.get(i).getY());
            }
            quasiBullets.get(i).translate((int) quasiDirections.get(i).getX(), (int) quasiDirections.get(i).getY());
            if(quasiBullets.get(i).getY()+quasiBullets.get(i).getHeight() < 0) quasiBullets.set(i, null);
            for (int j = 0; j < quasiEnemies.get(i).size(); j++) {
                if(quasiBullets.get(i)==null) continue;
                //System.out.println("bullet shot by " + i);
                if(quasiBullets.get(i).intersects(quasiEnemies.get(i).get(j)) &&
                        !overallPopulation.getPlayers().get(i).getComponent(PlayerComponent.class).getStudent().isChamp()){
                    quasiCollision(i, j);
                }
            }
        }

        if(Config.printBulletCoords) {
            for (int i = 0; i < fIdArr.size(); i++) {
                int fId = fIdArr.get(i);
                if (quasiBullets.get(fId) == null) continue;
                System.out.println(numRuns + " ID: " + fId + " X: " + quasiBullets.get(fId).getCenterX() + " Y: " + quasiBullets.get(fId).getCenterY() +
                        " θ: " + angleArr.get(i)[0] + " rotation: " + angleArr.get(i)[1] + " node: " + angleArr.get(i)[2] +
                        " dX: " + quasiDirections.get(fId).getX() + " dY: " + quasiDirections.get(fId).getY());
            }
        }
    }

    /*TODO:
     *  x Fix the ghost player in the corner of the screen - It's player #0
     *  x Figure out why the targets aren't going to the correct position - It's player #0
     *  x Make the network actually load from the exportBrain - It's the stupidity of the Anmay
     */

    /**
     * This creates a new target and updates the necessary fields when a bullet and a target (or its substitute) collide
     * @param i The id of the player that fired the bullet
     * @param j The index of the target within the array
     */
    public void quasiCollision(int i, int j){
        //FIXME: This is doing something weird, the targets aren't adding correctly
        //System.out.println("bullet hit by " + i);
        int x = Config.randInt(-100, 0);
        int y = Math.random() < 0.5 ? 0 : 100;
        quasiEnemies.get(i).get(j).setLocation(x, y);
        quasiEnemies.get(i).sort(Comparator.comparingDouble(Rectangle::getX).reversed());
        numTicksLasted.get(i)[j] = 0;
        quasiBullets.set(i, null);
        quasiDirections.set(i, new Point2D(0, 0));
        //network.getPlayers().get(i).getComponent(PlayerComponent.class).addScore(100);
    }

    /**
     * This updates the substitutes for when an actual target entity is hit by a bullet
     * @param i The id of the player that fired the bullet
     * @param j The index of the target within the array
     * @param x The x-coordinate to set the substitute to
     * @param y They y-coordinate to set the substitute to
     */
    public void quasiCollision(int i, int j, int x, int y){
        //FIXME: This is doing something weird, the targets aren't adding correctly
        quasiEnemies.get(i).get(j).setLocation(x, y);
        quasiEnemies.get(i).sort(Comparator.comparingDouble(Rectangle::getX).reversed());
        for (int k = 0; k < targets.size(); k++) {
            Point p = quasiEnemies.get(i).get(k).getLocation();
            targets.get(k).setX(p.getX());
            targets.get(k).setY(p.getY());
        }
        numTicksLasted.get(i)[j] = 0;
        quasiBullets.set(i, null);
        quasiDirections.set(i, new Point2D(0, 0));
        //network.getPlayers().get(i).getComponent(PlayerComponent.class).addScore(100);
    }

    /**
     * This fires a bullet substitute so that a new bullet entity isn't created for all 1000 players
     * @param player The entity that fired the bullet
     * @param id The id of the player entity
     */
    public static void quasiShoot(Entity player, int id){
        if(quasiBullets.get(id) == null){
            double angle = PlayerComponent.angleToPolarRadians(player.getRotation());
            Point2D currentPosition = player.getCenter().add(new Point2D(-5*Math.cos(angle) - 25, -10*Math.sin(angle)))
                    .add(new Point2D(100*Math.cos(angle), -1*100*Math.sin(angle)));
            //The 250 is the speed of the bullet
            quasiDirections.set(id, new Point2D(250/60.0*Math.cos(angle), -1*250/60.0*Math.sin(angle)));
            quasiBullets.set(id, new Rectangle((int)currentPosition.getX(), (int)currentPosition.getY(), 50, 26));
            if(!fIdArr.contains(id)){
                fIdArr.add(id);
                angleArr.add(new double[]{angle*180.0/Math.PI, player.getRotation(),
                        player.getComponent(PlayerComponent.class).getStudent().getBrain().getNodeById(7).getOutput()});
            }
        }
    }

    /**
     * Setter for firstInit
     * @param firstInit The boolean to set firstInit to
     */
    public static void setFirstInit(boolean firstInit) {
        Game.firstInit = firstInit;
    }

    @Override
    protected void initGame(){
        if(firstInit) {
            getGameWorld().addEntityFactory(new GameFactory());
            getGameScene().setBackgroundColor(Color.GREEN);
        }

        if(firstInit) {
            if(targets.size()>0){
                for (Entity target:targets) {
                    target.removeFromWorld();
                }
            }
            targets = new ArrayList<>();
            for (int i = 0; i < Config.numTargets; i++) createTarget();
        }
        switch (Config.mode){
            case("Player") -> player = spawn("Player", new SpawnData(screenWidth/2.0, screenHeight/2.0)
                    .put("mode", 0).put("bads", targets));
            case("Slider") -> player = spawn("Player", new SpawnData(screenWidth/2.0, screenHeight/2.0)
                    .put("mode", 1).put("bads", targets));
            case("Loaded") -> player = spawn("Player", new SpawnData(screenWidth/2.0, screenHeight/2.0)
                    .put("mode", 3).put("bads", targets));
            case("AI") -> {
//                player = spawn("Player", new SpawnData(screenWidth/2.0, screenHeight/2.0)
//                        .put("mode", 2).put("bads", targets));
//                player.getComponent(PlayerComponent.class).setStudent(new Student(targets, 0, false, player, true));
//                Brain brain = player.getComponent(PlayerComponent.class).getStudent().getBrain();
//                brain.saveNetwork();
//                brain.loadNetwork(new ArrayList<>());
            }
        }
        if(player != null) {
            player.setX(screenWidth / 2.0 - player.getWidth() / 2.0);
            player.setY(screenHeight - player.getHeight());
            TargetComponent.setCurrentPlayer(player);
        }

        values.clear();
        values.add(new ArrayList<>());
        values.add(new ArrayList<>());

        if(firstInit) {
            spawn("Background");
            numRuns = 0;
            initSimEnemies();
            firstInit = false;
        }
    }

    /* TODO:
     *  x Fix the crossover bug that doesn't transfer over all nodes
     *  x Have the UI display the current nodes using tableviews
     *  x Try and make a button to turn the UI on and off using the Game UI Controller Class
     *  no it's just stupid Might be cool to have a click function so you click on a node to see what its value is, etc.
     *  Make sure that it actually works like it's supposed to
     *  x Save the best network and change the main menu
     */

    @Override
    public void onUpdate(double tpf){
        super.onUpdate(tpf);
        numRuns++;
        if(Config.mode.equals("AI")) player = null;
        if(player != null){
            //we have a cool exclusive player that we need to focus on
            player.getComponent(PlayerComponent.class).moveOnUpdate();
            GameUIMenu.label1.setText("Score: " + numRuns);
            if(!(Config.mode.equals("Player") || Config.mode.equals("Slider"))){
                drawNetwork(player.getComponent(PlayerComponent.class).getBrain());
            }
            if(player.getComponent(PlayerComponent.class).isDead()){
                Canvas c = GameUIMenu.backgroundCanvas;
                boolean firstTime = !c.isVisible();
                c.setVisible(true);
                GraphicsContext gc = c.getGraphicsContext2D();
                gc.clearRect(0, 0, c.getWidth(), c.getHeight());
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, c.getWidth(), c.getHeight());
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(100));
                gc.fillText("End of Simulation", 120, 300);
                if(firstTime){
                    Button b = new Button("Main Menu");
                    firstInit = true;
                    b.setOnMouseClicked(mouseEvent -> getGameController().gotoMainMenu());
                    b.setLayoutX(screenWidth/2.0 - 50);
                    b.setLayoutY(screenHeight - 100);
                    b.setPrefWidth(100);
                    b.setFont(new Font(15));
                    addUINode(b);
                }
            }
            return;
        }
        if(numRuns == 1){
            System.out.println("Started AI");
            overallPopulation = new Overall(targets, generationSize);
            lastHigh = 0;
            lastAvg = 0;
        }
        ArrayList<Boolean> dead = new ArrayList<>();
        ArrayList<Entity> players = overallPopulation.getPlayers();
        players.forEach(player->dead.add(player.getComponent(PlayerComponent.class).isDead()));
        if(numRuns == 2 && Config.printGenerationBrains){
            players.forEach(player -> {
                System.out.println("player " + player.getComponent(PlayerComponent.class).getStudent().getIdNum() + " = " +
                        player.getComponent(PlayerComponent.class).getStudent().getBrain().getOutput());
                player.getComponent(PlayerComponent.class).getStudent().getBrain().printStuff();
            });
        }
        if(dead.contains(false)) {
            //still running
            for (int i = 0; i < players.size(); i++) {
                players.get(i).getComponent(PlayerComponent.class).receiveInfo(targets, quasiSpeeds.get(i),
                        numTicksLasted.get(i), quasiEnemies.get(i));
            }
            for (Entity p:players) {
                p.getComponent(PlayerComponent.class).moveOnUpdate();
                Brain b = p.getComponent(PlayerComponent.class).getStudent().getBrain();
                StringBuilder stringBuilder = new StringBuilder();
                if(Config.printBulletShoot) {
                    for (AINode n : b.getNetwork()) {
                        for (Connection c : n.getOutto()) {
                            if ((/*c.getN2().getId() == 8 ||*/ c.getN2().getId() == 7) && n.getOutput() != 0 && c.isEnabled()) {
                                //jesus mary muhammad and vishnu what is this monstrosity
                                stringBuilder.append(c.getWeight()).append("|").append(n.getId()).append("|")
                                        .append(c.getN2().getId()).append("|").append(n.getInput()).append(" ");
                            }
                        }
                    }
                    if (!stringBuilder.toString().equals("") &&
                            (/*b.getNodeById(8).getOutput() == 0 ||*/ b.getNodeById(7).getOutput() == 0)) {
                        b.printStuff();
                        System.out.println(p.getComponent(PlayerComponent.class).getStudent().getIdNum() + " " +
                                stringBuilder.toString() + b.getNodeById(8).getOutput());
                    }
                }
            }
            //closest one first
            targets.sort(Comparator.comparingDouble(Entity::getX).reversed());
            simulateEnemies();
            showCounter++;
            Student champ = overallPopulation.honorsStudent();
            TargetComponent.setCurrentPlayer(champ.getPlayer());
            //this is useful I guess.
            //But at this rate my performance is O(nO!)
            //if(showCounter > 1 /*&& oldChamp != champ */) {
//            if(!oldChamp.getPlayer().getComponent(PlayerComponent.class).isDead()) {
//                if (targets.size() > 2) {
//                    targets.removeIf(t -> t.getX() > screenWidth);
//                }
//                for (int i = 0; i < Config.numTargets; i++) {
//                    targets.get(i).setX(quasiEnemies.get(champ.getIdNum()).get(i).getX());
//                    targets.get(i).getComponent(TargetComponent.class).setSpeed(quasiSpeeds.get(champ.getIdNum())[i]);
//                }
//                oldChamp = champ;
//                showCounter = 0;
//            }else{
//                oldChamp = network.honorsStudent();
//            }
            //}
            showCounter++;
            scores = overallPopulation.updateScores(1);
            drawNetwork(overallPopulation.honorsStudent().getBrain());
            Label label1 = GameUIMenu.label1;
            label1.setText("Score: " + numRuns +"\nNumber Dead: " + Collections.frequency(dead, true) +
                    "\nLast High: " + lastHigh + "\nLast Avg: " + lastAvg + "\nGen: " + overallPopulation.getGen() +
                    "\nShowing Student #: " + overallPopulation.honorsStudent().getIdNum() +
                    "\nNum Species: " + overallPopulation.speciesSize());
        }else{
            //the generation ended and it's time to make a new one
            lastHigh = scores.get(0);
            lastAvg = 0;
            for (int i = 1; i < scores.size(); i++){
                lastAvg += scores.get(i);
                if (scores.get(i) > lastHigh) lastHigh = scores.get(i);
            }
            lastAvg /= scores.size();
            lastAvg = Config.trimDouble(lastAvg, 3);
            System.out.println(lastHigh + " " + lastAvg + " " + scores.size() + " " + scores);
            System.out.println();
            values.get(0).add(lastHigh);
            values.get(1).add(lastAvg);
            seriesArrayList.get(0).getData().add(new XYChart.Data<>(overallPopulation.getGen(), lastHigh));
            seriesArrayList.get(1).getData().add(new XYChart.Data<>(overallPopulation.getGen(), lastAvg));
            for (XYChart.Series<Number, Number> s:seriesArrayList) {
                if(!GameUIMenu.chart1.getData().contains(s)) GameUIMenu.chart1.getData().add(s);
            }
            overallPopulation.magic();
            overallPopulation.parsePopulation(players);
            players.forEach(player->{
                player.getComponent(PlayerComponent.class).setDead(false);
                player.getComponent(PlayerComponent.class).getStudent().setScore(0);
            });
            numRuns = 1;
            initGame();
        }
    }

    /**
     * Updates the display within the UI based on the network passed into it
     * @param brain The network to display the information of
     */
    public void drawNetwork(Brain brain){
        //this is a cool thing to do if you want to see things.
        //But the weight changes are so small you can never really see them.
        //This updates the circle graph on the left
        GraphicsContext gc = GameUIMenu.canvas1.getGraphicsContext2D();
        gc.clearRect(0, 0, GameUIMenu.canvas1.getWidth(), GameUIMenu.canvas1.getHeight());
        ArrayList<ArrayList<AINode>> networkSplit = new ArrayList<>();
        for (int i = 0; i < brain.getLayers(); i++) networkSplit.add(new ArrayList<>());
        for (int i = 0; i < brain.getLayers(); i++) {
            for (AINode n:brain.getNodes()) {
                if(n.getLayer()==i) networkSplit.get(i).add(n);
            }
        }
        ArrayList<Point2D> nodePositions = new ArrayList<>();
        ArrayList<Integer> nodeIds = new ArrayList<>();
        for (int i = 0; i < networkSplit.size(); i++) {
            for (int j = 0; j < networkSplit.get(i).size(); j++) {
                nodePositions.add(new Point2D(25 + i*75, 25 + j*25));
                nodeIds.add(networkSplit.get(i).get(j).getId());
            }
        }
        int r = 20;
        for (Connection c:brain.getConnections()) {
            Point2D from = nodePositions.get(nodeIds.indexOf(c.getN1().getId()));
            Point2D to = nodePositions.get(nodeIds.indexOf(c.getN2().getId()));
            Color stroke;
            if(c.isEnabled()){
                //Positive weight is green and negative weight is blue
                stroke = c.getWeight()>=0?Color.LIME:Color.BLUE;
            }else {
                //Disabled is red
                stroke = Color.RED;
            }
            gc.setStroke(stroke);
            gc.setLineWidth(Math.abs(c.getWeight()*2));
            gc.strokeLine(from.getX() + r/2.0, from.getY() + r/2.0, to.getX() + r/2.0, to.getY() + r/2.0);
        }
        gc.setStroke(Color.BLACK);
        for (int i = 0; i < nodePositions.size(); i++) {
            if(brain.getNodes().get(i).getId() == -100){
                //Bias is orange
                gc.setFill(Color.ORANGE);
            }else if(brain.getNetwork().get(i).getLayer() >= brain.getLayers() - 1){
                //Output is any color, warm colors are positive and cool colors are negative
                //Source: https://stackoverflow.com/questions/44326765/color-mapping-for-specific-range
                //It essentially maps the output to a color between the two provided using hsb instead of rgb format
                double value = brain.getNodes().get(i).getOutput();
                double hue = value*120 + (1-value)*0;
                gc.setFill(Color.hsb(hue, 0.75, 1));
            }else{
                //Inputs and hidden layer are white
                gc.setFill(Color.WHITE);
            }
            gc.fillOval(nodePositions.get(i).getX(), nodePositions.get(i).getY(), r, r);
        }
        gc.setFill(Color.BLACK);
        //This updates the list view based on the values provided
        GameUIMenu.list1.getItems().clear();
        GameUIMenu.list1.getItems().add("Angle: " + Config.trimDouble(brain.getOutput(), 4));
        GameUIMenu.list1.getItems().add("Bullet: " + Config.trimDouble(brain.shouldShoot(), 4));
        for (int i = 0; i < brain.getNetwork().size() - 2; i++) {
            int id = brain.getNodes().get(i).getId();
            String name = switch (id){
                case(-100) -> "Bias: "; //these are pretty self explanatory
                case(0) -> "T1 X: ";
                case(1) -> "T1 Time: ";
                case(2) -> "T1 Speed: ";
                case(3) -> "T2 X: ";
                case(4) -> "T2 Time: ";
                case(5) -> "T2 Speed: ";
                case(6) -> "Cool Time: ";
                default -> "Node " + id + ": ";
            };
            GameUIMenu.list1.getItems().add(name + Config.trimDouble(brain.getNodes().get(i).getOutput(), 4));
        }
    }

    @Override
    protected void initInput(){
        getInput().addAction(new UserAction("Shoot Mouse") {
            @Override
            protected void onActionBegin() {
                super.onActionBegin();
                if(player != null) player.getComponent(PlayerComponent.class).shoot();
            }
        }, MouseButton.PRIMARY);
        getInput().addAction(new UserAction("Shoot Mouse Keypad") {
            @Override
            protected void onActionBegin() {
                super.onActionBegin();
                if(player != null) player.getComponent(PlayerComponent.class).shoot();
            }
        }, KeyCode.ENTER);
    }

    @Override
    protected void initPhysics(){
        PhysicsWorld physics = getPhysicsWorld();
        physics.addCollisionHandler(new CollisionHandler(Type.BULLET, Type.ENEMY) {
            @Override
            protected void onCollisionBegin(Entity a, Entity b) {
                super.onCollisionBegin(a, b);
                a.removeFromWorld();
                if (player == null) {
                    b.getComponent(TargetComponent.class).setNumTicksLasted(0);
                    int prevId = targets.indexOf(b);
                    targets.remove(b);
                    b.removeFromWorld();
                    int id = createTarget();
                    //FIXME: There's something going on here that I don't understand
                    quasiCollision(a.getComponent(BulletComponent.class).getAttachment().getComponent(PlayerComponent.class)
                                    .getStudent().getIdNum(), prevId, (int) targets.get(id).getX(), (int) targets.get(id).getY());
                }else{
                    removeTarget(b);
                    b.removeFromWorld();
                    createTarget();
                }
            }
        });
        physics.addCollisionHandler(new CollisionHandler(Type.ENEMY, Type.ENEMY) {
            @Override
            protected void onCollisionBegin(Entity a, Entity b) {
                super.onCollisionBegin(a, b);
                if(a.getX() >= b.getX()) {
                    b.setX(a.getX() - b.getWidth() - 5);
                    b.getComponent(TargetComponent.class).setSpeed(a.getComponent(TargetComponent.class).getSpeed());
                }else{
                    a.setX(b.getX() - a.getWidth() - 5);
                    a.getComponent(TargetComponent.class).setSpeed(b.getComponent(TargetComponent.class).getSpeed());
                }
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
