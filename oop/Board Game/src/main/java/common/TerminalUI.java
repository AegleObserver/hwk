package common;

import common.ui.GameUiBridge;
import common.ui.GameUiRenderer;
import common.ui.GameUiState;
import java.util.List;

public class TerminalUI implements GameUiBridge {
    private static final int INITIAL_GAMES = 4;
    private static final int MAX_ALLOWED_GAMES = 10;

    private final GameSession[] games = new GameSession[MAX_ALLOWED_GAMES];
    private final List<GameDefinition> gameDefinitions = GameRegistry.defaultGames();
    private int currentGameCount = INITIAL_GAMES;
    private int activeGameIndex = 0;
    private volatile boolean demoRunning = false;
    private volatile boolean demoPaused = false;
    private String messageText = "";
    private GameUiRenderer renderer;

    public TerminalUI() {
        initializeGames();
    }

    public void start() {
        try {
            new common.ui.TerminalUiPlugin().start(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attach(GameUiRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public GameUiState snapshot() {
        return new GameUiState(
            "Board Games Terminal",
            currentGame(),
            gameDefinitions,
            currentGame().getActionButtons(),
            activeGameIndex,
            currentGameCount,
            demoRunning,
            demoPaused,
            buildGameInfoText(),
            buildCurrentPlayerText(),
            buildScoreText(),
            buildBoardIndexText(),
            messageText
        );
    }

    @Override
    public void onRawInput(String rawInput) {
        String input = rawInput == null ? "" : rawInput.trim().toUpperCase();
        if (input.isEmpty()) {
            setMessage("Err: invalid input.");
            return;
        }

        ProcessInput parsed = ProcessInput.parse(input);
        if (parsed.action == ProcessInput.Action.QUIT) {
            onQuitRequested();
            return;
        }
        if (parsed.action == ProcessInput.Action.UNDO) {
            if (currentGame().undoLastMove()) {
                refreshView();
                setMessage("Info: undo completed.");
            } else {
                setMessage("Err: undo is unavailable in this mode.");
            }
            return;
        }
        if (parsed.action == ProcessInput.Action.SWITCH_BOARD) {
            onSwitchGameRequested(parsed.value - 1);
            return;
        }

        processMove(rawInput);
    }

    @Override
    public void onAction(GameAction action, String rawInput) {
        handleActionButton(action, rawInput);
    }

    @Override
    public void onResetRequested() {
        if (currentGame() == null) return;
        games[activeGameIndex] = currentGame().newGame(8);
        refreshView();
        setMessage("Info: board " + (activeGameIndex + 1) + " reset.");
    }

    @Override
    public void onNewGameRequested() {
        if (currentGameCount >= MAX_ALLOWED_GAMES) {
            setMessage("Err: cannot create new game. Maximum board count reached.");
            return;
        }
        GameDefinition defaultType = gameDefinitions.get(1);
        GameDefinition gameType = renderer == null
            ? defaultType
            : renderer.promptGameType(gameDefinitions, defaultType, "Game Type");
        if (gameType == null) {
            setMessage("Info: new game creation canceled.");
            return;
        }
        games[currentGameCount] = gameType.create();
        currentGameCount++;
        activeGameIndex = currentGameCount - 1;
        refreshView();
        setMessage("Info: new game added: " + games[activeGameIndex].getDisplayName());
    }

    @Override
    public void onSwitchGameRequested(int index) {
        if (index < 0 || index >= currentGameCount) return;
        activeGameIndex = index;
        refreshView();
        if (isGameFinished(games[index])) {
            setMessage("Info: switched to board " + (index + 1) + " (finished, view only).");
        } else {
            setMessage("Info: switched to board " + (index + 1) + ".");
        }
    }

    @Override
    public void onQuitRequested() {
        if (renderer != null) {
            renderer.close();
        }
        System.exit(0);
    }

    private void refreshView() {
        if (renderer != null) {
            renderer.render(snapshot());
        }
    }

    private void setMessage(String text) {
        messageText = text == null ? "" : text;
        refreshView();
    }

    private GameSession currentGame() {
        return games[activeGameIndex];
    }

    private void initializeGames() {
        for (int i = 0; i < currentGameCount; i++) {
            games[i] = gameDefinitions.get(typeByIndex(i)).create();
        }
    }

    private int typeByIndex(int index) {
        if (index < gameDefinitions.size()) return index;
        return gameDefinitions.size() - 1;
    }

    private boolean processMove(String rawInput) {
        if (rejectDuringDemo()) return false;
        TurnResult result = currentGame().handleRawInput(translateCoordinateInput(rawInput));
        currentGame().recordDemoDebuggerStep(rawInput, null, result);
        switch (result) {
            case SUCCESS:
                setMessage("Info: move accepted.");
                return true;
            case INVALID_INPUT:
                if ("Chess".equalsIgnoreCase(currentGame().getDisplayName())) {
                    setMessage("Err: invalid input. Enter m A1-H8 A1-H8 instead.");
                } else {
                    setMessage("Err: invalid input. Enter A1-" + (char) ('A' + currentBoardSize() - 1) + currentBoardSize() + " instead.");
                }
                return false;
            case OCCUPIED:
                setMessage("Err: occupied square.");
                return false;
            case EMPTY_SOURCE:
                setMessage("Err: source square is empty.");
                return false;
            case WRONG_SIDE:
                setMessage("Err: source piece belongs to the opponent.");
                return false;
            case ILLEGAL_MOVE:
                setMessage("Err: move not permitted.");
                return false;
            case PASS_TURN:
                setMessage("Info: turn passed. No legal move.");
                return true;
            case GAME_OVER:
                finishDemoDebuggerIfNeeded();
                handleCurrentBoardFinished();
                return true;
            default:
                setMessage("Err: unexpected move state.");
                return false;
        }
    }

    private boolean handleActionButton(GameAction action, String rawInput) {
        if (rejectDuringDemo()) return false;
        String input = rawInput == null ? "" : rawInput.trim().toUpperCase();
        if (input.isEmpty()) {
            setMessage("Err: enter a coordinate or board number first.");
            return false;
        }
        if ("H".equals(input) && "Minesweeper".equalsIgnoreCase(currentGame().getDisplayName())) {
            return processMove("H");
        }

        ProcessInput parsed = ProcessInput.parse(input);
        if (parsed.action == ProcessInput.Action.QUIT) {
            onQuitRequested();
            return true;
        }

        if (parsed.action == ProcessInput.Action.UNDO) {
            setMessage("Err: undo is unavailable in this mode.");
            return false;
        }

        if (parsed.action == ProcessInput.Action.SWITCH_BOARD) {
            return processMove(parsed);
        }

        if (isCurrentGameFinished()) {
            return false;
        }

        TurnResult result = currentGame().handleAction(action, translateCoordinateInput(input));
        currentGame().recordDemoDebuggerStep(input, action, result);
        switch (result) {
            case SUCCESS:
                setMessage("Info: " + action.label + " completed.");
                return true;
            case INVALID_INPUT:
                setMessage("Err: invalid input.");
                return false;
            case OCCUPIED:
                setMessage("Err: occupied square.");
                return false;
            case EMPTY_SOURCE:
                setMessage("Err: source square is empty.");
                return false;
            case WRONG_SIDE:
                setMessage("Err: source piece belongs to the opponent.");
                return false;
            case GAME_OVER:
                finishDemoDebuggerIfNeeded();
                handleCurrentBoardFinished();
                return true;
            default:
                setMessage("Err: unexpected move state.");
                return false;
        }
    }

    private String buildGameInfoText() {
        String summary = currentGame().getStatusSummary();
        String commands = buildCommandsInfo();
        StringConstructor sc = new StringConstructor();
        sc.append(" Game ").append(String.valueOf(activeGameIndex + 1)).append(' ').append(currentGame().getDisplayName()).append('\n');
        if (summary == null || summary.isEmpty()) {
            sc.append('\n');
        } else {
            sc.append(summary).append('\n');
        }
        sc.append('\n').append("Commands").append('\n').append(commands);
        return sc.toString();
    }

    private String buildCurrentPlayerText() {
        if (currentGame().getCurrentPlayer() == 'B') return "P1:Tairitsu ●\nP2:Hikari";
        if (currentGame().getCurrentPlayer() == 'W') return "P1:Tairitsu\nP2:Hikari ●";
        return "";
    }

    private String buildScoreText() {
        return currentGame().shouldShowScore() ? currentGame().getScoreSummary() : "";
    }

    private String buildBoardIndexText() {
        StringConstructor sc = new StringConstructor();
        for (int i = 0; i < currentGameCount; i++) {
            sc.append(String.valueOf(i + 1)).append(".").append(games[i].getDisplayName());
            if (i == activeGameIndex) sc.append(" <- current");
            if (isGameFinished(games[i])) sc.append(" [Finished]");
            if (i < currentGameCount - 1) sc.append('\n');
        }
        return sc.toString();
    }

    private String buildCommandsInfo() {
        boolean isMinesweeper = "Minesweeper".equalsIgnoreCase(currentGame().getDisplayName());
        boolean isChessBoard = "Chess".equalsIgnoreCase(currentGame().getDisplayName());
        StringConstructor sc = new StringConstructor();
        sc.append(isMinesweeper ? "Input 'H' to get hint\n" : "Input 'U' to undo\n");
        sc.append(isChessBoard ? "Input m A1-H8 A1-H8 to move\n" : "Input A1-H8 to move\n");
        sc.append("Input 1-").append(String.valueOf(currentGameCount)).append(" to switch games\n");
        sc.append("Input 'Q' to quit.");
        return sc.toString();
    }

    private boolean isGameFinished(GameSession game) {
        return game.isFinished();
    }

    private boolean isCurrentGameFinished() {
        return currentGame().isFinished();
    }

    private boolean processMove(ProcessInput parsedInput) {
        if (parsedInput == null) {
            return false;
        }

        switch (parsedInput.action) {
            case SWITCH_BOARD:
                if (parsedInput.value < 1 || parsedInput.value > currentGameCount) {
                    setMessage("Err: invalid board number.");
                    return false;
                }
                onSwitchGameRequested(parsedInput.value - 1);
                return true;
            case QUIT:
                onQuitRequested();
                return true;
            case UNDO:
                setMessage("Err: undo is unavailable in this mode.");
                return false;
            default:
                setMessage("Err: invalid input.");
                return false;
        }
    }

    private boolean rejectDuringDemo() {
        if (!demoRunning) return false;
        setMessage("Info: demo is running. Use Demo button to pause/resume.");
        return true;
    }

    private void handleCurrentBoardFinished() {
        int finishedBoard = activeGameIndex + 1;
        int targetIndex = findNextAvailableGameIndex();
        String finishSummary = currentGame().getFinishSummary();

        if (targetIndex == -1) {
            String msg = "Board " + finishedBoard + " finished.";
            if (finishSummary != null && !finishSummary.isEmpty()) msg += " " + finishSummary;
            msg += "\nNo remnant boards. All games over.";
            if (renderer != null) {
                renderer.showMessage("Game Over", msg);
            }
            setMessage("All boards finished. Game over.");
            onQuitRequested();
            return;
        }

        String msg = "Board " + finishedBoard + " finished.";
        if (finishSummary != null && !finishSummary.isEmpty()) msg += " " + finishSummary;
        msg += "\nSwitching to board " + (targetIndex + 1) + ".";
        if (renderer != null) {
            renderer.showMessage("Board Finished", msg);
        }
        activeGameIndex = targetIndex;
        refreshView();
        setMessage("Switched to board " + (targetIndex + 1) + ".");
    }

    private int findNextAvailableGameIndex() {
        for (int i = 0; i < currentGameCount; i++) {
            if (!isGameFinished(games[i])) return i;
        }
        return -1;
    }

    private void finishDemoDebuggerIfNeeded() {
        if (!currentGame().isDemoDebuggerRecording()) return;
        String result = currentGame().finishDemoDebuggerIfSuccessful();
        if (result == null || result.isEmpty()) return;
        if (renderer != null) {
            renderer.showMessage("Demo Debugger", result);
        }
    }

    private String translateCoordinateInput(String rawInput) {
        if (rawInput == null) return null;
        String input = rawInput.trim().toUpperCase();
        if (input.isEmpty()) return input;
        if ("Chess".equalsIgnoreCase(currentGame().getDisplayName())) return input;
        if (input.length() == 1) return input;
        if ("AUTO".equals(input)) return input;
        if (input.startsWith("FLAG ")) {
            return "FLAG " + translateCoordinateInput(input.substring(5));
        }

        char file = input.charAt(0);
        if (!Character.isLetter(file)) return input;

        String rankPart = input.substring(1);
        try {
            int rank = Integer.parseInt(rankPart);
            int size = currentGame().getBoard().getSize();
            if (rank < 1 || rank > size) return input;
            int internalRank = size - rank + 1;
            return String.valueOf(file) + internalRank;
        } catch (NumberFormatException e) {
            return input;
        }
    }

    private int currentBoardSize() {
        return currentGame().getBoard().getSize();
    }

}