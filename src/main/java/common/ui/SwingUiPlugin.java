package common.ui;

import common.GameDefinition;

import javax.swing.JOptionPane;
import java.util.List;

public final class SwingUiPlugin implements GameUiPlugin {
    public static final String ID = "swing";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Swing (stub)";
    }

    @Override
    public void start(GameUiBridge bridge) throws Exception {
        GameUiRenderer renderer = new GameUiRenderer() {
            @Override
            public void render(GameUiState state) {
            }

            @Override
            public GameDefinition promptGameType(List<GameDefinition> availableGames, GameDefinition defaultType, String promptTitle) {
                return defaultType;
            }

            @Override
            public void showMessage(String title, String message) {
                JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void close() {
            }
        };

        bridge.attach(renderer);
        renderer.render(bridge.snapshot());
        renderer.showMessage("Info", "Swing UI plugin is a stub — implement later.");
    }
}
