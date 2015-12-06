
class Superblock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head

    public Superblock( int diskSize ) {
       // ...
        byte[] blockSize = new byte[512];
        SysLib.rawread(0, blockSize);
        this.totalBlocks = SysLib.bytes2int(blockSize, 0);
        this.totalInodes = SysLib.bytes2int(blockSize, 4);
        this.freeList = SysLib.bytes2int(blockSize, 8);

        if (this.totalBlocks != diskSize || this.totalInodes <= 0 || this.freeList < 2)
        {
            this.totalBlocks = diskSize;
            SysLib.cerr("default format( 64 )\n");
            this.format();
        }
    } // end SuperBlock (int diskSize)

    void sync()
    {
        byte[] blockSize = new byte[512];
        SysLib.int2bytes(this.totalBlocks, blockSize, 0);
        SysLib.int2bytes(this.totalInodes, blockSize, 4);
        SysLib.int2bytes(this.freeList, blockSize, 8);
        SysLib.rawwrite(0, blockSize);
        SysLib.cerr("Superblock synchronized\n");
    } // end sync()

    // format iNodes and set all flags == 0 and write to disk
    void format(int num)
    {
        this.totalInodes = num;

        // Instantiate iNodes and write to disk
        for(short i = 0; i < this.totalInodes; i++)
        {
            Inode node = new Inode();
            node.flag = 0;
            node.toDisk(i);
        }

        this.freeList = 2 + this.totalInodes * 32 / 512;

        // Instantiate freeList and set all bytes to 0
        for (int freeBlock = this.freeList; freeBlock < this.totalBlocks; ++freeBlock)
        {
            byte[] block = new byte[512];

            for (int blockByte = 0; blockByte < 512; ++blockByte)
            {
                block[blockByte] = 0;
            }

            SysLib.int2bytes(freeBlock + 1, block, 0);
            SysLib.rawwrite(freeBlock, block);
        }
        this.sync();
    } // end format(int num)

    void format()
    {
        this.format(64);
    }

    public int getFreeBlock()
    {
        int freeBlock = this.freeList;
        if (freeBlock != -1)
        {
            byte[] blockSize = new byte[512];
            SysLib.rawread(freeBlock, blockSize)
            this.freeList = SysLib.bytes2int(blockSize, 0);
            SysLib.int2bytes(0, blockSize, 0);
            SysLib.rawwrite(freeBlock, blockSize);
        }
        return freeBlock;
    } // end getFreeBlock

    // 
    public boolean returnBlock(int block)
    {
        if (block < 0)
        {
            return false;
        }
        else
        {
            byte[] blockSize = new byte[512];

            for(int blockByte = 0; blockByte < 512; ++blockByte)
            {
                blockSize[blockByte] = 0;
            }
            SysLib.int2bytes(this.freeList, blockSize, 0);
            SysLib.rawwrite(block, blockSize);
            this.freeList = block;
            return true;
        }
    } // end returnBlock(int block)
    //...
} // end class SuperBlock