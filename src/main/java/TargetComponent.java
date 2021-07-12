import com.almasb.fxgl.dsl.components.ExpireCleanComponent;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.entity.components.BoundingBoxComponent;

import static com.almasb.fxgl.dsl.FXGL.*;

public class TargetComponent extends Component {
    private BoundingBoxComponent bbox;
    private boolean oneTargetMade = false;
    private long numTicksLasted = 0;
    private int speed;
    private static Entity currentPlayer;

    /*
     * I've been thinking that it needs a sense of time since everything moves at a speed
     * Basically, you need to base the input nodes on what you would normally use
     * When aiming the gun by yourself, you use:
     *  The current x-coordinate
     *  The time that it would take for your bullet to hit
     *  Probably some other stuff that I can't think of right now
     *  I might pass in the velocity if I do something interesting
     * But essentially I just need to pass in those kinds of things.
     * Now, think of it like a neural network would
     * They see the x coordinate and the time passed, and it forces them to bias themselves
     * The x coordinate would make it point at the target and the time passed would make it lead the target
     * I'm also thinking of adding something else, like the score that each target is worth or whatever
     * These extra things would make it a lot easier and a lot more fun to do things, right?
     * But then the AI is just going to ignore it and I'll be sad.
     * I'm also thinking about putting in other stuff, like a second output node for the click
     * I mean, it would add a lot to the timing section of it.
     * I could put in like how long you need until it can activate again, etc. so that it can fire at the right time
     * Or I could just have it fire every second, but that feels kind of boring
     * I'm kind of in over my head right now, but I need to have at least 2 inputs per target or I'm a failure
     * It's just not complex enough if I only have 2 input nodes
     * But I don't want it to take hours upon hours
     * Plus I want it to actually work like normal without screwing up so this is kind of important
     */

    /**
     * The component of the enemy entities
     * @param speed The speed of the target
     */
    public TargetComponent(int speed){
        this.speed = speed;
    }

    public long getNumTicksLasted() {
        return numTicksLasted;
    }

    //They're all going to have the same player so this actually makes it easier/better
    //Since I only have to do 1 command instead of a for each loop
    public static void setCurrentPlayer(Entity c){
        currentPlayer = c;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed){
        this.speed = speed;
    }

    public void setNumTicksLasted(long numTicksLasted) {
        this.numTicksLasted = numTicksLasted;
    }

    /**
     * If trying to display a single network/player, move the target every tick and destroy it if it reaches the end
     * @param tpf The time per frame
     */
    @Override
    public void onUpdate(double tpf){
        numTicksLasted++;
        boolean hasPlayer = Config.mode.equals("Player") || Config.mode.equals("Slider") || Config.mode.equals("Loaded");
        if(hasPlayer){
            entity.translateX(speed*60*tpf);
            if (bbox.getMaxXWorld() > getAppWidth() || bbox.getMinYWorld() < 0 || bbox.getMaxYWorld() > getAppHeight()) {
                entity.getComponent(ExpireCleanComponent.class).resume();
                if(!oneTargetMade) {
                    numTicksLasted = 0;
                    Game.removeTarget(entity);
                    Game.createTarget();
                    currentPlayer.getComponent(PlayerComponent.class).setDead(true);
                    oneTargetMade = true;
                }
            }
        }
    }
}
