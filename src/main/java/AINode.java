import java.util.ArrayList;

public class AINode {
    private final int id;
    private int layer;
    private double input = 0;
    private double output = 0;
    private double lastInput = 0;
    private ArrayList<Connection> outto = new ArrayList<>();

    public AINode(int id, int layer){
        this.id = id;
        this.layer = layer;
    }

    public int getId() {
        return id;
    }
    public ArrayList<Connection> getOutto(){return this.outto;}
    public int getLayer() {
        return layer;
    }
    public double getOutput() {
        return this.output;
    }
    public double getInput() {
        return input;
    }
    public double getLastInput() {
        return lastInput;
    }
    public void setInput(double input) {
        this.input = input;
    }
    public void addInput(double input){
        this.input += input;
    }
    public void setOutput(double output) {
        this.output = output;
    }
    public void setOutto(ArrayList<Connection> outto) {
        this.outto = outto;
    }
    public void setLayer(int l){this.layer = l;}

    /**
     * Calculates the output from the node and passes it to all forward connections
     */
    public void calcOutput(){
        //I may have screwed up by not integrating the bias node correctly but it should be fine
        //I mean, I'm adding it in the end anyways so...
        this.output = this.input;
        //I'm using a tanh function as my activation function because there's already a Math. function for it
        //Plus because it's centered between -1 and 1 I can have a negative correlation
        //That's important because I might want to wait until a certain time or something before I fire
        //Like I'll have to wait until the cool-down ends in order to fire
        //That kind of stuff needs a negative output.
        //As for why you need an activation function, it's because not all patterns are linear.
        //For something like "How much does this house cost based on its age and location?" it's mostly a linear problem
        //The older the house, the cheaper. The more out-of-the-way, the cheaper.
        //But the thing is, that's not the exact pattern.
        //An older house could be more expensive because it's an antique, etc.
        //Because of this, a network with a bad activation function can never figure out the true relation.
        //Think of it like the difference between the line of best fit and the actual graph
        //Non-linear activation functions allow the function to find the actual pattern
        //For a better explanation go here:
        //https://towardsdatascience.com/everything-you-need-to-know-about-activation-functions-in-deep-learning-models-84ba9f82c253
        if(this.layer!=0) this.output = Math.tanh(this.input);
        this.lastInput = this.input;
        for (Connection c:this.outto) {
            if(c.isEnabled()) c.getN2().addInput(c.getWeight() * this.output);
        }
    }

    /**
     * Returns if this node is connected to the node passed in
     * @param n2 The node passed in
     * @return Whether these two nodes are connected or not
     */
    public boolean isConnectedTo(AINode n2){
        for (Connection c:outto) {
            if(c.getN2().equals(n2)) return true;
        }
        return false;
    }

    /**
     * Returns a new node with the same id and layer as this one
     * @return The duplicated node.
     */
    public AINode duplicate(){
        return new AINode(this.id, this.layer);
    }
}
