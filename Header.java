package sample;

public class Header {
    public int idx;
    public int size;
    public int prevSize;
    public boolean pBit;
    public boolean aBit;

    @Override
    public String toString() {
        return ("Index: " + idx + " Size: " + size);
    }
}
