import com.almasb.fxgl.entity.Entity;
import javafx.geometry.Point2D;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Brain {
    private ArrayList<Connection> connections = new ArrayList<>();
    //don't know what this is
    private ArrayList<AINode> network = new ArrayList<>();
    //know what these are
    private ArrayList<AINode> nodes = new ArrayList<>();
    private final int inputs;
    private final int outputs;
    private double[] output;
    private int layers = 2;
    private int next = 0;
    private static int conval = 1;

    /**
     * Constructor for a new neural network brain
     * @param ins The number of input nodes
     * @param outs The number of output nodes
     * @param cross Whether the brain has been created as the result of a crossover or not
     */
    public Brain(int ins, int outs, boolean cross){
        this.inputs = ins;
        this.outputs = outs;
        this.output = new double[outs];
        if(cross) return;
        for (int i = 0; i < this.inputs; i++) nodes.add(new AINode(i, 0));
        nodes.add(new AINode(-100, 0));
        for (int i = 0; i < this.outputs; i++) {
            nodes.add(new AINode(this.inputs + i, 1));
        }
        this.next = this.inputs + this.outputs;
    }

    public ArrayList<Connection> getConnections() {
        return connections;
    }
    public ArrayList<AINode> getNodes() {
        return nodes;
    }
    public int getLayers() {
        return layers;
    }

    /**
     * Saves the brain to the exportBrain File using syntax that can be read by the loadNetwork function
     */
    public void saveNetwork(){
        try{
            PrintWriter out = new PrintWriter("src/main/resources/assets/exportBrain");
            out.flush();
            for (AINode n:this.network) {
                out.println("Node " + n.getId() + ":" + n.getLayer());
            }
            for (Connection c:this.connections) {
                out.println("Connection " + c.getN1().getId() + ":" + c.getN2().getId() + ":" + c.getWeight() + ";" + c.getInno());
            }
            out.close();
        } catch (FileNotFoundException e) {
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("Something went wrong");
        }
    }

    /**
     * Sets this brain to have the same topology and weights as the one listed in the exportBrain or amazingBrain file
     * @param innovationHistory The list of connection markers to add to
     * @param actualSave Whether the method should load from the exportBrain or amazingBrain file
     */
    public void loadNetwork(ArrayList<Conh> innovationHistory, boolean actualSave){
        try{
            this.reset(2, this.inputs+2);
            this.network = new ArrayList<>();
            FileReader reader = new FileReader(actualSave?"src/main/resources/assets/exportBrain":
                    "src/main/resources/assets/amazingBrain");
            Scanner in = new Scanner(reader);
            while(in.hasNextLine()){
                String temp = in.nextLine();
                int id = Integer.parseInt(temp.substring(temp.indexOf(" ") + 1, temp.indexOf(":")));
                if(temp.contains("Node")){
                    int layer = Integer.parseInt(temp.substring(temp.indexOf(":") + 1));
                    if(layer >= this.layers){
                        this.layers = layer + 1;
                    }
                    this.nodes.add(new AINode(id, layer));
                }else if(temp.contains("Connection")){
                    //I was intelligent and did this correctly
                    AINode n1 = getNodeById(id);
                    AINode n2 = getNodeById(Integer.parseInt(temp.substring(temp.indexOf(":")+1, temp.lastIndexOf(":"))));
                    ArrayList<Integer> innovationNumbers = new ArrayList<>();
                    connections.forEach(c -> innovationNumbers.add(c.getInno()));
                    int newInnoNum = Integer.parseInt(temp.substring(temp.indexOf(";") + 1));
                    innovationHistory.add(new Conh(n1.getId(), n2.getId(), newInnoNum, innovationNumbers));
                    double weight = Double.parseDouble(temp.substring(temp.lastIndexOf(":") + 1, temp.indexOf(";")));
                    this.connections.add(new Connection(n1, n2, weight, newInnoNum));
                }
            }
            this.connectNodes();
            this.generateNetwork();
        }catch (FileNotFoundException e){
            Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, e);
            System.out.println("Something else went wrong");
        }
    }

    /**
     * Returns the node with the id input
     * @param idnum The id to find
     * @return The node with an id of idnum
     */
    public AINode getNodeById(int idnum){
        for (AINode n:nodes) {
            if(n.getId()==idnum) return n;
        }
        return null;
    }

    /**
     * Synchronizes the information between the connections and the nodes so that the nodes know what to output to
     */
    public void connectNodes(){
        for (AINode n:nodes) { n.setOutto(new ArrayList<>()); }
        for (Connection c:this.connections) {
            if(c.getN1()!=null){
                c.getN1().getOutto().add(c);
            }else{
                System.out.println("this should NOT be here");
            }
        }
    }

    /**
     * Maps a value between a range to a value between 0 and 1
     * @param range1 The range that the value is contained in
     * @param value The value to change
     * @return The value mapped to a range of 0 and 1
     */
    public double mapToRange(Point2D range1, double value){
        //Maps to 0 and 1, first constant is the min and second constant is the range (1-0=1)
        return 0 + ((value - range1.getX())*1/(range1.getY()-range1.getX()));
    }

    /**
     * Moves a player based on the substituted rectangles instead of the target component
     * @param player The player to move
     * @param bads The rectangles that correlate to the player
     * @param numTicksLasted The number of ticks that each rectangle has lasted
     * @param speed The speed of each rectangle
     * @return The angle that the player should turn
     */
    public double moveRectangle(Entity player, ArrayList<Rectangle> bads, int[] numTicksLasted, int[] speed){
        ArrayList<Double> inputInfo = new ArrayList<>();
        for (int i = 0; i < bads.size(); i++) {
            inputInfo.add(mapToRange(new Point2D(-100, 1000), bads.get(i).getX()));
            inputInfo.add(mapToRange(new Point2D(0, 212), numTicksLasted[i]));
            inputInfo.add(mapToRange(new Point2D(1, 5), speed[i]));
        }
        inputInfo.add(mapToRange(new Point2D(0, 65), player.getComponent(PlayerComponent.class).getCountdown()));
        return turnPlayer(inputInfo);
    }

    /**
     * Moves a player based on the current target entities and their positions
     * @param player The player to move
     * @param bads The current target entities
     * @return The angle that the player should turn
     * @see TargetComponent
     */
    public double move(Entity player, ArrayList<Entity> bads){
        /*
         *  This is what I'm working on right now, I'm not really sure as to what I should use as the input values yet
         *  I don't want to throw off the network by feeding it useless data, but on the other hand this is meant to be
         *  a filter for useless data and able to choose the best data.
         *  Also it kind of feels like cheating if I give it only the information that it needs, but I don't want to
         *  spend hours per test period.
         *  So I'm kind of just ignoring this and focusing on the other work, like the overall network
         *  and the other structures.
         *  The best solution is always to procrastinate
         *  For more info see TargetComponent
         */
        bads.removeIf(entity -> entity.getComponentOptional(TargetComponent.class).equals(Optional.empty()));
        ArrayList<Double> inputInfo = new ArrayList<>();
        for (Entity e:bads) {
            inputInfo.add(mapToRange(new Point2D(-100, 1000), e.getX()));
            double d = mapToRange(new Point2D(0, 1160), e.getComponent(TargetComponent.class).getNumTicksLasted());
            inputInfo.add(d);
            inputInfo.add(mapToRange(new Point2D(1, 5), e.getComponent(TargetComponent.class).getSpeed()));
        }
        inputInfo.add(mapToRange(new Point2D(0, 65), player.getComponent(PlayerComponent.class).getCountdown()));
        return turnPlayer(inputInfo);
    }

    /**
     * Turns a player by receiving input information and feeding it through the network
     * @param inputInfo The input information that the network should use
     * @return The angle that the player should turn
     */
    public double turnPlayer(ArrayList<Double> inputInfo){
        for (int i = 0; i < inputs; i++){
            this.nodes.get(i).setInput(inputInfo.get(i));
        }
        //bias node needs to be biased, I wonder why
        getNodeById(-100).setInput(1);
        generateNetwork();
        //a wondrously stupid move by the fantastically idiotic Anmay Gupta.
        //Today, Anmay learns that loops count UP not in the random order that Anmay decided for his convenience
        //And it only took him a few hours!
        for (AINode n:this.network){
            n.calcOutput();
        }
        double output = this.network.get(this.network.size()-2).getOutput();
        double output2 = this.network.get(this.network.size()-1).getOutput();
        this.output = new double[]{output, output2};
        for (AINode n:this.network) {
            n.setInput(0);
        }
        return output;
    }

    public double getOutput() {
        return output[0];
    }

    public double shouldShoot(){
        return output[1];
    }

    public ArrayList<AINode> getNetwork(){
        return this.network;
    }

    /**
     * Organizes the nodes by layer and id into the network array list
     */
    public void generateNetwork(){
        this.connectNodes();
        this.network = new ArrayList<>();
        for (int i = 0; i < this.layers; i++) {
            for (AINode n:this.nodes) {
                if(n.getLayer()==i) this.network.add(n);
            }
        }
    }

    /**
     * Connects all of the nodes into a basic neural network topology
     * @param innovationHistory The list of connection markers within the population
     */
    public void completeNetwork(ArrayList<Conh> innovationHistory){
        for (AINode n1:network) {
            for (AINode n2:network) {
                if (n2.getLayer() > n1.getLayer()) {
                    ArrayList<Integer> innovationNumbers = new ArrayList<>();
                    connections.forEach(c -> innovationNumbers.add(c.getInno()));
                    innovationHistory.add(new Conh(n1.getId(), n2.getId(), conval, innovationNumbers));
                    this.connections.add(new Connection(n1, n2, 1, conval));
                    conval++;
                }
            }
        }
        this.connectNodes();
    }

    /*TO DO:
     *  A deer a female deer
     *  Re, a drop of golden sun
     *  Mi, a name, I call myself,
     *  Fa, a long long way to run
     *  So, a needle pulling thread
     *  La, a note to follow so
     *  Ti, a drink with jam and bread
     *  And I'm slowly going insane bum bum bum
     *  x Add a bias node to the inputs and allow it to connect with anything
     */

    /**
     * Adds a random node to the network
     * @param innovationHistory The list of connection markers within the population
     */
    public void addNode(ArrayList<Conh> innovationHistory){
        if(this.connections.size()==0){
            this.addConnection(innovationHistory);
            return;
        }
        int randomConnectionId = Config.randInt(0, this.connections.size() - 1);
        Connection randomConnection = this.connections.get(randomConnectionId);
        randomConnection.setEnabled(false);
        int newNodeId = this.next;
        AINode newNode = new AINode(newNodeId, this.connections.get(randomConnectionId).getN1().getLayer()+1);
        this.nodes.add(newNode);
        this.next += 1;
        int newConnectionNum = this.getInnovationNumber(innovationHistory, randomConnection.getN1(), newNode);
        this.connections.add(new Connection(randomConnection.getN1(), newNode, 1, newConnectionNum));
        newConnectionNum = this.getInnovationNumber(innovationHistory, newNode, randomConnection.getN2());
        this.connections.add(new Connection(newNode, randomConnection.getN2(), randomConnection.getWeight(), newConnectionNum));
        if(newNode.getLayer()==randomConnection.getN2().getLayer()){
            for (AINode n:nodes) {
                if(n.getLayer() >= newNode.getLayer() && n != newNode) n.setLayer(n.getLayer()+1);
            }
            this.layers++;
        }
        this.connectNodes();
    }

    /**
     * Adds a random connection to the network
     * @param innovationHistory The list of connection markers within the population
     */
    public void addConnection(ArrayList<Conh> innovationHistory){
        if(this.isFull()) return;
        //nodes.size()-1 so not inclusive and -2 for bias node
        AINode randNode1 = nodes.get(Config.randInt(0, nodes.size()-1));
        AINode randNode2 = nodes.get(Config.randInt(0, nodes.size()-1));
        while(randNode1.getLayer() == randNode2.getLayer() || randNode1.isConnectedTo(randNode2) ||
                randNode2.isConnectedTo(randNode1)){
            randNode1 = nodes.get(Config.randInt(0, nodes.size()-1));
            randNode2 = nodes.get(Config.randInt(0, nodes.size()-1));
        }
        if(randNode1.getLayer()>randNode2.getLayer()){
            AINode temp = randNode2;
            randNode2 = randNode1;
            randNode1 = temp;
        }
        int newConnectionInnoNum = this.getInnovationNumber(innovationHistory, randNode1, randNode2);
        connections.add(new Connection(randNode1, randNode2, -1+(Math.random()*2), newConnectionInnoNum));
        this.connectNodes();
    }

    /**
     * Returns the innovation number of a connection between 2 nodes
     * @param innovationHistory The list of connection markers within the population
     * @param n1 The first node in the connection
     * @param n2 The second node in the connection
     * @return The innovation number of the connection, if not present then returns the next innovation number
     */
    //Think of innovation numbers as basically ids for connection mutations.
    //This makes more sense as a "find by id" method than anything else
    public int getInnovationNumber(ArrayList<Conh> innovationHistory, AINode n1, AINode n2){
        boolean newThing = true;
        int newConnectionInnoNum = conval;
        for (Conh ch:innovationHistory) {
            if(ch.match(this, n1, n2)){
                newThing = false;
                newConnectionInnoNum = ch.getInno();
                break;
            }
        }
        if(newThing){
            ArrayList<Integer> innoNumbers = new ArrayList<>();
            for (Connection c:this.connections) {
                innoNumbers.add(c.getInno());
            }
            innovationHistory.add(new Conh(n1.getId(), n2.getId(), newConnectionInnoNum, innoNumbers));
            conval++;
        }
        return newConnectionInnoNum;
    }

    /**
     * Returns whether the network can hold more connections or not
     * @return Whether the network can hold more connections or not
     */
    public boolean isFull(){
        int maxConnections = 0;
        int[] nodesInLayers = new int[this.layers];
        for (AINode n:this.nodes) {
            if(n.getLayer() >= nodesInLayers.length) System.out.println(n.getLayer() + " " + nodesInLayers.length);
            nodesInLayers[n.getLayer()]++;
        }
        for (int i = 0; i < this.layers; i++) {
            int nextNodes = 0;
            int j = i+1;
            //was freshman me just dumb or am i dumb
            //because this could be a for loop but its not
            //but i adore for loops so something i don't remember must have made me do this
            while(j < this.layers){
                nextNodes += nodesInLayers[j];
                j += 1;
            }
            maxConnections += nodesInLayers[i]*nextNodes;
        }
        return maxConnections <= this.connections.size();
    }

    /**
     * Mutates the network
     * @param innovationHistory The list of connection markers within the population
     */
    public void mutate(ArrayList<Conh> innovationHistory){
        if(this.connections.size() == 0) this.addConnection(innovationHistory);
        if(Math.random()<0.8) this.connections.forEach(Connection::mutate);
        if(Math.random()<0.05) this.addConnection(innovationHistory);
        if(Math.random()<0.01) this.addNode(innovationHistory);
    }

    /**
     * Resets the network to an empty state
     * @param layers The number of layers the network should have
     * @param next The id of the next node
     */
    public void reset(int layers, int next){
        this.connections = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.layers = layers;
        this.next = next;
    }

    public void setNodes(ArrayList<AINode> nodes) {
        this.nodes = nodes;
    }

    public void setConnections(ArrayList<Connection> connections) {
        this.connections = connections;
    }

    /**
     * Returns a new child brain as a result of the crossover between this brain and parent2
     * @param parent2 The student with the network that will crossover with this one
     * @return The resulting brain of the crossover
     */
    public Brain crossover(Student parent2){
        Brain childBrain = new Brain(this.inputs, this.outputs, true);
        childBrain.reset(this.layers, this.next);
        ArrayList<Connection> kidConnections = new ArrayList<>();
        ArrayList<Boolean> enabledGenes = new ArrayList<>();
        for (Connection c:this.connections) {
            boolean enabled = true;
            int parent2GeneIndex = this.matchGene(parent2, c.getInno());
            if(parent2GeneIndex != -1){
                Connection parent2Gene = parent2.getBrain().getConnections().get(parent2GeneIndex);
                if(!c.isEnabled() || !parent2Gene.isEnabled()) if (Math.random() < 0.75) enabled = false;
                if(Math.random() < 0.5){
                    kidConnections.add(c);
                }else{
                    kidConnections.add(parent2Gene);
                    if(kidConnections.get(kidConnections.size() - 1).getN1().getId() > this.getNodes().size() - 2){
                        System.out.println("i'm going insane and you can't stop me");
                    }
                }
            }else{
                kidConnections.add(c);
                enabled = c.isEnabled();
                if(kidConnections.get(kidConnections.size() - 1).getN1().getId() > this.getNodes().size() - 2){
                    System.out.println("i'm going insane and you can't stop me");
                }
            }
            enabledGenes.add(enabled);
        }
        ArrayList<AINode> newNodes = new ArrayList<>();
        for (AINode n : this.nodes) newNodes.add(n.duplicate());
        childBrain.setNodes(newNodes);

        for (int i = 0; i < kidConnections.size(); i++) {
            Connection c = kidConnections.get(i);
            if (childBrain.getNodeById(c.getN1().getId()) == null) {
                System.out.println("c.getN1().getId() = " + c.getN1().getId() + " " + childBrain.getNodes().size() + " " + parent2.getBrain().getNodes().size());
                this.nodes.forEach(ainode -> System.out.print(ainode.getId() + ", "));
                System.out.print("|");
                childBrain.getNodes().forEach(ainode -> System.out.print(ainode.getId() + ", "));
            }
            if (childBrain.getNodeById(c.getN2().getId()) == null) {
                System.out.println("c.getN2().getId() = " + c.getN2().getId() + " " + childBrain.getNodes().size() + " " + parent2.getBrain().getNodes().size());
                childBrain.getNodes().forEach(ainode -> System.out.print(ainode.getId() + ", "));
            }

            childBrain.getConnections().add(c.duplicate(childBrain.getNodeById(c.getN1().getId()), childBrain.getNodeById(c.getN2().getId())));
            childBrain.getConnections().get(i).setEnabled(enabledGenes.get(i));
            if(childBrain.getConnections().get(i).getN1()==null || childBrain.getConnections().get(i).getN2()==null){
                System.out.println("Error; Please Try Again");
            }
        }
        childBrain.connectNodes();
        childBrain.generateNetwork();
        return childBrain;
    }

    /**
     * Checks if two brains have the same connection by checking if their innovation numbers are equal
     * @param parent2 The student to check the brain of
     * @param innovationNumber The innovation number of this brain's connection
     * @return The index of the connection in parent2's brain
     */
    public int matchGene(Student parent2, int innovationNumber){
        for (int i = 0; i < parent2.getBrain().getConnections().size(); i++) {
            if(parent2.getBrain().getConnections().get(i).getInno() == innovationNumber){
                return i;
            }
        }
        return -1;
    }

    public void setLayers(int layers) {
        this.layers = layers;
    }

    public void setNext(int next) {
        this.next = next;
    }

    /**
     * Duplicates this brain with the same structure and weights
     * @return A duplicate of this brain
     */
    public Brain duplicate(){
        Brain clone = new Brain(this.inputs, this.outputs, true);
        ArrayList<AINode> cloneNodes = new ArrayList<>();
        this.nodes.forEach(n->cloneNodes.add(n.duplicate()));
        clone.setNodes(cloneNodes);
        ArrayList<Connection> newConnections = new ArrayList<>();
        this.connections.forEach(c->newConnections.add(c.duplicate(this.getNodeById(c.getN1().getId()), this.getNodeById(c.getN2().getId()))));
        clone.setConnections(newConnections);
        clone.setLayers(this.layers);
        clone.setNext(this.next);
        return clone;
    }

    /**
     * Prints important information about this network, including its structure and its weights
     */
    public void printStuff(){
        ArrayList<Integer> nodeIds = new ArrayList<>();
        this.network.forEach(n->nodeIds.add(n.getId()));
        System.out.printf("Layers: %d Network: %s Connections: %s\n",
                this.layers, nodeIds.toString(), this.connections.toString());
        for (AINode node:this.network) {
            System.out.printf("Node %d Input: %f Output: %f\n", node.getId(), node.getLastInput(), node.getOutput());
        }
        for (Connection c:this.connections) {
            System.out.printf("Gene: %d From: %d To: %d Enabled %b From Layer: %d To Layer: %d Weight: %f\n",
                    c.getInno(), c.getN1().getId(), c.getN2().getId(), c.isEnabled(), c.getN1().getLayer(),
                    c.getN2().getLayer(), c.getWeight());
        }
        System.out.println();
    }
}
