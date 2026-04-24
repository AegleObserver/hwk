import java.util.Random;

public class MinesweeperBoard implements MinesweeperBoardView {
    private static final int DEFAULT_MINE_COUNT = 10;

    private final int size;
    private final int mineCount;
    private final String[][] cells;
    private final String[][] answer;
    private final Random random;
    private boolean minesPlaced;

    public MinesweeperBoard() {
        this(FIXED_SIZE, DEFAULT_MINE_COUNT);
    }

    public MinesweeperBoard(int size, int mineCount) {
        this.size = FIXED_SIZE;
        this.mineCount = DEFAULT_MINE_COUNT;
        this.cells = new String[FIXED_SIZE][FIXED_SIZE];
        this.answer = new String[FIXED_SIZE][FIXED_SIZE];
        this.random = new Random();
        initialize();
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getCell(int row, int col) {
        return cells[row][col];
    }

    @Override
    public String[][] copyCells() {
        String[][] copy = new String[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                copy[row][col] = cells[row][col];
            }
        }
        return copy;
    }

    @Override
    public void loadCells(String[][] state) {
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
    }

    @Override
    public void initialize() {
        initializeBoardState();
    }

    @Override
    public boolean reveal(int row, int col) {
        if (!inBounds(row, col)) return false;
        if (FLAG.equals(cells[row][col])) return false;
        if (!UNFLIPPED.equals(cells[row][col])) return true;

        if (!minesPlaced) {
            placeMinesExcluding(row, col);
            fillNumbers();
            minesPlaced = true;
        }

        cells[row][col] = answer[row][col];
        return true;
    }

    @Override
    public boolean toggleFlag(int row, int col) {
        if (!inBounds(row, col)) return false;
        if (UNFLIPPED.equals(cells[row][col])) {
            cells[row][col] = FLAG;
            return true;
        }
        if (FLAG.equals(cells[row][col])) {
            cells[row][col] = UNFLIPPED;
            return true;
        }
        return false;
    }

    public boolean revealRandomSafeCell() {
        if (!minesPlaced) {
            placeMinesExcluding(-1, -1);
            fillNumbers();
            minesPlaced = true;
        }

        int safeCandidates = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (MINE.equals(answer[row][col])) continue;
                if (UNFLIPPED.equals(cells[row][col])) safeCandidates++;
            }
        }
        if (safeCandidates == 0) return false;

        int target = random.nextInt(safeCandidates);
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (MINE.equals(answer[row][col])) continue;
                if (!UNFLIPPED.equals(cells[row][col])) continue;
                if (target-- > 0) continue;
                cells[row][col] = answer[row][col];
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isMineRevealed() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (MINE.equals(cells[row][col])) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCleared() {
        return isAllSafeCellsRevealed();
    }

    public boolean isAllSafeCellsRevealed() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (!MINE.equals(answer[row][col]) && (UNFLIPPED.equals(cells[row][col]) || FLAG.equals(cells[row][col]))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void initializeBoardState() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                cells[row][col] = UNFLIPPED;
                answer[row][col] = EMPTY;
            }
        }
        minesPlaced = false;
    }

    private void placeMinesExcluding(int firstRow, int firstCol) {
        int placed = 0;
        while (placed < mineCount) {
            int row = random.nextInt(size);
            int col = random.nextInt(size);
            if (firstRow >= 0 && row == firstRow && col == firstCol) continue;
            if (MINE.equals(answer[row][col])) continue;
            answer[row][col] = MINE;
            placed++;
        }
    }

    private void fillNumbers() {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (MINE.equals(answer[row][col])) continue;
                int aroundMines = countNeighborMines(row, col);
                answer[row][col] = (aroundMines == 0) ? EMPTY : String.valueOf(aroundMines);
            }
        }
    }

    private int countNeighborMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = row + dr;
                int nc = col + dc;
                if (inBounds(nr, nc) && MINE.equals(answer[nr][nc])) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }
}
