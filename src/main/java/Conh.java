import java.util.ArrayList;

public class Conh {
    private final int n1;
    private final int n2;
    private final int inno;
    private final ArrayList<Integer> innoNums;

    /**
     * A connection record including its innovation number, the nodes it's connected to, and a list of past connections
     * @param from The node the connection connects from
     * @param to The node the connection connects to
     * @param inno The innovation number of the connection
     * @param innoNums A list of past connections and their innovation numbers
     */
    public Conh(int from, int to, int inno, ArrayList<Integer> innoNums){
        //This was actually in the initial NEAT study paper, where it basically records new connections by the
        //amount that they "innovate" by. So new connections get a new innovation number (basically an id) and
        //something very innovative would be moved to the top.
        this.n1 = from;
        this.n2 = to;
        this.inno = inno;
        this.innoNums = new ArrayList<>(innoNums);
    }

    public int getInno() {
        return inno;
    }

    /**
     * Returns whether a brain has a connection that matches with this connection record
     * @param student The brain to check
     * @param node1 The node that the brain's connection connects from
     * @param node2 The node that the brain's connection connects to
     * @return Whether the connection matches this connection history or not
     */
    public boolean match(Brain student, AINode node1, AINode node2){
        if(student.getConnections().size() == innoNums.size()){
            if(n1 == node1.getId() && n2 == node2.getId()){
                for (Connection c:student.getConnections()) {
                    if(!innoNums.contains(c.getInno())) return false;
                }
                return true;
            }
        }
        return false;
    }
}
