package tracks.singlePlayer.evaluacion.src_RAMON;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

public class AgenteLRTAStar extends AbstractPlayer{
	// LRTA* Agent:
	// At each step it moves to the node in the local space with the lowest
	// cost (1) plus heuristics, and updates the heuristics of what was the
	// current node.
	// The initial heuristic for each node is the Manhattam distance, but
	// it will change as the algorithm runs.

	Vector2d fescala;
	Vector2d portal;
	int gridWidth, gridHeight;
	boolean[][] forbiddenBoxes, blueWalls, redWalls; // Boxes that we cannot cross, and walls
	ArrayList<Vector2d> initialBlueCapes, initialRedCapes; // Blue and red capes availables at startup

	int numberOfExpandedNodes = 0; // Variable to count the number of expanded nodes
	long accumulatedTime = 0; // Accumulated time of execution

	Node currentNode;
	HashMap<Node, Integer> nodes = new HashMap<> (); // Set with the nodes and the heuristics
											// associated with them, which are updated over time
	
	
	/**
	 * initialize all variables for the agent
	 * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
	 */
	public AgenteLRTAStar(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		// Save the grid dimensions
		gridWidth = stateObs.getObservationGrid().length;
		gridHeight = stateObs.getObservationGrid()[0].length;

		// Calculate the scale factor between worlds (pixels -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length , 
        		stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

      
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
		// Calculate the next movement
		long time1 = System.nanoTime();
		ACTIONS action = calculateNextMovement(stateObs);
		long time2 = System.nanoTime();
		accumulatedTime += (time2-time1);

		// If we are next to the goal, we show the information
		int x = (int) (stateObs.getAvatarPosition().x / fescala.x);
		int y = (int) (stateObs.getAvatarPosition().y / fescala.y);
		if ((Math.abs(x - portal.x) + Math.abs(y - portal.y)) <= 1){
			System.out.println("* Number of expanded nodes and path size: " + numberOfExpandedNodes);
			System.out.println("* Time taken in ms: " + accumulatedTime / 1000000);
		}

		return action;
	}

	/**
	 * It creates and return the children of the given node.
	 * @param father The father from whom the children will be created.
	 * @return Children of the given node.
	 */
	private ArrayList<Node> nodeChildren(Node father){

		ArrayList<Node> children = new ArrayList<>();

		// We create the children with the new position and the rest of the
		// values null, which we will update afterwards. We add these children
		// only if there are no forbidden boxes in the new position
        if (father.x + 1 <= gridWidth - 1 && !forbiddenBoxes[father.x+1][father.y])
			children.add(new Node(father.x+1, father.y, 1, father, false, false, null, null, ACTIONS.ACTION_RIGHT));

		if (father.x - 1 >= 0 && !forbiddenBoxes[father.x-1][father.y])
			children.add(new Node(father.x-1, father.y, 1, father, false, false, null, null, ACTIONS.ACTION_LEFT));

		if (father.y - 1 >= 0 && !forbiddenBoxes[father.x][father.y-1])
			children.add(new Node(father.x, father.y-1, 1, father, false, false, null, null, ACTIONS.ACTION_UP));

        if (father.y + 1 <= gridHeight-1 && !forbiddenBoxes[father.x][father.y+1])
			children.add(new Node(father.x, father.y+1, 1, father, false, false, null, null, ACTIONS.ACTION_DOWN));

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

			// If there is, we use the heuristic we saved before, if not,
			// we use the Manhattam distance as heuristic
			if (nodes.containsKey(children.get(i)))
				children.get(i).heuristic = nodes.get(children.get(i));
			else
				children.get(i).heuristic = (int) (Math.abs(children.get(i).x - portal.x) + Math.abs(children.get(i).y - portal.y));
		}

		return children;
	}

	/**
	 * It calculates the next movement to arrive to the nearest portal.
	 * @return The next action to arriveto the nearest portal.
	 */
	private ACTIONS calculateNextMovement(StateObservation stateObs){
		// If the currentNode variable is null, we create it based
		// on the current values of the character
		if (currentNode == null){
			// Avatar current position
			int x = (int) (stateObs.getAvatarPosition().x / fescala.x);
			int y = (int) (stateObs.getAvatarPosition().y / fescala.y);

			currentNode = new Node(x, y, 1, (int) (Math.abs(x - portal.x) + Math.abs(y - portal.y)), null, false, false, initialBlueCapes, initialRedCapes, ACTIONS.ACTION_NIL);
		}

		numberOfExpandedNodes++;
		
		// Check if it is the goal
		if (currentNode.x == portal.x && currentNode.y == portal.y)
			return ACTIONS.ACTION_NIL;

		// Calculate the children
		ArrayList<Node> children = nodeChildren(currentNode);

		// Add the nodes that do not yet exist to the list
		for (int i=0; i<children.size(); i++)
			if (!nodes.containsKey(children.get(i)))
				nodes.put(children.get(i), children.get(i).heuristic);

		// Calculate the best and second best child
		Collections.sort(children);
		Node firstChild = children.get(0);
		Node secondChild = firstChild;
		if (children.size() > 1)
			secondChild = children.get(1);
		
		// Update heuristic value of the current node with the max between
		// the heuristic of the current node and the cost plus the value of the
		// heuristic of the first child
		nodes.put(currentNode, Math.max(currentNode.heuristic, secondChild.cost + firstChild.heuristic));

		// We move to the best neighbor
		currentNode = firstChild;

		return firstChild.lastAction;
	}
}
