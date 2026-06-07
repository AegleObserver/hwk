package common;

public class StringConstructor {
    private final StringBuilder delegate;

    public StringConstructor() {
        this.delegate = new StringBuilder();
    }

    public StringConstructor(int capacity) {
        this.delegate = new StringBuilder(capacity);
    }

    public StringConstructor append(char c) {
        delegate.append(c);
        return this;
    }

    public StringConstructor append(CharSequence cs) {
        delegate.append(cs);
        return this;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}