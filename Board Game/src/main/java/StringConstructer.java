public class StringConstructer {
    private final StringBuilder delegate;

    public StringConstructer() {
        this.delegate = new StringBuilder();
    }

    public StringConstructer(int capacity) {
        this.delegate = new StringBuilder(capacity);
    }

    public StringConstructer append(char c) {
        delegate.append(c);
        return this;
    }

    public StringConstructer append(CharSequence cs) {
        delegate.append(cs);
        return this;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
