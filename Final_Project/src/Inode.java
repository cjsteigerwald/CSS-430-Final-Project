
public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
    private final static int maxBytes = 512;
    private final static int blockSize = 16;
    private final static int intBlock = 4;
    private final static int shortBlock = 2;
    private final static int ERROR = -1;

    public int fileSize;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this, 0 for deletion
    public short usedFlag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    Inode( ) {                                     // a default constructor
        fileSize = 0;
        count = 0;
        usedFlag = 1;
        for ( int i = 0; i < directSize; i++ )
            direct[i] = -1;
        indirect = -1;
    }

    // 0'th block is reserved for SuperBlock therefore adding 1 skips SuperBlock
    Inode( short iNumber ) {                       // retrieving iNode from disk
        // design it by yourself.
        // Block number where inode is located on disk
        int nodeBlock = (iNumber % blockSize) + 1;

        // buffer to hold data from nodeBlock
        byte[] dataBuffer = new byte[maxBytes];
        // take block in from disk and put into dataBuffer
        SysLib.rawread(nodeBlock, dataBuffer);

        // spaceTracker is used to iterate through the block based on variable thickness
        int spaceTracker = (iNumber % blockSize) * iNodeSize;
        // read from dataBuffer into iNode, then iterate thickness variable forward
        fileSize = SysLib.bytes2int(dataBuffer, spaceTracker);
        spaceTracker += intBlock;
        count = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        usedFlag = SysLib.bytes2short(dataBuffer, spaceTracker);
        spaceTracker += shortBlock;
        // for loop to iterate through direct pointers
        for (int i = 0; i < directSize; i++)
        {
            direct[i] = SysLib.bytes2short(dataBuffer, spaceTracker);
            spaceTracker += shortBlock;
        }
        indirect = SysLib.bytes2short(dataBuffer, spaceTracker);
    }

    int toDisk( short iNumber ) {                  // save to disk as the i-th iNode
        // design it by yourself.
        // nodeBlock is the iNode number * blockSize + 1 (+1 skips SuperBlock) to determine block to write to
        int nodeBlock = (iNumber % blockSize) + 1;

        byte[] nodeBuffer = new byte[iNodeSize];
        int spaceTracker = 0;
        // writing to temp buffer
        SysLib.int2bytes(fileSize, nodeBuffer, spaceTracker);
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

        // reset spaceTracker for inserting nodeBuffer into dataBuffer
        spaceTracker = (iNumber % blockSize) * iNodeSize;
        // nodeBuffer holds iNode
        byte[] dataBuffer = new byte[iNodeSize];
        // for holding the entire block from disk for the iNode buffer to be inserted into at the right location
        byte[] blockBuffer = new byte[maxBytes];
        SysLib.rawread(nodeBlock, blockBuffer);
        // due to the inability for rawwrite to copy a subsection of a block, the iNode must be written to the buffer
        // System.lang arraycopy is used to copy the buffered iNode into the proper location in the buffered block
        System.arraycopy(nodeBuffer, 0, blockBuffer, spaceTracker, iNodeSize);
        // overwrites the block in the location where the original buffered block was pulled
        SysLib.rawwrite(nodeBlock, blockBuffer);
        // returns the block number where the iNode was inserted
        return nodeBlock;
    }
}