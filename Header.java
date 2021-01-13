package allocator;

/**
 * Header that represents a block in the heap.
 *
 * @author Brandon Kenter
 */
public class Header {
    public int idx;
    public int size;
    public int prevSize;
    public String pBit;
    public String aBit;
}
