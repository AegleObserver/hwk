package chess;

import common.GameAction;
import common.TurnResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class ChessDemoDebugger {
    private static final boolean DebuggerEnabled = false;
    private static final Path GAME_SOURCE = Path.of("src", "main", "java", "chess", "ChessGame.java");
    private static final int STEPS_PER_LINE = 2;

    private final List<String> steps = new ArrayList<String>();
    private boolean recording;

    static boolean isEnabled() {
        return DebuggerEnabled;
    }

    boolean isRecording() {
        return recording;
    }

    void start() {
        steps.clear();
        recording = true;
        System.out.println("Chess demo debugger started.");
    }

    void record(String rawInput, GameAction action, TurnResult result) {
        if (!recording || rawInput == null) return;
        if (result != TurnResult.SUCCESS && result != TurnResult.GAME_OVER && result != TurnResult.PASS_TURN) return;
        String step = rawInput.trim().toLowerCase();
        if (!step.isEmpty()) steps.add(step);
    }

    String finishIfSuccessful(boolean finished) {
        if (!recording) return "";
        recording = false;
        if (!finished) return "Chess demo debugger stopped without solidifying because the game was not finished.";
        try {
            solidify();
            return "Chess demo debugger solidified " + steps.size() + " steps.";
        } catch (IOException e) {
            return "Chess demo debugger recorded steps, but failed to update source: " + e.getMessage();
        }
    }

    private void solidify() throws IOException {
        if (!Files.exists(GAME_SOURCE)) {
            throw new IOException("cannot find " + GAME_SOURCE.toAbsolutePath());
        }

        String source = Files.readString(GAME_SOURCE, StandardCharsets.UTF_8);
        String oldBlockPattern = "(?s)public List<String> getDemoInputs\\(\\) \\{.*?\\n\\}\\n    @Override\\n    public String getDemoSummary";
        String newBlock = "public List<String> getDemoInputs() {\n"
            + "    // Recorded by ChessDemoDebugger.\n"
            + "    return List.of(\n"
            + formatSteps()
            + "    );\n"
            + "}\n"
            + "    @Override\n"
            + "    public String getDemoSummary";

        source = source.replaceFirst(oldBlockPattern, java.util.regex.Matcher.quoteReplacement(newBlock));
        Files.writeString(GAME_SOURCE, source, StandardCharsets.UTF_8);
    }

    private String formatSteps() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i % STEPS_PER_LINE == 0) builder.append("        ");
            builder.append('"').append(steps.get(i)).append('"');
            if (i < steps.size() - 1) builder.append(", ");
            builder.append('\n');
        }
        return builder.toString();
    }
}
