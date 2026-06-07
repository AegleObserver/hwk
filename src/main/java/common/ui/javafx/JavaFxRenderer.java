package common.ui.javafx;

import common.ui.GameUiRenderer;
import common.ui.GameUiState;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;

public class JavaFxRenderer implements GameUiRenderer {
    private final JavaFxApp app;

    public JavaFxRenderer(JavaFxApp app) {
        this.app = app;
    }

    @Override
    public void render(GameUiState state) {
        app.renderState(state);
    }

    @Override
    public common.GameDefinition promptGameType(List<common.GameDefinition> availableGames, common.GameDefinition defaultType, String promptTitle) {
        if (availableGames == null || availableGames.isEmpty()) {
            return null;
        }

        AtomicReference<String> selectedName = new AtomicReference<>(null);
        Runnable showDialog = () -> {
            List<String> names = new ArrayList<>();
            for (common.GameDefinition gameDefinition : availableGames) {
                names.add(gameDefinition.getName());
            }

            String defaultName = defaultType == null ? names.get(0) : defaultType.getName();
            javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(defaultName, names);
            dialog.setTitle(promptTitle == null ? "Game Type" : promptTitle);
            dialog.setHeaderText("Choose mode:");
            java.util.Optional<String> chosen = dialog.showAndWait();
            chosen.ifPresent(selectedName::set);
        };

        if (Platform.isFxApplicationThread()) {
            showDialog.run();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    showDialog.run();
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        String name = selectedName.get();
        if (name == null) {
            return null;
        }
        for (common.GameDefinition gameDefinition : availableGames) {
            if (name.equals(gameDefinition.getName())) {
                return gameDefinition;
            }
        }
        return null;
    }

    @Override
    public void showMessage(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(message);
            a.showAndWait();
        });
    }

    @Override
    public void close() {
        Platform.runLater(() -> javafx.application.Platform.exit());
    }
}
