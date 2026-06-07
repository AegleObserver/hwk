package chess;

public final class SpecialMoveRule {
    public enum Type {
        NONE,
        CASTLING,
        EN_PASSANT,
        PROMOTION
    }

    private SpecialMoveRule() {
    }

    public static Type detect(ChessBoard board, ProcessInputInChess move, char player) {
        if (board == null || move == null) return Type.NONE;
        if (isPromotion(board, move, player)) return Type.PROMOTION;
        if (isEnPassant(board, move, player, board.getLastMove())) return Type.EN_PASSANT;
        if (isCastling(board, move, player)) return Type.CASTLING;
        return Type.NONE;
    }

    public static boolean isCastling(ChessBoard board, ProcessInputInChess move, char player) {
        if (board == null || move == null) return false;
        char piece = board.getCell(move.fromRow, move.fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'K', 'k')) return false;

        int homeRow = player == 'W' ? board.getSize() - 1 : 0;
        if (move.fromRow != homeRow || move.toRow != homeRow) return false;
        if (move.fromCol != 4) return false;
        if (Math.abs(move.toCol - move.fromCol) != 2) return false;
        if (board.hasKingMoved(player)) return false;

        char opponent = player == 'W' ? 'B' : 'W';

        if (move.toCol == 6) {
            if (isSquareAttacked(board, homeRow, 4, opponent)) return false;
            if (isSquareAttacked(board, homeRow, 5, opponent)) return false;
            if (isSquareAttacked(board, homeRow, 6, opponent)) return false;
            return board.getCell(homeRow, 5) == '.'
                && board.getCell(homeRow, 6) == '.'
                && ChessRuleSupport.isPlayersPiece(board.getCell(homeRow, 7), player, 'R', 'r')
                && !board.hasBishopMoved(player, 1);
        }

        if (move.toCol == 2) {
            if (isSquareAttacked(board, homeRow, 4, opponent)) return false;
            if (isSquareAttacked(board, homeRow, 3, opponent)) return false;
            if (isSquareAttacked(board, homeRow, 2, opponent)) return false;
            return board.getCell(homeRow, 1) == '.'
                && board.getCell(homeRow, 2) == '.'
                && board.getCell(homeRow, 3) == '.'
                && ChessRuleSupport.isPlayersPiece(board.getCell(homeRow, 0), player, 'R', 'r')
                && !board.hasBishopMoved(player, 0);
        }

        return false;
    }

    private static boolean isSquareAttacked(ChessBoard board, int row, int col, char opponent) {
        int size = board.getSize();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!board.hasPieceAt(r, c, opponent)) continue;
                ProcessInputInChess attack = new ProcessInputInChess(r, c, row, col);
                if (board.isMoveLegalByCache(attack, opponent)) return true;
            }
        }
        return false;
    }

    public static boolean isEnPassant(ChessBoard board, ProcessInputInChess move, char player, ProcessInputInChess lastMove) {
        if (board == null || move == null || lastMove == null) return false;

        char piece = board.getCell(move.fromRow, move.fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'P', 'p')) return false;

        int direction = player == 'W' ? -1 : 1;
        int rowDelta = move.toRow - move.fromRow;
        int colDelta = Math.abs(move.toCol - move.fromCol);
        if (rowDelta != direction || colDelta != 1) return false;
        if (!ChessRuleSupport.isEmpty(board.getCell(move.toRow, move.toCol))) return false;

        char enemyPawn = player == 'W' ? 'p' : 'P';
        if (board.getCell(lastMove.toRow, lastMove.toCol) != enemyPawn) {
            return false;
        }

        if (Math.abs(lastMove.toRow - lastMove.fromRow) != 2) return false;
        if (lastMove.toRow != move.fromRow) return false;
        if (lastMove.toCol != move.toCol) return false;

        return true;
    }

    public static boolean isPromotion(ChessBoard board, ProcessInputInChess move, char player) {
        if (board == null || move == null) return false;
        char piece = board.getCell(move.fromRow, move.fromCol);
        if (!ChessRuleSupport.isPlayersPiece(piece, player, 'P', 'p')) return false;

        int promotionRow = player == 'W' ? 0 : board.getSize() - 1;
        return move.toRow == promotionRow;
    }
}