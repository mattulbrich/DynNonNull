package de.matul.nonnull;

import java.util.Arrays;

public class DynamicArray<T> {

    private Object[] array;
    private final Object lock = new Object();

    public DynamicArray() {
        this(10);
    }

    public DynamicArray(int initialSize) {
        array = new Object[initialSize];
    }

    public void put(int index, T entry) {
        ensureSize(index);
        array[index] = entry;
    }

    private void ensureSize(int index) {
        if(index >= array.length) {
            synchronized (lock) {
                // check original size again in case of concurrent change
                int newSize = array.length;
                while(index >= newSize) {
                    newSize = newSize * 4 / 3;
                }
                array = Arrays.copyOf(array, newSize);
            }
        }
    }

    public T get(int index) {
        if(index < array.length) {
            return (T)array[index];
        } else {
            return null;
        }
    }

    public void remove(int index) {
        if(index < array.length) {
            put(index, null);
        }
    }
}
