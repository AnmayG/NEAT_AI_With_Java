import java.util.Random;

public class Connection {
    private boolean enabled = true;
    private final AINode n1;
    private final AINode n2;
    private double weight = 1.0;
    private final int inno;
    private final Random r = new Random();

    /**
     * Connects two nodes together with a weight
     * @param n1 The node to connect from
     * @param n2 The node to connect to
     * @param w The weight of the connection
     * @param inno The innovation number/id number of this connection
     */
    public Connection(AINode n1, AINode n2, double w, int inno){
        this.n1 = n1;
        this.n2 = n2;
        this.weight = w;
        this.inno = inno;
    }

    public double getWeight() {
        return weight;
    }
    public AINode getN1() {
        return n1;
    }
    public AINode getN2() {
        return n2;
    }
    public boolean isEnabled(){
        return this.enabled;
    }
    public int getInno() {
        return inno;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Mutates this connection by randomly changing the weight or changing it by a slight amount
     */
    public void mutate(){
        if(Math.random() < 0.1){
            this.weight = -1 + Math.random()*2;
        }else{
            //It's kind of annoying that r.nextGaussian() generates a random number from -infinity to +infinity
            //But this is what the study says and it makes sense.
            this.weight += r.nextGaussian()/50;
            if(this.weight > 1){
                this.weight = 1;
            }else if(this.weight < -1){
                this.weight = -1;
            }
        }
    }

    /**
     * Duplicates the connection with 2 different nodes
     * @param n1 The new node to connect from
     * @param n2 The new node to connect to
     * @return The new connection
     */
    public Connection duplicate(AINode n1, AINode n2){
        return new Connection(n1, n2, this.weight, this.inno);
    }
}
