/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Hunter Grayson, Chris Steigerwald, Michael Voight
 * @last edit: 12/6/2015
 * The "/" root directory maintains each file in a different directory entry that contains its file name
 *     (in maximum 30 characters = in max. 60 bytes in Java) and the corresponding Inode number. The directory
 *     receives the maximum number of Inodes to be created, (i.e., thus the max. number of files to be created)
 *     and keeps track of which Inode numbers (iNumbers) are in use. Since the directory itself is considered
 *     as a file, its contents are maintained by an Inode, specifically saying Inode 0. This can be located in
 *     the first 32 bytes of the disk block 1.
 *
 *     Upon a boot, the file system instantiates the following Directory class as the root directory through
 *     its constructor, reads the file from the disk that can be found at Inode 0 (the first 32 bytes of the
 *     disk block 1), and initializes the Directory instance with the file contents. Upon shutdown, the file
 *     system must write back the Directory information onto the disk. The bytes2directory() method will
 *     initialize the Directory instance with a byte array read from the disk and the directory2bytes()
 *     method converts the Directory instance into a byte array that can be written back to the disk.
 */

public class FileSystem
{
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private boolean[] freeBlockTable;   // "true" equals free, "false" equals full

    /**
     * FileSystem(int diskBlocks)
     *     Overloaded constructor takes in the number of diskBlocks and instantiates a virtual file system of passed
     *     in size and creates and creates a freeBlockTable for tracking free and used blocks within the virtual file
     *     system.
     * @param diskBlocks
     */
    public FileSystem(int diskBlocks)
    {
        // create SuperBlock, and format disk with 64 iNodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.totalInodes);

        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

        // set freeBlockTable all blocks to free
        // i = 3 to skip SuperBlock and iNodes
        freeBlockTable = new boolean[diskBlocks];
        // covers SuperBlock, and iNode blocks from being overwritten
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
    void sync()
    {
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
    boolean format(int files)
    {
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

    FileTableEntry open(String filename, String mode)
    {
        if (mode == "w" || mode == "w+" || mode == "r" || mode == "a")
        {
            FileTableEntry entry = filetable.fAlloc(filename, mode);
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
     * @param fileEntry
     * @return true is close is successful else false indicating error
     */
    boolean close(FileTableEntry fileEntry)
    {
        synchronized (fileEntry)
        {
            fileEntry.count--;
        }
        if (fileEntry.count == 0)
        {
            filetable.fFree(fileEntry);
            return true;
        }
        return false;
    }

    /**
     * fsize(FileTableEntry ftEnt)
     * <p>
     *     Returns the file size in bytes of file associated with FileTableEntry passed in
     * </p>
     * @param fileEntry
     * @return int size of file in bytes
     */
    int fsize(FileTableEntry fileEntry)
    {
        return fileEntry.iNode.fileSize;
    }

    /**
     * read(FileTableEntry fileEntry, byte[] buffer)
     * <p>
     *     Reads into memory the file associated with FileTableEntry passed in and returns the size of the file
     *     in bytes to calling method.  Mode must be "r" or "w+" indicating read or read/write. If mode of
     *     FileTableEntry is "w" or "a" error (-1) is returned to calling method.  Using synchronized to allows
     *     multi-threading and prevents write and append from causing race conditions.
     * </p>
     * @param fileEntry
     * @param buffer
     * @return int size in bytes of read in file
     */
    int read(FileTableEntry fileEntry, byte[] buffer)
    {
        // if write or append return error
        if ((fileEntry.mode == "w") || (fileEntry.mode == "a"))
            return -1;

        int size  = buffer.length;
        int rBuffer = 0;
        int rError = -1;
        int blockSize = 512;
        int itrSize = 0;

        // FileTableEntry object is synchronized to prevent multiple threads access congruently
        synchronized(fileEntry)
        {
            // Loop for reading data from disk, seekPtr must be less than file size, and file must have data
            while (fileEntry.seekPtr < fsize(fileEntry) && (size > 0))
            {
                // find block that contains data that corresponds with file
                int currentBlock = findTargetBlock(fileEntry, fileEntry.seekPtr);
                // if system cannot find block corresponding with file break from loop
                if (currentBlock == rError)
                {
                    break;
                }
                // block sized buffer for reading in data from disk block
                byte[] data = new byte[blockSize];
                // read from disk to data buffer
                SysLib.rawread(currentBlock, data);

                // keeps track of offset from FileTableEntry seekPtr for location within block
                int dataOffset = fileEntry.seekPtr % blockSize;
                // number of blocks left to read into system
                int blocksLeft = blockSize - itrSize;
                // number of bytes left to read in from disk
                int fileLeft = fsize(fileEntry) - fileEntry.seekPtr;

                // check to reset size of
                if (blocksLeft < fileLeft)
                    itrSize = blocksLeft;
                else
                    itrSize = fileLeft;

                if (itrSize > size)
                    itrSize = size;

                // copy data from from disk into buffer
                System.arraycopy(data, dataOffset, buffer, rBuffer, itrSize);
                rBuffer += itrSize;
                fileEntry.seekPtr += itrSize;
                size -= itrSize;
            }
            return rBuffer;
        }
    }

    /**
     * findFreeBlock()
     * Finds a free block in memory by iterating through the freeBlockTable (table of boolean values: True indicates
     * block is free, False indicates that block is currently has data.  Returns short value that is free block number
     * @return short indicating free block table
     */
    short findFreeBlock()
    {
        for(short i = 3; i < freeBlockTable.length; i++)
        {
            if (freeBlockTable[i])
            {
                freeBlockTable[i] = false;
                return i;
            }
        }
        System.out.print("Error, disk is full");
        return -1;
    }

    /**
     *  write(FileTableEntry entry, byte[] buffer)
     *  Method writes data from memory to disk.  Takes in a FileTableEntry and byte array.  The entry contains
     *  an inode, and data regarding the files size, mode, and the inode contains pointers for direct and indirect
     *  addressing to blocks in memory.  This method will fill take data from the buffer and write to disk, and
     *  record in the inode all block addresses.
     * @param entry
     * @param buffer
     * @return int number of bytes read in from buffer or -1 indicating error in write
     */
    int write( FileTableEntry entry, byte[] buffer)
    {
        // number of bytes written, and will be returned
        int bytesWritten = 0;
        // length of buffer being passed in, indicating length of file
        int bufferSize = buffer.length;
        // size of a block of memory, currently 512 bytes
        int blockSize = Disk.blockSize;

        // if mode is read exit method with -1 indicating error
        if (entry == null || entry.mode.equals("r"))
            return -1;
        synchronized (entry)
        {
            // while buffer has data continue
            while (bufferSize > 0)
            {
                // variable for holding write location within target block
                int location = findTargetBlock(entry, entry.seekPtr);
                // if location is -1 get a new block for writing
                if (location == -1)
                {
                    short newLocation = this.findFreeBlock();

                    int pointer = registerTargetBlock(entry, entry.seekPtr, newLocation);

                    // if testPtr = -3 block error return -1
                    if (pointer == -3)
                    {
                        short freeBlock = this.findFreeBlock();
                        if (!registerIndexBlock(entry, freeBlock))
                            return -1;

                        if (registerTargetBlock(entry, entry.seekPtr, newLocation) != 0)
                            return -1;
                    }
                    else if (pointer == -2 || pointer == -1)
                        return -1;
                    location = newLocation;
                }

                // buffer for holding data read from memory befor pushed to disk
                byte [] tempBuff = new byte[blockSize];
                // read memory from memory to temp buffer
                SysLib.rawread(location, tempBuff);
                int tempPtr = entry.seekPtr % blockSize;
                // counter for holding how much more data the block can hold
                int difference = blockSize - tempPtr;

                // As long as there is more memory in the block than temp buffer write to disk
                if (difference > bufferSize)
                {
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, bufferSize);
                    SysLib.rawwrite(location, tempBuff);
                    entry.seekPtr += bufferSize;
                    bytesWritten += bufferSize;
                    bufferSize = 0;
                }
                else {
                    System.arraycopy(buffer, bytesWritten, tempBuff, tempPtr, difference);
                    SysLib.rawwrite(location, tempBuff);
                    entry.seekPtr += difference;
                    bytesWritten += difference;
                    bufferSize -= difference;
                }
            }

            // reset FileTableEntry object seek pointer
            if (entry.seekPtr > entry.iNode.fileSize)
            {
                entry.iNode.fileSize = entry.seekPtr;
            }
            entry.iNode.toDisk(entry.iNumber);
            return bytesWritten;
        }
    }

    /**
     * delete(String filename)
     * This method takes in a filename and deletes it from system.  Returns True if deleted, else false error
     * @param filename
     * @return
     */
    boolean delete(String filename)
    {
        FileTableEntry entry = open(filename, "w");
        return close(entry) && directory.iFree(entry.iNumber);
    }

    /**
     * seek(FileTableEntry fileEntry, int offset, int whence)
     * This method sets the seek pointer within a FileTableEntry object.
     * @param fileEntry
     * @param offset
     * @param cameFrom
     * @return int location of seek pointer
     */
    int seek(FileTableEntry fileEntry, int offset, int cameFrom)
    {
        synchronized (fileEntry)
        {
            switch(cameFrom)
            {
                case 0:
                    if(offset < 0)
                        fileEntry.seekPtr = 0;
                    else if (offset > this.fsize(fileEntry))
                        fileEntry.seekPtr = this.fsize(fileEntry);
                    else
                        fileEntry.seekPtr = offset;
                    break;
                case 1:
                    if((fileEntry.seekPtr + offset) < 0)
                        fileEntry.seekPtr = 0;
                    else if((fileEntry.seekPtr + offset) > this.fsize(fileEntry))
                        fileEntry.seekPtr = this.fsize(fileEntry);
                    else
                        fileEntry.seekPtr += offset;
                    break;
                case 2:
                    if((this.fsize(fileEntry) + offset) < 0)
                        fileEntry.seekPtr = 0;
                    else if((this.fsize(fileEntry) + offset) > this.fsize(fileEntry))
                        fileEntry.seekPtr = this.fsize(fileEntry);
                    else
                        fileEntry.seekPtr = this.fsize(fileEntry) + offset;
                    break;
            }
            return fileEntry.seekPtr;
        }
    }

    /**
     * registerTargetBlock(FileTableEntry fileEntry, int entry, short offset)
     * This method adds a block to an inodes pointer if 11 or less blocks added to
     * direct pointer, if greater than 11 but less than 267 indirect pointer
     * @param fileEntry
     * @param entry
     * @param offset
     * @return
     */
    int registerTargetBlock(FileTableEntry fileEntry, int entry, short offset){
        int target = entry/Disk.blockSize;

        if (target < 11)
        {
            if(fileEntry.iNode.direct[target] >= 0){
                return -1;
            }

            if ((target > 0 ) && (fileEntry.iNode.direct[target - 1 ] == -1))
            {
                return -2;
            }

            fileEntry.iNode.direct[target] = offset;
            return 0;
        }

        if (fileEntry.iNode.indirect < 0)
        {
            return -3;
        }

        else{
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(fileEntry.iNode.indirect,data);

            int blockSpace = (target - 11) * 2;
            if ( SysLib.bytes2short(data, blockSpace) > 0)
            {
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset, data, blockSpace);
                SysLib.rawwrite(fileEntry.iNode.indirect, data);
            }
        }
        return 0;
    }

    /**
     * registerIndexBlock(FileTableEntry fileEntry, short blockNumber)
     * This method finds a free block to assign to the 12th direct address pointer in an inode
     * to be used as an index block for indirect pointers within the inode
     * @param fileEntry
     * @param blockNumber
     * @return true if index block allocated, else return false
     */
    boolean registerIndexBlock(FileTableEntry fileEntry, short blockNumber)
    {
        for (int i = 0; i < 11; i++)
        {
            if (fileEntry.iNode.direct[i] == -1)
            {
                return false;
            }
        }

        if (fileEntry.iNode.indirect != -1)
        {
            return false;
        }

        fileEntry.iNode.indirect = blockNumber;
        byte[ ] data = new byte[Disk.blockSize];

        for(int i = 0; i < (Disk.blockSize/2); i++)
        {
            SysLib.short2bytes((short) -1, data, i * 2);
        }
        SysLib.rawwrite(blockNumber, data);
        return true;
    }

    /**
     * findTargetBlock(FileTableEntry fileEntry, int offset)
     * This is used to find the address of a target block that is being read from.  If block
     * is found on disk returns address, else -1 indicating error
     * @param fileEntry
     * @param offset
     * @return int address of block, -1 indicating error
     */
    int findTargetBlock(FileTableEntry fileEntry, int offset)
    {
        int target = (offset / Disk.blockSize);

        if (target < 11){
            return fileEntry.iNode.direct[target];
        }

        if (fileEntry.iNode.indirect < 0){
            return -1;
        }

        byte[] data = new byte[Disk.blockSize];
        SysLib.rawread(fileEntry.iNode.indirect, data);

        int blk = (target - 11) * 2;
        return SysLib.bytes2short(data, blk);
    }
}