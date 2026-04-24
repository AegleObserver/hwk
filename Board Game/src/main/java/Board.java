import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Board implements ReversiBoardView {
    public static final int FIXED_SIZE = 8;
    private final int size;
    private final char[][] cells;
    private final Map<Character, Set<Integer>> legalMovesCache;
    public static final char EMPTY = '.';
    public static final char BLACK = 'B';
    public static final char WHITE = 'W';
    public static final char LEGAL_MOVE = '+';

    public Board(int size) {
        this.size = FIXED_SIZE;
        this.cells = new char[FIXED_SIZE][FIXED_SIZE];
        this.legalMovesCache = new HashMap<Character, Set<Integer>>();
        this.legalMovesCache.put(BLACK, new HashSet<Integer>());
        this.legalMovesCache.put(WHITE, new HashSet<Integer>());
        initialize();
    }

    public void initialize() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                cells[row][col] = EMPTY;
            }
        }
        int center = size / 2;
        cells[center - 1][center - 1] = WHITE;
        cells[center - 1][center] = BLACK;
        cells[center][center - 1] = BLACK;
        cells[center][center] = WHITE;
        refreshLegalMovesCache();
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public char getCell(int row, int col) {
        return cells[row][col];
    }

    public void setCell(int row, int col, char value) {
        cells[row][col] = value;
        refreshLegalMovesCache();
    }

    public char[][] copyCells() {
        char[][] copy = new char[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                copy[row][col] = cells[row][col];
            }
        }
        return copy;
    }

    public void loadCells(char[][] state) {
        if (state == null || state.length != size) {
            throw new IllegalArgumentException("Invalid board state size");
        }
        for (int row = 0; row < size; row++) {
            if (state[row] == null || state[row].length != size) {
                throw new IllegalArgumentException("Invalid board state row size");
            }
            for (int col = 0; col < size; col++) {
                cells[row][col] = state[row][col];
            }
        }
        refreshLegalMovesCache();
    }

    public void refreshLegalMovesCache() {
        Set<Integer> blackMoves = legalMovesCache.get(BLACK);
        Set<Integer> whiteMoves = legalMovesCache.get(WHITE);
        blackMoves.clear();
        whiteMoves.clear();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (cells[row][col] != EMPTY) continue;
                if (isLegalByRules(row, col, BLACK)) blackMoves.add(encode(row, col));
                if (isLegalByRules(row, col, WHITE)) whiteMoves.add(encode(row, col));
            }
        }
    }

    @Override
    public boolean hasLegalMove(char player) {
        Set<Integer> moves = legalMovesCache.get(player);
        return moves != null && !moves.isEmpty();
    }

    @Override
    public Set<Integer> getLegalMoves(char player) {
        Set<Integer> moves = legalMovesCache.get(player);
        if (moves == null) return Collections.emptySet();
        return Collections.unmodifiableSet(moves);
    }

    @Override
    public boolean isLegalMove(int row, int col, char player) {
        if (!inBounds(row, col)) return false;
        Set<Integer> moves = legalMovesCache.get(player);
        if (moves == null) return false;
        return moves.contains(encode(row, col));
    }

    public int processMove(int row, int col, char player) {
        if (!isLegalMove(row, col, player)) return -1;
        cells[row][col] = player;
        int flipped = flipPieces(row, col, player);
        refreshLegalMovesCache();
        return flipped;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    private int encode(int row, int col) {
        return row * size + col;
    }

    private char getOpponent(char player) {
        if (player == BLACK) return WHITE;
        if (player == WHITE) return BLACK;
        return EMPTY;
    }

    private boolean isLegalByRules(int row, int col, char player) {
        if (!inBounds(row, col)) return false;
        if (cells[row][col] != EMPTY) return false;
        char opponent = getOpponent(player);
        if (opponent == EMPTY) return false;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                if (canFlipInDirection(row, col, dr, dc, player, opponent)) return true;
            }
        }
        return false;
    }

    private boolean canFlipInDirection(int row, int col, int dr, int dc, char player, char opponent) {
        int r = row + dr;
        int c = col + dc;
        boolean seenOpponent = false;

        while (inBounds(r, c)) {
            char cell = cells[r][c];
            if (cell == opponent) {
                seenOpponent = true;
            } else if (cell == player) {
                return seenOpponent;
            } else {
                return false;
            }
            r += dr;
            c += dc;
        }
        return false;
    }

    private int flipPieces(int row, int col, char player) {
        char opponent = getOpponent(player);
        if (opponent == EMPTY) return 0;

        int totalFlipped = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                totalFlipped += flipInDirection(row, col, dr, dc, player, opponent);
            }
        }
        return totalFlipped;
    }

    private int flipInDirection(int row, int col, int dr, int dc, char player, char opponent) {
        if (!canFlipInDirection(row, col, dr, dc, player, opponent)) return 0;

        int flipped = 0;
        int r = row + dr;
        int c = col + dc;
        while (inBounds(r, c) && cells[r][c] == opponent) {
            cells[r][c] = player;
            flipped++;
            r += dr;
            c += dc;
        }
        return flipped;
    }

    @Override
    public boolean isFull() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (cells[row][col] == EMPTY) return false;
            }
        }
        return true;
    }

    @Override
    public String toAsciiString(char currentPlayer) {
        StringConstructer sc = new StringConstructer();
        Set<Integer> legalMoves = legalMovesCache.get(currentPlayer);
        sc.append("  ");
        for (char c = 'A'; c < 'A' + size; c++) sc.append(' ').append(c);
        sc.append('\n');
        for (int row = 0; row < size; row++) {
            sc.append(String.format("%2d", row + 1));
            for (int col = 0; col < size; col++) {
                char ch = cells[row][col];
                String draw = ".";
                if (ch == BLACK) draw = "B";
                else if (ch == WHITE) draw = "W";
                else if (legalMoves != null && legalMoves.contains(encode(row, col))) draw = String.valueOf(LEGAL_MOVE);
                sc.append(' ').append(draw);
            }
            sc.append('\n');
        }
        return sc.toString();
    }

    @Override
    public String toAsciiString() {
        return toAsciiString(EMPTY);
    }
}