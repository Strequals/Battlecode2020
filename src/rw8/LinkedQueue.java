package rw8;

public class LinkedQueue<T> {
    private LinkedElement<T> first;
    private LinkedElement<T> last;

    public LinkedQueue() {
    }

    public void add(T value) {
        LinkedElement<T> n = new LinkedElement<>(value);
        if (last == null) {
            first = n;
            last = n;
        } else {
            last.next = n;
            last = n;
        }
    }

    public boolean hasNext() {
        return first != null;
    }

    public T poll() {
        T first = this.first.value;
        this.first = this.first.next;
        return first;
    }

    public void clear() {
        first = null;
        last = null;
    }
}

class LinkedElement<T> {
    T value;
    LinkedElement<T> next;

    LinkedElement(T value) {
        this.value = value;
    }
}