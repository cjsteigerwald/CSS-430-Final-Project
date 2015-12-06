
class Superblock {
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public Superblock( int diskSize ) {
        // read data from disk
        byte[] sbData = new byte[Disk.blockSize];
        SysLib.rawread(0, sbData);

        totalBlocks = SysLib.bytes2int(sbData, 0);
        totalInodes = SysLib.bytes2int(sbData, 4);
        freeList = SysLib.bytes2int(sbData, 8);

        // check disk contents for problems. If problem, format disk
        if(this.totalBlocks != diskSize || this.totalInodes <= 0 || this.freeList < 2) {
            SysLib.cerr("Error: Initializing Superblock, formatting disk!");
            this.totalBlocks = diskSize;
            format();
        }
    }

    // format disk with number of Inodes
    public synchronized void format() {
        this.format(64);
    }

    public synchronized void format(int iNodes) {
        this.totalInodes = iNodes;

        // set freeList pointer to first free block
        this.freeList = ((this.totalInodes * 32) / Disk.blockSize) + 2;

        // write Superblock contents to disk
        for( int i = this.freeList; i < this.totalBlocks; i++ ) {
            byte[] data = new byte[Disk.blockSize];
            // remove data
            for( int j = 0; j < Disk.blockSize; j++ ) {
                data[j] = 0;
            }

            SysLib.int2bytes(i + 1, data, 0);
            SysLib.rawwrite(i, data); // save to disk
        }

        this.sync();
    }

    // grab free block off the free list
    public int getFreeBlock() {
        int returnVal = this.freeList;

        // check if block is valid
        if(this.freeList != -1) {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(this.freeList, data);

            // find new block to use
            this.freeList = SysLib.bytes2int(data, 0);
            SysLib.int2bytes(0, data, 0);
            SysLib.rawwrite(this.freeList, data);
        }

        return returnVal;
    }

    // put a free block onto the freeList
    public boolean returnBlock(short blockNum) {
        if(blockNum < 0) {
            return false;
        } else {
            byte[] data = new byte[Disk.blockSize];
            for (int i = 0; i < Disk.blockSize; i++) {
                data[i] = 0;
            }
            SysLib.int2bytes(this.freeList, data, 0);
            SysLib.rawwrite(blockNum, data);

            this.freeList = blockNum;
        }

        return true;
    }

    void sync() {
        // create buffer
        byte rawData[] = new byte[Disk.blockSize];

        // save Superblock data
        SysLib.int2bytes(this.totalBlocks, rawData, 0);
        SysLib.int2bytes(this.totalInodes, rawData, 4);
        SysLib.int2bytes(this.freeList, rawData, 8);

        // write Superblock data to disk
        SysLib.rawwrite(0, rawData);
    }
}