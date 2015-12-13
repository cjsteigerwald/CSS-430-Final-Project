/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Hunter Grayson, Chris Steigerwald, Michael Voight
 * @last edit: 12/6/2015
 *
 * The first block, block 0, is called the superblock.  It is used to describe
 *      1. The number of disk blocks.
 *      2. The number of inodes
 *      3. The block number of the head block of the free list.
 *
 * It is the OS-managed block. No other information must be recorded in and no user threads must be able to
 * get access to the superblock
 */

class SuperBlock
{
    public int totalBlocks;                     // the number of disk blocks
    public int totalInodes;                     // the number of inodes
    public int freeList;                        // the block number of the free list's head

    /**
     * SuperBlock(int blockAmount)
     * Overloaded SuperBlock() passes in the number of blocks to be instantiated and sets variables to their values.
     * @param blockAmount
     */
    public SuperBlock(int blockAmount)
    {
        byte[] theSuperBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, theSuperBlock);
        totalBlocks = SysLib.bytes2int(theSuperBlock, 0);
        totalInodes = SysLib.bytes2int(theSuperBlock, 4);
        freeList = SysLib.bytes2int(theSuperBlock, 8);
        totalInodes = totalBlocks;
        totalBlocks = blockAmount;
        format(64);
    }

    /**
     * format(int nodeAmount)
     * This method takes in the number of nodes in the system and formats the blocks, cannot be undone.  Sets all
     * blocks to free.
     * @param nodeAmount
     */
    void format(int nodeAmount)
    {
        this.totalInodes = nodeAmount;
        for(short i = 0; i < this.totalInodes; i++)
        {
            Inode newInode = new Inode();
            newInode.usedFlag = 0;
            newInode.toDisk(i);
        }
        this.freeList = 2 + this.totalInodes * 32 / 512;
        for(int i = this.freeList; i < this.totalBlocks; i++)
        {
            byte[] theBlock = new byte[512];
            for(int j = 0; j < 512; j++)
            {
                theBlock[j] = 0;
            }
            SysLib.int2bytes(i + 1, theBlock, 0);
            SysLib.rawwrite(i, theBlock);
        }
        this.sync();
    }

    /**
     * sync()
     * Syncs the cache back to the physical disk.
     */
    void sync()
    {
        byte data[] = new byte[Disk.blockSize];
        SysLib.int2bytes(this.totalBlocks, data, 0);
        SysLib.int2bytes(this.totalInodes, data, 4);
        SysLib.int2bytes(this.freeList, data, 8);
        SysLib.rawwrite(0, data);
    }
}