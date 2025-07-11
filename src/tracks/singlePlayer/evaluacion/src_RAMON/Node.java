package tracks.singlePlayer.evaluacion.src_RAMON;

import java.util.Comparator;
import java.util.Objects;
import java.util.ArrayList;

import tools.Vector2d;
import ontology.Types.ACTIONS;

public class Node implements Comparable<Node> {
    protected int x, y;
    protected int cost;
    protected int heuristic;
    protected Node father;
    protected boolean blueCape, redCape;
    protected ArrayList<Vector2d> blueCapesRemaining;
    protected ArrayList<Vector2d> redCapesRemaining;
    protected ACTIONS lastAction;
    protected int orderOfCreation = 0;
    
    // Constructor with no heuristic
    public Node(int x, int y, int cost, Node father, boolean redCape, boolean blueCape, ArrayList<Vector2d> blueCapesRemaining, ArrayList<Vector2d> redCapesRemaining, ACTIONS lastAction){
        this(x, y, cost, 0, father, redCape, blueCape, blueCapesRemaining, redCapesRemaining, lastAction);
    }

    // Constructor with heuristic
    public Node(int x, int y, int cost, int heuristic, Node father, boolean blueCape, boolean redCape, ArrayList<Vector2d> blueCapesRemaining, ArrayList<Vector2d> redCapesRemaining, ACTIONS lastAction){
        this.x = x;
        this.y = y;
        this.cost = cost;
        this.heuristic = heuristic;
        this.father = father;
        this.blueCape = blueCape;
        this.redCape = redCape;
        this.blueCapesRemaining = blueCapesRemaining;
        this.redCapesRemaining = redCapesRemaining;
        this.lastAction = lastAction;
    }


    @Override
    public boolean equals(Object obj) {
        // Reference comparison
        if (this == obj) return true;

        // Check if they are the same type
        if (obj == null || getClass() != obj.getClass()) return false;

        // Compares the attributes
        Node node = (Node) obj;

        if (!(x == node.x && y == node.y
            && redCape == node.redCape && blueCape == node.blueCape))
            return false;
        
        // Check that the remaining capes are the same
        if (blueCapesRemaining.size() != node.blueCapesRemaining.size()
            || redCapesRemaining.size() != node.redCapesRemaining.size())
            return false;

        for (int i = 0; i < blueCapesRemaining.size(); i++)
            if (!blueCapesRemaining.get(i).equals(node.blueCapesRemaining.get(i)))
                return false;
        
        for (int i = 0; i < redCapesRemaining.size(); i++)
            if (!redCapesRemaining.get(i).equals(node.redCapesRemaining.get(i)))
                return false;
        
        return true;
    }
    
    @Override
    public int compareTo(Node other) {
        // First compare f, if equal, then compare the cost g
        int comparison = Integer.compare(this.cost+this.heuristic, other.cost+other.heuristic);

        if (comparison != 0)
            return comparison;

        // Compare g
        comparison = Integer.compare(this.cost, other.cost);

        if (comparison != 0)
            return comparison;
        
        comparison = Integer.compare(this.orderOfCreation, other.orderOfCreation);

        return comparison;
    }

    @Override
    public int hashCode() {
        //int result = Integer.hashCode((int) location.x);
        //result = 31 * result + Integer.hashCode((int) location.y);
        //return result;

        return Objects.hash(x, y, redCape, redCapesRemaining.size(), blueCapesRemaining.size());
    }
}
