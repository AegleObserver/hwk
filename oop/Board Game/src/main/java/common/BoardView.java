package common;

public interface BoardView {
    int getSize();

    char getCell(int row, int col);

    default boolean isHiddenCell(int row, int col) {
        return false;
    }

    default boolean isMarkedCell(int row, int col) {
        return false;
    }

    default boolean isDangerCell(int row, int col) {
        return false;
    }

    default boolean isLegalMove(int row, int col, char player) {
        return false;
    }

    boolean isFull();

    String toAsciiString();
}