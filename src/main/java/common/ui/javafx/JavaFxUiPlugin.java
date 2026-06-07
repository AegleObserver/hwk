package common.ui.javafx;

import common.ui.GameUiBridge;
import common.ui.GameUiPlugin;
import javafx.application.Application;

public final class JavaFxUiPlugin implements GameUiPlugin {
    public static final String ID = "javafx";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "JavaFX UI";
    }

    @Override
    public void start(GameUiBridge bridge) throws Exception {
        JavaFxApp.startWithBridge(bridge);
    }
}
