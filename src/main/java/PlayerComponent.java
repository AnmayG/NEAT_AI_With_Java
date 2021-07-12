import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.time.LocalTimer;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static com.almasb.fxgl.dsl.FXGLForKtKt.*;

public class PlayerComponent extends Component {
    private Point2D center = null;
    private final LocalTimer weaponTimer = FXGL.newLocalTimer();
    private final int mode;
    private Student student;
    private ArrayList<Entity> bads;
    private Entity bullet;
    private boolean dead = false;
    private Brain brain;
    private int countdown = 0;
    private int[] enemySpeeds = new int[2];
    private int[] enemyTime = new int[2];
    private ArrayList<Rectangle> enemies = new ArrayList<>(Arrays.asList(new Rectangle(0, 0, 0, 0),
            new Rectangle(0, 0, 0, 0)));

    /**
     * This is the component within every player entity
     * @param mode The mode that the player is in. 0 is manual, 1 is slider-control, 2 is AI control, and 3 is network display
     * @param bads The entities that the player should regard as enemies
     */
    public PlayerComponent(int mode, ArrayList<Entity> bads){
        this.mode = mode;
        this.bads = bads;
    }

    public void setStudent(Student student){
        this.student = student;
    }

    public Student getStudent() {
        return student;
    }

    /**
     * Centers and rotates the player
     */
    @Override
    public void onAdded(){
        //this.center = entity.getCenter();
        this.center = new Point2D(400, 550);
        //needs to at least be pointing straight up at the beginning
        entity.setRotation(270.0);
        if(Config.mode.equals("Loaded")){
            this.brain = new Brain(3*bads.size(), 2, false);
            this.brain.loadNetwork(new ArrayList<>(), false);
        }
    }

    /**
     * Converts FXGL rotations into radians according to a polar coordinate system
     * @param angle The rotation angle
     * @return The rotation angle in radians
     */
    //stolen from previous project
    public static double angleToPolarRadians(double angle){
        //what the heckity heck is the rotation system anyways
        //the same number can mean two different angles and its stupidly difficult to actually put into the polar system
        //please, just make your program follow polar coordinates for once in your life
        //it's literally what you're looking for and allows you to do actual math
        //we use cartesian coordinates anyway, don't make it so that using Math.abs actually has to make sense
        //and the Math library uses radians anyways, so just base rotations on the polar system
        //on another note i'm really starting to like these types of conditionals. so smooth, so easy to write.
        double output = angle<0?Math.abs(angle%360):360-Math.abs(angle%360);
        return output*Math.PI/180.0;
    }

    /**
     * Turns the entity based on whether this is an AI or not
     * @param movement The target angle
     * @param isAI Whether the movement originates from a neural network or not
     * @return The movement angle
     */
    public double move(double movement, boolean isAI){
        double angle;
        if(isAI) {
            return 90*(movement + 1)+180;
        }else{
            Point2D temp = getInput().getMousePositionWorld().subtract(entity.getCenter());
            temp = new Point2D(temp.getX(), -1*temp.getY());
            angle = Math.atan2(temp.getY(), temp.getX());
            if(angle<0) angle+=Math.PI*2;
            return -1*angle*180.0/Math.PI;
        }
    }

    public int getMode(){
        return this.mode;
    }

    public int getCountdown(){
        return countdown;
    }

    public Entity getBullet() {
        return this.bullet;
    }

    public Brain getBrain() {
        return brain;
    }

    /**
     * Shoots the gun by creating a bullet entity
     */
    public void shoot(){
        if(weaponTimer.elapsed(Duration.seconds(1))) {
            double angle = angleToPolarRadians(entity.getRotation());
            Point2D currentPosition = entity.getCenter().add(new Point2D(-5*Math.cos(angle) - 25, -10*Math.sin(angle)))
                    .add(new Point2D(100*Math.cos(angle), -1*100*Math.sin(angle)));
            int hypotenuse = 250;
            Point2D vectorToMouse = new Point2D(hypotenuse*Math.cos(angle), -1*hypotenuse*Math.sin(angle));
            bullet = spawn("Bullet", new SpawnData(currentPosition.getX(), currentPosition.getY())
                    .put("direction", vectorToMouse).put("attachment", entity));
            weaponTimer.capture();
        }
    }

    /**
     * Receives information from another class
     * @param targets The enemy entities
     * @param speeds The speed of the enemy entities
     * @param time The time that each enemy has been alive
     * @param enemies The enemy entity substitute rectangles
     */
    public void receiveInfo(ArrayList<Entity> targets, int[] speeds, int[] time, ArrayList<Rectangle> enemies){
        this.bads = targets;
        this.enemySpeeds = speeds;
        this.enemyTime = time;
        this.enemies = enemies;
    }

    /**
     * Moves the player based on either the brain that it's connected to or the other inputs.
     */
    public void moveOnUpdate(){
        double newRot = angleToPolarRadians(entity.getRotation());
        int xDist = 35;
        entity.setPosition(this.center.add(new Point2D(xDist*Math.cos(newRot), -1*xDist*Math.sin(newRot))));
        if(mode == 3){
            double move = move(brain.move(entity, bads), true);
            entity.setRotation(move);
            if(brain.shouldShoot() > 0){
                shoot();
            }
            countdown++;
            if(countdown >= 65) countdown = 0;
            return;
        }
        if(student == null && mode == 2) return;
        if(student != null){
            entity.getViewComponent().setVisible(!dead);
            if (student.isChamp()) {
                entity.setOpacity(1);
                entity.getViewComponent().setZIndex(1001);
            } else {
                entity.setOpacity(0.075);
                entity.getViewComponent().setZIndex(1000);
            }
        }
        double move = switch (mode) {
            case 0 -> move(0, false);
            case 1 -> 90*(GameUIController.getSlider1Value() + 1)+180;
            case 2 -> move(student.move(bads, enemies, enemyTime, enemySpeeds), true);
            default -> 0;
        };
        if(mode==2){
            //negative values are a thing now
            if(student.getBrain().shouldShoot() > 0){
                if(student.isChamp()){
                    shoot();
                }else{
                    Game.quasiShoot(entity, student.getIdNum());
                }
            }
        }
        entity.setRotation(move);
        countdown++;
        if(countdown >= 65) countdown = 0;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }
}
