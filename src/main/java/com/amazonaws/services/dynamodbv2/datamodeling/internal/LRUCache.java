/*
 * Copyright 2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.amazonaws.services.dynamodbv2.datamodeling.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.amazonaws.annotation.ThreadSafe;

/**
 * A bounded cache that has a LRU eviction policy when the cache is full.
 *
 * @param <T>
 *            value type
 */
@ThreadSafe
public final class LRUCache<T> {
    private final Map<String, T> map;
    private final RemovalListener<T> listener;
    private final int maxSize;

    /**
     * @param maxSize
     *            the maximum number of entries of the cache
     * @param listener
     *            object which is notified immediately prior to the removal of any objects from the
     *            cache
     */
    public LRUCache(final int maxSize, final RemovalListener<T> listener) {
        if (maxSize < 1) {
            throw new IllegalArgumentException("maxSize " + maxSize + " must be at least 1");
        }
        this.maxSize = maxSize;
        this.listener = listener;
        map = Collections.synchronizedMap(new LRUHashMap<T>(maxSize, listener));
    }

    /**
     * @param maxSize
     *            the maximum number of entries of the cache
     */
    public LRUCache(final int maxSize) {
        this(maxSize, null);
    }

    /**
     * Adds an entry to the cache, evicting the earliest entry if necessary.
     */
    public T add(final String key, final T value) {
        return map.put(key, value);
    }

    /** Returns the value of the given key; or null of no such entry exists. */
    public T get(final String key) {
        return map.get(key);
    }

    /**
     * Returns the current size of the cache.
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns the maximum size of the cache.
     */
    public int getMaxSize() {
        return maxSize;
    }

    public void clear() {
        // The more complicated logic is to ensure that the listener is
        // actually called for all entries.
        if (listener != null) {
            Set<Map.Entry<String, T>> entries = new HashSet<Map.Entry<String, T>>(map.entrySet());
            if (entries != null) {
                for (final Map.Entry<String, T> e : entries) {
                    listener.onRemoval(e);
                    map.remove(e.getKey());
                }
            }
        } else {
            map.clear();
        }
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @SuppressWarnings("serial")
    private static class LRUHashMap<T> extends LinkedHashMap<String, T> {
        private final int maxSize;
        private final RemovalListener<T> listener;

        private LRUHashMap(final int maxSize, final RemovalListener<T> listener) {
            super(10, 0.75F, true);
            this.maxSize = maxSize;
            this.listener = listener;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<String, T> eldest) {
            if (size() > maxSize) {
                if (listener != null) {
                    listener.onRemoval(eldest);
                }
                return true;
            }
            return false;
        }
    }

    public static interface RemovalListener<T> {
        public void onRemoval(Map.Entry<String, T> entry);
    }
}
