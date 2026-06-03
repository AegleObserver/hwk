package chess;

import common.BoardView;

public final class ChessMoveRules {
    private ChessMoveRules() {
    }

    public static boolean isPawnLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isPawnLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isPawnLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (board == null) return false;
        if (!inBounds(board, fromRow, fromCol) || !inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!isPlayersPawn(piece, player)) return false;

        char target = board.getCell(toRow, toCol);
        int direction = player == 'W' ? -1 : 1;
        int startRow = player == 'W' ? board.getSize() - 2 : 1;

        int rowDelta = toRow - fromRow;
        int colDelta = Math.abs(toCol - fromCol);

        if (colDelta == 0) {
            if (rowDelta == direction && isEmpty(target)) return true;
            if (fromRow == startRow && rowDelta == 2 * direction) {
                int middleRow = fromRow + direction;
                return isEmpty(board.getCell(middleRow, fromCol)) && isEmpty(target);
            }
            return false;
        }

        if (colDelta == 1 && rowDelta == direction) {
            return isOpponentPiece(target, player);
        }

        return false;
    }

    public static boolean isKnightLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isKnightLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isKnightLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'N', 'n')) return false;

        int rowDelta = Math.abs(toRow - fromRow);
        int colDelta = Math.abs(toCol - fromCol);
        if (!((rowDelta == 2 && colDelta == 1) || (rowDelta == 1 && colDelta == 2))) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'N', 'n');
    }

    public static boolean isBishopLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isBishopLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isBishopLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'B', 'b')) return false;

        int rowDelta = Math.abs(toRow - fromRow);
        int colDelta = Math.abs(toCol - fromCol);
        if (rowDelta != colDelta || rowDelta == 0) return false;
        if (!ChessRuleSupport.pathClearStraight(board, fromRow, fromCol, toRow, toCol)) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'B', 'b');
    }

    public static boolean isRookLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isRookLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isRookLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'R', 'r')) return false;

        boolean sameRow = fromRow == toRow;
        boolean sameCol = fromCol == toCol;
        if (!sameRow && !sameCol) return false;
        if (sameRow && sameCol) return false;
        if (!ChessRuleSupport.pathClearStraight(board, fromRow, fromCol, toRow, toCol)) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'R', 'r');
    }

    public static boolean isQueenLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isQueenLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isQueenLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'Q', 'q')) return false;

        int rowDelta = Math.abs(toRow - fromRow);
        int colDelta = Math.abs(toCol - fromCol);
        boolean straight = fromRow == toRow || fromCol == toCol;
        boolean diagonal = rowDelta == colDelta && rowDelta != 0;
        if (!straight && !diagonal) return false;
        if (!ChessRuleSupport.pathClearStraight(board, fromRow, fromCol, toRow, toCol)) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'Q', 'q');
    }

    public static boolean isKingLegalMove(BoardView board, ProcessInputInChess move, char player) {
        if (move == null) return false;
        return isKingLegalMove(board, move.fromRow, move.fromCol, move.toRow, move.toCol, player);
    }

    public static boolean isKingLegalMove(BoardView board, int fromRow, int fromCol, int toRow, int toCol, char player) {
        if (!ChessRuleSupport.inBounds(board, fromRow, fromCol) || !ChessRuleSupport.inBounds(board, toRow, toCol)) return false;

        char piece = board.getCell(fromRow, fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'K', 'k')) return false;

        int rowDelta = Math.abs(toRow - fromRow);
        int colDelta = Math.abs(toCol - fromCol);
        if (rowDelta > 1 || colDelta > 1 || (rowDelta == 0 && colDelta == 0)) return false;

        return !ChessRuleSupport.isPlayersPiece(board.getCell(toRow, toCol), player, 'K', 'k');
    }

    private static boolean isPlayersPawn(char piece, char player) {
        if (player == 'W') return piece == 'P';
        if (player == 'B') return piece == 'p';
        return false;
    }

    private static boolean isOpponentPiece(char piece, char player) {
        if (piece == '.') return false;
        if (player == 'W') return Character.isLowerCase(piece);
        if (player == 'B') return Character.isUpperCase(piece);
        return false;
    }

    private static boolean isEmpty(char piece) {
        return piece == '.';
    }

    private static boolean inBounds(BoardView board, int row, int col) {
        return row >= 0 && row < board.getSize() && col >= 0 && col < board.getSize();
    }
}
