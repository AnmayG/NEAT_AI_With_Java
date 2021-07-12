import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;

import java.util.ArrayList;
import java.util.Comparator;

import static com.almasb.fxgl.dsl.FXGLForKtKt.spawn;

public class Overall {
    private ArrayList<Student> students = new ArrayList<>();
    private final ArrayList<Conh> innovationHistory = new ArrayList<>();
    private double bestScore = 0;
    private double globalBest = 0;
    private int gen = 1;
    private final ArrayList<Species> species = new ArrayList<>();
    private boolean killThemAll = false;

    /**
     * This is the overall population that will evolve these networks through NEAT
     * @param enemies The list of enemy entities
     * @param generationSize The number of students to create
     */
    public Overall(ArrayList<Entity> enemies, int generationSize){
        //initialize this.students with actual proper values, mutate them, and create their network
        for (int i = 0; i < generationSize; i++) {
            Entity player = spawn("Player", new SpawnData(Game.getScreenWidth()/2.0,
                    Game.getScreenHeight()/2.0).put("mode", 2).put("bads", enemies));
            Student s = new Student(enemies, i, false, player, false);
            s.getBrain().mutate(innovationHistory);
            s.getBrain().generateNetwork();
            students.add(s);
            player.getComponent(PlayerComponent.class).setStudent(s);
        }
    }

    /**
     * Returns the list of players that the students currently have
     * @return The list of players that the students currently have
     */
    public ArrayList<Entity> getPlayers() {
        ArrayList<Entity> players = new ArrayList<>();
        students.forEach(student -> players.add(student.getPlayer()));
        return players;
    }

    /**
     * Updates all player scores to the inputted score
     * @param score The inputted score
     * @return The list of scores from all of the players
     */
    public ArrayList<Double> updateScores(int score){
        this.students.sort(Comparator.comparingDouble(Student::getScore).reversed());
        ArrayList<Double> scores = new ArrayList<>();
        for (Student s:this.students) {
            if(!s.getPlayer().getComponent(PlayerComponent.class).isDead()) s.setScore(s.getScore() + score);
            if (s.getScore() > this.globalBest) this.globalBest = s.getScore();
            scores.add(s.getScore());
        }
        return scores;
    }

    /**
     * Returns the student with the highest fitness
     * @return The student with the highest fitness
     */
    public Student honorsStudent(){
        Student out = this.students.get(0);
        boolean notFoundChamp = true;
        ArrayList<Student> studentArrayList = this.students;
        for (Student s : studentArrayList) {
            if (!s.getPlayer().getComponent(PlayerComponent.class).isDead() && notFoundChamp) {
                s.setChamp(true);
                out = s;
                notFoundChamp = false;
            } else {
                s.setChamp(false);
            }
        }
        return out;
    }

    /**
     * Creates a new generation of students through crossover and speciation
     */
    public void magic(){
        //no joke, this is literally what I named the method last year
        Student previous = this.students.get(0);
        for (Species s2:this.species) s2.getStudents().clear();
        for (Student s:this.students) {
            boolean notFoundSpecies = true;
            for (Species species:this.species) {
                if(species.sameSpecies(s.getBrain())){
                    species.addStudent(s);
                    notFoundSpecies = false;
                    break;
                }
            }
            if(notFoundSpecies) this.species.add(new Species(s));
        }
        this.students.sort(Comparator.comparingDouble(Student::getScore).reversed());
        this.species.forEach(Species::sort);
        this.species.sort(Comparator.comparingDouble(Species::getBestScore).reversed());
        ArrayList<Species> speciesArrayList = this.species;
        for (int i = 0; i < speciesArrayList.size(); i++) {
            System.out.print("Species " + i + " Score: " + speciesArrayList.get(i).getBestScore() + " ");
        }
        System.out.println();

        if(Config.printGenerationScores) {
            System.out.print("bar [");
            for (Species species : this.species)
                for (Student s2 : species.getStudents()) System.out.print(s2.getScore() + ", ");
            System.out.println("]");
            System.out.print("foobar [");
            for (Student student : this.students) System.out.print(student.getScore() + ", ");
            System.out.println("]");
            System.out.print("foo [");
            this.species.forEach(s -> System.out.print(s.getBestScore() + ", "));
            System.out.print("]");
        }

        if(this.killThemAll){
            while(5 < this.species.size()){
                this.species.remove(5);
            }
            this.killThemAll = false;
            System.out.println("Extinction Complete");
        }
        for (Species s:this.species) {
            s.eliminate();
            s.shareScore();
            s.setAverage();
        }

        Student temp = this.species.get(0).getStudents().get(0);
        temp.setGen(this.gen);
        if(temp.getScore() >= this.bestScore){
            //Here it is! The beholden, the stupid, the idiotic, player #0
            //its alive!
            //temp.getPlayer().getComponent(PlayerComponent.class).setDead(false);
            //temp = temp.duplicate(temp.getPlayer());
            System.out.printf("Old Best: %f New Best: %f", this.bestScore, temp.getScore());
            this.bestScore = temp.getScore();
            temp.getBrain().saveNetwork();
        }
        System.out.println();
        int i = 2;
        while(i < this.species.size()){
            if(this.species.get(i).getNoChange() >= 15){
                this.species.remove(i);
                i -= 1;
            }
            i += 1;
        }
        System.out.printf("Generation: %d Number of Changes: %d Number of Species: %d\n",
                this.gen, this.innovationHistory.size(), this.species.size());
        //very convenient and hopefully not slower
        double avgSum = this.species.stream().mapToDouble(Species::getBestScore).sum();
        ArrayList<Student> kids = new ArrayList<>();
        for (Species s:this.species) {
            kids.add(s.getChamp().duplicate(s.getChamp().getPlayer()));
            int studentNumber = (int)(Math.floor(s.getAverage() / avgSum * this.students.size()) - 1);
            for (int j = 0; j < studentNumber; j++) {
                kids.add(s.newStudent(this.innovationHistory));
            }
        }
        if(kids.size() < this.students.size()) kids.add(previous.duplicate(previous.getPlayer()));
        while(kids.size() < this.students.size()) kids.add(this.species.get(0).newStudent(this.innovationHistory));
        this.students = new ArrayList<>(kids);
        this.gen++;
        for (Student s:this.students) {
            s.getBrain().generateNetwork();
        }
    }

    public int speciesSize(){
        return this.species.size();
    }

    public int getGen() {
        return gen;
    }

    /**
     * Assigns every student in this overall population a player entity to control
     * @param players The list of player entities to assign to the students
     */
    public void parsePopulation(ArrayList<Entity> players){
        for (int i = 0; i < players.size(); i++) {
            players.get(i).getComponent(PlayerComponent.class).setStudent(students.get(i));
            students.get(i).setPlayer(players.get(i));
            students.get(i).setIdNum(i);
        }
    }
}
