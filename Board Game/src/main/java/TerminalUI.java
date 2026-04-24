import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import java.awt.Font;
import javax.swing.UIManager;

public class TerminalUI {
    private enum GameType {
        PEACE,
        REVERSI,
        MINESWEEPER
    }

    private static final int FONT_SIZE = 26;//控制字体大小
    private static final int INITIAL_GAMES = 3;
    private static final int MAX_ALLOWED_GAMES = 5;
    private int cellWidthChars = 1;
    private int cellHeightChars = 1;
    private final GameSession[] games = new GameSession[MAX_ALLOWED_GAMES];
    private int currentGameCount = INITIAL_GAMES;
    private int activeGameIndex = 0;
    private Label gameInfoLabel;
    private Label currentPlayerLabel;
    private Label scoreLabel;
    private Label boardIndexLabel;
    private Label messageLabel;
    private Panel mainPanel;
    private Panel boardPanel;
    private Panel actionPanel;
    private Button flipButton;
    private Button flagButton;
    private Button playButton;
    private BasicWindow window;
    private MultiWindowTextGUI gui;
    private SwingTerminalFrame terminalFrame;

    public TerminalUI() {
        initializeGames();
    }

    private static GameType promptGameType(GameType defaultType) {
        Object[] options = {"Peace", "Reversi", "Minesweeper"};
        int defaultOptionIndex = 1;
        if (defaultType == GameType.PEACE) defaultOptionIndex = 0;
        else if (defaultType == GameType.MINESWEEPER) defaultOptionIndex = 2;

        int choice = javax.swing.JOptionPane.showOptionDialog(
            null,
            "Choose mode:",
            "Game Type",
            javax.swing.JOptionPane.DEFAULT_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[defaultOptionIndex]
        );

        if (choice == javax.swing.JOptionPane.CLOSED_OPTION) return null;
        if (choice == 0) return GameType.PEACE;
        if (choice == 2) return GameType.MINESWEEPER;
        return GameType.REVERSI;
    }

