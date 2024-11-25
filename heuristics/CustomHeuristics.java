package src.pas.chess.heuristics;

// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;
import edu.bu.chess.game.Game;
import edu.bu.chess.game.piece.Piece;
import edu.bu.chess.game.piece.PieceType;
import edu.bu.chess.game.player.Player;
import edu.bu.chess.game.Board;
import edu.bu.chess.utils.Coordinate;
import edu.cwru.sepia.util.Direction;

// JAVA PROJECT IMPORTS
// Assuming DefaultHeuristics is available if needed

public class CustomHeuristics extends Object {

    /**
     * Calculates the heuristic value for the MAX player in the given game node.
     * The heuristic considers material balance, piece mobility, king safety, and pawn structure.
     * @param node The current game node.
     * @return The heuristic value representing the "goodness" of the game state for the MAX player.
     */
    public static double getMaxPlayerHeuristicValue(DFSTreeNode node) {
        Game game = node.getGame();
        Player maxPlayer = node.getMaxPlayer();
        Player minPlayer = getMinPlayer(node);

		// Check if the opponent's king is captured
        if (isOpponentKingCaptured(game, minPlayer)) {
            // Assign a very high heuristic value, less than Double.MAX_VALUE to avoid overflow
            return Double.MAX_VALUE / 2;
        }

        double materialScore = evaluateMaterial(game, maxPlayer, minPlayer);
        double mobilityScore = evaluateMobility(game, maxPlayer, minPlayer);
        double kingSafetyScore = evaluateKingSafety(game, maxPlayer, minPlayer);
        double pawnStructureScore = evaluatePawnStructure(game, maxPlayer, minPlayer);

        // Combine the scores with appropriate weights
        double heuristicValue = (12 * materialScore) + (5 * mobilityScore) + (8 * kingSafetyScore) + (2 * pawnStructureScore);

        // Ensure the heuristic value is within the bounds [-Double.MAX_VALUE, Double.MAX_VALUE]
        heuristicValue = Math.max(-Double.MAX_VALUE, Math.min(Double.MAX_VALUE, heuristicValue));

        return heuristicValue;
    }

    /**
     * Helper method to get the MIN player based on the node's information.
     * @param node The current game node.
     * @return The opponent player (MIN player).
     */
    private static Player getMinPlayer(DFSTreeNode node) {
        Game game = node.getGame();
        Player maxPlayer = node.getMaxPlayer();
        Player currentPlayer = game.getCurrentPlayer();

        // Determine the MIN player based on whose turn it is
        if (maxPlayer.equals(currentPlayer)) {
            return game.getOtherPlayer();
        } else {
            return currentPlayer;
        }
    }

	/**
     * Checks if the opponent's king is captured.
     * @param game The current game state.
     * @param minPlayer The opponent player (MIN player).
     * @return True if the opponent's king is captured, false otherwise.
     */
    private static boolean isOpponentKingCaptured(Game game, Player minPlayer) {
        // Get the set of the opponent's kings
        boolean kingExists = game.getBoard().getPieces(minPlayer, PieceType.KING).isEmpty();
        return kingExists;
    }

    private static double evaluateMaterial(Game game, Player maxPlayer, Player minPlayer) {
        int maxMaterial = 0;
        int minMaterial = 0;

        for (Piece piece : game.getBoard().getPieces(maxPlayer)) {
            maxMaterial += Piece.getPointValue(piece.getType());
        }

        for (Piece piece : game.getBoard().getPieces(minPlayer)) {
            minMaterial += Piece.getPointValue(piece.getType());
        }

        return maxMaterial - minMaterial;
    }

    private static double evaluateMobility(Game game, Player maxPlayer, Player minPlayer) {
        int maxMobility = 0;
        int minMobility = 0;

        for (Piece piece : game.getBoard().getPieces(maxPlayer)) {
            maxMobility += piece.getAllMoves(game).size();
        }

        for (Piece piece : game.getBoard().getPieces(minPlayer)) {
            minMobility += piece.getAllMoves(game).size();
        }

        return maxMobility - minMobility;
    }

