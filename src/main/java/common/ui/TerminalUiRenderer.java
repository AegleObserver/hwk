package common.ui;

import common.BoardView;
import common.GameAction;
import common.GameDefinition;
import common.StringConstructor;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.BorderLayout;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFontConfiguration;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;
import java.awt.Font;
import java.util.List;
import javax.swing.UIManager;

public final class TerminalUiRenderer implements GameUiRenderer {
    private static final int FONT_SIZE = 26;

    private final GameUiBridge bridge;
    private Panel mainPanel;
    private Panel boardPanel;
    private Panel actionPanel;
    private Label gameInfoLabel;
    private Label currentPlayerLabel;
    private Label scoreLabel;
    private Label boardIndexLabel;
    private Label messageLabel;
    private TextBox inputBox;
    private Button playButton;
    private Button demoBtn;
    private BasicWindow window;
    private MultiWindowTextGUI gui;
    private SwingTerminalFrame terminalFrame;
    private Screen screen;
    private int cellWidthChars = 1;
    private int cellHeightChars = 1;
    private boolean started;

    public TerminalUiRenderer(GameUiBridge bridge) {
        this.bridge = bridge;
    }

    public void start(GameUiState initialState) throws Exception {
        setupUI(initialState);
        render(initialState);
        gui.addWindowAndWait(window);
        if (screen != null) {
            screen.stopScreen();
        }
    }

    @Override
    public void render(GameUiState state) {
        if (!started) {
            return;
        }
        rebuildBoardPanel(state);
        if (gameInfoLabel != null) {
            gameInfoLabel.setText(buildGameInfoText(state));
        }
        if (currentPlayerLabel != null) {
            currentPlayerLabel.setText(buildCurrentPlayerText(state));
        }
        if (scoreLabel != null) {
            scoreLabel.setText(buildScoreText(state));
        }
        if (boardIndexLabel != null) {
            boardIndexLabel.setText(state.getBoardIndexText());
        }
        if (messageLabel != null) {
            messageLabel.setText(state.getMessageText());
            messageLabel.setForegroundColor(messageColor(state.getMessageText()));
        }
        refreshActionButtons(state);
        if (demoBtn != null) {
            if (state.isDemoRunning() && !state.isDemoPaused()) {
                demoBtn.setLabel("Pause");
            } else if (state.isDemoRunning() && state.isDemoPaused()) {
                demoBtn.setLabel("Resume");
            } else {
                demoBtn.setLabel("Demo");
            }
        }
    }

    @Override
    public GameDefinition promptGameType(List<GameDefinition> availableGames, GameDefinition defaultType, String promptTitle) {
        Object[] options = availableGames.stream().map(GameDefinition::getName).toArray();
        int defaultOptionIndex = 0;
        for (int i = 0; i < availableGames.size(); i++) {
            if (availableGames.get(i) == defaultType) {
                defaultOptionIndex = i;
                break;
            }
        }

        int choice = javax.swing.JOptionPane.showOptionDialog(
            null,
            "Choose mode:",
            promptTitle == null ? "Game Type" : promptTitle,
            javax.swing.JOptionPane.DEFAULT_OPTION,
            javax.swing.JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[Math.min(defaultOptionIndex, Math.max(0, options.length - 1))]
        );

        if (choice == javax.swing.JOptionPane.CLOSED_OPTION) {
            return null;
        }
        if (choice < 0 || choice >= availableGames.size()) {
            return null;
        }
        return availableGames.get(choice);
    }

    @Override
    public void showMessage(String title, String message) {
        if (gui != null) {
            MessageDialog.showMessageDialog(gui, title, message, MessageDialogButton.OK);
        }
    }

    @Override
    public void close() {
        if (terminalFrame != null) {
            terminalFrame.dispose();
        }
    }

    public void clearInputText() {
        if (inputBox != null) {
            inputBox.setText("");
        }
    }

