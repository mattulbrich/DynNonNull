package de.matul.nonnull.jspecify_test;

import nonnull.NonNull;

interface TestInterface {
    @NonNull Object method(int x, int[] y, String z);
}
