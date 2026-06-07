package common.ui.javafx;

import common.ui.GameUiBridge;
import common.ui.GameUiState;
import common.GameDefinition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.stage.Stage;

import java.util.List;

public class JavaFxApp extends Application {
    private static GameUiBridge bridgeStatic;

    private GameUiBridge bridge;
    private Label titleLabel;
    private Label infoLabel;
    private Label playerLabel;
    private Label scoreLabel;
    private Label boardIndexLabel;
    private Label modeHintLabel;
    private Label messageLabel;
    private GridPane boardGrid;
    private TextField inputField;
    private Button hintButton;
    private Button undoButton;
    private Button sendBtn;
    private Button newGamesBtn;
    private Button resetBtn;
    private Button demoBtn;
    private Thread demoThread;
    private volatile boolean demoRunning;
    private volatile boolean demoPaused;
    private Integer selectedChessRow;
    private Integer selectedChessCol;

    public static void startWithBridge(GameUiBridge bridge) throws Exception {
        bridgeStatic = bridge;
        Application.launch(JavaFxApp.class);
    }

    @Override
    public void init() throws Exception {
        this.bridge = bridgeStatic;
    }

    @Override
    public void start(Stage primaryStage) {
        titleLabel = new Label("Board Games - JavaFX");
        infoLabel = new Label("");
        playerLabel = new Label("");
        scoreLabel = new Label("");
        boardIndexLabel = new Label("");
        modeHintLabel = new Label("");
        messageLabel = new Label("");
        boardGrid = new GridPane();
        boardGrid.setHgap(2);
        boardGrid.setVgap(2);
        boardGrid.setGridLinesVisible(false);
        inputField = new TextField();
        Button sendBtn = this.sendBtn = new Button("Send");
        Button newGamesBtn = this.newGamesBtn = new Button("New Games");
        Button resetBtn = this.resetBtn = new Button("Reset");
        demoBtn = new Button("Demo");
        sendBtn.setOnAction(e -> {
            String txt = inputField.getText();
            if (txt == null) return;
            String input = txt.trim();
            if (input.isEmpty()) return;

            common.ProcessInput parsed = common.ProcessInput.parse(input);
            if (parsed.action == common.ProcessInput.Action.SWITCH_BOARD) {
                bridge.onSwitchGameRequested(parsed.value - 1);
            } else if (parsed.action == common.ProcessInput.Action.QUIT) {
                bridge.onQuitRequested();
            } else {
                bridge.onRawInput(input);
            }

            inputField.clear();
        });
        newGamesBtn.setOnAction(e -> bridge.onNewGameRequested());
        resetBtn.setOnAction(e -> {
            if (demoRunning) stopDemo();
            bridge.onResetRequested();
        });
        demoBtn.setOnAction(e -> toggleDemo());

        hintButton = new Button("Hint");
        hintButton.setOnAction(e -> bridge.onRawInput("H"));
        undoButton = new Button("Undo");
        undoButton.setOnAction(e -> bridge.onRawInput("U"));
        selectedChessRow = null;
        selectedChessCol = null;

        infoLabel.setWrapText(true);
        playerLabel.setWrapText(true);
        scoreLabel.setWrapText(true);
        boardIndexLabel.setWrapText(true);
        modeHintLabel.setWrapText(true);
        messageLabel.setWrapText(true);

        HBox inputRow = new HBox(8, inputField);
        HBox buttonRow = new HBox(8, sendBtn, newGamesBtn, resetBtn);
        HBox actionRow = new HBox(8, demoBtn, hintButton, undoButton);
        VBox bottomControls = new VBox(6, inputRow, buttonRow, actionRow);
        Label gameInfoHeader = new Label("Game Information");
        gameInfoHeader.setStyle("-fx-text-fill: #f0f0f0; -fx-font-weight: bold; -fx-font-size: 13px;");
        VBox currentGamePanel = new VBox(8, gameInfoHeader, infoLabel, playerLabel, scoreLabel, modeHintLabel, bottomControls, messageLabel);
        currentGamePanel.setMinWidth(240);
        currentGamePanel.setPadding(new Insets(12));
        currentGamePanel.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: #f0f0f0;");

        Label matchInfoHeader = new Label("Match Information");
        matchInfoHeader.setStyle("-fx-text-fill: #f0f0f0; -fx-font-weight: bold; -fx-font-size: 13px;");
        VBox allGamesPanel = new VBox(8, matchInfoHeader, boardIndexLabel);
        allGamesPanel.setMinWidth(200);
        allGamesPanel.setPadding(new Insets(12));
        allGamesPanel.setStyle("-fx-background-color: #2e2e2e; -fx-text-fill: #f0f0f0;");

        VBox boardPanelContainer = new VBox(8, boardGrid);
        boardPanelContainer.setPadding(new Insets(12));
        boardPanelContainer.setStyle("-fx-background-color: #1b1b1b;");
        HBox.setHgrow(boardPanelContainer, Priority.ALWAYS);
        HBox.setHgrow(currentGamePanel, Priority.ALWAYS);
        HBox.setHgrow(allGamesPanel, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(titleLabel);
        HBox centerLayout = new HBox(6, boardPanelContainer, currentGamePanel, allGamesPanel);
        root.setCenter(centerLayout);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #101010;");

        titleLabel.setStyle("-fx-text-fill: #f2f2f2; -fx-font-size: 18px; -fx-font-weight: bold;");
        infoLabel.setStyle("-fx-text-fill: #f0f0f0;");
        playerLabel.setStyle("-fx-text-fill: #f0f0f0;");
        scoreLabel.setStyle("-fx-text-fill: #f0f0f0;");
        boardIndexLabel.setStyle("-fx-text-fill: #f0f0f0;");
        modeHintLabel.setStyle("-fx-text-fill: #9fb7ff;");
        messageLabel.setStyle("-fx-text-fill: #9fe870;");

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Board Games - JavaFX");
        primaryStage.setMinWidth(780);
        primaryStage.setMinHeight(550);
        primaryStage.show();

        // initial render and attach renderer
        JavaFxRenderer r = new JavaFxRenderer(this);
        bridge.attach(r);
        r.render(bridge.snapshot());
    }

    public void renderState(GameUiState state) {
        Platform.runLater(() -> {
            titleLabel.setText(state.getTitle());
            infoLabel.setText(state.getGameInfoText());
            playerLabel.setText(state.getCurrentPlayerText());
            scoreLabel.setText(state.getScoreText());
            boardIndexLabel.setText(state.getBoardIndexText());
            boolean isMinesweeper = "Minesweeper".equalsIgnoreCase(state.getActiveGame().getDisplayName());
            boolean isChessBoard = "Chess".equalsIgnoreCase(state.getActiveGame().getDisplayName());
            if (!isChessBoard) {
                clearChessSelection();
            }
            if (isMinesweeper) {
                modeHintLabel.setText("提示：左键翻开，右键插旗。");
            } else if (isChessBoard) {
                modeHintLabel.setText("提示：先点击棋子，再点击目标格。");
            } else {
                modeHintLabel.setText("提示：左键落子；可用 Undo 撤销。");
            }
            hintButton.setVisible(isMinesweeper);
            hintButton.setManaged(isMinesweeper);
            undoButton.setVisible(!isMinesweeper);
            undoButton.setManaged(!isMinesweeper);
            messageLabel.setText(state.getMessageText());
            // simple board render
            boardGrid.getChildren().clear();
            int size = state.getActiveGame().getBoard().getSize();
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    char ch = state.getActiveGame().getBoard().getCell(r, c);
                    boolean hidden = state.getActiveGame().getBoard().isHiddenCell(r, c);
                    boolean marked = state.getActiveGame().getBoard().isMarkedCell(r, c);
                    boolean danger = state.getActiveGame().getBoard().isDangerCell(r, c);
                    boolean isStoneGame = "Reversi".equalsIgnoreCase(state.getActiveGame().getDisplayName()) || "Peace".equalsIgnoreCase(state.getActiveGame().getDisplayName());
                    boolean isSelectedChessSquare = isChessBoard
                        && selectedChessRow != null
                        && selectedChessCol != null
                        && selectedChessRow == r
                        && selectedChessCol == c;

                    String content;
                    String textFill = "#d9d9d9";
                    String background = "#2b4a2f";
                    if (hidden) content = " ";
                    else if (marked) { content = "F"; textFill = "#f6d365"; background = "#334a2f"; }
                    else if (danger) { content = "x"; textFill = "#ff7373"; background = "#4a2b2b"; }
                    else if (isChessBoard && "PNKQRBpnkqrb".indexOf(ch) >= 0) {
                        content = Character.isUpperCase(ch) ? String.valueOf(Character.toLowerCase(ch)) : String.valueOf(Character.toUpperCase(ch));
                        textFill = Character.isUpperCase(ch) ? "#f4f4f4" : "#101010";
                        background = Character.isUpperCase(ch) ? "#6c7a89" : "#d6b36a";
                    } else if (isStoneGame && (ch == 'B' || ch == 'W')) {
                        content = "●";
                        if (ch == 'W') {
                            textFill = "#f7f4ea";
                            background = "#264c3c";
                        } else {
                            textFill = "#e1b94d";
                            background = "#243d28";
                        }
                    } else if (ch == 'B' || ch == 'W') {
                        content = "●";
                    } else if (state.getActiveGame().shouldShowLegalMoves() && state.getActiveGame().getBoard().isLegalMove(r, c, state.getActiveGame().getCurrentPlayer())) {
                        content = "+";
                        textFill = "#ffd966";
                        background = "#35543a";
                    } else if (ch >= '1' && ch <= '8') {
                        content = String.valueOf(ch);
                        textFill = "#8ad0ff";
                    } else {
                        content = ".";
                        textFill = "#88938e";
                    }

                    if (isSelectedChessSquare) {
                        boolean isWhitePiece = Character.isUpperCase(ch);
                        background = isWhitePiece ? "#443e3e" : "#af6807";
                    }

                    String borderColor = isSelectedChessSquare ? "#c9a035" : "#1f2a21";

                    Label cell = new Label(content);
                    cell.setMinSize(34, 34);
                    cell.setPrefSize(34, 34);
                    cell.setMaxSize(34, 34);
                    cell.setStyle(
                        "-fx-border-color: " + borderColor + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-alignment: center; " +
                        "-fx-font-size: 15px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: " + textFill + "; " +
                        "-fx-background-color: " + (hidden ? "#1c2d1e" : background) + ";"
                    );
                    int rr = r, cc = c;
                    cell.setOnMouseClicked(ev -> {
                        if (isDemoActive()) return;
                        String coord = String.valueOf((char) ('A' + cc)) + (size - rr);
                        if (ev.getButton() == MouseButton.SECONDARY && isMinesweeper) {
                            bridge.onRawInput("FLAG " + coord);
                            return;
                        }

                        if (isChessBoard) {
                            handleChessClick(rr, cc, coord);
                        } else {
                            bridge.onRawInput(coord);
                        }
                    });
                    boardGrid.add(cell, c, r);
                }
            }
        });
    }

    private void toggleDemo() {
        if (!demoRunning) {
            startDemo();
        } else {
            demoPaused = !demoPaused;
            Platform.runLater(() -> {
                demoBtn.setText(demoPaused ? "Resume" : "Pause");
                updateControlsEnabled();
            });
        }
    }

    private void startDemo() {
        demoRunning = true;
        demoPaused = false;
        Platform.runLater(() -> {
            bridge.onResetRequested();
            bridge.setDemoRunning(true);
            demoBtn.setText("Pause");
            updateControlsEnabled();
        });
        demoThread = new Thread(() -> {
            try {
                while (demoRunning) {
                    if (!demoPaused) {
                        Platform.runLater(() -> {
                            if (!demoRunning) return;
                            bridge.onRawInput("AUTO");
                            if (bridge.snapshot().getActiveGame().isFinished()) {
                                stopDemo();
                            }
                        });
                    }
                    Thread.sleep(500);
                }
            } catch (InterruptedException ignored) {
            } finally {
                demoRunning = false;
                Platform.runLater(() -> {
                    demoBtn.setText("Demo");
                    updateControlsEnabled();
                    bridge.setDemoRunning(false);
                });
            }
        });
        demoThread.setDaemon(true);
        demoThread.start();
    }

    private void stopDemo() {
        demoRunning = false;
        bridge.setDemoRunning(false);
        if (demoThread != null) {
            demoThread.interrupt();
        }
    }

    private void updateControlsEnabled() {
        boolean blocked = demoRunning;
        inputField.setDisable(blocked);
        sendBtn.setDisable(blocked);
        newGamesBtn.setDisable(blocked);
        hintButton.setDisable(blocked);
        undoButton.setDisable(blocked);
    }

    private boolean isDemoActive() {
        return demoRunning;
    }

    private void handleChessClick(int row, int col, String coord) {
        if (selectedChessRow == null || selectedChessCol == null) {
            if (!isChessPieceCell(row, col)) {
                return;
            }
            selectedChessRow = row;
            selectedChessCol = col;
            renderState(bridge.snapshot());
            return;
        }

        if (selectedChessRow == row && selectedChessCol == col) {
            clearChessSelection();
            renderState(bridge.snapshot());
            return;
        }

        String from = toChessCoordinate(selectedChessRow, selectedChessCol);
        String to = coord.toLowerCase();
        clearChessSelection();
        bridge.onRawInput("m " + from + " " + to);
    }

    private boolean isChessPieceCell(int row, int col) {
        if (boardGrid == null || bridge == null) {
            return false;
        }
        GameUiState state = bridge.snapshot();
        if (state == null || state.getActiveGame() == null || state.getActiveGame().getBoard() == null) {
            return false;
        }
        if (!"Chess".equalsIgnoreCase(state.getActiveGame().getDisplayName())) {
            return false;
        }
        char ch = state.getActiveGame().getBoard().getCell(row, col);
        return "PNKQRBpnkqrb".indexOf(ch) >= 0;
    }

    private String toChessCoordinate(int row, int col) {
        return String.valueOf((char) ('A' + col)).toLowerCase() + (8 - row);
    }

    private void clearChessSelection() {
        selectedChessRow = null;
        selectedChessCol = null;
    }
}