    private void setupUI(GameUiState state) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        String[] preferredFonts = {"Microsoft YaHei", "Microsoft YaHei UI", "微软雅黑", "Consolas", "Monospaced"};
        java.util.List<String> installed = java.util.Arrays.asList(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        String chosen = null;
        for (String font : preferredFonts) {
            if (installed.contains(font)) {
                chosen = font;
                break;
            }
        }
        if (chosen == null) {
            chosen = "Monospaced";
        }
        Font awtFont = new Font(chosen, Font.BOLD, FONT_SIZE);
        SwingTerminalFontConfiguration fontConfig;
        try {
            fontConfig = SwingTerminalFontConfiguration.newInstance(awtFont);
        } catch (IllegalArgumentException e) {
            String[] monos = {"Consolas", "Courier New", "Lucida Console", "DejaVu Sans Mono", "Monospaced"};
            String monoChosen = null;
            for (String mono : monos) {
                if (installed.contains(mono)) {
                    monoChosen = mono;
                    break;
                }
            }
            if (monoChosen == null) {
                monoChosen = "Monospaced";
            }
            awtFont = new Font(monoChosen, Font.PLAIN, FONT_SIZE);
            fontConfig = SwingTerminalFontConfiguration.newInstance(awtFont);
        }

        java.awt.FontMetrics fm = new java.awt.Canvas().getFontMetrics(awtFont);
        int charPixelWidth = Math.max(1, fm.getMaxAdvance());
        int charPixelHeight = Math.max(1, fm.getHeight());
        cellHeightChars = 1;
        cellWidthChars = Math.max(1, (int) Math.ceil((double) charPixelHeight / (double) charPixelWidth * cellHeightChars));

        int totalCols = state.getActiveGame().getBoard().getSize() + 1;
        int infoPanelCols = 30;
        int leftMarginCols = 6;
        int rightMarginCols = 6;
        int topRows = 5;
        int bottomRows = 6;

        int windowWidthChars = totalCols * cellWidthChars + infoPanelCols + leftMarginCols + rightMarginCols;
        int windowHeightChars = state.getActiveGame().getBoard().getSize() * cellHeightChars + topRows + bottomRows;
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
        try {
            java.awt.Color white = java.awt.Color.WHITE;
            terminalFrame.getContentPane().setBackground(white);
            terminalFrame.setBackground(white);
        } catch (Exception ignored) {
        }

        screen = new TerminalScreen(terminalFrame);
        screen.startScreen();

        mainPanel = new Panel(new BorderLayout());
        Label title = new Label(" BOARD GAMES ");
        title.addStyle(SGR.BOLD);
        title.setForegroundColor(TextColor.ANSI.BLACK);
        mainPanel.addComponent(title, BorderLayout.Location.TOP);

        gameInfoLabel = new Label("");
        currentPlayerLabel = new Label("");
        scoreLabel = new Label("");
        boardIndexLabel = new Label("");
        messageLabel = new Label("");
        actionPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));

        Panel outerInfoPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));

        Panel leftInfoPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        leftInfoPanel.addComponent(gameInfoLabel.setBackgroundColor(TextColor.ANSI.WHITE));
        leftInfoPanel.addComponent(currentPlayerLabel.setBackgroundColor(TextColor.ANSI.WHITE));
        leftInfoPanel.addComponent(scoreLabel.setBackgroundColor(TextColor.ANSI.WHITE));
        outerInfoPanel.addComponent(leftInfoPanel.withBorder(Borders.singleLine("Game info")));

        Panel rightInfoPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        rightInfoPanel.addComponent(boardIndexLabel.setBackgroundColor(TextColor.ANSI.WHITE));
        outerInfoPanel.addComponent(rightInfoPanel.withBorder(Borders.singleLine("More games")));

        mainPanel.addComponent(outerInfoPanel, BorderLayout.Location.RIGHT);

        Panel inputPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        inputPanel.addComponent(new Label("Hint: print \"Q\" to exit; \n\"H\" to hint in Minesweeper;\n\"U\" to undo.").setBackgroundColor(TextColor.ANSI.WHITE));
        inputBox = new TextBox(new TerminalSize(5, 1));
        inputPanel.addComponent(inputBox);
        messageLabel.setForegroundColor(TextColor.ANSI.GREEN);
        actionPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
        playButton = new Button("Play", () -> {
            String input = inputBox.getText().trim().toUpperCase();
            if (input.isEmpty()) {
                return;
            }
            bridge.onRawInput(input);
            clearInputText();
        });
        inputPanel.addComponent(actionPanel);
        inputPanel.addComponent(new Button("Reset", () -> bridge.onResetRequested()));
        inputPanel.addComponent(new Button("New Games", () -> bridge.onNewGameRequested()));
        demoBtn = new Button("Demo", () -> bridge.onRawInput("DEMO_TOGGLE"));
        inputPanel.addComponent(demoBtn);
        inputPanel.addComponent(messageLabel);
        mainPanel.addComponent(inputPanel, BorderLayout.Location.BOTTOM);

        boardPanel = new Panel(new GridLayout(state.getActiveGame().getBoard().getSize() + 1));
        boardPanel.setPreferredSize(new TerminalSize(windowWidthChars, state.getActiveGame().getBoard().getSize() * cellHeightChars + 1));
        mainPanel.addComponent(boardPanel, BorderLayout.Location.CENTER);

        refreshActionButtons(state);

        gui = new MultiWindowTextGUI(screen);
        window = new BasicWindow();
        window.setComponent(mainPanel);
        window.setHints(java.util.Collections.singletonList(Window.Hint.CENTERED));

        started = true;
    }

    private void rebuildBoardPanel(GameUiState state) {
        if (mainPanel != null && boardPanel != null) {
            mainPanel.removeComponent(boardPanel);
        }
        GridLayout gridLayout = new GridLayout(state.getActiveGame().getBoard().getSize() + 1);
        gridLayout.setHorizontalSpacing(0);
        gridLayout.setVerticalSpacing(0);
        boardPanel = new Panel(gridLayout);
        updateBoardDisplay(state);
        if (mainPanel != null) {
            mainPanel.addComponent(boardPanel, BorderLayout.Location.CENTER);
        }
    }

    private void updateBoardDisplay(GameUiState state) {
        boardPanel.removeAllComponents();
        int displayCellWidth = Math.max(1, cellWidthChars);
        int displayCellHeight = Math.max(1, cellHeightChars);
        BoardView board = state.getActiveGame().getBoard();
        boolean isChessBoard = "Chess".equalsIgnoreCase(state.getActiveGame().getDisplayName());
        boolean isMinesweeper = "Minesweeper".equalsIgnoreCase(state.getActiveGame().getDisplayName());
        char currentPlayer = state.getActiveGame().getCurrentPlayer();
        boolean showLegalMoves = state.getActiveGame().shouldShowLegalMoves();

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
                    if (Character.isUpperCase(ch)) {
                        content = String.valueOf(Character.toLowerCase(ch));
                        fg = TextColor.ANSI.WHITE;
                    } else {
                        content = String.valueOf(Character.toUpperCase(ch));
                        fg = TextColor.ANSI.BLACK;
                    }
                } else if (ch == 'B') {
                    content = "●";
                    fg = TextColor.ANSI.BLACK;
                } else if (ch == 'W') {
                    content = "●";
                    fg = TextColor.ANSI.WHITE;
                } else if (showLegalMoves && board.isLegalMove(i, j, currentPlayer)) {
                    content = String.valueOf('+');
                    fg = TextColor.ANSI.YELLOW;
                } else if (ch == '1' || ch == '2' || ch == '3' || ch == '4' || ch == '5' || ch == '6' || ch == '7' || ch == '8') {
                    content = String.valueOf(ch);
                    fg = TextColor.ANSI.CYAN;
                }
                TextColor bg;
                if (isMinesweeper) {
                    if (!board.isHiddenCell(i, j) && !board.isMarkedCell(i, j)) {
                        bg = TextColor.ANSI.BLUE;
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

        boardPanel.addComponent(new Label(buildCellText("", displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setBackgroundColor(TextColor.ANSI.WHITE));
        for (char c = 'A'; c < 'A' + board.getSize(); c++) {
            String header = String.valueOf(c);
            boardPanel.addComponent(new Label(buildCellText(header, displayCellWidth, displayCellHeight)).setPreferredSize(new TerminalSize(displayCellWidth, displayCellHeight)).setForegroundColor(TextColor.ANSI.CYAN).setBackgroundColor(TextColor.ANSI.WHITE));
        }
    }

    private void refreshActionButtons(GameUiState state) {
        if (actionPanel == null || playButton == null) {
            return;
        }
        actionPanel.removeAllComponents();
        List<GameAction> actions = state.getActionButtons();
        if (!actions.isEmpty()) {
            for (GameAction action : actions) {
                actionPanel.addComponent(new Button(action.label, () -> bridge.onAction(action, inputBox.getText())));
            }
        } else {
            actionPanel.addComponent(playButton);
        }
    }

    private static String buildGameInfoText(GameUiState state) {
        String summary = state.getActiveGame().getStatusSummary();
        String commands = buildCommandsInfo(state);
        StringConstructor sc = new StringConstructor();
        sc.append(" Game ").append(String.valueOf(state.getActiveGameIndex() + 1)).append(' ').append(state.getActiveGame().getDisplayName()).append('\n');
        if (summary == null || summary.isEmpty()) {
            sc.append('\n');
        } else {
            sc.append(summary).append('\n');
        }
        sc.append('\n').append("Commands").append('\n').append(commands);
        return sc.toString();
    }

    private static String buildCurrentPlayerText(GameUiState state) {
        if (state.getActiveGame().getCurrentPlayer() == 'B') {
            return "P1:Tairitsu ●\nP2:Hikari";
        }
        if (state.getActiveGame().getCurrentPlayer() == 'W') {
            return "P1:Tairitsu\nP2:Hikari ●";
        }
        return "";
    }

    private static String buildScoreText(GameUiState state) {
        return state.getActiveGame().shouldShowScore() ? state.getActiveGame().getScoreSummary() : "";
    }

    private String buildBoardIndexText(GameUiState state) {
        StringConstructor sc = new StringConstructor();
        for (int i = 0; i < state.getGameCount(); i++) {
            sc.append(String.valueOf(i + 1)).append('.').append(state.getAvailableGames().get(i).getName());
            if (i == state.getActiveGameIndex()) {
                sc.append(" <- current");
            }
            if (i < state.getGameCount() - 1) {
                sc.append('\n');
            }
        }
        return sc.toString();
    }

    private static String buildCommandsInfo(GameUiState state) {
        boolean isMinesweeper = "Minesweeper".equalsIgnoreCase(state.getActiveGame().getDisplayName());
        boolean isChessBoard = "Chess".equalsIgnoreCase(state.getActiveGame().getDisplayName());
        StringConstructor sc = new StringConstructor();
        sc.append(isMinesweeper ? "Input 'H' to get hint\n" : "Input 'U' to undo\n");
        sc.append(isChessBoard ? "Input m A1-H8 A1-H8 to move\n" : "Input A1-H8 to move\n");
        sc.append("Input 1-").append(String.valueOf(state.getGameCount())).append(" to switch games\n");
        sc.append("Input 'Q' to quit.");
        return sc.toString();
    }

    private static String buildCellText(String symbol, int width, int height) {
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        int symLen = symbol == null ? 0 : symbol.length();
        if (symLen > width) {
            symbol = symbol.substring(0, width);
        }
        int left = Math.max(0, (width - symLen) / 2);
        int right = Math.max(0, width - symLen - left);
        String line = spaces(left) + (symbol == null ? "" : symbol) + spaces(right);
        if (height == 1) {
            return line;
        }
        int top = (height - 1) / 2;
        int bottom = height - 1 - top;
        StringConstructor full = new StringConstructor();
        for (int i = 0; i < top; i++) {
            full.append(spaces(width));
            full.append('\n');
        }
        full.append(line);
        for (int i = 0; i < bottom; i++) {
            full.append('\n');
            full.append(spaces(width));
        }
        return full.toString();
    }

    private static String spaces(int n) {
        if (n <= 0) {
            return "";
        }
        StringConstructor sc = new StringConstructor(n);
        for (int i = 0; i < n; i++) {
            sc.append(' ');
        }
        return sc.toString();
    }

    private static TextColor messageColor(String message) {
        String text = message == null ? "" : message.trim().toLowerCase();
        if (text.startsWith("err:")) {
            return TextColor.ANSI.RED;
        }
        if (text.startsWith("warn:") || text.startsWith("info: demo")) {
            return TextColor.ANSI.YELLOW;
        }
        if (text.startsWith("info:")) {
            return TextColor.ANSI.CYAN;
        }
        return TextColor.ANSI.GREEN;
    }
}
