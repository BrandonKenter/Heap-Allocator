package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;

public class Controller extends ButtonsAndLabels implements Initializable {
    int allocsize = 64; // Size of the allocatable space in the block of memory
    public static ObservableList<String> allocOptions = FXCollections.observableArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
    public static ObservableList<String> freeOptions = FXCollections.observableArrayList();

    /**
     * Method for allocating 'size' bytes of heap memory.
     * Argument size: Requested size for the payload
     * Update cells upon block allocation success.
     * Returns error if allocation fails.
     *
     * This method:
     * - Checks size - Returns if not positive or if larger than heap space.
     * - Determines block size rounding up to a multiple of 8 and possibly adding padding as a result.
     * - Uses next-fit placement policy to choose a free block
     * - Uses splitting to divide the hcosen free block into two if it is too large.
     * - Updates header(s) as needed.
     *
     */
    public void allocBtnClicked0() {
        // Get size wanted to be allocated
        int size = Integer.parseInt((String) comboBoxAlloc.getValue());

        // Loop counter for next-fit algorithm
        int z = 0;

        // Make sure requested space < available space
        if (size > allocsize - 4) {
            // Requested is too much
            return;
        }

        // Make sure requested space > 0
        if (size <= 0) {
            return;
        }

        // If first allocation, set heapRecent and current
        if (Heap.heapRecent == null) {
            Heap.heapRecent = Heap.heapStart;
            Heap.current = Heap.heapRecent;
        }

        // Otherwise set current to heapRecent
        else {
            Heap.current = Heap.heapRecent;
        }

        // Calculate block size without padding
        int headerPayloadSize = 4 + size;

        // If requested size is not a multiple of 8, add padding
        if (headerPayloadSize % 8 != 0) {
            int offset = (8 - (headerPayloadSize % 8));
            headerPayloadSize = headerPayloadSize + offset;
        }

        // ---- Check if size > heap space by iterating through heap using next-fit placement policy ----

        //Iterate through blocks
        while (Heap.current.idx != Heap.heapRecent.idx || z == 0 || z == 1) {

            // If 1, terminate if already wrapped around, otherwise increment loop counter
            if (Heap.current.size == 1) {
                if (Heap.current.size == Heap.heapRecent.size && z >= 1) {
                    // Return, no block big enough
                    return;
                }
                else {
                    Heap.current = Heap.heapStart;
                    z += 1;
                    continue;
                }
            }

            // If free block with enough size is found
            if ((Heap.current.aBit.equals("0")) && (Heap.current.size >= headerPayloadSize)) {

                // Split if possible (available size - alloc size >= alloc size + 8)
                if (Heap.current.size - headerPayloadSize >= 8) {

                    // Get size of free block
                    int freeSize = Heap.current.size - headerPayloadSize;

                    // Instantiate newly split free block's header and set p-bit
                    Heap.bytes[Heap.current.idx + headerPayloadSize] = new Header();
                    Heap.bytes[Heap.current.idx + headerPayloadSize].idx = Heap.current.idx + headerPayloadSize;
                    Heap.bytes[Heap.current.idx + headerPayloadSize].size = freeSize;
                    Heap.bytes[Heap.current.idx + headerPayloadSize].pBit = "1";
                    Heap.bytes[Heap.current.idx + headerPayloadSize].aBit = "0";
                    Heap.bytes[Heap.current.idx + headerPayloadSize].prevSize = Heap.current.size - freeSize;

                    // SetText for split freeheader
                    setHeaderCell(Heap.bytes[Heap.current.idx + headerPayloadSize].idx);

                    // update heapRecent to recently allocated block
                    Heap.heapRecent = Heap.current;

                    // Set size of current
                    Heap.current.size = headerPayloadSize;
                    Heap.current.aBit = "1";

                    // Return ptr of allocated block's payload (in this case set ptr in address row)
                    setAllocColor(Heap.current.idx, Heap.current.size);
                    setPointerAddressCell(Heap.current.idx);
                    setHeaderCell(Heap.current.idx);
                    return;
                }

                // Otherwise allocate single block
                else {
                    // Set a-bit of current block
                    Heap.current.aBit = "1";
                    Heap.current.size = headerPayloadSize;

                    // Make sure next header is not the end, then update
                    if (Heap.bytes[Heap.current.idx + Heap.current.size].size != 1) {
                        Heap.bytes[Heap.current.idx + Heap.current.size].pBit = "1";
                        updateHeaderCell(0, Heap.current.idx);
                    }
                    // Update heapRecent to recently allocated block
                    Heap.heapRecent = Heap.current;

                    // Return ptr of allocated block's payload (in this case set ptr in address row)
                    setAllocColor(Heap.current.idx, Heap.current.size);
                    setPointerAddressCell(Heap.current.idx);
                    setHeaderCell(Heap.current.idx);
                    return;
                }
            }

            // Current block is allocated or not big enough, iterate to next block header.
            Heap.current = Heap.bytes[Heap.current.idx + Heap.current.size];
        }
    }

