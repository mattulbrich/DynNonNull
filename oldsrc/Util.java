/*
 * This file is part of
 *    ivil - Interactive Verification on Intermediate Language
 *
 * Copyright (C) 2009-2010 Universitaet Karlsruhe, Germany
 * 
 * The system is protected by the GNU General Public License. 
 * See LICENSE.TXT (distributed with this file) for details.
 */

import java.util.Collection;

import nonnull.NonNull;

/**
 * This is a collection of static methods  
 */
public class Util {
    
    @SuppressWarnings({"unchecked", "nullness"}) 
    public static <E> E[] listToArray(@NonNull Collection<? extends E> collection, @NonNull Class<E> clss) {
        E[] array = (E[]) java.lang.reflect.Array.newInstance(clss, collection.size());
        return collection.toArray(array);
    }

}
