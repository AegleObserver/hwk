package common.ui;

public interface GameUiPlugin {
    String getId();

    String getDisplayName();

    void start(GameUiBridge bridge) throws Exception;
}
