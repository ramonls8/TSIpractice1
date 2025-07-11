package tracks.singlePlayer.evaluacion.src_RAMON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.Iterator;
import java.util.Vector;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AgenteDijkstra extends AbstractPlayer{
	// Dijkstra Agent:
	// It considers the cost of edges, that is, of movement from one node to
	// another, and selects those with the lowest cumulative cost. It guarantees
	// to find the optimal path.
	// 1) It looks for the nearest door.
	// 2) It selects the actions that minimize the cost to the door.

	Vector2d fescala;
	Vector2d portal;
	Vector2d avatarInitialPosition;
	int gridWidth, gridHeight;
	boolean[][] forbiddenBoxes, blueWalls, redWalls; // Boxes that we cannot cross, and walls
	ArrayList<Vector2d> initialBlueCapes, initialRedCapes; // Blue and red capes availables at startup
	Stack<ACTIONS> path = new Stack<>(); // Calculated path to the goal

	int numberOfExpandedNodes = 0; // Variable to count the number of expanded nodes
	int orderOfCreation = 0; // Number to identify which nodes were created first

	PriorityQueue<Node> openNodes = new PriorityQueue<> (); // Open nodes
	HashSet<Node> closedNodes = new HashSet<> (); // Closed nodes
	
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteDijkstra(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		// Save the grid dimensions
		gridWidth = stateObs.getObservationGrid().length;
		gridHeight = stateObs.getObservationGrid()[0].length;

		// Calculate the scale factor between worlds (pixels -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length , 
        		stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

	
        // Avatar initial position
        avatarInitialPosition =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x, stateObs.getAvatarPosition().y / fescala.y);
      
        // It creates a list of portals, sorted by proximity to the avatar
        ArrayList<Observation>[] posiciones = stateObs.getPortalsPositions(stateObs.getAvatarPosition());
        // Select the nearest portal
        portal = posiciones[0].get(0).position;
        portal.x = Math.floor(portal.x / fescala.x);
        portal.y = Math.floor(portal.y / fescala.y);

		// Save the boxes where we cannot walk and the walls
		forbiddenBoxes = new boolean[gridWidth][gridHeight];
		blueWalls = new boolean[gridWidth][gridHeight];
		redWalls = new boolean[gridWidth][gridHeight];

		ArrayList<Observation>[] immovable = stateObs.getImmovablePositions();
		for (int i=0; i < immovable.length; i++) {
			for (int j=0; j < immovable[i].size() ; j++) {
				Observation box = immovable[i].get(j);
				int x = (int) Math.floor(box.position.x/fescala.x);
				int y = (int) Math.floor(box.position.y/fescala.y);

				if (box.itype == 3 || box.itype == 5) // Walls and traps
					forbiddenBoxes[x][y] = true;
				else if (box.itype == 6) // Red walls
					redWalls[x][y] = true;
				else if (box.itype == 7) // Blue walls
					blueWalls[x][y] = true;
			}
		}

		// Save the locations where the capes are
		initialRedCapes = new ArrayList<>();
		initialBlueCapes = new ArrayList<>();

		ArrayList<Observation>[] resources = stateObs.getResourcesPositions();
		for (int i=0; i < resources.length; i++) {
			for (int j=0; j < resources[i].size() ; j++) {
				Observation box = resources[i].get(j);
				int x = (int) Math.floor(box.position.x/fescala.x);
				int y = (int) Math.floor(box.position.y/fescala.y);

				if (box.itype == 8) // Red capes
					initialRedCapes.add(new Vector2d(x,y));
				if (box.itype == 9) // Blue capes
					initialBlueCapes.add(new Vector2d(x,y));
			}
		}
	}
	
	/**
	 * return the best action to arrive faster to the closest portal
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 * @return best	ACTION to arrive faster to the closest portal
	 */
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		if (path.empty() && !stateObs.isGameOver()){
			// Calculate the path and the time it takes
			long time1 = System.nanoTime();
			calculatePath();
			long time2 = System.nanoTime();
			long time = (time2-time1) / 1000000;
			System.out.println("* Time taken in ms: " + time);

			System.out.println("* Size of the calculated path: " + path.size());
			System.out.println("* Number of expanded nodes: " + numberOfExpandedNodes);
		}
		
		if (!path.empty())
			return path.pop();
		else
			return ACTIONS.ACTION_NIL;
	}

	/**
	 * It creates and return the children of the given node.
	 * @param father The father from whom the children will be created.
	 * @return The children of the given node.
	 */
	private ArrayList<Node> nodeChildren(Node father){

		ArrayList<Node> children = new ArrayList<>();

		// We create the children with the new position and the rest of the
		// values null, which we will update afterwards. We add these children
		// only if there are no forbidden boxes in the new position
        if (father.x + 1 <= gridWidth - 1 && !forbiddenBoxes[father.x+1][father.y])
			children.add(new Node(father.x+1, father.y, father.cost+1, father, false, false, null, null, ACTIONS.ACTION_RIGHT));

		if (father.x - 1 >= 0 && !forbiddenBoxes[father.x-1][father.y])
			children.add(new Node(father.x-1, father.y, father.cost+1, father, false, false, null, null, ACTIONS.ACTION_LEFT));

		if (father.y - 1 >= 0 && !forbiddenBoxes[father.x][father.y-1])
			children.add(new Node(father.x, father.y-1, father.cost+1, father, false, false, null, null, ACTIONS.ACTION_UP));

        if (father.y + 1 <= gridHeight-1 && !forbiddenBoxes[father.x][father.y+1])
			children.add(new Node(father.x, father.y+1, father.cost+1, father, false, false, null, null, ACTIONS.ACTION_DOWN));

		// If there is a color wall and we do not have the cape,
		// we delete that child, since it can not cross it
		for (int i = children.size()-1; i >= 0; i--)
			if (blueWalls[children.get(i).x][children.get(i).y] && !father.blueCape)
				children.remove(i);
		for (int i = children.size()-1; i >= 0; i--)
			if (redWalls[children.get(i).x][children.get(i).y] && !father.redCape)
				children.remove(i);
		
		// We update the rest of the values of the children
		for (int i = 0; i < children.size(); i++){
			children.get(i).blueCapesRemaining = new ArrayList<>(father.blueCapesRemaining);
			children.get(i).redCapesRemaining = new ArrayList<>(father.redCapesRemaining);
			children.get(i).blueCape = father.blueCape;
			children.get(i).redCape = father.redCape;

			// We check if it has picked up a cape
			Vector2d currentLocation = new Vector2d(children.get(i).x, children.get(i).y);
			if (children.get(i).blueCapesRemaining.contains(currentLocation)){
				children.get(i).blueCapesRemaining.remove(currentLocation);
				children.get(i).blueCape = true;
				children.get(i).redCape = false;
			}
			if (children.get(i).redCapesRemaining.contains(currentLocation)){
				children.get(i).redCapesRemaining.remove(currentLocation);
				children.get(i).blueCape = false;
				children.get(i).redCape = true;
			}

			children.get(i).orderOfCreation = orderOfCreation++;
		}

		return children;
	}

	/**
	 * It calculates and saves the best path to arrive to the nearest portal.
	 * @return True if it found the path, false if it did not.
	 */
	private boolean calculatePath(){
		// We add the node of the initial position
		openNodes.add(new Node((int) avatarInitialPosition.x, (int) avatarInitialPosition.y, 0, null, false, false, initialBlueCapes, initialRedCapes, ACTIONS.ACTION_NIL));

		Node currentNode = openNodes.peek();
		
		// Search for the best path
		while (!openNodes.isEmpty() && !(currentNode.x == portal.x && currentNode.y == portal.y)){
			numberOfExpandedNodes++;
			
			// Get and delete the current node from open nodes
			currentNode = openNodes.poll();

			// Check if it is the goal
			if (currentNode.x == portal.x && currentNode.y == portal.y)
				break;
			
			// Add the current node to closed nodes
			closedNodes.add(currentNode);

			// We calculate the children and check if we have to add them
			ArrayList<Node> children = nodeChildren(currentNode);

			for (int i=0; i<children.size(); i++){
				// Check that it is not in closed nodes
				if (!closedNodes.contains(children.get(i))){
					// If it is not on open nodes either, we add it
					if (!openNodes.contains(children.get(i))){
						openNodes.add(children.get(i));
					}
					// If it is in open nodes but the new one has a better cost,
					// we remove the old one and add this one.
					// (Doing this is not necessary as the cost of each movement is constant).
					/*
					else {
						Iterator<Node> iterator = openNodes.iterator();
						boolean found = false;
				
						while (iterator.hasNext() && !found) {
							Node element = iterator.next();
							if (children.get(i) == element && children.get(i).compareTo(element) < 0) {
								openNodes.remove(element);
								openNodes.add(children.get(i));
								found = true;
							}
						}
					}
					*/
				}
			}
		}

		// If we have not reached the portal, we return false
		if (!(currentNode.x == portal.x && currentNode.y == portal.y))
			return false;
		else {
			// We calculate the path, from the last node, up through the parents to the first node
			Node nextNode = currentNode;
			while (nextNode != null && nextNode.lastAction != ACTIONS.ACTION_NIL){
				path.add(nextNode.lastAction);
				nextNode = nextNode.father;
			}

			return true;
		}
	}
}
