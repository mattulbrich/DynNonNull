/*
 * NonNull Runtime Checking for Methods
 * 
 * 2009 by Mattias Ulbrich
 * 
 * published under GPL.
 */

package de.matul.nonnull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A dynamic array is similar to an {@link ArrayList}. However, no attention
 * must be paid on the index range: Values can be read from and written to any non-negative index.
 * If the index has not been used before, <code>null</code> is returned as the read value.
 *
 * @author mattias ulbrich
 * 
 * @param <T>
 *            the type of elements to be stored in the dynamic array
 */
public class DynamicArray<T> implements Serializable {

    private static final long serialVersionUID = -7539901196038036280L;

    /**
     * The array that holds the actual data
     */
    private Object[] array;

    /**
     * The lock used for synchronisation during writing
     */
    private transient final Object lock = new Object();

    /**
     * Instantiates a new empty dynamic array with default initial size 10.
     */
    public DynamicArray() {
        this(10);
    }

    /**
     * Instantiates a new dynamic array with the given initial size.
     * 
     * @param initialSize
     *            the initial size, {@literal > 0}
     */
    public DynamicArray(int initialSize) {
        if(initialSize <= 0) {
            throw new IllegalArgumentException("Size of a dynamic array must be positive");
        }
        array = new Object[initialSize];
    }

    /**
     * Write an entry to the array. If the index is not within the range of the
     * array, the range is augmented automatically.
     * 
     * @param index
     *            the index to write to, {@literal >= 0}
     * @param entry
     *            the value to store
     */
    public void put(int index, T entry) {
        ensureSize(index);
        array[index] = entry;
    }

    /**
     * Gets the value at an index. If the index has not been written to, yet, it
     * returns null.
     * 
     * @param index
     *            the index, {@literal >= 0);
     * @return the value stored at the index, or <code>null</code> if non stored.
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if(index < array.length) {
            return (T)array[index];
        } else {
            return null;
        }
    }

    /**
     * Removes an index from the array.
     * 
     * @param index
     *            the index, {@literal >= 0}
     */
    public void remove(int index) {
        if(index < array.length) {
            put(index, null);
        }
    }

    /*
     * Ensure size of the array such that index is within the range.
     * 
     * This is synchronised as only one thread should modify the array.
     */
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
}
