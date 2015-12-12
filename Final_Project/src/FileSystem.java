/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Chris Steigerwald, Hunter Grayson, Michael Voight
 * @last edit: 12/6/2015
 *
 * Description:
 *
 *  The "/" root directory maintains each file in a different directory entry that contains its file name
 *  (in maximum 30 characters = in max. 60 bytes in Java) and the corresponding Inode number. The directory
 *  receives the maximum number of Inodes to be created, (i.e., thus the max. number of files to be created)
 *  and keeps track of which Inode numbers (iNumbers) are in use. Since the directory itself is considered as
 *  a file, its contents are maintained by an Inode, specifically saying Inode 0. This can be located in the
 *  first 32 bytes of the disk block 1.
 *
 *  Upon a boot, the file system instantiates the following Directory class as the root directory through its
 *  constructor, reads the file from the disk that can be found at Inode 0 (the first 32 bytes of the disk block 1),
 *  and initializes the Directory instance with the file contents. Upon shutdown, the file system must write back t
 *  he Directory information onto the disk. The bytes2directory() method will initialize the Directory instance with
 *  a byte array read from the disk and the directory2bytes() method converts the Directory instance into a byte
 *  array that can be written back to the disk.
 */



public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private boolean[] freeBlockTable;   // "true" equals free, "false" equals full
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;




    /**
     * FileSystem(int diskBlocks)
     * <p>
     *     The "/" root directory maintains each file in a different directory entry that contains its file name
     *     (in maximum 30 characters = in max. 60 bytes in Java) and the corresponding Inode number. The directory
     *     receives the maximum number of Inodes to be created, (i.e., thus the max. number of files to be created)
     *     and keeps track of which Inode numbers (iNumbers) are in use. Since the directory itself is considered
     *     as a file, its contents are maintained by an Inode, specifically saying Inode 0. This can be located in
     *     the first 32 bytes of the disk block 1.
     * <p>
     *     Upon a boot, the file system instantiates the following Directory class as the root directory through
     *     its constructor, reads the file from the disk that can be found at Inode 0 (the first 32 bytes of the
     *     disk block 1), and initializes the Directory instance with the file contents. Upon shutdown, the file
     *     system must write back the Directory information onto the disk. The bytes2directory() method will
     *     initialize the Directory instance with a byte array read from the disk and the directory2bytes()
     *     method converts the Directory instance into a byte array that can be written back to the disk.
     * </p>
     * </p>
     *     Overloaded constructor takes in the number of diskBlocks and instantiates a virtual file system of passed
     *     in size and creates and creates a freeBlockTable for tracking free and used blocks within the virtual file
     *     system.
     * @param diskBlocks
     */
    public FileSystem(int diskBlocks) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);

        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

        // set freeBlockTable all blocks to free
        // i = 3 to skip superblock and iNodes
        freeBlockTable = new boolean[diskBlocks];
        // covers superblock, and iNode blocks from being overwritten
        for (int i = 0; i < 3; i++)
        {
            freeBlockTable[i] = false;
        }
        for (int i = 3; i < diskBlocks; i++)
        {
            freeBlockTable[i] = true;
        }
        // directory reconstruction
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close(dirEnt);
    } // FileSystem overloaded

    /**
     * sync
     * Syncs the cache back to the physical disk.
     */
    void sync() {
        FileTableEntry ftEnt = this.open("/", "w");
        byte[] data = this.directory.directory2bytes();
        this.write(ftEnt, data);
        this.close(ftEnt);
        this.superblock.sync();
    }

    /**
     * format(int files)
     * <p>
     *     Performs a full format of the disk, erases all data and recreates the superblock, directory, and file
     *     tables.  Once called all data is lost.
     * </p>
     * @param files amount of files to be formatted
     * @return true upon successful format of file system
     */
    boolean format(int files) {
        superblock.format(files);
        directory = new Directory(superblock.totalInodes);
        filetable = new FileTable(directory);
        return true;
    }

    /**
     * open(String filename, String mode)
     * <p>
     *     Opens a file with name (filename), using corresponding modes:
     *              "w+": write/read
     *              "w": write
     *              "r": read
     *              "a": append- write to end of file
     *     Creates a FileTableEntry object passing in filename and mode to FileTable class to be instantiated.
     * </p>
     * @param filename
     * @param mode
     * @return FileTableEntry to calling function, null if unsuccessful
     */
    FileTableEntry open(String filename, String mode) {
        if (mode == "w" || mode == "w+" || mode == "r" || mode == "a")
        {
            FileTableEntry entry = filetable.falloc(filename, mode);
            return entry;
        }
        else
        {
            return null;
        }
    }

    /**
     * close(FileTableEntry)
     * <p>
     *      Closes the the file corresponding to the FileTableEntry that is passed in.  If count (number of threads
     *      currently using file) is greater than 0, means other threads are currently using file and will not close
     *      file until count is 0.  Returns true if file is successfully closed.  Using synchronized to protect
     *      threads from loosing access to file before they finish with it.
     * </p>
     * @param ftEnt
     * @return true is close is successful else false indicating error
     */
    boolean close(FileTableEntry ftEnt) {
        synchronized (ftEnt)
        {
            ftEnt.count--;
        }
        if (ftEnt.count == 0)
        {
            filetable.ffree(ftEnt);
            return true;
        }
        return false;
    }

    /**
     * fsize(FileTableEntry ftEnt)
     * <p>
     *     Returns the file size in bytes of file associated with FileTableEntry passed in
     * </p>
     * @param ftEnt
     * @return int size of file in bytes
     */
    int fsize(FileTableEntry ftEnt)
    {
        return ftEnt.iNode.fileSize;
    }

    /**
     * read(FileTableEntry ftEnt, byte[] buffer)
     * <p>
     *     Reads into memory the file associated with FileTableEntry passed in and returns the size of the file
     *     in bytes to calling method.  Mode must be "r" or "w+" indicating read or read/write. If mode of
     *     FileTableEntry is "w" or "a" error (-1) is returned to calling method.  Using synchronized to allows
     *     multi-threading and prevents write and append from causing race conditions.
     * </p>
     * @param ftEnt
     * @param buffer
     * @return int size in bytes of read in file
     */
    int read(FileTableEntry ftEnt, byte[] buffer) { // TODO
        // if write or append return error
        if ((ftEnt.mode == "w") || (ftEnt.mode == "a"))
            return -1;

        int size  = buffer.length;
        int rBuffer = 0;
        int rError = -1;
        int blockSize = 512;
        int itrSize = 0;

        // FileTableEntry object is synchronized to prevent multiple threads access congruently
        synchronized(ftEnt)
        {
            // Loop for reading data from disk, seekPtr must be less than file size, and file must have data
            while (ftEnt.seekPtr < fsize(ftEnt) && (size > 0))
            {
                int currentBlock = findTargetBlock(ftEnt, ftEnt.seekPtr);
                if (currentBlock == rError)
                {
                    break;
                }
                byte[] data = new byte[blockSize];
                SysLib.rawread(currentBlock, data);

                int dataOffset = ftEnt.seekPtr % blockSize;
                int blocksLeft = blockSize - itrSize;
                int fileLeft = fsize(ftEnt) - ftEnt.seekPtr;

                if (blocksLeft < fileLeft)
                    itrSize = blocksLeft;
                else
                    itrSize = fileLeft;

                if (itrSize > size)
                    itrSize = size;

                System.arraycopy(data, dataOffset, buffer, rBuffer, itrSize);
                rBuffer += itrSize;
                ftEnt.seekPtr += itrSize;
                size -= itrSize;
            }
            return rBuffer;
        }
    }