    /**
     * Method for freeing up a previously allocated block.
     * Argument ptr: address of the block to be freed up.
     * Returns positively on success.
     * Returns negatively on failure.
     *
     * This method:
     * - Frees allocated blocks.
     * - Coalesces adjacent free blocks.
     * - Updates headers as necessary.
     */
    public void freeButtonClicked() {
        if (comboBoxFree.getValue() == null) {
            return;
        }

        // Get pointer index and header index
        String address = (String) comboBoxFree.getValue();
        comboBoxFree.getItems().remove(address);
        address = address.substring(3);
        int ptrIdx = Integer.parseInt(address, 16);
        int headerIdx = ptrIdx - 4;

        // ---------- Free original block by setting abit and next's pbit ----------
        Heap.bytes[headerIdx].aBit = "0";
        Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].pBit = "0";
        // Update header, pointer address cells and block colors
        updateHeaderCell(0, headerIdx);
        updatePointerAddressCell(0, ptrIdx);
        updateAllocColor(Heap.bytes[headerIdx].idx, Heap.bytes[headerIdx].size);

        // ---------- If next is not the end of the heap and is free, coalesce original and next ----------
        if (Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].size != 1 && Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].aBit.equals("0")) {
            clearHeaderCell(Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].idx);
            // Free next
            int nextSize = Heap.bytes[Heap.bytes[headerIdx].idx + Heap.bytes[headerIdx].size].size;
            Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].aBit = "0";
            // Coalesce with original
            Heap.bytes[headerIdx].size += nextSize;
            // Update header, pointer address cells and block colors
            updateHeaderCell(1, headerIdx);
            updatePointerAddressCell(1, ptrIdx);
            updateAllocColor(headerIdx, Heap.bytes[headerIdx].size);
        }
        // Else if still not end of heap, but next block is not free, just update p-bit of next block
        if (Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].size != 1) {
            Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].pBit = "0";
        }

        // ---------- If previous is free, coalesce original and previous ---------- //
        if (Heap.bytes[headerIdx].pBit.equals("0")) {
            // Get size of previous block
            int prevBlockSize = Heap.bytes[headerIdx].prevSize;
            // Update size of previous block
            Heap.bytes[headerIdx - Heap.bytes[headerIdx].prevSize].size += Heap.bytes[headerIdx].size;
            // Update header cells, pointer address cell and block colors
            updateHeaderCell(2, headerIdx - Heap.bytes[headerIdx].prevSize);
            updatePointerAddressCell(2, ptrIdx);
            clearHeaderCell(headerIdx);
            updateAllocColor(headerIdx, Heap.bytes[headerIdx].size);
            // Update prevSize of next header if next is not at end of heap
            if (Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].size != 1) {
                Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].prevSize = Heap.bytes[Heap.bytes[headerIdx].idx].size + prevBlockSize;
            }
            // Free original header
            Heap.bytes[Heap.bytes[headerIdx].idx].aBit = "0";
        }
        // Update allocated cells to reflect a free operation

        // If all blocks are freed, update heapRecent to the first block
        if (Heap.bytes[4].size == allocsize) {
            Heap.heapRecent = Heap.bytes[0];
        }
    }

    // TODO IMPLEMENT
    /**
     * Method for
     *
     * This method:
     * -
     */
    public void simulateButtonClicked() {

    }

    /**
     * Method for setting the pointer address cell
     *
     * This method:
     * -
     */
    private void setPointerAddressCell(int headerIdx) {
        int ptrIdx = headerIdx + 4;
        String hexAddress = Integer.toHexString(ptrIdx);
        if (headerIdx == 4) {
            comboBoxFree.getItems().add("0x_08");
        }
        else {
            comboBoxFree.getItems().add("0x_" + hexAddress);
        }
        Collections.sort(freeOptions);

        switch (ptrIdx) {
            case 4 -> ptr1.setText("0x_08");
            case 8 -> ptr2.setText("0x_" + hexAddress);
            case 12 -> ptr3.setText("0x_" + hexAddress);
            case 16 -> ptr4.setText("0x_" + hexAddress);
            case 20 -> ptr5.setText("0x_" + hexAddress);
            case 24 -> ptr6.setText("0x_" + hexAddress);
            case 28 -> ptr7.setText("0x_" + hexAddress);
            case 32 -> ptr8.setText("0x_" + hexAddress);
            case 36 -> ptr9.setText("0x_" + hexAddress);
            case 40 -> ptr10.setText("0x_" + hexAddress);
            case 44 -> ptr11.setText("0x_" + hexAddress);
            case 48 -> ptr12.setText("0x_" + hexAddress);
            case 52 -> ptr13.setText("0x_" + hexAddress);
            case 56 -> ptr14.setText("0x_" + hexAddress);
            case 60 -> ptr15.setText("0x_" + hexAddress);
            case 64 -> ptr16.setText("0x_" + hexAddress);
        }
    }

    // TODO region 2?
    /**
     * Method for updating the pointer address cell.
     *
     * This method:
     * -
     */
    private void updatePointerAddressCell(int region, int ptrIdx) {
        if (region == 0) {
            switch (ptrIdx) {
                case 4 -> ptr1.setText("");
                case 8 -> ptr2.setText("");
                case 12 -> ptr3.setText("");
                case 16 -> ptr4.setText("");
                case 20 -> ptr5.setText("");
                case 24 -> ptr6.setText("");
                case 28 -> ptr7.setText("");
                case 32 -> ptr8.setText("");
                case 36 -> ptr9.setText("");
                case 40 -> ptr10.setText("");
                case 44 -> ptr11.setText("");
                case 48 -> ptr12.setText("");
                case 52 -> ptr13.setText("");
                case 56 -> ptr14.setText("");
                case 60 -> ptr15.setText("");
                case 64 -> ptr16.setText("");
            }
        }
        else if (region == 1) {
            ptrIdx = Heap.bytes[Heap.bytes[ptrIdx - 4].idx +Heap.bytes[ptrIdx - 4].size].idx;
            switch (ptrIdx) {
                case 4 -> ptr1.setText("");
                case 8 -> ptr2.setText("");
                case 12 -> ptr3.setText("");
                case 16 -> ptr4.setText("");
                case 20 -> ptr5.setText("");
                case 24 -> ptr6.setText("");
                case 28 -> ptr7.setText("");
                case 32 -> ptr8.setText("");
                case 36 -> ptr9.setText("");
                case 40 -> ptr10.setText("");
                case 44 -> ptr11.setText("");
                case 48 -> ptr12.setText("");
                case 52 -> ptr13.setText("");
                case 56 -> ptr14.setText("");
                case 60 -> ptr15.setText("");
                case 64 -> ptr16.setText("");
            }
        }
    }

    // TODO implement
    /**
     * Method for clearing the pointer address cell.
     *
     * This method:
     * -
     */
    private void clearPointerAddressCell(int ptrIdx) {
    }

    // TODO helper methods for switches
    /**
     * Method for setting the header cell.
     *
     * This method:
     * -
     */
    private void setHeaderCell(int headerIdx) {
        String size = String.valueOf(Heap.bytes[headerIdx].size);

        switch (headerIdx) {
            case 4 -> bits1.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 8 -> bits2.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 12 -> bits3.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 16 -> bits4.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 20 -> bits5.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 24 -> bits6.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 28 -> bits7.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 32 -> bits8.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 36 -> bits9.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 40 -> bits10.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 44 -> bits11.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 48 -> bits12.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 52 -> bits13.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 56 -> bits14.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 60 -> bits15.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            case 64 -> bits16.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
        }
        }

        // TODO helper methods for switches
    /**
     * Method for updating the header cell.
     *
     * This method:
     * -
     */
    private void updateHeaderCell(int region, int headerIdx) {
        //------------------ MIDDLE FREE ------------------
        if (region == 0) {
            String size = String.valueOf(Heap.bytes[headerIdx].size);

            switch (headerIdx) {
                case 4 -> bits1.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 8 -> bits2.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 12 -> bits3.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 16 -> bits4.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 20 -> bits5.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 24 -> bits6.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 28 -> bits7.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 32 -> bits8.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 36 -> bits9.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 40 -> bits10.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 44 -> bits11.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 48 -> bits12.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 52 -> bits13.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 56 -> bits14.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 60 -> bits15.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 64 -> bits16.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            }

            // Update next's p-bit to reflect freeing block
            int nextHeaderIdx = Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].idx;
            size = String.valueOf(Heap.bytes[nextHeaderIdx].size);

            switch (nextHeaderIdx) {
                case 4 -> bits1.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 8 -> bits2.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 12 -> bits3.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 16 -> bits4.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 20 -> bits5.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 24 -> bits6.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 28 -> bits7.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 32 -> bits8.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 36 -> bits9.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 40 -> bits10.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 44 -> bits11.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 48 -> bits12.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 52 -> bits13.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 56 -> bits14.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 60 -> bits15.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 64 -> bits16.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
            }
        }

        //------------------ MIDDLE/RIGHT COALESCE ------------------
        else if (region == 1) {
            String size = String.valueOf(Heap.bytes[headerIdx].size);

            switch (headerIdx) {
                case 4 -> bits1.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 8 -> bits2.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 12 -> bits3.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 16 -> bits4.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 20 -> bits5.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 24 -> bits6.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 28 -> bits7.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 32 -> bits8.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 36 -> bits9.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 40 -> bits10.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 44 -> bits11.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 48 -> bits12.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 52 -> bits13.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 56 -> bits14.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 60 -> bits15.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 64 -> bits16.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            }
        }

        //------------------ LEFT/MIDDLE COALESCE ------------------
        else {
            // Update left header
            String size = String.valueOf(Heap.bytes[headerIdx].size);

            switch (headerIdx) {
                case 4 -> bits1.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 8 -> bits2.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 12 -> bits3.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 16 -> bits4.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 20 -> bits5.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 24 -> bits6.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 28 -> bits7.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 32 -> bits8.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 36 -> bits9.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 40 -> bits10.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 44 -> bits11.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 48 -> bits12.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 52 -> bits13.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 56 -> bits14.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 60 -> bits15.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
                case 64 -> bits16.setText(size + "/" + Heap.bytes[headerIdx].pBit + "/" + Heap.bytes[headerIdx].aBit);
            }

            // Update next's p-bit to reflect freeing block
            int nextHeaderIdx = Heap.bytes[headerIdx + Heap.bytes[headerIdx].size].idx;
            size = String.valueOf(Heap.bytes[nextHeaderIdx].size);

            switch (nextHeaderIdx) {
                case 4 -> bits1.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 8 -> bits2.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 12 -> bits3.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 16 -> bits4.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 20 -> bits5.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 24 -> bits6.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 28 -> bits7.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 32 -> bits8.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 36 -> bits9.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 40 -> bits10.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 44 -> bits11.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 48 -> bits12.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 52 -> bits13.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 56 -> bits14.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 60 -> bits15.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
                case 64 -> bits16.setText(size + "/" + Heap.bytes[nextHeaderIdx].pBit + "/" + Heap.bytes[nextHeaderIdx].aBit);
            }
        }
    }

    /**
     * Method for clearing the header cell.
     *
     * This method:
     * -
     */
    private void clearHeaderCell(int headerIdx) {
        switch (headerIdx) {
            case 4 -> bits1.setText("");
            case 8 -> bits2.setText("");
            case 12 -> bits3.setText("");
            case 16 -> bits4.setText("");
            case 20 -> bits5.setText("");
            case 24 -> bits6.setText("");
            case 28 -> bits7.setText("");
            case 32 -> bits8.setText("");
            case 36 -> bits9.setText("");
            case 40 -> bits10.setText("");
            case 44 -> bits11.setText("");
            case 48 -> bits12.setText("");
            case 52 -> bits13.setText("");
            case 56 -> bits14.setText("");
            case 60 -> bits15.setText("");
            case 64 -> bits16.setText("");
        }
    }

    /**
     * Method for setting the allocated color.
     *
     * This method:
     * -
     */
    private void setAllocColor(int idx, int size) {
        for (int i = idx; i < idx + size; i++) {
            switch (i) {
                case 0 -> bit0.setStyle("-fx-background-color: #008244");
                case 1 -> bit1.setStyle("-fx-background-color: #008244");
                case 2 -> bit2.setStyle("-fx-background-color: #008244");
                case 3 -> bit3.setStyle("-fx-background-color: #008244");
                case 4 -> bit4.setStyle("-fx-background-color: #008244");
                case 5 -> bit5.setStyle("-fx-background-color: #008244");
                case 6 -> bit6.setStyle("-fx-background-color: #008244");
                case 7 -> bit7.setStyle("-fx-background-color: #008244");
                case 8 -> bit8.setStyle("-fx-background-color: #008244");
                case 9 -> bit9.setStyle("-fx-background-color: #008244");
                case 10 -> bit10.setStyle("-fx-background-color: #008244");
                case 11 -> bit11.setStyle("-fx-background-color: #008244");
                case 12 -> bit12.setStyle("-fx-background-color: #008244");
                case 13 -> bit13.setStyle("-fx-background-color: #008244");
                case 14 -> bit14.setStyle("-fx-background-color: #008244");
                case 15 -> bit15.setStyle("-fx-background-color: #008244");
                case 16 -> bit16.setStyle("-fx-background-color: #008244");
                case 17 -> bit17.setStyle("-fx-background-color: #008244");
                case 18 -> bit18.setStyle("-fx-background-color: #008244");
                case 19 -> bit19.setStyle("-fx-background-color: #008244");
                case 20 -> bit20.setStyle("-fx-background-color: #008244");
                case 21 -> bit21.setStyle("-fx-background-color: #008244");
                case 22 -> bit22.setStyle("-fx-background-color: #008244");
                case 23 -> bit23.setStyle("-fx-background-color: #008244");
                case 24 -> bit24.setStyle("-fx-background-color: #008244");
                case 25 -> bit25.setStyle("-fx-background-color: #008244");
                case 26 -> bit26.setStyle("-fx-background-color: #008244");
                case 27 -> bit27.setStyle("-fx-background-color: #008244");
                case 28 -> bit28.setStyle("-fx-background-color: #008244");
                case 29 -> bit29.setStyle("-fx-background-color: #008244");
                case 30 -> bit30.setStyle("-fx-background-color: #008244");
                case 31 -> bit31.setStyle("-fx-background-color: #008244");
                case 32 -> bit32.setStyle("-fx-background-color: #008244");
                case 33 -> bit33.setStyle("-fx-background-color: #008244");
                case 34 -> bit34.setStyle("-fx-background-color: #008244");
                case 35 -> bit35.setStyle("-fx-background-color: #008244");
                case 36 -> bit36.setStyle("-fx-background-color: #008244");
                case 37 -> bit37.setStyle("-fx-background-color: #008244");
                case 38 -> bit38.setStyle("-fx-background-color: #008244");
                case 39 -> bit39.setStyle("-fx-background-color: #008244");
                case 40 -> bit40.setStyle("-fx-background-color: #008244");
                case 41 -> bit41.setStyle("-fx-background-color: #008244");
                case 42 -> bit42.setStyle("-fx-background-color: #008244");
                case 43 -> bit43.setStyle("-fx-background-color: #008244");
                case 44 -> bit44.setStyle("-fx-background-color: #008244");
                case 45 -> bit45.setStyle("-fx-background-color: #008244");
                case 46 -> bit46.setStyle("-fx-background-color: #008244");
                case 47 -> bit47.setStyle("-fx-background-color: #008244");
                case 48 -> bit48.setStyle("-fx-background-color: #008244");
                case 49 -> bit49.setStyle("-fx-background-color: #008244");
                case 50 -> bit50.setStyle("-fx-background-color: #008244");
                case 51 -> bit51.setStyle("-fx-background-color: #008244");
                case 52 -> bit52.setStyle("-fx-background-color: #008244");
                case 53 -> bit53.setStyle("-fx-background-color: #008244");
                case 54 -> bit54.setStyle("-fx-background-color: #008244");
                case 55 -> bit55.setStyle("-fx-background-color: #008244");
                case 56 -> bit56.setStyle("-fx-background-color: #008244");
                case 57 -> bit57.setStyle("-fx-background-color: #008244");
                case 58 -> bit58.setStyle("-fx-background-color: #008244");
                case 59 -> bit59.setStyle("-fx-background-color: #008244");
                case 60 -> bit60.setStyle("-fx-background-color: #008244");
                case 61 -> bit61.setStyle("-fx-background-color: #008244");
                case 62 -> bit62.setStyle("-fx-background-color: #008244");
                case 63 -> bit63.setStyle("-fx-background-color: #008244");
                case 64 -> bit64.setStyle("-fx-background-color: #008244");
                case 65 -> bit65.setStyle("-fx-background-color: #008244");
                case 66 -> bit66.setStyle("-fx-background-color: #008244");
                case 67 -> bit67.setStyle("-fx-background-color: #008244");
                case 68 -> bit68.setStyle("-fx-background-color: #008244");
                case 69 -> bit69.setStyle("-fx-background-color: #008244");
                case 70 -> bit70.setStyle("-fx-background-color: #008244");
                case 71 -> bit71.setStyle("-fx-background-color: #008244");
            }
        }
    }

    /**
     * Method for updating the allocated color.
     *
     * This method:
     * -
     */
    private void updateAllocColor(int idx, int size) {
        for (int i = idx; i < idx + size; i++) {
            switch (i) {
                case 0 -> bit0.setStyle("-fx-background-color: ");
                case 1 -> bit1.setStyle("-fx-background-color: ");
                case 2 -> bit2.setStyle("-fx-background-color: ");
                case 3 -> bit3.setStyle("-fx-background-color: ");
                case 4 -> bit4.setStyle("-fx-background-color: ");
                case 5 -> bit5.setStyle("-fx-background-color: ");
                case 6 -> bit6.setStyle("-fx-background-color: ");
                case 7 -> bit7.setStyle("-fx-background-color: ");
                case 8 -> bit8.setStyle("-fx-background-color: ");
                case 9 -> bit9.setStyle("-fx-background-color: ");
                case 10 -> bit10.setStyle("-fx-background-color: ");
                case 11 -> bit11.setStyle("-fx-background-color: ");
                case 12 -> bit12.setStyle("-fx-background-color: ");
                case 13 -> bit13.setStyle("-fx-background-color: ");
                case 14 -> bit14.setStyle("-fx-background-color: ");
                case 15 -> bit15.setStyle("-fx-background-color: ");
                case 16 -> bit16.setStyle("-fx-background-color: ");
                case 17 -> bit17.setStyle("-fx-background-color: ");
                case 18 -> bit18.setStyle("-fx-background-color: ");
                case 19 -> bit19.setStyle("-fx-background-color: ");
                case 20 -> bit20.setStyle("-fx-background-color: ");
                case 21 -> bit21.setStyle("-fx-background-color: ");
                case 22 -> bit22.setStyle("-fx-background-color: ");
                case 23 -> bit23.setStyle("-fx-background-color: ");
                case 24 -> bit24.setStyle("-fx-background-color: ");
                case 25 -> bit25.setStyle("-fx-background-color: ");
                case 26 -> bit26.setStyle("-fx-background-color: ");
                case 27 -> bit27.setStyle("-fx-background-color: ");
                case 28 -> bit28.setStyle("-fx-background-color: ");
                case 29 -> bit29.setStyle("-fx-background-color: ");
                case 30 -> bit30.setStyle("-fx-background-color: ");
                case 31 -> bit31.setStyle("-fx-background-color: ");
                case 32 -> bit32.setStyle("-fx-background-color: ");
                case 33 -> bit33.setStyle("-fx-background-color: ");
                case 34 -> bit34.setStyle("-fx-background-color: ");
                case 35 -> bit35.setStyle("-fx-background-color: ");
                case 36 -> bit36.setStyle("-fx-background-color: ");
                case 37 -> bit37.setStyle("-fx-background-color: ");
                case 38 -> bit38.setStyle("-fx-background-color: ");
                case 39 -> bit39.setStyle("-fx-background-color: ");
                case 40 -> bit40.setStyle("-fx-background-color: ");
                case 41 -> bit41.setStyle("-fx-background-color: ");
                case 42 -> bit42.setStyle("-fx-background-color: ");
                case 43 -> bit43.setStyle("-fx-background-color: ");
                case 44 -> bit44.setStyle("-fx-background-color: ");
                case 45 -> bit45.setStyle("-fx-background-color: ");
                case 46 -> bit46.setStyle("-fx-background-color: ");
                case 47 -> bit47.setStyle("-fx-background-color: ");
                case 48 -> bit48.setStyle("-fx-background-color: ");
                case 49 -> bit49.setStyle("-fx-background-color: ");
                case 50 -> bit50.setStyle("-fx-background-color: ");
                case 51 -> bit51.setStyle("-fx-background-color: ");
                case 52 -> bit52.setStyle("-fx-background-color: ");
                case 53 -> bit53.setStyle("-fx-background-color: ");
                case 54 -> bit54.setStyle("-fx-background-color: ");
                case 55 -> bit55.setStyle("-fx-background-color: ");
                case 56 -> bit56.setStyle("-fx-background-color: ");
                case 57 -> bit57.setStyle("-fx-background-color: ");
                case 58 -> bit58.setStyle("-fx-background-color: ");
                case 59 -> bit59.setStyle("-fx-background-color: ");
                case 60 -> bit60.setStyle("-fx-background-color: ");
                case 61 -> bit61.setStyle("-fx-background-color: ");
                case 62 -> bit62.setStyle("-fx-background-color: ");
                case 63 -> bit63.setStyle("-fx-background-color: ");
                case 64 -> bit64.setStyle("-fx-background-color: ");
                case 65 -> bit65.setStyle("-fx-background-color: ");
                case 66 -> bit66.setStyle("-fx-background-color: ");
                case 67 -> bit67.setStyle("-fx-background-color: ");
                case 68 -> bit68.setStyle("-fx-background-color: ");
                case 69 -> bit69.setStyle("-fx-background-color: ");
                case 70 -> bit70.setStyle("-fx-background-color: ");
                case 71 -> bit71.setStyle("-fx-background-color: ");
            }
        }
    }

    /**
     * Method for initializing the heap.
     *
     * This method:
     * -
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set alloc combo box options
        comboBoxAlloc.setItems(allocOptions);
        comboBoxFree.setItems(freeOptions);

        // Initialize heapStart
        Heap.bytes[4] = new Header();
        Heap.heapStart = Heap.bytes[4];
        Heap.heapStart.idx = 4;
        Heap.heapStart.aBit = "0";
        Heap.heapStart.pBit = "1";
        Heap.heapStart.size = 64;

        // Initialize heapStart's header block
        bits1.setText("64/1/0");

        // Initialize first reserved block
        Heap.bytes[0] = new Header();
        Heap.bytes[0].idx = 0;
        Heap.bytes[0].aBit = "1";
        Heap.bytes[0].pBit = "1";
        Heap.bytes[0].size = 4;

        // Initialize end of heap area
        Heap.bytes[68] = new Header();
        Heap.bytes[68].size = 1;
        Heap.bytes[68].idx = 68;
    }
}
