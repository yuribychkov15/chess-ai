package src.pas.chess.moveorder;

// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;
import edu.bu.chess.game.move.Move;

import java.util.LinkedList;
import java.util.List;

public class CustomMoveOrderer extends Object {

    /**
     * This method performs move ordering for Alpha-Beta pruning.
     * The goal is to see nodes which are beneficial first, so we can prune as much as possible during the search.
     * We prioritize capture moves, promotion moves, and finally quiet moves.
     * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
     * @return The ordered nodes.
     */
    public static List<DFSTreeNode> order(List<DFSTreeNode> nodes) {
        List<DFSTreeNode> captureNodes = new LinkedList<>(); // capture moves (highest priority)
        List<DFSTreeNode> promotionNodes = new LinkedList<>(); // promotion moves (medium priority)
        List<DFSTreeNode> otherNodes = new LinkedList<>(); // other moves (lowest priority)

		// loop through each node
        for (DFSTreeNode node : nodes) {
			// get move associated with current node
            Move move = node.getMove();
			// check if the node has a valid move assoicated with it
            if (move != null) {
				// check the type of move
                switch (move.getType()) {
					// if the move is a capture move, add it to the captureNodes list
                    case CAPTUREMOVE:
                        captureNodes.add(node);
                        break;
                    case PROMOTEPAWNMOVE:  // if move is a pawn promotion move, add to promotionNodes
                        promotionNodes.add(node);
                        break;
                    default:
                        otherNodes.add(node); // any other type of node just add to this (regular non-capturing ones)
                        break;
                }
            } else {
				// if no valid move for the node, treat as a quiet node 
                otherNodes.add(node);
            }
        }

        // Order: captures -> promotions -> other
        captureNodes.addAll(promotionNodes);
        captureNodes.addAll(otherNodes);

        return captureNodes;
    }
}
