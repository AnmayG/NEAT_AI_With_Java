import com.almasb.fxgl.entity.Entity;

import java.awt.*;
import java.util.ArrayList;

public class Student {
    private Brain brain;
    private int idNum;
    private int best = 0;
    private Entity player;
    private int gen;
    private double score;
    private final ArrayList<Entity> bads;
    private boolean champ;

    /**
     * This is a bridge between the players and the networks as it acts as an intermediary
     * @param bads The enemy entities
     * @param idnum The id number this student should have
     * @param champ Whether this student is the best overall or not
     * @param player The player entity attached to this student
     * @param completeNetwork Whether the brain should follow the complete topology or not
     */
    public Student(ArrayList<Entity> bads, int idnum, boolean champ, Entity player, boolean completeNetwork){
        this.bads = bads;
        this.champ = champ;
        this.idNum = idnum;
        this.player = player;
        //Each target will have 2 coordinates: their x and the time that each target has existed
        //They will also have a bias node that will just output 1 and the connections can weight it in
        //This is a legitimate tactic and not just me being mad that it's not working
        this.brain = new Brain(3*bads.size() + 1, 2, false);
        this.brain.generateNetwork();
        if(completeNetwork) {
            ArrayList<Conh> innoHis = new ArrayList<>();
            this.brain.completeNetwork(innoHis);
        }
    }

    public void setIdNum(int idNum){
        this.idNum = idNum;
    }

    public void setGen(int gen) {
        this.gen = gen;
    }

    public void setPlayer(Entity player) {
        this.player = player;
    }

    public boolean isChamp() {
        return champ;
    }

    /**
     * Returns the brain's movement based on the input information
     * @param bads The enemy entities
     * @param enemies The enemy entities' substitute rectangles
     * @param numTicks The number of ticks each rectangle has survived
     * @param speed The speed of the substitute rectangle
     * @return The brain's movement
     */
    public double move(ArrayList<Entity> bads, ArrayList<Rectangle> enemies, int[] numTicks, int[] speed){
        if(champ) {
            return brain.move(this.player, bads);
        }else{
            return brain.moveRectangle(this.player, enemies, numTicks, speed);
        }
    }

    /**
     * Generates a new student based on the input information
     * @param parent The other student to crossover with
     * @param idnum The id number to assign the new student
     * @param bads The enemy entities
     * @param newPlayer The player entity to assign the new student
     * @return A new student resulting from the crossover between 2 students
     */
    public Student crossover(Student parent, int idnum, ArrayList<Entity> bads, Entity newPlayer){
        Student kid = new Student(bads, idnum, this.champ, newPlayer, false);
        kid.setBrain(this.brain.crossover(parent));
        kid.getBrain().generateNetwork();
        return kid;
    }

    public ArrayList<Entity> getBads() {
        return bads;
    }

    public int getIdNum() {
        return idNum;
    }

    /**
     * Duplicates the student
     * @param newPlayer The player entity to assign to the new student
     * @return A duplicated student
     */
    public Student duplicate(Entity newPlayer){
        Student clone = new Student(this.bads, this.idNum, false, newPlayer, false);
        clone.setBrain(this.brain.duplicate());
        clone.getBrain().generateNetwork();
        clone.setBest(this.best);
        return clone;
    }

    public Entity getPlayer() {
        return player;
    }

    public void setBest(int best) {
        this.best = best;
    }

    public void setBrain(Brain brain) {
        this.brain = brain;
    }

    public Brain getBrain() {
        return brain;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double size) {
        this.score = size;
    }

    public int getGen() {
        return gen;
    }

    public void setChamp(boolean b){
        this.champ = b;
    }
}