//    // Remember that direct links 0-2 are used for stdin, stout, err
//    int read(FileTableEntry ftEnt, byte[] buffer)
//    {
//        // get INode number from FileTableEntry object
//        int iNumber = ftEnt.iNumber;
//        ftEnt.seekPtr = 0;
//        // testing to make sure object is valid
//        if (ftEnt.mode == "w" || ftEnt.mode == "a" || iNumber == -1)
//        {
//            return -1;
//        }
//        // assumption that object is valid for read
//        else
//        {
//                    //int fileSize = ftEnt.iNode.fileSize;
//            // size of block in memory
//            int blockSize = 512;
//            // number of address locations in a block
//            int addInBlock = 256;
//            // temp buffer for rawread to hold a block, and passed as source to arraycopy
//            byte readBuffer[] = new byte[blockSize];
//
//            //byte addBuffer[] = new byte[address];
//                    //int maxBufferSize = buffer.length;     // 136,704 bytes max size
//            // byte tracker for keeping track of position
//                    //int readSize = 0;
//            // for loop for iterating through iNode direct pointers
//            // i = 3 because the first pointer are for stdin, stdout, and err
//            for (int i = 0; i < ftEnt.iNode.direct.length; i++, ftEnt.seekPtr += blockSize)
//            {
//                // if iNode's direct pointer points to block, read in block and copy to buffer
//                if(ftEnt.iNode.direct[i] != -1)
//                {
//                    SysLib.rawread(ftEnt.iNode.direct[i], buffer);
//                    System.arraycopy(readBuffer, 0, buffer, ftEnt.seekPtr, blockSize);
//                    ftEnt.seekPtr += blockSize;
//                }
//                else
//                {
//                    return ftEnt.seekPtr;
//                }
//            }
//            // if iNode's indirect pointer is pointing to block read into buffer
//            if (ftEnt.iNode.indirect != -1)
//            {
//                // tracker for indirect block addresses
//                int addressPointer = 0;
//                // int for holding size of address in indirect block
//                int addressLength = 4;
//                // buffer for holding address of block from indirect block addresses
//                byte[] addressBuffer = new byte[blockSize];
//                // holds address being read from addressBuffer for indirect links
//                byte[] readAddress = new byte[addressLength];
//                // reads buffer of 4 bytes into addressBuffer
//                SysLib.rawread(ftEnt.iNode.indirect, addressBuffer);
//                // iterator for indirect links, while addressBuffer input is not 0 will iterate
//                // to next block
//                for (int i = 0; i < addInBlock; i += addressLength, ftEnt.seekPtr += blockSize)
//                {
//                    System.arraycopy(addressBuffer, i, readAddress, 0, addressLength);
//                    addressPointer = SysLib.bytes2int(readAddress, 0);
//                    // address block is now equal 0 indicating no more indirect addresses
//                    if (addressPointer == 0)
//                    {
//                        return ftEnt.seekPtr;
//                    }
//                    // // reads addressPointer buffer into readBuffer to prepare to be added to buffer
//                    SysLib.rawread(addressPointer, readBuffer);
//                    // uses arraycopy to copy readBuffer onto buffer at placement spaceTracker
//                    System.arraycopy(readBuffer, 0, buffer, ftEnt.seekPtr, blockSize);
//                }
//                return ftEnt.seekPtr;
//            }
//        } // end else statement
//        return -1;
//    }

    short findFreeBlock()
    {
        for(short i = 3; i < freeBlockTable.length; i++)
        {
            if (freeBlockTable[i] == true)
            {
                freeBlockTable[i] = false;
                return i;
            }
        }
        System.out.print("Error, disk is full");
        return -1;
    }

    int write( FileTableEntry entry, byte[] buffer) {
        int bytesWritten = 0;
        int bufferSize = buffer.length;
        int blockSize = Disk.blockSize;

        if (entry == null || entry.mode == "r")
            return -1;
        synchronized (entry)
        {
            while (bufferSize > 0)
            {
                int location = findTargetBlock(entry, entry.seekPtr);
                if (location == -1)
                {
                    short newLocation = this.findFreeBlock();

                    int testPtr = registerTargetBlock(entry, entry.seekPtr, newLocation);

                    if (testPtr == -3)
                    {
                        short freeBlock = this.findFreeBlock();
                        if (!registerIndexBlock(entry, freeBlock))
                            return -1;

                        if (registerTargetBlock(entry, entry.seekPtr, newLocation) != 0)
                            return -1;
                    }
                    else if (testPtr == -2 || testPtr == -1)
                        return -1;
                    location = newLocation;
                }
                byte [] tempBuff = new byte[blockSize];
                SysLib.rawread(location, tempBuff);
                int tempPtr = entry.seekPtr % blockSize;
                int diff = blockSize - tempPtr;

                if (diff > bufferSize)
                {
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, bufferSize);
                    SysLib.rawwrite(location, tempBuff);
                    entry.seekPtr += bufferSize;
                    bytesWritten += bufferSize;
                    bufferSize = 0;
                }
                else {
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, diff);
                    SysLib.rawwrite(location, tempBuff);
                    entry.seekPtr += diff;
                    bytesWritten += diff;
                    bufferSize -= diff;
                }
            }

            if (entry.seekPtr > entry.iNode.fileSize)
            {
                entry.iNode.fileSize = entry.seekPtr;
            }
            entry.iNode.toDisk(entry.iNumber);
            return bytesWritten;
        }
    }