    private static double evaluateKingSafety(Game game, Player maxPlayer, Player minPlayer) {
        Piece maxKing = game.getBoard().getPieces(maxPlayer, PieceType.KING).iterator().next();
        Piece minKing = game.getBoard().getPieces(minPlayer, PieceType.KING).iterator().next();

        int maxKingSafety = calculateKingSafety(game, maxKing);
        int minKingSafety = calculateKingSafety(game, minKing);

        return minKingSafety - maxKingSafety; // Higher value if opponent's king is less safe
    }

    private static int calculateKingSafety(Game game, Piece king) {
        int safetyScore = 0;
        Coordinate kingPosition = game.getCurrentPosition(king);

        // Evaluate the area around the king
        for (Direction dir : Direction.values()) {
            Coordinate neighbor = kingPosition.getNeighbor(dir);
            if (game.getBoard().isInbounds(neighbor)) {
                if (game.getBoard().isPositionOccupied(neighbor)) {
                    Piece adjacentPiece = game.getBoard().getPieceAtPosition(neighbor);
                    if (!king.isEnemyPiece(adjacentPiece)) {
                        // Friendly piece near the king increases safety
                        safetyScore += 2;
                    } else {
                        // Enemy piece near the king decreases safety
                        safetyScore -= 3;
                    }
                } else {
                    // Empty squares near the king decrease safety slightly
                    safetyScore -= 1;
                }
            }
        }
        return safetyScore;
    }

    private static double evaluatePawnStructure(Game game, Player maxPlayer, Player minPlayer) {
        int maxPawnStructure = calculatePawnStructure(game, maxPlayer);
        int minPawnStructure = calculatePawnStructure(game, minPlayer);

        return maxPawnStructure - minPawnStructure;
    }

    private static int calculatePawnStructure(Game game, Player player) {
        int pawnStructureScore = 0;

        for (Piece pawn : game.getBoard().getPieces(player, PieceType.PAWN)) {
            Coordinate pawnPosition = game.getCurrentPosition(pawn);

			// penalize doubled pawns
			if (isDoubledPawn(game, pawn, player)) {
				pawnStructureScore -= 2;
			}
			
			// penalize isolated pawns
			if (isIsolatedPawn(game, pawn, player)) {
				pawnStructureScore -= 3;
			}
		}

		return pawnStructureScore;
    }

	private static boolean isDoubledPawn(Game game, Piece pawn, Player player) {
		Coordinate pawnPosition = game.getCurrentPosition(pawn);
		for (Piece otherPawn : game.getBoard().getPieces(player, PieceType.PAWN)) {
			if (!pawn.equals(otherPawn) && game.getCurrentPosition(otherPawn).getXPosition() == pawnPosition.getXPosition()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isIsolatedPawn(Game game, Piece pawn, Player player) {
		Coordinate pawnPosition = game.getCurrentPosition(pawn);
		for (int dx = -1; dx <= 1; dx += 2) {
			int neighborF = pawnPosition.getXPosition() + dx;
			Coordinate neighbor = new Coordinate(neighborF, pawnPosition.getYPosition());
			if (game.getBoard().isInbounds(neighbor)) {
				for (Piece neighborPawn : game.getBoard().getPieces(player, PieceType.PAWN)) {
					if (!pawn.equals(neighborPawn) && game.getCurrentPosition(neighborPawn).getXPosition() == neighborF) {
						return false;
					}
				}
			}
		}
		return true;
	}
}

// Offensive Heuristics: 
 	// Number of enemy pieces that can be captured (threats)
	// Points gained from capturing pieces
// Defensive Heuristics:
	// Number of remaining friendly pieces
	// Safety of important pieces (king, queen, rook)
	// Number of enemy pieces threatening friendly pieces