    public void start() {
        try {
            setupUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() throws Exception {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        String[] preferredFonts = {"Microsoft YaHei", "Microsoft YaHei UI", "微软雅黑", "Consolas", "Monospaced"};
        java.util.List<String> installed = java.util.Arrays.asList(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        String chosen = null;
        for (String f : preferredFonts) { if (installed.contains(f)) { chosen = f; break; } }
        if (chosen == null) chosen = "Monospaced";
        Font awtFont = new Font(chosen, Font.BOLD, FONT_SIZE);
        SwingTerminalFontConfiguration fontConfig;
        try { fontConfig = SwingTerminalFontConfiguration.newInstance(awtFont); }
        catch (IllegalArgumentException e) {
            String[] monos = {"Consolas", "Courier New", "Lucida Console", "DejaVu Sans Mono", "Monospaced"};
            String monoChosen = null;
            for (String m : monos) if (installed.contains(m)) { monoChosen = m; break; }
            if (monoChosen == null) monoChosen = "Monospaced";
            awtFont = new Font(monoChosen, Font.PLAIN, FONT_SIZE);
            fontConfig = SwingTerminalFontConfiguration.newInstance(awtFont);
        }

        java.awt.FontMetrics fm = new java.awt.Canvas().getFontMetrics(awtFont);
        int charPixelWidth = fm.getMaxAdvance();
        int charPixelHeight = fm.getHeight();
        cellHeightChars = 1;
        cellWidthChars = Math.max(1, (int) Math.ceil((double) charPixelHeight / (double) charPixelWidth * cellHeightChars));

        int totalCols = currentGame().getBoard().getSize() + 1;
        int infoPanelCols = 30;
        int leftMarginCols = 6;
        int rightMarginCols = 6;
        int topRows = 5;
        int bottomRows = 6;

        int windowWidthChars = totalCols * cellWidthChars + infoPanelCols + leftMarginCols + rightMarginCols;
        int windowHeightChars = currentGame().getBoard().getSize() * cellHeightChars + topRows + bottomRows;
        int windowWidth = Math.max(120, windowWidthChars);
        int windowHeight = Math.max(36, windowHeightChars);

        terminalFrame = new SwingTerminalFrame(
            "Reversi Deluxe",
            new TerminalSize(windowWidth, windowHeight),
            TerminalEmulatorDeviceConfiguration.getDefault(),
            fontConfig,
            TerminalEmulatorColorConfiguration.getDefault(),
            TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode
        );

        terminalFrame.pack();
        terminalFrame.setVisible(true);
        try { java.awt.Color white = java.awt.Color.WHITE; terminalFrame.getContentPane().setBackground(white); terminalFrame.setBackground(white); } catch (Exception ignored) {}

        Screen screen = new TerminalScreen(terminalFrame);
        screen.startScreen();

        mainPanel = new Panel(new BorderLayout());
        Label title = new Label(" BOARD GAMES ");
        title.addStyle(SGR.BOLD);
        title.setForegroundColor(TextColor.ANSI.BLACK);
        mainPanel.addComponent(title, BorderLayout.Location.TOP);

        rebuildBoardPanel();

        Panel outerInfoPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        
        Panel leftInfoPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        gameInfoLabel = new Label("");
        gameInfoLabel.setBackgroundColor(TextColor.ANSI.WHITE);
        updateGameInfoDisplay();
        leftInfoPanel.addComponent(gameInfoLabel);
        currentPlayerLabel = new Label("");
        currentPlayerLabel.setBackgroundColor(TextColor.ANSI.WHITE);
        exhibitPlayers();
        leftInfoPanel.addComponent(currentPlayerLabel);
        scoreLabel = new Label("");
        scoreLabel.setBackgroundColor(TextColor.ANSI.WHITE);
        updateScoreDisplay();
        leftInfoPanel.addComponent(scoreLabel);
        outerInfoPanel.addComponent(leftInfoPanel.withBorder(Borders.singleLine("Game info")));
        
        Panel rightInfoPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        boardIndexLabel = new Label("");
        boardIndexLabel.setBackgroundColor(TextColor.ANSI.WHITE);
        updateBoardIndexDisplay();
        rightInfoPanel.addComponent(boardIndexLabel);
        outerInfoPanel.addComponent(rightInfoPanel.withBorder(Borders.singleLine("More games")));
        
        mainPanel.addComponent(outerInfoPanel, BorderLayout.Location.RIGHT);

        Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        inputPanel.addComponent(new Label("Hint: print \"Q\" to exit; \n\"H\" to hint in Minesweeper.").setBackgroundColor(TextColor.ANSI.WHITE));
        TextBox inputBox = new TextBox(new TerminalSize(5, 1));
        inputPanel.addComponent(inputBox);
        messageLabel = new Label(" Welcome!").setForegroundColor(TextColor.ANSI.GREEN);
        actionPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        flipButton = new Button("Flip", () -> handleMinesweeperButton(ProcessInput.flip(), inputBox));
        flagButton = new Button("Flag", () -> handleMinesweeperButton(ProcessInput.flag(), inputBox));
        playButton = new Button("Play", () -> {
            String input = inputBox.getText().trim().toUpperCase();
            if (input.isEmpty()) return;

            ProcessInput parsed = ProcessInput.parse(input);
            if (parsed.action == ProcessInput.Action.QUIT) {
                terminalFrame.dispose();
                System.exit(0);
            }

            if (parsed.action == ProcessInput.Action.UNDO) {
                if (currentGame().undoLastMove()) {
                    updateBoardDisplay();
                    updateGameInfoDisplay();
                    exhibitPlayers();
                    updateScoreDisplay();
                    updateBoardIndexDisplay();
                    messageLabel.setText("Undo success.");
                    messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
                    inputBox.setText("");
                } else {
                    messageLabel.setText("Err: no move to undo.");
                    messageLabel.setForegroundColor(TextColor.ANSI.RED);
                }
                return;
            }

            if (parsed.action == ProcessInput.Action.SWITCH_BOARD) {
                processMove(parsed);
                inputBox.setText("");
                return;
            }

            if (isCurrentGameFinished()) {
                return;
            }

            TurnResult turnState = currentGame().evaluateTurnState();
            if (turnState == TurnResult.PASS_TURN || turnState == TurnResult.GAME_OVER) {
                handleTurnStateResult(turnState);
                inputBox.setText("");
                return;
            }

            processMove(parsed);
            inputBox.setText("");
        });
        inputPanel.addComponent(actionPanel);
        inputPanel.addComponent(new Button("Reset", () -> {
            games[activeGameIndex] = currentGame().newGame(Board.FIXED_SIZE);
            rebuildBoardPanel();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();
            inputBox.setText("");
            messageLabel.setText("Board " + (activeGameIndex + 1) + " reset.\nMODE:" + currentGame().getDisplayName());
            messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
        }));
        inputPanel.addComponent(new Button("New Games", () -> {
            if (currentGameCount == MAX_ALLOWED_GAMES) {
                messageLabel.setText("Err: cannot create new game. Max games reached.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return;
            }
            GameType gameType = promptGameType(GameType.REVERSI);
            if (gameType == null) {
                messageLabel.setText("Create new game canceled.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                return;
            }
            games[currentGameCount] = createGame(gameType);
            currentGameCount++;
            activeGameIndex = currentGameCount - 1;
            rebuildBoardPanel();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();
            messageLabel.setText("New game added: " + games[activeGameIndex].getDisplayName());
            messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
        }));
        inputPanel.addComponent(messageLabel);
        mainPanel.addComponent(inputPanel, BorderLayout.Location.BOTTOM);
        refreshActionButtons();

        gui = new MultiWindowTextGUI(screen);
        window = new BasicWindow();
        window.setComponent(mainPanel);
        window.setHints(java.util.Collections.singletonList(Window.Hint.CENTERED));
        gui.addWindowAndWait(window);
        screen.stopScreen();
    }

    private void updateBoardDisplay() {
        boardPanel.removeAllComponents();
        int displayCellWidth = Math.max(1, cellWidthChars);
        int displayCellHeight = Math.max(1, cellHeightChars);
        BoardView board = currentGame().getBoard();
        ReversiBoardView reversiBoard = (board instanceof ReversiBoardView) ? (ReversiBoardView) board : null;
        char currentPlayer = currentGame().getCurrentPlayer();
        boolean showLegalMoves = currentGame().shouldShowLegalMoves();

        boardPanel.addComponent(new Label(buildCellText("", displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setBackgroundColor(TextColor.ANSI.WHITE));
        for (char c = 'A'; c < 'A' + board.getSize(); c++) {
            String header = String.valueOf(c);
            boardPanel.addComponent(new Label(buildCellText(header, displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setForegroundColor(TextColor.ANSI.CYAN).setBackgroundColor(TextColor.ANSI.WHITE));
        }

        for (int i = 0; i < board.getSize(); i++) {
            boardPanel.addComponent(new Label(buildCellText(String.format("%d", i + 1), displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setForegroundColor(TextColor.ANSI.CYAN).setBackgroundColor(TextColor.ANSI.WHITE));
            for (int j = 0; j < board.getSize(); j++) {
                String content = " ";
                TextColor fg = TextColor.ANSI.DEFAULT;
                boolean minesweeperGame = currentGame() instanceof MinesweeperGame;
                boolean unrevealed = false;
                boolean revealedMine = false;
                char ch = board.getCell(i, j);
                if (ch == Board.BLACK) { content = "●"; fg = TextColor.ANSI.BLACK; }
                else if (ch == Board.WHITE) { content = "●"; fg = TextColor.ANSI.WHITE; }
                else if (showLegalMoves && reversiBoard != null && reversiBoard.isLegalMove(i, j, currentPlayer)) { content = String.valueOf(Board.LEGAL_MOVE); fg = TextColor.ANSI.YELLOW; }
                else if (minesweeperGame) {
                    MinesweeperGame minesweeper = (MinesweeperGame) currentGame();
                    String state = minesweeper.getCellState(i, j);
                    if (MinesweeperBoardView.UNFLIPPED.equals(state)) {
                        content = " ";
                        unrevealed = true;
                    } else if (MinesweeperBoardView.FLAG.equals(state)) {
                        content = "F";
                        fg = TextColor.ANSI.YELLOW;
                        unrevealed = true;
                    } else if (MinesweeperBoardView.MINE.equals(state)) {
                        content = "x";
                        fg = TextColor.ANSI.RED;
                        revealedMine = true;
                    } else if (MinesweeperBoardView.EMPTY.equals(state)) {
                        content = " ";
                    } else if (state != null && state.length() == 1 && state.charAt(0) >= '1' && state.charAt(0) <= '8') {
                        content = state;
                        fg = TextColor.ANSI.CYAN;
                    }
                }

                TextColor bg;
                if (minesweeperGame) {
                    bg = unrevealed ? ((i + j) % 2 == 0 ? new TextColor.RGB(0, 80, 0) : new TextColor.RGB(0, 100, 0)) : TextColor.ANSI.BLUE;
                    if (revealedMine) {
                        bg = TextColor.ANSI.BLUE;
                    }
                } else {
                    bg = ((i + j) % 2 == 0) ? new TextColor.RGB(0, 80, 0) : new TextColor.RGB(0, 100, 0);
                }
                String cellText = buildCellText(content, displayCellWidth, displayCellHeight);
                boardPanel.addComponent(new Label(cellText).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setBackgroundColor(bg).setForegroundColor(fg));
            }
        }
    }

    private void updateGameInfoDisplay() {
        if (isMinesweeperGame()) {
            MinesweeperGame minesweeper = (MinesweeperGame) currentGame();
            StringConstructer sc = new StringConstructer();
            sc.append(" Game ").append(String.valueOf(activeGameIndex + 1)).append(' ').append(currentGame().getDisplayName()).append('\n');
            sc.append(" Flipped: ").append(String.valueOf(minesweeper.getFlipCount())).append('\n');
            sc.append(" Flags: ").append(String.valueOf(minesweeper.getFlagCount())).append('\n');
            sc.append(" Remaining mines: ").append(String.valueOf(minesweeper.getRemainingMineHintCount()));
            gameInfoLabel.setText(sc.toString());
        } else {
            gameInfoLabel.setText(" Game " + (activeGameIndex + 1) + " " + currentGame().getDisplayName());
        }
        refreshActionButtons();
    }

    private void updateScoreDisplay() {
        scoreLabel.setText(currentGame().shouldShowScore() ? currentGame().getScoreSummary() : "");
    }

    private void updateBoardIndexDisplay() {
        StringConstructer sc = new StringConstructer();
        for (int i = 0; i < currentGameCount; i++) {
            sc.append(String.valueOf(i + 1)).append(".").append(games[i].getDisplayName());
            if (i == activeGameIndex) sc.append(" <- current");
            if (isGameFinished(games[i])) sc.append(" [Finished]");
            if (i < currentGameCount - 1) sc.append('\n');
        }
        boardIndexLabel.setText(sc.toString());
    }

    private void exhibitPlayers() {
        if (currentGame().getCurrentPlayer() == Board.BLACK) {
            currentPlayerLabel.setText("P1:Tairitsu ●\nP2:Hikari");
            return;
        }
        if (currentGame().getCurrentPlayer() == Board.WHITE) {
            currentPlayerLabel.setText("P1:Tairitsu\nP2:Hikari ●");
            return;
        }
        currentPlayerLabel.setText("");
    }

    private static String spaces(int n) {
        if (n <= 0) return "";
        StringConstructer sc = new StringConstructer(n);
        for (int i = 0; i < n; i++) sc.append(' ');
        return sc.toString();
    }//生成 n 个空格的字符串

    private static String buildCellText(String symbol, int width, int height) {
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        int symLen = symbol == null ? 0 : symbol.length();
        if (symLen > width) symbol = symbol.substring(0, width);
        int left = Math.max(0, (width - symLen) / 2);
        int right = Math.max(0, width - symLen - left);
        String line = spaces(left) + (symbol == null ? "" : symbol) + spaces(right);
        if (height == 1) return line;
        int top = (height - 1) / 2;
        int bottom = height - 1 - top;
        StringConstructer full = new StringConstructer();
        for (int i = 0; i < top; i++) { full.append(spaces(width)); full.append('\n'); }
        full.append(line);
        for (int i = 0; i < bottom; i++) { full.append('\n'); full.append(spaces(width)); }
        return full.toString();
    }//构建一个宽为 width、高为 height 的单元格文本，symbol 居中显示

    private boolean processMove(ProcessInput input) {
        if (input.action == ProcessInput.Action.SWITCH_BOARD) {
            int targetIndex = input.col - 1;
            if (targetIndex < 0) {
                messageLabel.setText("Err: invalid board index.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            }
            if (targetIndex >= currentGameCount) {
                messageLabel.setText("Err: Maximum board exceeded.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            }
            activeGameIndex = targetIndex;
            rebuildBoardPanel();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();
            if (isGameFinished(games[targetIndex])) {
                messageLabel.setText("Switched to board " + input.col + " (finished, view only).");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            } else {
                messageLabel.setText("Switched to board " + input.col + ".");
                messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
            }
            return true;
        }

        if (input.action != ProcessInput.Action.MOVE) {
            messageLabel.setText("Invalid input!");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }

        if (currentGame() instanceof MinesweeperGame) {
            messageLabel.setText("Use Flip/Flag buttons in Minesweeper mode.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            return false;
        }

        TurnResult result = currentGame().processMove(input);
        switch (result) {
            case SUCCESS:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText(" Nice move! ");
                messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
                return true;
            case INVALID_INPUT:
                messageLabel.setText("Invalid input!\nYou shall enter A1-" + (char)('A' + currentBoardSize() - 1) + currentBoardSize() + " instead.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case OCCUPIED:
                messageLabel.setText("Err: Occupied");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case ILLEGAL_MOVE:
                messageLabel.setText("Err:No pieces flipped.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case PASS_TURN:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText("Turn passed:\n no legal move.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                return true;
            case GAME_OVER:
                handleCurrentBoardFinished();
                return true;
            default:
                messageLabel.setText("Unexpected move state.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
        }
    }

    private boolean handleMinesweeperButton(ProcessInput action, TextBox inputBox) {
        if (!isMinesweeperGame()) {
            messageLabel.setText("Switch to Minesweeper to use this button.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            return false;
        }

        String input = inputBox.getText().trim().toUpperCase();
        if (input.isEmpty()) {
            messageLabel.setText("Enter a coordinate or board number first.");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }

        ProcessInput parsed = ProcessInput.parse(input);
        if (parsed.action == ProcessInput.Action.QUIT) {
            terminalFrame.dispose();
            System.exit(0);
        }

        if (parsed.action == ProcessInput.Action.UNDO) {
            messageLabel.setText("Undo is unavailable in Minesweeper mode.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            return false;
        }

        if (parsed.action == ProcessInput.Action.HINT) {
            if (isCurrentGameFinished()) {
                return false;
            }
            MinesweeperGame minesweeperGame = (MinesweeperGame) currentGame();
            TurnResult result = minesweeperGame.useHint();
            switch (result) {
                case SUCCESS:
                    updateBoardDisplay();
                    updateGameInfoDisplay();
                    exhibitPlayers();
                    updateScoreDisplay();
                    messageLabel.setText("Hint used.");
                    messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
                    inputBox.setText("");
                    return true;
                case GAME_OVER:
                    updateBoardDisplay();
                    handleCurrentBoardFinished();
                    return true;
                default:
                    messageLabel.setText("Unexpected move state.");
                    messageLabel.setForegroundColor(TextColor.ANSI.RED);
                    return false;
            }
        }

        if (parsed.action == ProcessInput.Action.SWITCH_BOARD) {
            processMove(parsed);
            inputBox.setText("");
            return true;
        }

        if (isCurrentGameFinished()) {
            return false;
        }

        if (parsed.action != ProcessInput.Action.MOVE) {
            messageLabel.setText("Invalid input!");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }

        MinesweeperGame minesweeperGame = (MinesweeperGame) currentGame();
        String targetCellState = minesweeperGame.getCellState(parsed.row, parsed.col);

        if (action.action == ProcessInput.Action.FLIP && MinesweeperBoardView.FLAG.equals(targetCellState)) {
            messageLabel.setText("Err:you should unflag before flipping");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }

        if ((action.action == ProcessInput.Action.FLIP || action.action == ProcessInput.Action.FLAG)
            && !MinesweeperBoardView.UNFLIPPED.equals(targetCellState)
            && !MinesweeperBoardView.FLAG.equals(targetCellState)) {
            messageLabel.setText("Err:already flipped");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }

        if (action.action == ProcessInput.Action.FLAG
            && MinesweeperBoardView.UNFLIPPED.equals(minesweeperGame.getCellState(parsed.row, parsed.col))
            && !minesweeperGame.canPlaceMoreFlags()) {
            messageLabel.setText("The flag is full.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            return false;
        }

        TurnResult result = (action.action == ProcessInput.Action.FLAG)
            ? minesweeperGame.toggleFlagAt(parsed.row, parsed.col)
            : minesweeperGame.revealAt(parsed.row, parsed.col);
        switch (result) {
            case SUCCESS:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText(action.action == ProcessInput.Action.FLAG ? "Flag toggled." : "Cell revealed.");
                messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
                inputBox.setText("");
                return true;
            case INVALID_INPUT:
                messageLabel.setText("Invalid input!");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case OCCUPIED:
                messageLabel.setText("Err: Occupied");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case GAME_OVER:
                updateBoardDisplay();
                handleCurrentBoardFinished();
                return true;
            default:
                messageLabel.setText("Unexpected move state.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
        }
    }

    private GameSession currentGame() {
        return games[activeGameIndex];
    }

    private int currentBoardSize() {
        return currentGame().getBoard().getSize();
    }

    private void initializeGames() {
        for (int i = 0; i < currentGameCount; i++) {
            games[i] = createGame(typeByIndex(i));
        }
    }

    private GameType typeByIndex(int index) {
        if (index == 0) return GameType.PEACE;
        if (index == 1) return GameType.REVERSI;
        return GameType.MINESWEEPER;
    }

    private GameSession createGame(GameType type) {
        if (type == GameType.PEACE) {
            return new PeaceGame(Board.FIXED_SIZE);
        }
        if (type == GameType.REVERSI) {
            return new ReversiGame(Board.FIXED_SIZE);
        }
        return new MinesweeperGame(Board.FIXED_SIZE);
    }

    private void rebuildBoardPanel() {
        if (mainPanel != null && boardPanel != null) {
            mainPanel.removeComponent(boardPanel);
        }
        GridLayout gridLayout = new GridLayout(currentGame().getBoard().getSize() + 1);
        gridLayout.setHorizontalSpacing(0);
        gridLayout.setVerticalSpacing(0);
        boardPanel = new Panel(gridLayout);
        updateBoardDisplay();
        if (mainPanel != null) {
            mainPanel.addComponent(boardPanel, BorderLayout.Location.CENTER);
        }
    }

    private void handleTurnStateResult(TurnResult state) {
        switch (state) {
            case PASS_TURN:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText("No legal move, turn passed.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                break;
            case GAME_OVER:
                handleCurrentBoardFinished();
                break;
            default:
                break;
        }
    }

    private void handleCurrentBoardFinished() {
        int finishedBoard = activeGameIndex + 1;
        int targetIndex = findNextAvailableGameIndex();
        String finishSummary = currentGame().getFinishSummary();

        if (targetIndex == -1) {
            String msg = "Board " + finishedBoard + " finished.";
            if (finishSummary != null && !finishSummary.isEmpty()) msg += " " + finishSummary;
            msg += "\nNo remnant boards. All games over.";
            MessageDialog.showMessageDialog(gui, "Game Over", msg, MessageDialogButton.OK);
            messageLabel.setText("All boards finished. Game over.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            System.exit(0);
            return;
        }

        String msg = "Board " + finishedBoard + " finished.";
        if (finishSummary != null && !finishSummary.isEmpty()) msg += " " + finishSummary;
        msg += "\nSwitching to board " + (targetIndex + 1) + ".";
        MessageDialog.showMessageDialog(gui, "Board Finished", msg, MessageDialogButton.OK);
        activeGameIndex = targetIndex;
        rebuildBoardPanel();
        updateGameInfoDisplay();
        exhibitPlayers();
        updateScoreDisplay();
        updateBoardIndexDisplay();
        messageLabel.setText("Switched to board " + (targetIndex + 1) + ".");
        messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
    }

    private int findNextAvailableGameIndex() {
        for (int i = 0; i < currentGameCount; i++) {
            if (!isGameFinished(games[i])) return i;
        }
        return -1;
    }

    private boolean isGameFinished(GameSession game) {
        return game.isFinished();
    }

    private boolean isMinesweeperGame() {
        return currentGame() instanceof MinesweeperGame;
    }

    private boolean isCurrentGameFinished() {
        return currentGame().isFinished();
    }

    private void refreshActionButtons() {
        boolean minesweeperGame = isMinesweeperGame();
        if (actionPanel == null || flipButton == null || flagButton == null || playButton == null) return;

        actionPanel.removeAllComponents();
        if (minesweeperGame) {
            actionPanel.addComponent(flipButton);
            actionPanel.addComponent(flagButton);
        } else {
            actionPanel.addComponent(playButton);
        }

    }
}
