package common;

import java.util.Objects;
import java.util.function.Supplier;

public final class GameDefinition {
    private final String name;
    private final Supplier<GameSession> factory;

    public GameDefinition(String name, Supplier<GameSession> factory) {
        this.name = Objects.requireNonNull(name, "name");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public String getName() {
        return name;
    }

    public GameSession create() {
        return factory.get();
    }
}