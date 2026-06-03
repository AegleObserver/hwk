package chess;

public final class ProcessInputInChess {
    public final int fromRow;
    public final int fromCol;
    public final int toRow;
    public final int toCol;

    ProcessInputInChess(int fromRow, int fromCol, int toRow, int toCol) {
        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
    }

    public static ProcessInputInChess parse(String rawInput) {
        if (rawInput == null) return null;

        String normalized = rawInput.trim().toLowerCase();
        if (normalized.isEmpty()) return null;

        String[] parts = normalized.split("\\s+");
        String fromText;
        String toText;
        if (parts.length == 3 && "m".equals(parts[0])) {
            fromText = parts[1];
            toText = parts[2];
        } else {
            String compact = normalized.replaceAll("\\s+", "");
            if (compact.length() != 5 || compact.charAt(0) != 'm') return null;
            fromText = compact.substring(1, 3);
            toText = compact.substring(3, 5);
        }

        int[] from = parseSquare(fromText);
        int[] to = parseSquare(toText);
        if (from == null || to == null) return null;

        return new ProcessInputInChess(from[0], from[1], to[0], to[1]);
    }

    private static int[] parseSquare(String square) {
        if (square == null || square.length() != 2) return null;

        char file = square.charAt(0);
        char rank = square.charAt(1);
        if (file < 'a' || file > 'h') return null;
        if (rank < '1' || rank > '8') return null;

        // Chess coordinates use A1 as bottom-left, so convert rank to top-based row index.
        return new int[] {'8' - rank, file - 'a'};
    }
}