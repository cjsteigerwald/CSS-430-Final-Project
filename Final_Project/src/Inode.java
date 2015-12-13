/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Chris Steigerwald, Hunter Grayson, Michael Voight
 * @last edit: 12/6/2015
 *
 * Starting from the blocks after the superblock will be the inode blocks. Each inode describes one file. This inode
 * is a simplified version of the Unix inode. It includes 12 pointers of the index block. The first 11 of these pointers
 * point to the direct blocks. The last pointer points to an indirect block. In addition each inode must include:
 *      1. the length of the corresponding file
 *      2. the number of file (structure) table entries that point to this inode
 *      3. the flag to indicate if is unused (=0), used (=1), errot (=-3)
 * Note that 16 inodes can be stored in one block, and 256 indirect inode addresses in an index block using short for
 * addressing.
 * Note that short pointer value is address of block that it is pointed at, and short indirect pointer is the address
 * of the indirect index block.  The indirect index block stores all addresses congruently as shorts pointing to
 * addresses to blocks targeted by this inode. If no block is targeted short is 0
 */
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int maxBytes = 512;       // size of a block in bytes
    private final static int blockSize = 16;       // number of inodes per block
    private final static int intBlock = 4;         // size of int in bytes
    private final static int shortBlock = 2;       // size of short in bytes

    public int fileSize;                           // file size in bytes
    public short count;                            // # file-table entries pointing to this, 0 for deletion
    public short usedFlag;                         // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    /**
     * Inode()
     * default constructor for inode class, sets class level variables to default values and sets all block pointers
     * to (-1) indicating that there are not pointing to data.
     */
    Inode( )
    {                                              // a default constructor
        fileSize = 0;
        count = 0;
        usedFlag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    /**
     * Inode(short iNumber)
     * Overloaded Inode constructor iNumber is the iNumber is the number associated with this inode and sets all
     * variables by reading from disk the values stored there.
     * @param iNumber this inode number
     */
    Inode( short iNumber )                          // retrieving iNode from disk
    {
        int nodeBlock = (iNumber / blockSize) + 1;  // Block number where inode is located on disk
        byte[] dataBuffer = new byte[maxBytes];     // buffer to hold data from nodeBlock
        SysLib.rawread(nodeBlock, dataBuffer);      // take block in from disk and put into dataBuffer

        int spaceTracker = (iNumber % blockSize) * iNodeSize;   // used to iterate through the block based on variable thickness
        fileSize = SysLib.bytes2int(dataBuffer, spaceTracker);  // read from dataBuffer into iNode, then iterate thickness variable forward
        spaceTracker += intBlock;
        count = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        usedFlag = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        for (int i = 0; i < directSize; i++)        // for loop to iterate through direct pointers
        {
            direct[i] = SysLib.bytes2short(dataBuffer, spaceTracker);
            spaceTracker += shortBlock;
        }
        indirect = SysLib.bytes2short(dataBuffer, spaceTracker);
    }

    /**
     * toDisk(short iNumber)
     * Method saves inode to disk. Takes in passed iNumber and reads from memory and stores to disk then returns the
     * block that inode is saved in.
      * @param iNumber
     * @return int block is the block number this inode is saved at
     */
    int toDisk( short iNumber )                     // save to disk as the i-th iNode
    {
        int nodeBlock = (iNumber / blockSize) + 1;  // skips SuperBlock to determine block to write to
        byte[] nodeBuffer = new byte[iNodeSize];
        int spaceTracker = 0;

        SysLib.int2bytes(fileSize, nodeBuffer, spaceTracker);  // writing to temp buffer
        spaceTracker += intBlock;
        SysLib.short2bytes(count, nodeBuffer, spaceTracker);
        spaceTracker += shortBlock;
        SysLib.short2bytes(usedFlag, nodeBuffer, spaceTracker);
        spaceTracker += shortBlock;

        for(int i = 0; i < directSize; i++)
        {
            SysLib.short2bytes(direct[i], nodeBuffer, spaceTracker);
            spaceTracker += shortBlock;
        }
        SysLib.short2bytes(indirect, nodeBuffer, spaceTracker);

        spaceTracker = (iNumber % blockSize) * iNodeSize;   // reset for inserting nodeBuffer into dataBuffer
        byte[] blockBuffer = new byte[maxBytes];            // holds the entire block from disk for iNode buffer insertion
        SysLib.rawread(nodeBlock, blockBuffer);
        // due to the inability for rawwrite to copy a subsection of a block, the iNode must be written to the buffer
        // System.lang arraycopy is used to copy the buffered iNode into the proper location in the buffered block
        System.arraycopy(nodeBuffer, 0, blockBuffer, spaceTracker, iNodeSize);
        SysLib.rawwrite(nodeBlock, blockBuffer);         // overwrites the location of original buffered block
        return nodeBlock;
    }
}