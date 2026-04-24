public interface MinesweeperBoardView {
    int FIXED_SIZE = 8;
    String UNFLIPPED = "UNFLIPPED";
    String EMPTY = "EMPTY";
    String MINE = "MINE";
    String FLAG = "FLAG";

    int getSize();

    String getCell(int row, int col);

    String[][] copyCells();

    void loadCells(String[][] state);

    void initialize();

    boolean reveal(int row, int col);

    boolean toggleFlag(int row, int col);

    boolean isMineRevealed();

    boolean isCleared();
}