//    int write(FileTableEntry ftEnt, byte[] buffer)
//    {
//        synchronized (ftEnt) {
//            if (ftEnt.mode == "w" || ftEnt.mode == "w+" || ftEnt.mode == "a") {
//                // size of block
//                int blockSize = 512;
//                int maxBlocks = 267;
//                if (buffer.length / blockSize > maxBlocks + 1) {
//                    System.out.println("Error: file size too large");
//                    return -1;
//                }
//
//                ftEnt.seekPtr = 0;
//                if (ftEnt.mode == "a") {
//                    seek(ftEnt, 0, SEEK_END);
//                } // end if(ftEnt.mode == "a")
//
//                // buffer used to write to disk
//                byte[] writeBuffer = new byte[blockSize];
//                // indirect Buffer
//                byte[] indirectBuffer = new byte[blockSize];
//
//                int indirectTracker = 0;
//
//                for (int blockTracker = 0; blockTracker < (buffer.length / blockSize); blockTracker++) {
//                    // for 11 direct pointer
//                    if (blockTracker < 11) {
//                        ftEnt.iNode.direct[blockTracker] = findFreeBlock();
//                        System.arraycopy(buffer, ftEnt.seekPtr, writeBuffer, 0, blockSize);
//                        SysLib.rawwrite(ftEnt.iNode.direct[blockTracker], writeBuffer);
//                        freeBlockTable[ftEnt.iNode.direct[blockTracker]] = false;
//                    }
//                    // sets Inode.indirect pointer to a free block
//                    if (blockTracker == 11) {
//                        // setting indirect pointer to a free block
//                        ftEnt.iNode.indirect = findFreeBlock();
//                        // set freeBlockTable entry to false
//                        freeBlockTable[ftEnt.iNode.indirect] = false;
//                    }
//                    if (blockTracker >= 11) {
//                        // find next free block to write from buffer to block indirect points to
//                        int newBlock = findFreeBlock();
//                        // from buffer to writeBuffer
//                        System.arraycopy(buffer, ftEnt.seekPtr, writeBuffer, 0, blockSize);
//                        // write to disk
//                        SysLib.rawwrite(newBlock, writeBuffer);
//
//                        // write address in bytes of new block to indirectBuffer for holding pointers
//                        SysLib.int2bytes(newBlock, indirectBuffer, indirectTracker);
//                        // increment pointer by 4 for int value in bytes
//                        indirectTracker += 4;
//                    }
//                    ftEnt.seekPtr += blockSize;
//                } // end for()
//                // write to disk if using indirect block, write buffer of addresses to block indirect points to
//                if ((buffer.length / blockSize) > 11) {
//                    SysLib.rawwrite(ftEnt.iNode.indirect, indirectBuffer);
//                }
//                    return ftEnt.seekPtr;
//            } // end else
//            //  end if(ftEnt.mode == "w" || ftEnt.mode == "w+" || ftEnt.mode == "a")
//            // mode "r" or incompatible mode types
//            else {
//                return -1;
//            }
//        }
//    }

    boolean delete(String filename) {
        FileTableEntry entry = open(filename, "w");
        return close(entry) && directory.ifree(entry.iNumber);
    }

