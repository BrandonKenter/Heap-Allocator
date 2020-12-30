package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.SpinnerValueFactory;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;


public class Controller extends ButtonsAndLabels implements Initializable {
    int allocsize = 64; // Size of the allocatable space in the block of memory

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
        int size = (int) allocSpinner.getValue(); // Get size wanted to be allocated

        int z = 0; // Loop counter for next-fit algorithm

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
                    setAllocColor(Heap.current.idx, Heap.current.size);
                    setPointerAddressCell();
                    setHeaderCell();
                    return;
                }

                // Otherwise allocate single block
                else {
                    // Set a-bit of current block
                    Heap.current.aBit = true;
                    Heap.current.size = headerPayloadSize;

                    // Make sure next header is not the end, then update
                    if (Heap.bytes[Heap.current.idx + Heap.current.size].size != 1) {
                        Heap.bytes[Heap.current.idx + Heap.current.size].pBit = true;
                    }

                    // Update heapRecent to recently allocated block
                    Heap.heapRecent = Heap.current;

                    // Return ptr of allocated block's payload (in this case set ptr in address row)

                    setAllocColor(Heap.current.idx, Heap.current.size);
                    setPointerAddressCell();
                    setHeaderCell();
                    return;
                }
            }
            // Current block is allocated or not big enough, iterate to next block header.

            Heap.current = Heap.bytes[Heap.current.idx + Heap.current.size];
        }
    }

    private void setPointerAddressCell() {
        int idx = Heap.current.idx;
        int ptrIdx = idx + 4;
        String hexAddress = Integer.toHexString(ptrIdx);
        comboBoxFree.getItems().add("0x_" + hexAddress);

        switch (ptrIdx) {
            case 4:
                ptr1.setText("0x_" + hexAddress);
                break;
            case 8:
                ptr2.setText("0x_" + hexAddress);
                break;
            case 12:
                ptr3.setText("0x_" + hexAddress);
                break;
            case 16:
                ptr4.setText("0x_" + hexAddress);
                break;
            case 20:
                ptr5.setText("0x_" + hexAddress);
                break;
            case 24:
                ptr6.setText("0x_" + hexAddress);
                break;
            case 28:
                ptr7.setText("0x_" + hexAddress);
                break;
            case 32:
                ptr8.setText("0x_" + hexAddress);
                break;
            case 36:
                ptr9.setText("0x_" + hexAddress);
                break;
            case 40:
                ptr10.setText("0x_" + hexAddress);
                break;
            case 44:
                ptr11.setText("0x_" + hexAddress);
                break;
            case 48:
                ptr12.setText("0x_" + hexAddress);
                break;
            case 52:
                ptr13.setText("0x_" + hexAddress);
                break;
            case 56:
                ptr14.setText("0x_" + hexAddress);
                break;
            case 60:
                ptr15.setText("0x_" + hexAddress);
                break;
            case 64:
                ptr16.setText("0x_" + hexAddress);
                break;
            case 68:
                ptr17.setText("0x_" + hexAddress);
                break;
        }
    }

    private void setHeaderCell() {
        String pBit = null;
        String aBit = null;
        String size = String.valueOf(Heap.current.size);

        if (Heap.current.aBit) {
            aBit = "1";
        }
        else {
            aBit = "0";
        }

        if (Heap.current.pBit) {
            pBit = "1";
        }
        else {
            aBit = "0";
        }

        int idx = Heap.current.idx;

        switch (idx) {
            case 4:
                bits1.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 8:
                bits2.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 12:
                bits3.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 16:
                bits4.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 20:
                bits5.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 24:
                bits6.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 28:
                bits7.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 32:
                bits8.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 36:
                bits9.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 40:
                bits10.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 44:
                bits11.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 48:
                bits12.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 52:
                bits13.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 56:
                bits14.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 60:
                bits15.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 64:
                bits16.setText(size + "/" + pBit + "/" + aBit);
                break;
            case 68:
                bits17.setText(size + "/" + pBit + "/" + aBit);
                break;
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
    public void freeButtonClicked(ActionEvent event) throws IOException {
        String address = (String) comboBoxFree.getValue();
        address = address.substring(3);
        int idx = Integer.parseInt(address, 16);

        // Free original block
        Heap.current = Heap.bytes[idx - 4];
        Heap.current.aBit = false;
        Heap.bytes[Heap.current.idx + Heap.current.size].pBit = false;

        // Update header and pointer address cells
        updateHeaderCell();
        updatePointerAddressCell();

        // If next is not the end of the heap and is free, coalesce original and next
        if (Heap.bytes[Heap.current.idx + Heap.current.size].size != 1 && !Heap.bytes[Heap.current.idx + Heap.current.size].aBit) {
            // Free next
            int nextSize = Heap.bytes[Heap.current.idx + Heap.current.size].size;
            Heap.bytes[Heap.current.idx + Heap.current.size] = null;
            // Coalesce with original
            Heap.bytes[Heap.current.idx].size += nextSize;

            // Update header and pointer address cells
            updateHeaderCell();
            updatePointerAddressCell();
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

            // Update current to remove CSS
            Heap.current = Heap.bytes[Heap.current.idx - Heap.bytes[Heap.current.idx].size];

            // Update header and pointer address cells
            updateHeaderCell();
            updatePointerAddressCell();
        }
        // Update allocated cells to reflect a free operation
        updateAllocColor(Heap.current.idx, Heap.current.size);
    }

    private void updateHeaderCell() {

    }

    private void updatePointerAddressCell() {

    }

    private void updateAllocColor(int idx, int size) throws IOException {
        for (int i = idx; i < idx + size; i++) {
            switch (i) {
                case 0:
                    bit0.setStyle("-fx-background-color: ");
                    break;
                case 1:
                    bit1.setStyle("-fx-background-color: ");
                    break;
                case 2:
                    bit2.setStyle("-fx-background-color: ");
                    break;
                case 3:
                    bit3.setStyle("-fx-background-color: ");
                    break;
                case 4:
                    bit4.setStyle("-fx-background-color: ");
                    break;
                case 5:
                    bit5.setStyle("-fx-background-color: ");
                    break;
                case 6:
                    bit6.setStyle("-fx-background-color: ");
                    break;
                case 7:
                    bit7.setStyle("-fx-background-color: ");
                    break;
                case 8:
                    bit8.setStyle("-fx-background-color: ");
                    break;
                case 9:
                    bit9.setStyle("-fx-background-color: ");
                    break;
                case 10:
                    bit10.setStyle("-fx-background-color: ");
                    break;
                case 11:
                    bit11.setStyle("-fx-background-color: ");
                    break;
                case 12:
                    bit12.setStyle("-fx-background-color: ");
                    break;
                case 13:
                    bit13.setStyle("-fx-background-color: ");
                    break;
                case 14:
                    bit14.setStyle("-fx-background-color: ");
                    break;
                case 15:
                    bit15.setStyle("-fx-background-color: ");
                    break;
                case 16:
                    bit16.setStyle("-fx-background-color: ");
                    break;
                case 17:
                    bit17.setStyle("-fx-background-color: ");
                    break;
                case 18:
                    bit18.setStyle("-fx-background-color: ");
                    break;
                case 19:
                    bit19.setStyle("-fx-background-color: ");
                    break;
                case 20:
                    bit20.setStyle("-fx-background-color: ");
                    break;
                case 21:
                    bit21.setStyle("-fx-background-color: ");
                    break;
                case 22:
                    bit22.setStyle("-fx-background-color: ");
                    break;
                case 23:
                    bit23.setStyle("-fx-background-color: ");
                    break;
                case 24:
                    bit24.setStyle("-fx-background-color: ");
                    break;
                case 25:
                    bit25.setStyle("-fx-background-color: ");
                    break;
                case 26:
                    bit26.setStyle("-fx-background-color: ");
                    break;
                case 27:
                    bit27.setStyle("-fx-background-color: ");
                    break;
                case 28:
                    bit28.setStyle("-fx-background-color: ");
                    break;
                case 29:
                    bit29.setStyle("-fx-background-color: ");
                    break;
                case 30:
                    bit30.setStyle("-fx-background-color: ");
                    break;
                case 31:
                    bit31.setStyle("-fx-background-color: ");
                    break;
                case 32:
                    bit32.setStyle("-fx-background-color: ");
                    break;
                case 33:
                    bit33.setStyle("-fx-background-color: ");
                    break;
                case 34:
                    bit34.setStyle("-fx-background-color: ");
                    break;
                case 35:
                    bit35.setStyle("-fx-background-color: ");
                    break;
                case 36:
                    bit36.setStyle("-fx-background-color: ");
                    break;
                case 37:
                    bit37.setStyle("-fx-background-color: ");
                    break;
                case 38:
                    bit38.setStyle("-fx-background-color: ");
                    break;
                case 39:
                    bit39.setStyle("-fx-background-color: ");
                    break;
                case 40:
                    bit40.setStyle("-fx-background-color: ");
                    break;
                case 41:
                    bit41.setStyle("-fx-background-color: ");
                    break;
                case 42:
                    bit42.setStyle("-fx-background-color: ");
                    break;
                case 43:
                    bit43.setStyle("-fx-background-color: ");
                    break;
                case 44:
                    bit44.setStyle("-fx-background-color: ");
                    break;
                case 45:
                    bit45.setStyle("-fx-background-color: ");
                    break;
                case 46:
                    bit46.setStyle("-fx-background-color: ");
                    break;
                case 47:
                    bit47.setStyle("-fx-background-color: ");
                    break;
                case 48:
                    bit48.setStyle("-fx-background-color: ");
                    break;
                case 49:
                    bit49.setStyle("-fx-background-color: ");
                    break;
                case 50:
                    bit50.setStyle("-fx-background-color: ");
                    break;
                case 51:
                    bit51.setStyle("-fx-background-color: ");
                    break;
                case 52:
                    bit52.setStyle("-fx-background-color: ");
                    break;
                case 53:
                    bit53.setStyle("-fx-background-color: ");
                    break;
                case 54:
                    bit54.setStyle("-fx-background-color: ");
                    break;
                case 55:
                    bit55.setStyle("-fx-background-color: ");
                    break;
                case 56:
                    bit56.setStyle("-fx-background-color: ");
                    break;
                case 57:
                    bit57.setStyle("-fx-background-color: ");
                    break;
                case 58:
                    bit58.setStyle("-fx-background-color: ");
                    break;
                case 59:
                    bit59.setStyle("-fx-background-color: ");
                    break;
                case 60:
                    bit60.setStyle("-fx-background-color: ");
                    break;
                case 61:
                    bit61.setStyle("-fx-background-color: ");
                    break;
                case 62:
                    bit62.setStyle("-fx-background-color: ");
                    break;
                case 63:
                    bit63.setStyle("-fx-background-color: ");
                    break;
                case 64:
                    bit64.setStyle("-fx-background-color: ");
                    break;
                case 65:
                    bit65.setStyle("-fx-background-color: ");
                    break;
                case 66:
                    bit66.setStyle("-fx-background-color: ");
                    break;
                case 67:
                    bit67.setStyle("-fx-background-color: ");
                    break;
                case 68:
                    bit68.setStyle("-fx-background-color: ");
                    break;
                case 69:
                    bit69.setStyle("-fx-background-color: ");
                    break;
                case 70:
                    bit70.setStyle("-fx-background-color: ");
                    break;
                case 71:
                    bit71.setStyle("-fx-background-color: ");
                    break;
            }
        }
    }

    private void setAllocColor(int idx, int size) throws IOException {
        for (int i = idx; i < idx + size; i++) {
            switch(i) {
                case 0:
                    bit0.setStyle("-fx-background-color: #58FF36");
                    break;
                case 1:
                    bit1.setStyle("-fx-background-color: #58FF36");
                    break;
                case 2:
                    bit2.setStyle("-fx-background-color: #58FF36");
                    break;
                case 3:
                    bit3.setStyle("-fx-background-color: #58FF36");
                    break;
                case 4:
                    bit4.setStyle("-fx-background-color: #58FF36");
                    break;
                case 5:
                    bit5.setStyle("-fx-background-color: #58FF36");
                    break;
                case 6:
                    bit6.setStyle("-fx-background-color: #58FF36");
                    break;
                case 7:
                    bit7.setStyle("-fx-background-color: #58FF36");
                    break;
                case 8:
                    bit8.setStyle("-fx-background-color: #58FF36");
                    break;
                case 9:
                    bit9.setStyle("-fx-background-color: #58FF36");
                    break;
                case 10:
                    bit10.setStyle("-fx-background-color: #58FF36");
                    break;
                case 11:
                    bit11.setStyle("-fx-background-color: #58FF36");
                    break;
                case 12:
                    bit12.setStyle("-fx-background-color: #58FF36");
                    break;
                case 13:
                    bit13.setStyle("-fx-background-color: #58FF36");
                    break;
                case 14:
                    bit14.setStyle("-fx-background-color: #58FF36");
                    break;
                case 15:
                    bit15.setStyle("-fx-background-color: #58FF36");
                    break;
                case 16:
                    bit16.setStyle("-fx-background-color: #58FF36");
                    break;
                case 17:
                    bit17.setStyle("-fx-background-color: #58FF36");
                    break;
                case 18:
                    bit18.setStyle("-fx-background-color: #58FF36");
                    break;
                case 19:
                    bit19.setStyle("-fx-background-color: #58FF36");
                    break;
                case 20:
                    bit20.setStyle("-fx-background-color: #58FF36");
                    break;
                case 21:
                    bit21.setStyle("-fx-background-color: #58FF36");
                    break;
                case 22:
                    bit22.setStyle("-fx-background-color: #58FF36");
                    break;
                case 23:
                    bit23.setStyle("-fx-background-color: #58FF36");
                    break;
                case 24:
                    bit24.setStyle("-fx-background-color: #58FF36");
                    break;
                case 25:
                    bit25.setStyle("-fx-background-color: #58FF36");
                    break;
                case 26:
                    bit26.setStyle("-fx-background-color: #58FF36");
                    break;
                case 27:
                    bit27.setStyle("-fx-background-color: #58FF36");
                    break;
                case 28:
                    bit28.setStyle("-fx-background-color: #58FF36");
                    break;
                case 29:
                    bit29.setStyle("-fx-background-color: #58FF36");
                    break;
                case 30:
                    bit30.setStyle("-fx-background-color: #58FF36");
                    break;
                case 31:
                    bit31.setStyle("-fx-background-color: #58FF36");
                    break;
                case 32:
                    bit32.setStyle("-fx-background-color: #58FF36");
                    break;
                case 33:
                    bit33.setStyle("-fx-background-color: #58FF36");
                    break;
                case 34:
                    bit34.setStyle("-fx-background-color: #58FF36");
                    break;
                case 35:
                    bit35.setStyle("-fx-background-color: #58FF36");
                    break;
                case 36:
                    bit36.setStyle("-fx-background-color: #58FF36");
                    break;
                case 37:
                    bit37.setStyle("-fx-background-color: #58FF36");
                    break;
                case 38:
                    bit38.setStyle("-fx-background-color: #58FF36");
                    break;
                case 39:
                    bit39.setStyle("-fx-background-color: #58FF36");
                    break;
                case 40:
                    bit40.setStyle("-fx-background-color: #58FF36");
                    break;
                case 41:
                    bit41.setStyle("-fx-background-color: #58FF36");
                    break;
                case 42:
                    bit42.setStyle("-fx-background-color: #58FF36");
                    break;
                case 43:
                    bit43.setStyle("-fx-background-color: #58FF36");
                    break;
                case 44:
                    bit44.setStyle("-fx-background-color: #58FF36");
                    break;
                case 45:
                    bit45.setStyle("-fx-background-color: #58FF36");
                    break;
                case 46:
                    bit46.setStyle("-fx-background-color: #58FF36");
                    break;
                case 47:
                    bit47.setStyle("-fx-background-color: #58FF36");
                    break;
                case 48:
                    bit48.setStyle("-fx-background-color: #58FF36");
                    break;
                case 49:
                    bit49.setStyle("-fx-background-color: #58FF36");
                    break;
                case 50:
                    bit50.setStyle("-fx-background-color: #58FF36");
                    break;
                case 51:
                    bit51.setStyle("-fx-background-color: #58FF36");
                    break;
                case 52:
                    bit52.setStyle("-fx-background-color: #58FF36");
                    break;
                case 53:
                    bit53.setStyle("-fx-background-color: #58FF36");
                    break;
                case 54:
                    bit54.setStyle("-fx-background-color: #58FF36");
                    break;
                case 55:
                    bit55.setStyle("-fx-background-color: #58FF36");
                    break;
                case 56:
                    bit56.setStyle("-fx-background-color: #58FF36");
                    break;
                case 57:
                    bit57.setStyle("-fx-background-color: #58FF36");
                    break;
                case 58:
                    bit58.setStyle("-fx-background-color: #58FF36");
                    break;
                case 59:
                    bit59.setStyle("-fx-background-color: #58FF36");
                    break;
                case 60:
                    bit60.setStyle("-fx-background-color: #58FF36");
                    break;
                case 61:
                    bit61.setStyle("-fx-background-color: #58FF36");
                    break;
                case 62:
                    bit62.setStyle("-fx-background-color: #58FF36");
                    break;
                case 63:
                    bit63.setStyle("-fx-background-color: #58FF36");
                    break;
                case 64:
                    bit64.setStyle("-fx-background-color: #58FF36");
                    break;
                case 65:
                    bit65.setStyle("-fx-background-color: #58FF36");
                    break;
                case 66:
                    bit66.setStyle("-fx-background-color: #58FF36");
                    break;
                case 67:
                    bit67.setStyle("-fx-background-color: #58FF36");
                    break;
                case 68:
                    bit68.setStyle("-fx-background-color: #58FF36");
                    break;
                case 69:
                    bit69.setStyle("-fx-background-color: #58FF36");
                    break;
                case 70:
                    bit70.setStyle("-fx-background-color: #58FF36");
                    break;
                case 71:
                    bit71.setStyle("-fx-background-color: #58FF36");
                    break;
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set alloc spinner options
        allocSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10));

        // Set alloc combo box options
        ObservableList<String> options = FXCollections.observableArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
        comboBoxAlloc.setItems(options);

        // Initialize heapStart
        Heap.bytes[4] = new Header();
        Heap.heapStart = Heap.bytes[4];
        Heap.heapStart.idx = 4;
        Heap.heapStart.aBit = false;
        Heap.heapStart.pBit = true;
        Heap.heapStart.size = 64;

        // Initialize first reserved block
        Heap.bytes[0] = new Header();
        Heap.bytes[0].idx = 0;
        Heap.bytes[0].aBit = true;
        Heap.bytes[0].pBit = true;
        Heap.bytes[0].size = 4;

        // Initialize end of heap area
        Heap.bytes[68] = new Header();
        Heap.bytes[68].size = 1;
        Heap.bytes[68].idx = 68;
    }
}
