package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class Controller extends ButtonsAndLabels {
    int allocsize = 64;
    int all = 0;


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
    public void allocBtnClicked0(ActionEvent event) throws IOException {
        int size = 0;
        if (all == 1) {
            Heap heap = new Heap();
            size = 0;
        }
        if (all == 2) {
            size = 0;
        }

        // Get size wanted to be allocated

        int z = 0; // Loop counter for next-fit algorithm

        // Make sure requested space < available space
        if (size > allocsize - 4) {
            // Requested is too much
        }
        // Make sure requested space > 0
        if (size <= 0) {
            // Request is too little
        }

        // If first allocation, set heapRecent and current
        if (Heap.heapRecent == null) {
            Heap.heapRecent = Heap.heapStart;
            Heap.current = Heap.heapRecent;
        }
        // Otherwise shift current forward (since this block will be allocated)
        else {
            Heap.current = Heap.heapRecent;
            //current.idx = current.idx + current.size;
        }

        // Calculate block size without padding
        int headerPayloadSize = 4 + size;

        // If requested size is not a mulitple of 8, add padding
        if (headerPayloadSize % 8 != 0) {
            int offset = (8 - (headerPayloadSize % 8));
            headerPayloadSize = headerPayloadSize + offset;
        }

        // Check if size > heap space by iterating through heap using next-fit placement policy

        //Iterate through blocks
        while (Heap.current.idx != Heap.heapRecent.idx || z == 0 || z == 1) {
            // If 1, terminate if already wrapped around, otherwise increment loop counter
            if (Heap.current.size == 1) {
                if (Heap.current.size == Heap.heapRecent.size && z >= 1) {
                    // return NULL, no block big enough
                    return;
                }
                else {
                    Heap.current = Heap.heapStart;
                    z += 1;
                    continue;
                }
            }

            // If free block with enough size is found
            if ((!Heap.current.aBit) && (Heap.current.size >= headerPayloadSize)) {
                // Split if possible (available size - alloc size >= alloc size + 8)
                if (Heap.current.size - headerPayloadSize >= 8) {
                    // Get size of free block
                    int freeSize = Heap.current.size - headerPayloadSize;

                    // Instantiate newly split free block's header and set p-bit
                    Heap.bytes[Heap.current.idx + headerPayloadSize] = new Header();
                    Heap.bytes[Heap.current.idx + headerPayloadSize].idx = Heap.current.idx + headerPayloadSize;
                    Heap.bytes[Heap.current.idx  + headerPayloadSize].size = freeSize;
                    Heap.bytes[Heap.current.idx  + headerPayloadSize].pBit = true;
                    Heap.bytes[Heap.current.idx  + headerPayloadSize].prevSize = Heap.current.size;

                    // update heapRecent to recently allocated block
                    Heap.heapRecent = Heap.current;

                    // Set size of current
                    Heap.current.size = headerPayloadSize;
                    Heap.current.aBit = true;

                    // Return ptr of allocated block's payload (in this case set ptr in address row)
                    all += 1;
                    color();
                    return;
                }

                // Otherwise allocate single block
                else {
                    // Set a-bit of current block
                    Heap.current.aBit = true;

                    // Iterate to next header to update (if not at end)
                    Heap.current = Heap.bytes[Heap.current.idx + Heap.current.size];

                    // Make sure next header is not the end, then update
                    if (Heap.current.size != 1) {
                        Heap.current.pBit = true;
                    }

                    // Update heapRecent to recently allocated block
                    Heap.heapRecent = Heap.current;

                    // Return ptr of allocated block's payload (in this case set ptr in address row)
                    all += 1;
                    return;
                }
            }
            // Current block is allocated or not big enough, iterate to next block header.

            Heap.current = Heap.bytes[Heap.current.idx + Heap.current.size];
        }
    }
    private void color() {
        for (int i = 0; i < Heap.bytes.length; i++) {
            if (Heap.bytes[i] != null && Heap.bytes[i].aBit) {
                int j = Heap.bytes[i].size;
                for (int z = i; z < j; z++) {
                    switch(z) {
                        case 0:
                            bit0.setStyle("-fx-fill: #58FF36");
                        case 1:
                            bit1.setStyle("-fx-fill: #58FF36");
                        case 2:
                            bit2.setStyle("-fx-fill: #58FF36");
                        case 3:
                            bit3.setStyle("-fx-fill: #58FF36");
                        case 4:
                            bit4.setStyle("-fx-fill: #58FF36");
                        case 5:
                            bit5.setStyle("-fx-fill: #58FF36");
                        case 6:
                            bit6.setStyle("-fx-fill: #58FF36");
                        case 7:
                            bit7.setStyle("-fx-fill: #58FF36");
                        case 8:
                            bit8.setStyle("-fx-fill: #58FF36");
                        case 9:
                            bit9.setStyle("-fx-fill: #58FF36");
                        case 10:
                            bit10.setStyle("-fx-fill: #58FF36");
                        case 11:
                            bit11.setStyle("-fx-fill: #58FF36");
                        case 12:
                            bit12.setStyle("-fx-fill: #58FF36");
                        case 13:
                            bit13.setStyle("-fx-fill: #58FF36");
                        case 14:
                            bit14.setStyle("-fx-fill: #58FF36");
                        case 15:
                            bit15.setStyle("-fx-fill: #58FF36");
                        case 16:
                            bit16.setStyle("-fx-fill: #58FF36");
                        case 17:
                            bit17.setStyle("-fx-fill: #58FF36");
                    }
                }
            }
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
    public void freeButtonClicked(ActionEvent event) {
        int idx = 0; // TODO PLACEHOLDER


        // Free original block
        Heap.current = Heap.bytes[idx - 4];
        Heap.current.aBit = false;
        Heap.bytes[Heap.current.idx + Heap.current.size].pBit = false;

        // If next is not the end of the heap and is free, coalesce original and next
        if (Heap.bytes[Heap.current.idx + Heap.current.size].size != 1 && !Heap.bytes[Heap.current.idx + Heap.current.size].aBit) {
            // Free next
            int nextSize = Heap.bytes[Heap.current.idx + Heap.current.size].size;
            Heap.bytes[Heap.current.idx + Heap.current.size] = null;
            // Coalesce with original
            Heap.bytes[Heap.current.idx].size += nextSize;
        }

        // Else if still not end of heap, but next block is not free, just update p-bit of next block
        if (Heap.bytes[Heap.current.idx + Heap.current.size].size != 0) {
            Heap.bytes[Heap.current.idx + Heap.current.size].pBit = false;
        }

        // If previous is free, coalesce original and previous
        if (Heap.bytes[Heap.current.idx].pBit = false) {
            // Update previous size
            Heap.bytes[Heap.current.idx - Heap.bytes[Heap.current.idx].size].size += Heap.bytes[Heap.current.idx].size;
            // Coalesce with original (free current)
            Heap.bytes[Heap.current.idx] = null;
            return;
        }
    }
}
