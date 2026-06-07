package common;

import common.ui.GameUiPlugin;
import common.ui.TerminalUiPlugin;
import common.ui.javafx.JavaFxUiPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public final class GameLauncher {
    private static final String DEFAULT_UI = JavaFxUiPlugin.ID;

    private GameLauncher() {
    }

    public static void launch(String[] args) {
        String uiId = resolveUiId(args);
        List<GameUiPlugin> plugins = loadPlugins();

        if ("list".equalsIgnoreCase(uiId)) {
            printAvailablePlugins(plugins);
            return;
        }

        GameUiPlugin plugin = selectPlugin(plugins, uiId);
        if (plugin == null) {
            printAvailablePlugins(plugins);
            throw new IllegalArgumentException("Unknown UI plugin: " + uiId);
        }

        try {
            plugin.start(new TerminalUI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to start UI plugin: " + plugin.getId(), e);
        }
    }

    private static String resolveUiId(String[] args) {
        if (args == null || args.length == 0) {
            return DEFAULT_UI;
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                continue;
            }
            if (arg.startsWith("--ui=")) {
                return arg.substring("--ui=".length()).trim();
            }
            if ("--ui".equalsIgnoreCase(arg) && i + 1 < args.length) {
                return args[i + 1].trim();
            }
            if ("--list-ui".equalsIgnoreCase(arg)) {
                return "list";
            }
        }

        return DEFAULT_UI;
    }

    private static List<GameUiPlugin> loadPlugins() {
        List<GameUiPlugin> plugins = new ArrayList<>();
        ServiceLoader<GameUiPlugin> loader = ServiceLoader.load(GameUiPlugin.class);
        try {
            for (GameUiPlugin plugin : loader) {
                plugins.add(plugin);
            }
        } catch (ServiceConfigurationError e) {
            System.err.println("Warning: failed to load UI plugin: " + e);
        }
        if (plugins.stream().noneMatch(p -> DEFAULT_UI.equalsIgnoreCase(p.getId()))) {
            plugins.add(new TerminalUiPlugin());
        }
        return plugins;
    }

    private static GameUiPlugin selectPlugin(List<GameUiPlugin> plugins, String uiId) {
        String normalized = uiId == null || uiId.isBlank() ? DEFAULT_UI : uiId.trim();
        for (GameUiPlugin plugin : plugins) {
            if (plugin.getId().equalsIgnoreCase(normalized)) {
                return plugin;
            }
        }
        return null;
    }

    private static void printAvailablePlugins(List<GameUiPlugin> plugins) {
        System.out.println("Available UI plugins:");
        for (GameUiPlugin plugin : plugins) {
            System.out.println("- " + plugin.getId() + " (" + plugin.getDisplayName() + ")");
        }
    }
}