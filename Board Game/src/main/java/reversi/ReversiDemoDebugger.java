package reversi;

import common.GameAction;
import common.TurnResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class ReversiDemoDebugger {
    private static final boolean DebuggerEnabled = false;
    private static final Path GAME_SOURCE = Path.of("src", "main", "java", "reversi", "ReversiGame.java");

    private final int boardSize;
    private final List<String> steps = new ArrayList<String>();
    private boolean recording;

    ReversiDemoDebugger(int boardSize) {
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
        System.out.println("Reversi demo debugger started.");
    }

    void record(String rawInput, GameAction action, TurnResult result) {
        if (!recording || rawInput == null) return;
        if (result != TurnResult.SUCCESS && result != TurnResult.GAME_OVER && result != TurnResult.PASS_TURN) return;
        String step = rawInput.trim().toUpperCase();
        if (!step.isEmpty()) steps.add(step);
    }

    String finishIfSuccessful(boolean finished) {
        if (!recording) return "";
        recording = false;
        if (!finished) return "Reversi demo debugger stopped without solidifying because the game was not finished.";
        try {
            solidify();
            return "Reversi demo debugger solidified " + steps.size() + " steps.";
        } catch (IOException e) {
            return "Reversi demo debugger recorded steps, but failed to update source: " + e.getMessage();
        }
    }

    private void solidify() throws IOException {
        if (!Files.exists(GAME_SOURCE)) {
            throw new IOException("cannot find " + GAME_SOURCE.toAbsolutePath());
        }

        String source = Files.readString(GAME_SOURCE, StandardCharsets.UTF_8);
        String oldBlockPattern = "(?s)public List<String> getDemoInputs\\(\\) \\{.*?\\n    \\}\\n\\n    @Override\\n    public String getDemoSummary";
        String newBlock = "public List<String> getDemoInputs() {\n"
            + "        // Recorded by ReversiDemoDebugger.\n"
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
}
