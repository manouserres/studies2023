package org.team100.lib.index;

import org.team100.lib.space.Point;

public final class KDNode<V extends Point> {
    private final V value;
    private KDNode<V> a;
    private KDNode<V> b;

    public KDNode(V v) {
        if (v == null)
            throw new IllegalArgumentException("null value");
        value = v;
    }

    void setA(KDNode<V> n) {
        a = n;
    }

    void setB(KDNode<V> n) {
        b = n;
    }

    public KDNode<V> getA() {
        return a;
    }

    public KDNode<V> getB() {
        return b;
    }

    public double[] getConfig() {
        return value.getState();
    }

    public V getValue() {
        return value;
    }
}