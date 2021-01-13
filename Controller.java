package allocator;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller is the main class that controls the functionality of the JavaFX application. This class handles interaction
 * with the buttons/slider in the application and holds the implementation of the heap.
 *
 * @author Brandon Kenter
 */
public class Controller extends ButtonsAndLabels implements Initializable {
    private static ObservableList<String> allocOptions = FXCollections.observableArrayList( // Alloc size combo box options
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
    private static ObservableList<String> freeOptions = FXCollections.observableArrayList(); // Free address combo box options
    private static Header[] bytes = new Header[72]; // Heap represented as an array of headers
    private static final int ALLOC_SIZE = 64; // Size of the allocatable space in the heap
    private static Header heapStart = null; // Start of the heap
    private static Header current = null; // Current position of the heap
    private static int headerPayloadSize; // Total payload size of a heap allocation
    private List<Integer> indexes = new ArrayList<>(); // List to hold indexes to help visualize the traversal

    private Thread circleThread; // Thread to delay update of the traversing status circles
    private Thread cellsThread; // Thread to delay update of the allocated cells
    private Thread freeThread; // Thread to delay update of the free portion of a block that was split upon allocation

    /**
     * Allocates a specified size of memory in the heap and updates the appropriate header, memory and pointer address
     * cells upon a successful allocation. A successful allocation requires enough space for not only the initially
     * requested size, but also space for the header and any additional padding to keep allocations 8-byte aligned.
     * The allocation also employs the next-fit placement policy to choose a potential available free block.
     */
    public void allocBtnClicked() {
        // Clear free/allocated size labels
        totalFreeSize.setText("");
        totalAllocatedSize.setText("");

        // Disable buttons and preserve style
        allocateBtn.setDisable(true);
        allocateBtn.setStyle("-fx-opacity: 1.0; -fx-background-radius: 5; -fx-border-radius: 5");
        freeBtn.setDisable(true);
        freeBtn.setStyle("-fx-opacity: 1.0; -fx-background-radius: 5; -fx-border-radius: 5");
        clearBtn.setDisable(true);
        clearBtn.setStyle("-fx-opacity: 1.0; -fx-background-radius: 5; -fx-border-radius: 5");
        totalAllocSizeBtn.setDisable(true);
        totalAllocSizeBtn.setStyle("-fx-opacity: 1.0; -fx-background-radius: 5; -fx-border-radius: 5");
        totalFreeSizeBtn.setDisable(true);
        totalFreeSizeBtn.setStyle("-fx-opacity: 1.0; -fx-background-radius: 5; -fx-border-radius: 5");

        // Make sure a size is selected
        if (comboBoxAlloc.getValue() == null) {
            return;
        }

        // Get size wanted to be allocated
        int size = Integer.parseInt((String) comboBoxAlloc.getValue());

        // Make sure requested space < available space
        if (size > ALLOC_SIZE - 4) {
            return;
        }

        // Make sure requested space > 0
        if (size <= 0) {
            return;
        }

        // If requested size is not a multiple of 8, add padding
        headerPayloadSize = 4 + size;
        if (headerPayloadSize % 8 != 0) {
            int offset = (8 - (headerPayloadSize % 8));
            headerPayloadSize = headerPayloadSize + offset;
        }

        // ---- Check if requested size fits in heap space by iterating through heap using first-fit placement policy ----

        // Iterate through blocks
        current = heapStart;
        while (current.size != 1) {
            indexes.add(current.idx);
            // If free block with enough size is found
            if ((current.aBit.equals("0")) && (current.size >= headerPayloadSize)) {
                // Split if possible (available size - alloc size >= alloc size + 8)
                if (current.size - headerPayloadSize >= 8) {
                    // Get size of free block
                    int freeSize = current.size - headerPayloadSize;

                    // Instantiate newly split free block's header and set pBit
                    bytes[current.idx + headerPayloadSize] = new Header();
                    bytes[current.idx + headerPayloadSize].idx = current.idx + headerPayloadSize;
                    bytes[current.idx + headerPayloadSize].size = freeSize;
                    bytes[current.idx + headerPayloadSize].pBit = "1";
                    bytes[current.idx + headerPayloadSize].aBit = "0";
                    bytes[current.idx + headerPayloadSize].prevSize = current.size - freeSize;

                    // Create circles thread
                    circleThread = new Thread(this::circleThread);
                    circleThread.start();
                    clearStatusCircles();

                    // Create free thread
                    freeThread = new Thread(this::freeThread);
                    freeThread.start();

                    // Set size of current
                    current.size = headerPayloadSize;
                    current.aBit = "1";

                    // Create cells thread and return ptr of allocated block's payload (in this case set ptr in address row)
                    cellsThread = new Thread(this::cellsThread);
                    cellsThread.start();
                    clearStatusCircles();
                    return;
                }
                // Otherwise allocate single block
                else {
                    // Create circles thread
                    circleThread = new Thread(this::circleThread);
                    circleThread.start();

                    // Set aBit of current block
                    current.aBit = "1";
                    current.size = headerPayloadSize;

                    // Create cells thread and return ptr of allocated block's payload (in this case set ptr in address row)
                    cellsThread = new Thread(this::cellsThread);
                    cellsThread.start();
                    clearStatusCircles();
                    return;
                }
            }
            // Current block is allocated or not big enough, iterate to next block header.
            current = bytes[current.idx + current.size];
        }
        // Enable buttons if allocation fails
        allocateBtn.setDisable(false);
        freeBtn.setDisable(false);
        clearBtn.setDisable(false);
        totalAllocSizeBtn.setDisable(false);
        totalFreeSizeBtn.setDisable(false);
    }

    /**
     * Frees an allocated block in the heap. This free operation is accompanied by coalescing of immediate adjacent
     * free blocks. Once freed, the appropriate header, heap memory and pointer cells are updated.
     */
    public void freeButtonClicked() {
        totalFreeSize.setText("");
        totalAllocatedSize.setText("");

        if (comboBoxFree.getValue() == null) {
            return;
        }

        // Get pointer index and header index
        String address = (String) comboBoxFree.getValue();
        comboBoxFree.getItems().remove(address);
        address = address.substring(3);
        int ptrIdx = Integer.parseInt(address, 16);
        int headerIdx = ptrIdx - 4;

        // ---------- Free original block by setting aBit and next's pBit ---------- //
        bytes[headerIdx].aBit = "0";
        bytes[headerIdx + bytes[headerIdx].size].pBit = "0";

        // Update header, pointer address cells and block colors
        updateHeaderCell(headerIdx, 0);
        clearPointerAddressCell(ptrIdx);
        clearAllocColor(bytes[headerIdx].idx, bytes[headerIdx].size);

        // ---------- If next is not the end of the heap and is free, coalesce original and next ---------- //
        if (bytes[headerIdx + bytes[headerIdx].size].size != 1 && bytes[headerIdx + bytes[headerIdx].size].aBit.equals("0")) {
            clearHeaderCell(bytes[headerIdx + bytes[headerIdx].size].idx);

            // Free next
            int nextSize = bytes[bytes[headerIdx].idx + bytes[headerIdx].size].size;
            bytes[headerIdx + bytes[headerIdx].size].aBit = "0";

            // Coalesce with original
            bytes[headerIdx].size += nextSize;

            // Update prevSize
            bytes[headerIdx + bytes[headerIdx].size].prevSize = bytes[headerIdx].size;

            // Update header, pointer address cells and block colors
            updateHeaderCell(headerIdx, 1);
            clearPointerAddressCell(ptrIdx);
            clearAllocColor(headerIdx, bytes[headerIdx].size);
        }
        // Else if still not end of heap, but next block is not free, just update pBit of next block
        if (bytes[headerIdx + bytes[headerIdx].size].size != 1) {
            bytes[headerIdx + bytes[headerIdx].size].pBit = "0";
        }

        // ---------- If previous is free, coalesce original and previous ---------- //
        if (bytes[headerIdx].pBit.equals("0")) {
            // Get size of previous block
            int prevBlockSize = bytes[headerIdx].prevSize;

            // Update prevSize
            bytes[bytes[bytes[headerIdx].idx + bytes[headerIdx].size].idx].prevSize = prevBlockSize + bytes[headerIdx].size;

            // Update size of previous block
            bytes[headerIdx - bytes[headerIdx].prevSize].size += bytes[headerIdx].size;

            // Update header cells, pointer address cell and block colors
            updateHeaderCell(headerIdx - bytes[headerIdx].prevSize, 2);
            clearPointerAddressCell(ptrIdx);
            clearHeaderCell(headerIdx);
            clearAllocColor(headerIdx, bytes[headerIdx].size);

            // Free original header
            bytes[bytes[headerIdx].idx].aBit = "0";
        }
    }

    /**
     * Displays the total size of the allocated blocks in the heap.
     */
    public void totalAllocSizeBtnClicked() {
        int size = 0;
        for(Header header : bytes) {
            if (header != null && header.idx == 0) { continue; }
            if (header != null && header.aBit != null && header.aBit.equals("1") && header.size != 1) {
                size += header.size;
            }
        }
        totalAllocatedSize.setText(String.valueOf(size));
    }

    /**
     * Displays the total size of the free blocks in the heap.
     */
    public void totalFreeSizeBtnClicked() {
        int size = 0;
        for(Header header : bytes) {
            if (header != null && header.idx == 0) { continue; }
            if (header != null && header.aBit != null && header.aBit.equals("1") && header.size != 1) {
                size += header.size;
            }
        }
        totalFreeSize.setText(String.valueOf(64 - size));
    }

    /**
     * Clears the entire heap by resetting the color of allocated blocks, header cells, pointer cells and
     * re-initializes the start and end of the heap.
     */
    public void clearBtnClicked() {
        indexes.clear();
        totalFreeSize.setText("");
        totalAllocatedSize.setText("");
        comboBoxFree.getItems().clear();

        // Reset header sizes
        for(Header header : bytes) {
            if (header != null && header.idx == 0) { continue; }
            if (header != null  && header.size != 1) {
                header.size = 0;
            }
        }

        // Clear cells
        for (int i = 4; i < 68; i += 4) {
            clearPointerAddressCell(i);
            clearHeaderCell(i);
        }
        clearAllocColor(4, 64);

        // Initialize heapStart
        bytes[4] = new Header();
        heapStart = bytes[4];
        heapStart.idx = 4;
        heapStart.aBit = "0";
        heapStart.pBit = "1";
        heapStart.size = 64;

        // Set heapStart's header cell
        bits1.setText("64/1/0");

        // Initialize first reserved block (start of heap)
        bytes[0] = new Header();
        bytes[0].idx = 0;
        bytes[0].aBit = "1";
        bytes[0].pBit = "1";
        bytes[0].size = 4;

        // Initialize last reserved block (end of heap)
        bytes[68] = new Header();
        bytes[68].size = 1;
        bytes[68].idx = 68;
    }

    /**
     * Clears all of the header's corresponding status circles.
     */
    private void clearStatusCircles() {
        status1.opacityProperty().set(0.44);
        status3.opacityProperty().set(0.44);
        status5.opacityProperty().set(0.44);
        status7.opacityProperty().set(0.44);
        status9.opacityProperty().set(0.44);
        status11.opacityProperty().set(0.44);
        status13.opacityProperty().set(0.44);
        status15.opacityProperty().set(0.44);
    }

    /**
     * Sets the header's corresponding status circle based on the traversal's location
     * during heap allocation by updating the opacity of the circle.
     *
     * @param  headerIdx The index of the header that corresponds to the desired status circle.
     */
    private void setStatusCircle(int headerIdx) {
        switch (headerIdx) {
            case 4 -> status1.opacityProperty().set(1);
            case 12 -> status3.opacityProperty().set(1);
            case 20 -> status5.opacityProperty().set(1);
            case 28 -> status7.opacityProperty().set(1);
            case 36 -> status9.opacityProperty().set(1);
            case 44 -> status11.opacityProperty().set(1);
            case 52 -> status13.opacityProperty().set(1);
            case 60 -> status15.opacityProperty().set(1);
        }
    }

    /**
     * Sets the allocated block's pointer address by updating the text in the pointer address cell.
     *
     * @param ptrIdx The index of the pointer address that is to be set.
     */
    private void setPointerAddressCell(int ptrIdx) {
        String hexAddress = Integer.toHexString(ptrIdx);

        // Add 0x_08 manually to help with sorting
        if (ptrIdx == 8) {
            comboBoxFree.getItems().add("0x_08");
        } else {
            comboBoxFree.getItems().add("0x_" + hexAddress);
        }
        Collections.sort(freeOptions);

        switch (ptrIdx) {
            case 8 -> ptr2.setText("0x_08");
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

    /**
     * Updates the allocated block's pointer address by updating the text in the pointer address cell.
     *
     * @param  ptrIdx The index of the pointer address that is to be updated.
     */
    private void clearPointerAddressCell(int ptrIdx) {
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

    /**
     * Sets the allocated block's header by initializing the text in the header cell.
     *
     * @param  headerIdx The index of the header that is to be initialized.
     */
    private void setHeaderCell(int headerIdx) {
        String size = String.valueOf(bytes[headerIdx].size);
        headerCellHelper(headerIdx, size);
    }

    /**
     * Updates the allocated block's header by updating the text in the header cell. Region 0
     * is when the block being updated is the original block. Region 1 is when the original block is
     * being coalesced with the next block. Region 2 is when the original block is being coalesced
     * with the previous block.
     *
     * @param  headerIdx The index of the header that is to be updated.
     * @param  region Type of region that is being updated
     */
    private void updateHeaderCell(int headerIdx, int region) {
        //------------------ MIDDLE FREE ------------------
        if (region == 0) {
            // Update original header
            String size = String.valueOf(bytes[headerIdx].size);
            headerCellHelper(headerIdx, size);

            // Update next's pBit to reflect freeing block
            int nextHeaderIdx = bytes[headerIdx + bytes[headerIdx].size].idx;
            size = String.valueOf(bytes[nextHeaderIdx].size);
            headerCellHelper(nextHeaderIdx, size);
        }

        //------------------ MIDDLE/RIGHT COALESCE ------------------
        else if (region == 1) {
            // Update original header
            String size = String.valueOf(bytes[headerIdx].size);
            headerCellHelper(headerIdx, size);
        }

        //------------------ LEFT/MIDDLE COALESCE ------------------
        else {
            // Update left header
            String size = String.valueOf(bytes[headerIdx].size);
            headerCellHelper(headerIdx, size);

            // Update next's pBit
            int nextHeaderIdx = bytes[headerIdx + bytes[headerIdx].size].idx;
            size = String.valueOf(bytes[nextHeaderIdx].size);
            headerCellHelper(nextHeaderIdx, size);
        }
    }

    /**
     * Clears the allocated block's header by removing the text from the header cell.
     *
     * @param  headerIdx The index of the header that is to be cleared.
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
     * Helper for updateHeaderCell() to choose which cell to update.
     *
     * @param  headerIdx Starting index of the region to be allocated.
     * @param  size Size of the allocated block.
     */
    private void headerCellHelper(int headerIdx, String size) {
        switch (headerIdx) {
            case 4 -> bits1.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 8 -> bits2.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 12 -> bits3.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 16 -> bits4.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 20 -> bits5.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 24 -> bits6.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 28 -> bits7.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 32 -> bits8.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 36 -> bits9.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 40 -> bits10.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 44 -> bits11.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 48 -> bits12.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 52 -> bits13.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 56 -> bits14.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 60 -> bits15.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
            case 64 -> bits16.setText(size + "/" + bytes[headerIdx].pBit + "/" + bytes[headerIdx].aBit);
        }
    }

    /**
     * Sets the allocated blocks by setting the background color.
     *
     * @param  startIdx Starting index of the region to be allocated.
     * @param  size Size of the region to be allocated.
     */
    private void setAllocColor(int startIdx, int size) {
        for (int i = startIdx; i < startIdx + size; i++) {
            switch (i) {
                case 0 -> bit0.setStyle("-fx-background-color: #007521");
                case 1 -> bit1.setStyle("-fx-background-color: #007521");
                case 2 -> bit2.setStyle("-fx-background-color: #007521");
                case 3 -> bit3.setStyle("-fx-background-color: #007521");
                case 4 -> bit4.setStyle("-fx-background-color: #007521");
                case 5 -> bit5.setStyle("-fx-background-color: #007521");
                case 6 -> bit6.setStyle("-fx-background-color: #007521");
                case 7 -> bit7.setStyle("-fx-background-color: #007521");
                case 8 -> bit8.setStyle("-fx-background-color: #007521");
                case 9 -> bit9.setStyle("-fx-background-color: #007521");
                case 10 -> bit10.setStyle("-fx-background-color: #007521");
                case 11 -> bit11.setStyle("-fx-background-color: #007521");
                case 12 -> bit12.setStyle("-fx-background-color: #007521");
                case 13 -> bit13.setStyle("-fx-background-color: #007521");
                case 14 -> bit14.setStyle("-fx-background-color: #007521");
                case 15 -> bit15.setStyle("-fx-background-color: #007521");
                case 16 -> bit16.setStyle("-fx-background-color: #007521");
                case 17 -> bit17.setStyle("-fx-background-color: #007521");
                case 18 -> bit18.setStyle("-fx-background-color: #007521");
                case 19 -> bit19.setStyle("-fx-background-color: #007521");
                case 20 -> bit20.setStyle("-fx-background-color: #007521");
                case 21 -> bit21.setStyle("-fx-background-color: #007521");
                case 22 -> bit22.setStyle("-fx-background-color: #007521");
                case 23 -> bit23.setStyle("-fx-background-color: #007521");
                case 24 -> bit24.setStyle("-fx-background-color: #007521");
                case 25 -> bit25.setStyle("-fx-background-color: #007521");
                case 26 -> bit26.setStyle("-fx-background-color: #007521");
                case 27 -> bit27.setStyle("-fx-background-color: #007521");
                case 28 -> bit28.setStyle("-fx-background-color: #007521");
                case 29 -> bit29.setStyle("-fx-background-color: #007521");
                case 30 -> bit30.setStyle("-fx-background-color: #007521");
                case 31 -> bit31.setStyle("-fx-background-color: #007521");
                case 32 -> bit32.setStyle("-fx-background-color: #007521");
                case 33 -> bit33.setStyle("-fx-background-color: #007521");
                case 34 -> bit34.setStyle("-fx-background-color: #007521");
                case 35 -> bit35.setStyle("-fx-background-color: #007521");
                case 36 -> bit36.setStyle("-fx-background-color: #007521");
                case 37 -> bit37.setStyle("-fx-background-color: #007521");
                case 38 -> bit38.setStyle("-fx-background-color: #007521");
                case 39 -> bit39.setStyle("-fx-background-color: #007521");
                case 40 -> bit40.setStyle("-fx-background-color: #007521");
                case 41 -> bit41.setStyle("-fx-background-color: #007521");
                case 42 -> bit42.setStyle("-fx-background-color: #007521");
                case 43 -> bit43.setStyle("-fx-background-color: #007521");
                case 44 -> bit44.setStyle("-fx-background-color: #007521");
                case 45 -> bit45.setStyle("-fx-background-color: #007521");
                case 46 -> bit46.setStyle("-fx-background-color: #007521");
                case 47 -> bit47.setStyle("-fx-background-color: #007521");
                case 48 -> bit48.setStyle("-fx-background-color: #007521");
                case 49 -> bit49.setStyle("-fx-background-color: #007521");
                case 50 -> bit50.setStyle("-fx-background-color: #007521");
                case 51 -> bit51.setStyle("-fx-background-color: #007521");
                case 52 -> bit52.setStyle("-fx-background-color: #007521");
                case 53 -> bit53.setStyle("-fx-background-color: #007521");
                case 54 -> bit54.setStyle("-fx-background-color: #007521");
                case 55 -> bit55.setStyle("-fx-background-color: #007521");
                case 56 -> bit56.setStyle("-fx-background-color: #007521");
                case 57 -> bit57.setStyle("-fx-background-color: #007521");
                case 58 -> bit58.setStyle("-fx-background-color: #007521");
                case 59 -> bit59.setStyle("-fx-background-color: #007521");
                case 60 -> bit60.setStyle("-fx-background-color: #007521");
                case 61 -> bit61.setStyle("-fx-background-color: #007521");
                case 62 -> bit62.setStyle("-fx-background-color: #007521");
                case 63 -> bit63.setStyle("-fx-background-color: #007521");
                case 64 -> bit64.setStyle("-fx-background-color: #007521");
                case 65 -> bit65.setStyle("-fx-background-color: #007521");
                case 66 -> bit66.setStyle("-fx-background-color: #007521");
                case 67 -> bit67.setStyle("-fx-background-color: #007521");
                case 68 -> bit68.setStyle("-fx-background-color: #007521");
                case 69 -> bit69.setStyle("-fx-background-color: #007521");
                case 70 -> bit70.setStyle("-fx-background-color: #007521");
                case 71 -> bit71.setStyle("-fx-background-color: #007521");
            }
        }
    }

    /**
     * Clears the allocated blocks by resetting the background color.
     *
     * @param  startIdx Starting index of the region to be allocated.
     * @param  size Size of the region to be allocated.
     */
    private void clearAllocColor(int startIdx, int size) {
        indexes.clear();

        for (int i = startIdx; i < startIdx + size; i++) {
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
     * Sets up the required items for the free combo box, alloc combo box and
     * initializes the heap including the start and end reserved blocks.
     *
     * @param  url The location used to resolve relative paths for the root object, or null if the location is not known.
     * @param  resourceBundle The resources used to localize the root object, or null if the root object was not localized.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set alloc combo box options
        comboBoxAlloc.setItems(allocOptions);
        comboBoxFree.setItems(freeOptions);

        // Initialize heapStart
        bytes[4] = new Header();
        heapStart = bytes[4];
        heapStart.idx = 4;
        heapStart.aBit = "0";
        heapStart.pBit = "1";
        heapStart.size = 64;

        // Set heapStart's header cell
        bits1.setText("64/1/0");

        // Initialize first reserved block (start of heap)
        bytes[0] = new Header();
        bytes[0].idx = 0;
        bytes[0].aBit = "1";
        bytes[0].pBit = "1";
        bytes[0].size = 4;

        // Initialize last reserved block (end of heap)
        bytes[68] = new Header();
        bytes[68].size = 1;
        bytes[68].idx = 68;
    }

    /**
     * Thread for stalling CSS updates of the status circles in the traversal row.
     */
    private void circleThread() {
        for (Integer idx : indexes) {
            int n = (int) (150 * traversalSpeedSlider.getValue());
            //Switches to the GUI thread
            Platform.runLater(() -> {
                setStatusCircle(idx);
            });
            try { Thread.sleep(n); }
            catch (InterruptedException iex) { }
        }
        clearStatusCircles();
        indexes.clear();
    }

    /**
     * Thread for stalling CSS updates of the allocated portion of an allocated block.
     */
    private void cellsThread() {
        int n = (int) (150 * traversalSpeedSlider.getValue());
        try { Thread.sleep(indexes.size() * n); }
        catch (InterruptedException iex) { }
        //Switches to the GUI thread
        Platform.runLater(() -> {
            // Make sure next header is not the end, then update
            if (bytes[current.idx + current.size].size != 1) {
                bytes[current.idx + current.size].pBit = "1";
                updateHeaderCell(0, current.idx);
            }
            setAllocColor(current.idx, current.size);
            setPointerAddressCell(current.idx + 4);
            setHeaderCell(current.idx);
        });

        allocateBtn.setDisable(false);
        freeBtn.setDisable(false);
        clearBtn.setDisable(false);
        totalAllocSizeBtn.setDisable(false);
        totalFreeSizeBtn.setDisable(false);
    }

    /**
     * Thread for stalling CSS updates of the free portion of an allocated block.
     */
    private void freeThread() {
        int n = (int) (150 * traversalSpeedSlider.getValue());
        try { Thread.sleep(indexes.size() * n); }
        catch (InterruptedException iex) { }

        //Switches to the GUI thread
        Platform.runLater(() -> {
            setHeaderCell(bytes[current.idx + headerPayloadSize].idx);
        });
    }
}
