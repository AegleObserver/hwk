package chess;

import common.BoardView;

public final class BishopRule {
    private BishopRule() {
    }

    public static boolean isLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'B', 'b')) return false;

        int rowDelta = Math.abs(toRow - fromRow);
        int colDelta = Math.abs(toCol - fromCol);
        if (rowDelta != colDelta || rowDelta == 0) return false;
        if (!ChessRuleSupport.pathClearStraight(board, fromRow, fromCol, toRow, toCol)) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'B', 'b');
    }
}