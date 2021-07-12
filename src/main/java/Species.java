import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Species {
    private ArrayList<Student> students;
    private double bestScore = 0;
    private Student champ;
    private double average = 0;
    private int noChange = 0;
    private Brain bestBrain;
    private final int excessThreshold;
    private final double weightThreshold;
    private final int differenceThreshold;

    /**
     * Separates networks into species in order to more effectively evolve them
     * @param firstPlayer The first player in the species
     */
    public Species(Student firstPlayer){
        this.students = new ArrayList<>(Collections.singleton(firstPlayer));
        this.champ = firstPlayer.duplicate(firstPlayer.getPlayer());
        this.bestBrain = champ.getBrain().duplicate();
        this.bestBrain.generateNetwork();
        //Change these in order to screw with the contents, I just wanted to look cool
        excessThreshold = 1;
        weightThreshold = 0.5;
        differenceThreshold = 3;
    }

    public void addStudent(Student s){
        this.students.add(s);
    }

    /**
     * Checks if a brain could be contained within this species
     * @param brain The brain to check
     * @return Whether the brain is part of this species or not
     */
    public boolean sameSpecies(Brain brain){
        double changed = this.extraConnections(brain);
        double weightDifference = this.weightConnections(brain);
        //I started getting really complicated networks and like 400 species which isn't the best idea
        //So this just counters that (massive networks are too complicated though)
        double networkSize = brain.getConnections().size() - 20;
        if(networkSize < 1) networkSize = 1;
        //This is just multiplying the change by how much I actually care about them
        double networkDifference = (excessThreshold * changed / networkSize) + (weightThreshold * weightDifference);
        return differenceThreshold > networkDifference;
    }

    /**
     * Determines how widely the weights vary between the brain and the model of this species
     * @param brain The brain to check
     * @return A decimal representing how widely the weights vary
     */
    public double weightConnections(Brain brain){
        if(brain.getConnections().size()==0 || this.bestBrain.getConnections().size() == 0) return 0;
        double m = 0;
        double td = 0;
        for (Connection c:brain.getConnections()) {
            for (Connection c2:this.bestBrain.getConnections()) {
                if(c.getInno() == c2.getInno()){
                    m++;
                    td += Math.abs(c.getWeight() - c2.getWeight());
                    break;
                }
            }
        }
        if(m==0)m=100;
        return td/m;
    }

    /**
     * Determines how many extra connections the brain has compared to the species model
     * @param brain The brain to check
     * @return The number of excess connections
     */
    public double extraConnections(Brain brain){
        double m = 0;
        for (Connection c:brain.getConnections()) {
            for (Connection c2:this.bestBrain.getConnections()) {
                if(c.getInno() == c2.getInno()){
                    m++;
                    break;
                }
            }
        }
        return brain.getConnections().size() + this.bestBrain.getConnections().size() - 2*m;
    }

    /**
     * Sorts the students from the highest to lowest score and determines if the species is improving or not
     */
    public void sort(){
        //highest to lowest
        this.students.sort(Comparator.comparingDouble(Student::getScore).reversed());
        if(this.students.size() == 0){
            this.noChange = Integer.MAX_VALUE;
            return;
        }
        if(this.students.get(0).getScore() > this.bestScore){
            this.noChange = 0;
            this.bestScore = this.students.get(0).getScore();
            this.bestBrain = this.students.get(0).getBrain().duplicate();
            this.champ = this.students.get(0).duplicate(this.students.get(0).getPlayer());
        }else{
            this.noChange++;
        }
    }

    /**
     * Determines the average score of the students
     */
    public void setAverage(){
        this.average = (this.students.stream().mapToDouble(Student::getScore).sum())/this.students.size();
    }

    /**
     * Creates a new student as part of 2 students within this species
     * @param innovationHistory The list of connection markers used by the program
     * @return The new student
     */
    public Student newStudent(ArrayList<Conh> innovationHistory){
        Student child;
        if(Math.random()<=0.25){
            child = this.chooseStudent();
            if(child == null) System.out.println("the f");
            assert child != null;
            child = child.duplicate(child.getPlayer());
        }else{
            Student player1 = this.chooseStudent();
            Student player2 = this.chooseStudent();
            if(player1 == null || player2 == null) System.out.println("the f");
            assert player1 != null;
            assert player2 != null;
            if(player1.getScore() < player2.getScore()){
                child = player2.crossover(player1, player1.getIdNum(), player1.getBads(), player1.getPlayer());
            }else{
                child = player1.crossover(player2, player2.getIdNum(), player2.getBads(), player2.getPlayer());
            }
        }
        child.getBrain().mutate(innovationHistory);
        return child;
    }

    /**
     * Chooses a student based on whether its score exceeds the threshold or not
     * @return The student
     */
    public Student chooseStudent(){
        double totalScore = 0;
        for (Student student:students) totalScore += student.getScore();
        double threshold = Math.random()*totalScore;
        double currentnum = 0;
        for (Student s:this.students) {
            currentnum += s.getScore();
            if(currentnum > threshold) return s;
        }
        return null;
    }

    /**
     * Eliminates all students below the halfway point
     */
    public void eliminate(){
        if(this.students.size() > 2){
            int t = (int)Math.floor(this.students.size()/2.0);
            while(t < this.students.size()){
                this.students.remove(t);
            }
        }
    }

    /**
     * Distributes the scores throughout the species in order to improve the chance of good genes passing on
     */
    public void shareScore(){
        for (Student s:this.students) {
            s.setScore(s.getScore()/((double)this.students.size()));
        }
    }

    public Student getChamp() {
        return champ;
    }

    public double getBestScore(){
        return this.bestScore;
    }

    public void setStudents(ArrayList<Student> students) {
        this.students = students;
    }

    public int getNoChange() {
        return noChange;
    }

    public void setNoChange(int nochange){
        this.noChange = nochange;
    }

    public double getAverage() {
        return average;
    }

    public ArrayList<Student> getStudents() {
        return this.students;
    }
}
