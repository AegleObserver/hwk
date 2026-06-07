package common.ui;

public final class TerminalUiPlugin implements GameUiPlugin {
    public static final String ID = "terminal";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayName() {
        return "Terminal";
    }

    @Override
    public void start(GameUiBridge bridge) throws Exception {
        TerminalUiRenderer renderer = new TerminalUiRenderer(bridge);
        bridge.attach(renderer);
        renderer.start(bridge.snapshot());
    }
}
