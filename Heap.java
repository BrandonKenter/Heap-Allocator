package sample;

public class Heap {
    public static Header[] bytes = new Header[65];
    public static Header heapStart = null;
    public static Header heapRecent = null;
    public static Header current = null;

    Heap() {
        // Initialize heapStart
        Heap.bytes[0] = new Header();
        Heap.heapStart = Heap.bytes[0];
        Heap.heapStart.idx = 0;
        Heap.heapStart.aBit = false;
        Heap.heapStart.pBit = true;
        Heap.heapStart.size = 64;

        // Initialize end of heap area
        Heap.bytes[64] = new Header();
        Heap.bytes[64].size = 1;
    }
}
