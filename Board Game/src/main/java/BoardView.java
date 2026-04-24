public interface BoardView {
    int getSize();

    char getCell(int row, int col);

    boolean isFull();

    String toAsciiString();
}