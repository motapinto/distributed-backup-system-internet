package Chord;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * The ChordID is an object identifier on a Distributed Hash Table
 */
public class ChordID implements Serializable {
    private static final long serialVersionUID = 9L;

    private final BigInteger id;

    public static final int SIZE = 8;
    public static final BigInteger MIN = BigInteger.ZERO;
    public static final BigInteger MAX = BigInteger.valueOf(2).pow(SIZE);

    public ChordID(long id) {
        this(BigInteger.valueOf(id));
    }

    public static ChordID fromHex(String idHex) {
        return new ChordID(new BigInteger(idHex, 16));
    }

    public static ChordID fromDec(String id) {
        return new ChordID(new BigInteger(id));
    }

    public ChordID(BigInteger id) {
        this.id = id.mod(MAX);
        if(this.id.compareTo(MIN) < 0) throw new IllegalArgumentException("Minimum value for key is " + MIN + ". Received: " + id);
        if(this.id.compareTo(MAX) >= 0) throw new IllegalArgumentException("Maximum value for key is " + MAX + ". Received: " + id);

    }
    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        ChordID id = (ChordID) obj;
        return id.id.equals(this.id);
    }

    public boolean isBetween(ChordID lower, ChordID upper) {
        if (lower.equals(upper))
            return true;
        if (lower.greaterThan(upper))
            return this.greaterThan(lower) || !this.greaterThan(upper);
        return this.greaterThan(lower) && !this.greaterThan(upper);
    }

    public boolean greaterThan(ChordID other) {
        return this.id.compareTo(other.id) > 0;
    }

    public ChordID add2Pow(int exp) {
        return new ChordID(this.id.add(BigInteger.valueOf(1 << exp)));
    }
}
