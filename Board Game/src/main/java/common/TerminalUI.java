package common;

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
import java.util.List;
import javax.swing.UIManager;

public class TerminalUI {
    private static final int FONT_SIZE = 26;//控制字体大小
    private static final int INITIAL_GAMES = 4;
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
    private Button playButton;
    private BasicWindow window;
    private MultiWindowTextGUI gui;
    private SwingTerminalFrame terminalFrame;
    private final List<GameDefinition> gameDefinitions = GameRegistry.defaultGames();
    private TextBox inputBox;
    private volatile boolean demoRunning = false;
    private volatile boolean demoPaused = false;
    private final Object demoLock = new Object();

    public TerminalUI() {
        initializeGames();
    }

    private GameDefinition promptGameType(GameDefinition defaultType) {
        Object[] options = gameDefinitions.stream().map(GameDefinition::getName).toArray();
        int defaultOptionIndex = 1;
        for (int i = 0; i < gameDefinitions.size(); i++) {
            if (gameDefinitions.get(i) == defaultType) {
                defaultOptionIndex = i;
                break;
            }
        }

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
        if (choice < 0 || choice >= gameDefinitions.size()) return null;
        return gameDefinitions.get(choice);
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
            "Board Games Terminal",
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
        inputPanel.addComponent(new Label("Hint: print \"Q\" to exit; \n\"H\" to hint in Minesweeper;\n\"U\" to undo.").setBackgroundColor(TextColor.ANSI.WHITE));
        inputBox = new TextBox(new TerminalSize(5, 1));
        inputPanel.addComponent(inputBox);
        messageLabel = new Label(" Welcome!").setForegroundColor(TextColor.ANSI.GREEN);
        actionPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        playButton = new Button("Play", () -> {
            String input = inputBox.getText().trim().toUpperCase();
            if (input.isEmpty()) return;

            ProcessInput parsed = ProcessInput.parse(input);
            if (parsed.action == ProcessInput.Action.QUIT) {
                terminalFrame.dispose();
                System.exit(0);
            }

            if (rejectDuringDemo()) {
                return;
            }

            if (parsed.action == ProcessInput.Action.UNDO) {
                if (currentGame().undoLastMove()) {
                    updateBoardDisplay();
                    updateGameInfoDisplay();
                    exhibitPlayers();
                    updateScoreDisplay();
                    updateBoardIndexDisplay();
                    messageLabel.setText("Info: undo completed.");
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

            processMove(input);
            inputBox.setText("");
        });
        inputPanel.addComponent(actionPanel);
        inputPanel.addComponent(new Button("Reset", () -> {
            if (rejectDuringDemo()) return;
            games[activeGameIndex] = currentGame().newGame(8);
            rebuildBoardPanel();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();
            inputBox.setText("");
            messageLabel.setText("Info: board " + (activeGameIndex + 1) + " reset.\nMode: " + currentGame().getDisplayName());
            messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
        }));
        
        inputPanel.addComponent(new Button("New Games", () -> {
            if (rejectDuringDemo()) return;
            if (currentGameCount == MAX_ALLOWED_GAMES) {
                messageLabel.setText("Err: cannot create new game. Maximum board count reached.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return;
            }
            GameDefinition gameType = promptGameType(gameDefinitions.get(1));
            if (gameType == null) {
                messageLabel.setText("Info: new game creation canceled.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                return;
            }
            games[currentGameCount] = gameType.create();
            currentGameCount++;
            activeGameIndex = currentGameCount - 1;
            rebuildBoardPanel();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();
            messageLabel.setText("Info: new game added: " + games[activeGameIndex].getDisplayName());
            messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
        }));
        inputPanel.addComponent(new Button("Demo", this::startCurrentGameDemo));
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
        boolean isChessBoard = "Chess".equalsIgnoreCase(currentGame().getDisplayName());
        boolean isMinesweeper = "Minesweeper".equalsIgnoreCase(currentGame().getDisplayName());
        char currentPlayer = currentGame().getCurrentPlayer();
        boolean showLegalMoves = currentGame().shouldShowLegalMoves();

        for (int i = 0; i < board.getSize(); i++) {
            int displayedRank = board.getSize() - i;
            boardPanel.addComponent(new Label(buildCellText(String.format("%d", displayedRank), displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setForegroundColor(TextColor.ANSI.CYAN).setBackgroundColor(TextColor.ANSI.WHITE));
            for (int j = 0; j < board.getSize(); j++) {
                String content = " ";
                TextColor fg = TextColor.ANSI.DEFAULT;
                boolean unrevealed = false;
                boolean revealedMine = false;
                char ch = board.getCell(i, j);
                if (board.isHiddenCell(i, j)) {
                    content = " ";
                    unrevealed = true;
                } else if (board.isMarkedCell(i, j)) {
                    content = "F";
                    fg = TextColor.ANSI.YELLOW;
                    unrevealed = true;
                } else if (board.isDangerCell(i, j)) {
                    content = "x";
                    fg = TextColor.ANSI.RED;
                    revealedMine = true;
                } else if (isChessBoard && "PNKQRBpnkqrb".indexOf(ch) >= 0) {
                    // Display white pieces in lowercase and black pieces in uppercase.
                    if (Character.isUpperCase(ch)) {
                        // white pieces (internal uppercase)
                        content = String.valueOf(Character.toLowerCase(ch));
                        fg = TextColor.ANSI.WHITE;
                    } else {
                        // black pieces (internal lowercase) — render as pure black
                        content = String.valueOf(Character.toUpperCase(ch));
                        fg = TextColor.ANSI.BLACK;
                    }
                }
                else if (ch == 'B') { content = "●"; fg = TextColor.ANSI.BLACK; }
                else if (ch == 'W') { content = "●"; fg = TextColor.ANSI.WHITE; }
                else if (showLegalMoves && board.isLegalMove(i, j, currentPlayer)) { content = String.valueOf('+'); fg = TextColor.ANSI.YELLOW; }
                else if (ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7' || ch == '8') {
                    content = String.valueOf(ch);
                    fg = TextColor.ANSI.CYAN;
                }
                TextColor bg;
                if (isMinesweeper) {
                    if (!board.isHiddenCell(i, j) && !board.isMarkedCell(i, j)) {
                        bg = TextColor.ANSI.BLUE; // revealed cells show blue in Minesweeper
                    } else {
                        bg = ((i + j) % 2 == 0) ? new TextColor.RGB(0, 100, 0) : new TextColor.RGB(0, 80, 0);
                    }
                    if (revealedMine) {
                        bg = TextColor.ANSI.BLUE;
                    }
                } else {
                    if (board.isHiddenCell(i, j) || board.isMarkedCell(i, j)) {
                        bg = unrevealed ? ((i + j) % 2 == 0 ? new TextColor.RGB(0, 100, 0) : new TextColor.RGB(0, 80, 0)) : TextColor.ANSI.BLUE;
                        if (revealedMine) {
                            bg = TextColor.ANSI.BLUE;
                        }
                    } else {
                        bg = ((i + j) % 2 == 0) ? new TextColor.RGB(0, 100, 0) : new TextColor.RGB(0, 80, 0);
                    }
                }
                String cellText = buildCellText(content, displayCellWidth, displayCellHeight);
                boardPanel.addComponent(new Label(cellText).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setBackgroundColor(bg).setForegroundColor(fg));
            }
        }

        // Put file letters below the board (A-H for 8x8)
        boardPanel.addComponent(new Label(buildCellText("", displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setBackgroundColor(TextColor.ANSI.WHITE));
        for (char c = 'A'; c < 'A' + board.getSize(); c++) {
            String header = String.valueOf(c);
            boardPanel.addComponent(new Label(buildCellText(header, displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setForegroundColor(TextColor.ANSI.CYAN).setBackgroundColor(TextColor.ANSI.WHITE));
        }
    }

    private void updateGameInfoDisplay() {
        String summary = currentGame().getStatusSummary();
        if (summary == null || summary.isEmpty()) {
            gameInfoLabel.setText(" Game " + (activeGameIndex + 1) + " " + currentGame().getDisplayName());
        } else {
            StringConstructer sc = new StringConstructer();
            sc.append(" Game ").append(String.valueOf(activeGameIndex + 1)).append(' ').append(currentGame().getDisplayName()).append('\n');
            sc.append(summary);
            gameInfoLabel.setText(sc.toString());
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
        if (currentGame().getCurrentPlayer() == 'B') {
            currentPlayerLabel.setText("P1:Tairitsu ●\nP2:Hikari");
            return;
        }
        if (currentGame().getCurrentPlayer() == 'W') {
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
        if (rejectDuringDemo()) return false;
        if (input.action == ProcessInput.Action.SWITCH_BOARD) {
            int targetIndex = input.value - 1;
            if (targetIndex < 0) {
                messageLabel.setText("Err: invalid board index.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            }
            if (targetIndex >= currentGameCount) {
                messageLabel.setText("Err: board index exceeds current board count.");
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
                messageLabel.setText("Info: switched to board " + input.value + " (finished, view only).");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            } else {
                messageLabel.setText("Info: switched to board " + input.value + ".");
                messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
            }
            return true;
        }

        messageLabel.setText("Err: invalid input.");
        messageLabel.setForegroundColor(TextColor.ANSI.RED);
        return false;
    }

    private boolean processMove(String rawInput) {
        if (rejectDuringDemo()) return false;
        TurnResult result = currentGame().handleRawInput(translateCoordinateInput(rawInput));
        switch (result) {
            case SUCCESS:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText("Info: move accepted.");
                messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
                return true;
            case INVALID_INPUT:
                if ("Chess".equalsIgnoreCase(currentGame().getDisplayName())) {
                    messageLabel.setText("Err: invalid input. Enter m A1-H8 A1-H8 instead.");
                } else {
                    messageLabel.setText("Err: invalid input. Enter A1-" + (char)('A' + currentBoardSize() - 1) + currentBoardSize() + " instead.");
                }
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case OCCUPIED:
                messageLabel.setText("Err: occupied square.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case EMPTY_SOURCE:
                messageLabel.setText("Err: source square is empty.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case WRONG_SIDE:
                messageLabel.setText("Err: source piece belongs to the opponent.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case ILLEGAL_MOVE:
                messageLabel.setText("Err: move not permitted.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case PASS_TURN:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText("Info: turn passed. No legal move.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                return true;
            case GAME_OVER:
                handleCurrentBoardFinished();
                return true;
            default:
                messageLabel.setText("Err: unexpected move state.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
        }
    }

    private boolean handleActionButton(GameAction action, TextBox inputBox) {
        if (rejectDuringDemo()) return false;
        String input = inputBox.getText().trim().toUpperCase();
        if (input.isEmpty()) {
            messageLabel.setText("Err: enter a coordinate or board number first.");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return false;
        }
        // Allow Minesweeper hint via 'H' even when action buttons (Flip/Flag) are present
        if ("H".equals(input) && "Minesweeper".equalsIgnoreCase(currentGame().getDisplayName())) {
            boolean ok = processMove("H");
            if (ok) inputBox.setText("");
            return ok;
        }

        ProcessInput parsed = ProcessInput.parse(input);
        if (parsed.action == ProcessInput.Action.QUIT) {
            terminalFrame.dispose();
            System.exit(0);
        }

        if (parsed.action == ProcessInput.Action.UNDO) {
            messageLabel.setText("Err: undo is unavailable in this mode.");
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
            return false;
        }

        if (parsed.action == ProcessInput.Action.SWITCH_BOARD) {
            processMove(parsed);
            inputBox.setText("");
            return true;
        }

        if (isCurrentGameFinished()) {
            return false;
        }

        TurnResult result = currentGame().handleAction(action, translateCoordinateInput(inputBox.getText()));
        switch (result) {
            case SUCCESS:
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                messageLabel.setText("Info: " + action.label + " completed.");
                messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
                inputBox.setText("");
                return true;
            case INVALID_INPUT:
                messageLabel.setText("Err: invalid input.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case OCCUPIED:
                messageLabel.setText("Err: occupied square.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case EMPTY_SOURCE:
                messageLabel.setText("Err: source square is empty.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case WRONG_SIDE:
                messageLabel.setText("Err: source piece belongs to the opponent.");
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                return false;
            case GAME_OVER:
                updateBoardDisplay();
                handleCurrentBoardFinished();
                return true;
            default:
                messageLabel.setText("Err: unexpected move state.");
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
            games[i] = gameDefinitions.get(typeByIndex(i)).create();
        }
    }

    private int typeByIndex(int index) {
        if (index < gameDefinitions.size()) return index;
        return gameDefinitions.size() - 1;
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

    private boolean isCurrentGameFinished() {
        return currentGame().isFinished();
    }

    private boolean rejectDuringDemo() {
        if (!demoRunning) return false;
        messageLabel.setText("Info: demo is running. \nUse Demo button to pause/resume.");
        messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        return true;
    }

    private void startCurrentGameDemo() {
        // If demo is running, toggle pause/resume
        if (demoRunning) {
            synchronized (demoLock) {
                if (!demoPaused) {
                    demoPaused = true;
                    messageLabel.setText("Info: demo paused.");
                    messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                } else {
                    demoPaused = false;
                    demoLock.notifyAll();
                    messageLabel.setText("Info: demo resumed.");
                    messageLabel.setForegroundColor(TextColor.ANSI.CYAN);
                }
            }
            return;
        }

        demoPaused = false;
        demoRunning = true;
        new Thread(() -> {
            try {
                runCurrentGameDemo();
            } finally {
                demoRunning = false;
                demoPaused = false;
            }
        }, "game-demo-runner").start();
    }

    private void runCurrentGameDemo() {
        int boardSize = currentGame().getBoard().getSize();
        games[activeGameIndex] = currentGame().newGame(boardSize);
        rebuildBoardPanel();
        updateGameInfoDisplay();
        exhibitPlayers();
        updateScoreDisplay();
        updateBoardIndexDisplay();

        List<String> demoInputs = currentGame().getDemoInputs();
        if (demoInputs.isEmpty()) {
            messageLabel.setText("Err: this game has no predefined demo script.");
            messageLabel.setForegroundColor(TextColor.ANSI.RED);
            return;
        }

        int appliedSteps = 0;
        for (String step : demoInputs) {
            if (currentGame().isFinished()) break;

            TurnResult result = currentGame().handleRawInput(translateCoordinateInput(step));
            if (result == TurnResult.INVALID_INPUT || result == TurnResult.OCCUPIED || result == TurnResult.EMPTY_SOURCE
                || result == TurnResult.WRONG_SIDE || result == TurnResult.ILLEGAL_MOVE) {
                messageLabel.setText("Err: demo stopped on scripted step " + (appliedSteps + 1) + ": " + step);
                messageLabel.setForegroundColor(TextColor.ANSI.RED);
                updateBoardDisplay();
                updateGameInfoDisplay();
                exhibitPlayers();
                updateScoreDisplay();
                updateBoardIndexDisplay();
                return;
            }

            appliedSteps++;

            // Update UI so the user sees each demo step
            updateBoardDisplay();
            updateGameInfoDisplay();
            exhibitPlayers();
            updateScoreDisplay();
            updateBoardIndexDisplay();

            // allow pause while demoRunning
            synchronized (demoLock) {
                while (demoPaused) {
                    try {
                        demoLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        messageLabel.setText("Info: demo interrupted.");
                        messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                        return;
                    }
                }
            }

            // short pause between demo steps
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                messageLabel.setText("Info: demo interrupted.");
                messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
                return;
            }

            if (result == TurnResult.GAME_OVER) break;
        }

        updateBoardDisplay();
        updateGameInfoDisplay();
        exhibitPlayers();
        updateScoreDisplay();
        updateBoardIndexDisplay();

        String summary = currentGame().getDemoSummary();
        if (currentGame().isFinished()) {
            messageLabel.setText("Info: demo completed in " + appliedSteps + " steps. " + summary);
            messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
        } else {
            messageLabel.setText("Info: demo script ended after " + appliedSteps + " steps. " + summary);
            messageLabel.setForegroundColor(TextColor.ANSI.YELLOW);
        }
    }

    private String translateCoordinateInput(String rawInput) {
        if (rawInput == null) return null;
        String input = rawInput.trim().toUpperCase();
        if (input.isEmpty()) return input;
        if ("Chess".equalsIgnoreCase(currentGame().getDisplayName())) return input;
        if (input.length() == 1) return input;
        if ("AUTO".equals(input)) return input;

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
    

    private void refreshActionButtons() {
        if (actionPanel == null || playButton == null) return;
        actionPanel.removeAllComponents();
        List<GameAction> actions = currentGame().getActionButtons();
        if (!actions.isEmpty()) {
            for (GameAction action : actions) {
                actionPanel.addComponent(new Button(action.label, () -> handleActionButton(action, inputBox)));
            }
        } else {
            actionPanel.addComponent(playButton);
        }
    }
}