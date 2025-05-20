package de.matul.nonnull.test;

import nonnull.NonNull;

interface TestInterface {
    @NonNull Object method(int x, int[] y, String z);
}
