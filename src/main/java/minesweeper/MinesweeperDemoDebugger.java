package minesweeper;

import common.GameAction;
import common.TurnResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class MinesweeperDemoDebugger {
    private static final boolean DebuggerEnabled = false;
    private static final Path GAME_SOURCE = Path.of("src", "main", "java", "minesweeper", "MinesweeperGame.java");

    private final long seed;
    private final int boardSize;
    private final List<String> steps = new ArrayList<String>();
    private boolean recording;
    private boolean solidified;

    MinesweeperDemoDebugger(long seed, int boardSize) {
        this.seed = seed;
        this.boardSize = boardSize;
    }

    static boolean isEnabled() {
        return DebuggerEnabled;
    }

    boolean isRecording() {
        return recording;
    }

    void start() {
        steps.clear();
        recording = true;
        solidified = false;
        System.out.println("Minesweeper demo debugger started. Seed: " + seed);
    }

    void record(String rawInput, GameAction action, TurnResult result) {
        if (!recording || rawInput == null) return;
        if (result != TurnResult.SUCCESS && result != TurnResult.GAME_OVER) return;

        String step = rawInput.trim().toUpperCase();
        if (step.isEmpty()) return;
        if (action != null && "flag".equals(action.id)) {
            step = "FLAG " + step;
        }
        steps.add(step);
        System.out.println("Minesweeper demo debugger recorded: " + step);
    }

    String finishIfCleared(boolean cleared) {
        if (!recording) return "";
        recording = false;

        if (!cleared) {
            return "Demo debugger kept the seed " + seed + ", but did not solidify because the game was not cleared.";
        }

        try {
            solidify();
            solidified = true;
            return "Demo debugger solidified seed " + seed + " with " + steps.size() + " steps.";
        } catch (IOException e) {
            return "Demo debugger recorded seed " + seed + ", but failed to update source: " + e.getMessage();
        }
    }

    private void solidify() throws IOException {
        if (!Files.exists(GAME_SOURCE)) {
            throw new IOException("cannot find " + GAME_SOURCE.toAbsolutePath());
        }

        String source = Files.readString(GAME_SOURCE, StandardCharsets.UTF_8);
        source = source.replaceFirst(
            "private static final long DEMO_SEED = \\d+L;",
            "private static final long DEMO_SEED = " + seed + "L;"
        );

        String oldBlockPattern = "(?s)public List<String> getDemoInputs\\(\\) \\{.*?\\n    \\}\\n\\n    @Override\\n    public String getDemoSummary";
        String newBlock = "public List<String> getDemoInputs() {\n"
            + "        // Recorded by MinesweeperDemoDebugger with seed " + seed + ".\n"
            + "        return List.of(\n"
            + formatSteps()
            + "        );\n"
            + "    }\n\n"
            + "    @Override\n"
            + "    public String getDemoSummary";

        source = source.replaceFirst(oldBlockPattern, java.util.regex.Matcher.quoteReplacement(newBlock));
        Files.writeString(GAME_SOURCE, source, StandardCharsets.UTF_8);
    }

    private String formatSteps() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i % boardSize == 0) builder.append("            ");
            builder.append('"').append(steps.get(i)).append('"');
            if (i < steps.size() - 1) builder.append(", ");
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        if (solidified) return "recorded demo seed " + seed + " is solidified";
        if (recording) return "recording demo seed " + seed + " with " + steps.size() + " steps";
        return "ready to record demo seed " + seed;
    }
}
