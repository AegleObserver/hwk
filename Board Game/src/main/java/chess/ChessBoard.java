package chess;

import common.BoardView;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChessBoard implements BoardView {
    public static final int PIECE_SLOT_COUNT = 16;
    private static final char CAPTURED_PIECE = 'x';
    private static final int WHITE_INDEX = 0;
    private static final int BLACK_INDEX = 1;

    private final int size;
    private final char[][] pieces;
    private final int[][] whiteSlotAtCell;
    private final int[][] blackSlotAtCell;
    private final int[][] pieceRows;
    private final int[][] pieceCols;
    private final boolean[][] pieceAlive;
    private final boolean[] isKingMoved;
    private final boolean[][] isBishopMoved;
    private final Set<Integer>[][] legalMovesCache;
    private ProcessInputInChess lastMove;

    @SuppressWarnings("unchecked")
    public ChessBoard(int size) {
        this.size = size;
        this.pieces = new char[size][size];
        this.whiteSlotAtCell = new int[size][size];
        this.blackSlotAtCell = new int[size][size];
        this.pieceRows = new int[2][PIECE_SLOT_COUNT];
        this.pieceCols = new int[2][PIECE_SLOT_COUNT];
        this.pieceAlive = new boolean[2][PIECE_SLOT_COUNT];
        this.isKingMoved = new boolean[2];
        this.isBishopMoved = new boolean[2][2];
        this.legalMovesCache = new Set[2][PIECE_SLOT_COUNT];
        for (int player = 0; player < 2; player++) {
            for (int slot = 0; slot < PIECE_SLOT_COUNT; slot++) {
                legalMovesCache[player][slot] = new HashSet<Integer>();
            }
        }
        initializeBoard();
    }

    public void initializeBoard() {
        clearBoard();
        clearSlotMaps();
        resetFlags();
        lastMove = null;

        placeInitialPieces();
        refreshLegalMovesCache();
    }

    public ChessBoardSnapshot snapshot() {
        return new ChessBoardSnapshot(
            copyBoard(pieces),
            copyBoard(whiteSlotAtCell),
            copyBoard(blackSlotAtCell),
            copyMatrix(pieceRows),
            copyMatrix(pieceCols),
            copyMatrix(pieceAlive),
            isKingMoved.clone(),
            copyMatrix(isBishopMoved),
            lastMove
        );
    }

    public void load(ChessBoardSnapshot snapshot) {
        if (snapshot == null) return;

        copyInto(snapshot.pieces, pieces);
        copyInto(snapshot.whiteSlotAtCell, whiteSlotAtCell);
        copyInto(snapshot.blackSlotAtCell, blackSlotAtCell);
        copyInto(snapshot.pieceRows, pieceRows);
        copyInto(snapshot.pieceCols, pieceCols);
        copyInto(snapshot.pieceAlive, pieceAlive);
        System.arraycopy(snapshot.isKingMoved, 0, isKingMoved, 0, isKingMoved.length);
        copyInto(snapshot.isBishopMoved, isBishopMoved);
        lastMove = snapshot.lastMove;
        refreshLegalMovesCache();
    }

    public ProcessInputInChess getLastMove() {
        return lastMove;
    }

    public boolean hasKingMoved(char player) {
        return isKingMoved[playerIndex(player)];
    }

    public boolean hasBishopMoved(char player, int sideIndex) {
        return isBishopMoved[playerIndex(player)][sideIndex];
    }

    public boolean isKingAlive(char player) {
        return pieceAlive[playerIndex(player)][4];
    }

    public boolean isLegalMove(ProcessInputInChess move, char player) {
        if (move == null) return false;
        if (!inBounds(move.fromRow, move.fromCol) || !inBounds(move.toRow, move.toCol)) return false;

        int playerIndex = playerIndex(player);
        int slot = slotAt(move.fromRow, move.fromCol, playerIndex);
        if (slot < 0) return false;
        return legalMovesCache[playerIndex][slot].contains(encode(move.toRow, move.toCol));
    }

    public boolean hasPieceAt(int row, int col, char player) {
        if (!inBounds(row, col)) return false;
        return slotAt(row, col, playerIndex(player)) >= 0;
    }

    public boolean isMoveLegalByCache(ProcessInputInChess move, char player) {
        if (move == null) return false;
        int playerIndex = playerIndex(player);
        int slot = slotAt(move.fromRow, move.fromCol, playerIndex);
        if (slot < 0) return false;
        return legalMovesCache[playerIndex][slot].contains(encode(move.toRow, move.toCol));
    }

    public void applyMove(ProcessInputInChess move, char player) {
        if (move == null) return;

        int playerIndex = playerIndex(player);
        int opponentIndex = opponentIndex(playerIndex);
        SpecialMoveRule.Type specialMoveType = SpecialMoveRule.detect(this, move, player);
        char piece = getCell(move.fromRow, move.fromCol);

        if (specialMoveType == SpecialMoveRule.Type.CASTLING) {
            applyCastling(move, player, playerIndex);
        } else if (specialMoveType == SpecialMoveRule.Type.EN_PASSANT) {
            movePiece(playerIndex, move.fromRow, move.fromCol, move.toRow, move.toCol);
            removePieceAt(opponentIndex, move.fromRow, move.toCol);
            updateMovedFlags(piece, player, move.fromRow, move.fromCol);
        } else {
            int targetSlot = slotAt(move.toRow, move.toCol, opponentIndex);
            if (targetSlot >= 0) {
                removePieceAt(opponentIndex, move.toRow, move.toCol);
            }
            movePiece(playerIndex, move.fromRow, move.fromCol, move.toRow, move.toCol);
            updateMovedFlags(piece, player, move.fromRow, move.fromCol);
            if (specialMoveType == SpecialMoveRule.Type.PROMOTION) {
                promoteToQueen(move.toRow, move.toCol, player);
            }
        }

        lastMove = move;
        refreshLegalMovesCache();
    }

    public Set<Integer> getLegalMovesForPiece(char player, int slot) {
        int playerIndex = playerIndex(player);
        if (slot < 0 || slot >= PIECE_SLOT_COUNT) return Collections.emptySet();
        return Collections.unmodifiableSet(legalMovesCache[playerIndex][slot]);
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public char getCell(int row, int col) {
        if (!inBounds(row, col)) return '.';
        return pieces[row][col] == CAPTURED_PIECE ? '.' : pieces[row][col];
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public String toAsciiString() {
        StringBuilder builder = new StringBuilder();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                builder.append(getCell(row, col));
                if (col < size - 1) builder.append(' ');
            }
            if (row < size - 1) builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public boolean isLegalMove(int row, int col, char player) {
        int playerIndex = playerIndex(player);
        int slot = slotAt(row, col, playerIndex);
        return slot >= 0 && !legalMovesCache[playerIndex][slot].isEmpty();
    }

    private void refreshLegalMovesCache() {
        for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
            for (int slot = 0; slot < PIECE_SLOT_COUNT; slot++) {
                legalMovesCache[playerIndex][slot].clear();
                if (!pieceAlive[playerIndex][slot]) continue;

                int fromRow = pieceRows[playerIndex][slot];
                int fromCol = pieceCols[playerIndex][slot];
                if (!inBounds(fromRow, fromCol)) continue;

                char player = playerIndex == WHITE_INDEX ? 'W' : 'B';
                for (int toRow = 0; toRow < size; toRow++) {
                    for (int toCol = 0; toCol < size; toCol++) {
                        if (fromRow == toRow && fromCol == toCol) continue;
                        ProcessInputInChess candidate = new ProcessInputInChess(fromRow, fromCol, toRow, toCol);
                        if (isCandidateLegal(candidate, player)) {
                            legalMovesCache[playerIndex][slot].add(encode(toRow, toCol));
                        }
                    }
                }
            }
        }
    }

    private boolean isCandidateLegal(ProcessInputInChess move, char player) {
        char piece = getCell(move.fromRow, move.fromCol);
        switch (Character.toLowerCase(piece)) {
            case 'p':
                return PawnRule.isLegalMove(this, move, player)
                    || SpecialMoveRule.isEnPassant(this, move, player, lastMove);
            case 'n':
                return KnightRule.isLegalMove(this, move, player);
            case 'b':
                return BishopRule.isLegalMove(this, move, player);
            case 'r':
                return RookRule.isLegalMove(this, move, player);
            case 'q':
                return QueenRule.isLegalMove(this, move, player);
            case 'k':
                return KingRule.isLegalMove(this, move, player)
                    || SpecialMoveRule.isCastling(this, move, player);
            default:
                return false;
        }
    }

    private void applyCastling(ProcessInputInChess move, char player, int playerIndex) {
        int homeRow = player == 'W' ? size - 1 : 0;
        int rookFromCol;
        int rookToCol;
        int rookSideIndex;

        if (move.toCol == 6) {
            rookFromCol = size - 1;
            rookToCol = 5;
            rookSideIndex = 1;
        } else {
            rookFromCol = 0;
            rookToCol = 3;
            rookSideIndex = 0;
        }

        movePiece(playerIndex, move.fromRow, move.fromCol, move.toRow, move.toCol);
        movePiece(playerIndex, homeRow, rookFromCol, homeRow, rookToCol);
        isKingMoved[playerIndex] = true;
        isBishopMoved[playerIndex][rookSideIndex] = true;
    }

    private void updateMovedFlags(char piece, char player, int fromRow, int fromCol) {
        int playerIndex = playerIndex(player);
        if (Character.toLowerCase(piece) == 'k') {
            isKingMoved[playerIndex] = true;
            return;
        }

        if (Character.toLowerCase(piece) == 'r') {
            int homeRow = player == 'W' ? size - 1 : 0;
            if (fromRow == homeRow && fromCol == 0) {
                isBishopMoved[playerIndex][0] = true;
            }
            if (fromRow == homeRow && fromCol == size - 1) {
                isBishopMoved[playerIndex][1] = true;
            }
        }
    }

    private void promoteToQueen(int row, int col, char player) {
        pieces[row][col] = player == 'W' ? 'Q' : 'q';
    }

    private void movePiece(int playerIndex, int fromRow, int fromCol, int toRow, int toCol) {
        int slot = slotAt(fromRow, fromCol, playerIndex);
        if (slot < 0) return;

        char piece = pieces[fromRow][fromCol];
        pieces[fromRow][fromCol] = '.';
        setSlotAtCell(playerIndex, fromRow, fromCol, -1);
        pieces[toRow][toCol] = piece;
        setSlotAtCell(playerIndex, toRow, toCol, slot);
        pieceRows[playerIndex][slot] = toRow;
        pieceCols[playerIndex][slot] = toCol;
    }

    private void removePieceAt(int playerIndex, int row, int col) {
        int slot = slotAt(row, col, playerIndex);
        if (slot < 0) return;

        pieces[row][col] = CAPTURED_PIECE;
        setSlotAtCell(playerIndex, row, col, -1);
        pieceAlive[playerIndex][slot] = false;
        pieceRows[playerIndex][slot] = -1;
        pieceCols[playerIndex][slot] = -1;
    }

    private void placeInitialPieces() {
        placeBlack(0, 0, 0, 'r');
        placeBlack(0, 1, 1, 'n');
        placeBlack(0, 2, 2, 'b');
        placeBlack(0, 3, 3, 'q');
        placeBlack(0, 4, 4, 'k');
        placeBlack(0, 5, 5, 'b');
        placeBlack(0, 6, 6, 'n');
        placeBlack(0, 7, 7, 'r');
        for (int col = 0; col < size; col++) {
            placeBlack(1, col, 8 + col, 'p');
        }

        placeWhite(7, 0, 0, 'R');
        placeWhite(7, 1, 1, 'N');
        placeWhite(7, 2, 2, 'B');
        placeWhite(7, 3, 3, 'Q');
        placeWhite(7, 4, 4, 'K');
        placeWhite(7, 5, 5, 'B');
        placeWhite(7, 6, 6, 'N');
        placeWhite(7, 7, 7, 'R');
        for (int col = 0; col < size; col++) {
            placeWhite(6, col, 8 + col, 'P');
        }
    }

    private void placeWhite(int row, int col, int slot, char piece) {
        pieces[row][col] = piece;
        whiteSlotAtCell[row][col] = slot;
        pieceRows[WHITE_INDEX][slot] = row;
        pieceCols[WHITE_INDEX][slot] = col;
        pieceAlive[WHITE_INDEX][slot] = true;
    }

    private void placeBlack(int row, int col, int slot, char piece) {
        pieces[row][col] = piece;
        blackSlotAtCell[row][col] = slot;
        pieceRows[BLACK_INDEX][slot] = row;
        pieceCols[BLACK_INDEX][slot] = col;
        pieceAlive[BLACK_INDEX][slot] = true;
    }

    private void clearBoard() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                pieces[row][col] = '.';
            }
        }
    }

    private void clearSlotMaps() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                whiteSlotAtCell[row][col] = -1;
                blackSlotAtCell[row][col] = -1;
            }
        }
        for (int playerIndex = 0; playerIndex < 2; playerIndex++) {
            for (int slot = 0; slot < PIECE_SLOT_COUNT; slot++) {
                pieceRows[playerIndex][slot] = -1;
                pieceCols[playerIndex][slot] = -1;
                pieceAlive[playerIndex][slot] = false;
            }
        }
    }

    private void resetFlags() {
        isKingMoved[WHITE_INDEX] = false;
        isKingMoved[BLACK_INDEX] = false;
        isBishopMoved[WHITE_INDEX][0] = false;
        isBishopMoved[WHITE_INDEX][1] = false;
        isBishopMoved[BLACK_INDEX][0] = false;
        isBishopMoved[BLACK_INDEX][1] = false;
    }

    private int slotAt(int row, int col, int playerIndex) {
        if (!inBounds(row, col)) return -1;
        return playerIndex == WHITE_INDEX ? whiteSlotAtCell[row][col] : blackSlotAtCell[row][col];
    }

    private void setSlotAtCell(int playerIndex, int row, int col, int slot) {
        if (!inBounds(row, col)) return;
        if (playerIndex == WHITE_INDEX) {
            whiteSlotAtCell[row][col] = slot;
        } else {
            blackSlotAtCell[row][col] = slot;
        }
    }

    private int playerIndex(char player) {
        return player == 'W' ? WHITE_INDEX : BLACK_INDEX;
    }

    private int opponentIndex(int playerIndex) {
        return playerIndex == WHITE_INDEX ? BLACK_INDEX : WHITE_INDEX;
    }

    private int encode(int row, int col) {
        return row * size + col;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    private char[][] copyBoard(char[][] source) {
        char[][] copy = new char[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, size);
        }
        return copy;
    }

    private int[][] copyBoard(int[][] source) {
        int[][] copy = new int[size][size];
        for (int row = 0; row < size; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, size);
        }
        return copy;
    }

    private int[][] copyMatrix(int[][] source) {
        int[][] copy = new int[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, source[row].length);
        }
        return copy;
    }

    private boolean[][] copyMatrix(boolean[][] source) {
        boolean[][] copy = new boolean[source.length][source[0].length];
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, source[row].length);
        }
        return copy;
    }

    private void copyInto(char[][] source, char[][] target) {
        for (int row = 0; row < size; row++) {
            System.arraycopy(source[row], 0, target[row], 0, size);
        }
    }

    private void copyInto(int[][] source, int[][] target) {
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, target[row], 0, source[row].length);
        }
    }

    private void copyInto(boolean[][] source, boolean[][] target) {
        for (int row = 0; row < source.length; row++) {
            System.arraycopy(source[row], 0, target[row], 0, source[row].length);
        }
    }

    public static final class ChessBoardSnapshot {
        private final char[][] pieces;
        private final int[][] whiteSlotAtCell;
        private final int[][] blackSlotAtCell;
        private final int[][] pieceRows;
        private final int[][] pieceCols;
        private final boolean[][] pieceAlive;
        private final boolean[] isKingMoved;
        private final boolean[][] isBishopMoved;
        private final ProcessInputInChess lastMove;

        private ChessBoardSnapshot(
            char[][] pieces,
            int[][] whiteSlotAtCell,
            int[][] blackSlotAtCell,
            int[][] pieceRows,
            int[][] pieceCols,
            boolean[][] pieceAlive,
            boolean[] isKingMoved,
            boolean[][] isBishopMoved,
            ProcessInputInChess lastMove
        ) {
            this.pieces = pieces;
            this.whiteSlotAtCell = whiteSlotAtCell;
            this.blackSlotAtCell = blackSlotAtCell;
            this.pieceRows = pieceRows;
            this.pieceCols = pieceCols;
            this.pieceAlive = pieceAlive;
            this.isKingMoved = isKingMoved;
            this.isBishopMoved = isBishopMoved;
            this.lastMove = lastMove;
        }
    }
}