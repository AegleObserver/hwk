import java.util.Set;

public interface ReversiBoardView extends BoardView {
    boolean hasLegalMove(char player);

    Set<Integer> getLegalMoves(char player);

    boolean isLegalMove(int row, int col, char player);

    String toAsciiString(char currentPlayer);
}
