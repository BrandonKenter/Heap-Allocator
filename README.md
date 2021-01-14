# Heap-Allocator
Visualization of general heap allocation processes performed by a heap allocator. This allocator supports arbitrary allocation and free requests. 

# To Do List
- Test suite
- Remove code duplication

# Getting Started
Executable not available yet

# Using the App
General Details:
- This allocator implements 8-byte alignment, so the first and last four bytes are reserved and cannot be altered.
- The alignment requirement also means that requested sizes that do not meet alignment requirements will be automatically padded.
- The first-fit plaecement policy is used because it is best for visualization of traversal given this heap size.
- Free operations make use of immediate coalescing if appropriate.
- The traverser nodes are aligned with the headers to reflect jumping to the next header when searching for a free block.

# Preview
![alt text](https://i.gyazo.com/91bf26a535459b7e022be466b16b7840.png)
