# Heap Allocation Visualizer

## Description

Heap Allocator is an application to visualize dynamic memory allocation in C using the first-fit placement policy and immediate coalescing. This allocator supports arbitrary allocation and free requests. 

- This allocator implements 8-byte alignment, so the first and last four bytes are reserved and cannot be allocated or freed.
- The alignment requirement also means that requested sizes that do not meet alignment requirements will be automatically padded.
- The first-fit placement policy is used because it is best for visualization of traversal given this heap size.
- Free operations make use of immediate coalescing when appropriate.
- The traverser nodes are aligned with the headers to reflect jumping to the next header when searching for a free block. This simulates traversal of the heap using an implicit free list.
- A footer is mostly useless for this project and it introduces clutter in the visualization, so previous block sizes are stored in the header and are not represented in the visualization. 
- The header format is as follows: Allocated Size / Previous Bit / Allocated Bit.


# Preview
![alt text](https://i.gyazo.com/91bf26a535459b7e022be466b16b7840.png)

## Features

- Allocate: Select a size in bytes to send an allocation request to the allocator in the drop-down list to the left of the 'Allocate' button and click 'Allocate'.
- Free: Select a pointer address to send a free request to the allocator in the drop-down list to the left of the 'Free' button and click 'Free'.
- Get Size: Click one of the 'Get Size' buttons to get either the total allocated size or the total free size of the heap.
- Clear: Click the 'Clear' button to clear the heap (free all allocated blocks of memory).
- Traversal Speed: Move the slider left or right to either slow down or speed up the traversal when finding a free block during allocation.


## 1-Minute Demo 

[Demo](https://www.youtube.com/watch?v=i0wBru4_v_A)

## Contributing

Guidelines on how others can contribute.

## License

This project is licensed under the [MIT License](LICENSE).

# Getting Started
Executable not available.
