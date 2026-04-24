public class MinesweeperBoardAdapter implements BoardView {
    private final MinesweeperBoardView board;

    public MinesweeperBoardAdapter(MinesweeperBoardView board) {
        this.board = board;
    }

    @Override
    public int getSize() {
        return board.getSize();
    }

    @Override
    public char getCell(int row, int col) {
        String cell = board.getCell(row, col);
        if (MinesweeperBoardView.UNFLIPPED.equals(cell)) return ' ';
        if (MinesweeperBoardView.FLAG.equals(cell)) return 'F';
        if (MinesweeperBoardView.EMPTY.equals(cell)) return ' ';
        if (MinesweeperBoardView.MINE.equals(cell)) return 'x';
        if (cell != null && cell.length() == 1) return cell.charAt(0);
        return '?';
    }

    @Override
    public boolean isFull() {
        return board.isMineRevealed() || board.isCleared();
    }

    @Override
    public String toAsciiString() {
        StringConstructer sc = new StringConstructer();
        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                sc.append(getCell(row, col));
                if (col < board.getSize() - 1) sc.append(' ');
            }
            if (row < board.getSize() - 1) sc.append('\n');
        }
        return sc.toString();
    }
}