//    boolean delete(String filename)
//    {
//        short iNumber = directory.namei(filename);
//        Inode inode = filetable.getInode(iNumber);
//        byte[] killer = new byte [512];
//
//        //FileTableEntry entry = open(filename, "w");
//        //short iNumber = directory.namei(filename);
//        for( int i = 0; i < 11; i++ )
//        {
//            if(inode.direct[i] != -1)
//            {
//                SysLib.rawwrite(inode.direct[i], killer);
//            }
//        }
//        if (inode.indirect != -1)
//        {
//            short blockTracker = 0;
//            byte[] address = new byte [512];
//            SysLib.rawread(inode.indirect, address);
//            for(int i = 0; i < 256; i++, blockTracker +=2)
//            {
//                short theAddress = SysLib.bytes2short(address, blockTracker);
//                if (theAddress > 2)
//                {
//                    SysLib.rawwrite(theAddress, killer);
//                    freeBlockTable[theAddress] = true;
//                }
//            }
//            SysLib.rawwrite(inode.indirect, killer);
//            freeBlockTable[inode.indirect] = true;
//        }
//        if (directory.ifree(directory.namei(filename)))
//        {
//            return true;
//        }
//        else
//        {
//            return false;
//        }
//    }

    int seek(FileTableEntry ftEnt, int offset, int whence)
    {
        synchronized (ftEnt) {
            switch(whence) {
                case 0:
                    if(offset < 0)
                        ftEnt.seekPtr = 0;
                    else if (offset > this.fsize(ftEnt))
                        ftEnt.seekPtr = this.fsize(ftEnt);
                    else
                        ftEnt.seekPtr = offset;
                    break;
                case 1:
                    if((ftEnt.seekPtr + offset) < 0)
                        ftEnt.seekPtr = 0;
                    else if((ftEnt.seekPtr + offset) > this.fsize(ftEnt))
                        ftEnt.seekPtr = this.fsize(ftEnt);
                    else
                        ftEnt.seekPtr += offset;
                    break;
                case 2:
                    if((this.fsize(ftEnt) + offset) < 0)
                        ftEnt.seekPtr = 0;
                    else if((this.fsize(ftEnt) + offset) > this.fsize(ftEnt))
                        ftEnt.seekPtr = this.fsize(ftEnt);
                    else
                        ftEnt.seekPtr = this.fsize(ftEnt) + offset;
                    break;
            }
            return ftEnt.seekPtr;
        }
    }

    int blockFormattor(int target)
    {
        byte[] format = new byte[512];
        SysLib.rawwrite(target, format);
        return target;
    }

    private boolean deallocAllBlocks(FileTableEntry file)
    {
        return  true;
    }

    int registerTargetBlock(FileTableEntry ftEnt, int entry, short offset){
        int target = entry/Disk.blockSize;

        if (target < 11){
            if(ftEnt.iNode.direct[target] >= 0){
                return -1;
            }

            if ((target > 0 ) && (ftEnt.iNode.direct[target - 1 ] == -1)){
                return -2;
            }

            ftEnt.iNode.direct[target] = offset;
            return 0;
        }

        if (ftEnt.iNode.indirect < 0){
            return -3;
        }

        else{
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(ftEnt.iNode.indirect,data);

            int blockSpace = (target - 11) * 2;
            if ( SysLib.bytes2short(data, blockSpace) > 0){
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset, data, blockSpace);
                SysLib.rawwrite(ftEnt.iNode.indirect, data);
            }
        }
        return 0;
    }

    boolean registerIndexBlock(FileTableEntry ftEnt, short blockNumber){
        for (int i = 0; i < 11; i++){
            if (ftEnt.iNode.direct[i] == -1){
                return false;
            }
        }

        if (ftEnt.iNode.indirect != -1){
            return false;
        }

        ftEnt.iNode.indirect = blockNumber;
        byte[ ] data = new byte[Disk.blockSize];

        for(int i = 0; i < (Disk.blockSize/2); i++){
            SysLib.short2bytes((short) -1, data, i * 2);
        }
        SysLib.rawwrite(blockNumber, data);

        return true;

    }


    int findTargetBlock(FileTableEntry ftEnt, int offset){
        int target = (offset / Disk.blockSize);

        if (target < 11){
            return ftEnt.iNode.direct[target];
        }

        if (ftEnt.iNode.indirect < 0){
            return -1;
        }

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(ftEnt.iNode.indirect, data);

        int blk = (target - 11) * 2;
        return SysLib.bytes2short(data, blk);

    }
} // end FileSystem class default
