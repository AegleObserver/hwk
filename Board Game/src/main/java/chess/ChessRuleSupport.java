package chess;

import common.BoardView;

final class ChessRuleSupport {
    private ChessRuleSupport() {
    }

    static boolean inBounds(BoardView board, int row, int col) {
        return board != null && row >= 0 && row < board.getSize() && col >= 0 && col < board.getSize();
    }

    static boolean isEmpty(char piece) {
        return piece == '.' || piece == 'x';
    }

    static boolean isPlayersPiece(char piece, char player, char whitePiece, char blackPiece) {
        if (player == 'W') return piece == whitePiece;
        if (player == 'B') return piece == blackPiece;
        return false;
    }

    static boolean isOpponentPiece(char piece, char player) {
        if (piece == '.') return false;
        if (player == 'W') return Character.isLowerCase(piece);
        if (player == 'B') return Character.isUpperCase(piece);
        return false;
    }

    static boolean pathClearStraight(BoardView board, int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int row = fromRow + rowStep;
        int col = fromCol + colStep;
        while (row != toRow || col != toCol) {
            if (!isEmpty(board.getCell(row, col))) return false;
            row += rowStep;
            col += colStep;
        }
        return true;
    }
}