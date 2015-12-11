/**
 * Created by Chris Steigerwald, Hunter Grayson, Michael Voight on 12/6/2015.
 */
public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private boolean[] freeBlockTable;   // "true" equals free, "false" equals full
    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    public FileSystem(int diskBlocks) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.inodeBlocks);

        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

        // set freeBlockTable all blocks to free
        // i = 3 to skip superblock and iNodes
        freeBlockTable = new boolean[diskBlocks];
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

    void sync() {
        FileTableEntry ftEnt = this.open("/", "w");
        byte[] data = this.directory.directory2bytes();
        this.write(ftEnt, data);
        this.close(ftEnt);
        this.superblock.sync();
    }

    int format(int files) {
        superblock.format(files);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);
        return 0;
    }

    FileTableEntry open(String filename, String mode) {
        if (mode != "w" || mode != "w+" || mode != "r" || mode != "a")
        {
            return null;
        }
        else
        {
            FileTableEntry entry = filetable.falloc(filename, mode);
            return entry;
        }
    }

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

    int fsize(FileTableEntry ftEnt)
    {
        return ftEnt.iNode.fileSize;
    }


    // Remember that direct links 0-2 are used for stdin, stout, err
    int read(FileTableEntry ftEnt, byte[] buffer)
    {
        // get INode number from FileTableEntry object
        int iNumber = ftEnt.iNumber;
        ftEnt.seekPtr = 0;
        // testing to make sure object is valid
        if (ftEnt.mode == "w" || ftEnt.mode == "a" || iNumber == -1)
        {
            return -1;
        }
        // assumption that object is valid for read
        else
        {
                    //int fileSize = ftEnt.iNode.fileSize;
            // size of block in memory
            int blockSize = 512;
            // number of address locations in a block
            int addInBlock = 256;
            // temp buffer for rawread to hold a block, and passed as source to arraycopy
            byte readBuffer[] = new byte[blockSize];

            //byte addBuffer[] = new byte[address];
                    //int maxBufferSize = buffer.length;     // 136,704 bytes max size
            // byte tracker for keeping track of position
                    //int readSize = 0;
            // for loop for iterating through iNode direct pointers
            // i = 3 because the first pointer are for stdin, stdout, and err
            for (int i = 0; i < ftEnt.iNode.direct.length; i++, ftEnt.seekPtr += blockSize)
            {
                // if iNode's direct pointer points to block, read in block and copy to buffer
                if(ftEnt.iNode.direct[i] != -1)
                {
                    SysLib.rawread(ftEnt.iNode.direct[i], buffer);
                    System.arraycopy(readBuffer, 0, buffer, ftEnt.seekPtr, blockSize);
                    ftEnt.seekPtr += blockSize;
                }
                else
                {
                    return ftEnt.seekPtr;
                }
            }
            // if iNode's indirect pointer is pointing to block read into buffer
            if (ftEnt.iNode.indirect != -1)
            {
                // tracker for indirect block addresses
                int addressPointer = 0;
                // int for holding size of address in indirect block
                int addressLength = 4;
                // buffer for holding address of block from indirect block addresses
                byte[] addressBuffer = new byte[blockSize];
                // holds address being read from addressBuffer for indirect links
                byte[] readAddress = new byte[addressLength];
                // reads buffer of 4 bytes into addressBuffer
                SysLib.rawread(ftEnt.iNode.indirect, addressBuffer);
                // iterator for indirect links, while addressBuffer input is not 0 will iterate
                // to next block
                for (int i = 0; i < addInBlock; i += addressLength, ftEnt.seekPtr += blockSize)
                {
                    System.arraycopy(addressBuffer, i, readAddress, 0, addressLength);
                    addressPointer = SysLib.bytes2int(readAddress, 0);
                    // address block is now equal 0 indicating no more indirect addresses
                    if (addressPointer == 0)
                    {
                        return ftEnt.seekPtr;
                    }
                    // // reads addressPointer buffer into readBuffer to prepare to be added to buffer
                    SysLib.rawread(addressPointer, readBuffer);
                    // uses arraycopy to copy readBuffer onto buffer at placement spaceTracker
                    System.arraycopy(readBuffer, 0, buffer, ftEnt.seekPtr, blockSize);
                }
                return ftEnt.seekPtr;
            }
        } // end else statement
        return -1;
    }

    short findFreeBlock()
    {
        for(short i = 3; i < freeBlockTable.length; i++)
        {
            if (freeBlockTable[i] == true)
            {
                return i;
            }
        }
        System.out.print("Error, disk is full");
        return -1;
    }

    int write(FileTableEntry ftEnt, byte[] buffer)
    {
        synchronized (ftEnt) {
            if (ftEnt.mode == "w" || ftEnt.mode == "w+" || ftEnt.mode == "a") {
                // size of block
                int blockSize = 512;
                int maxBlocks = 267;
                if (buffer.length / blockSize > maxBlocks + 1) {
                    System.out.println("Error: file size too large");
                    return -1;
                }

                ftEnt.seekPtr = 0;
                if (ftEnt.mode == "a") {
                    seek(ftEnt, 0, SEEK_END);
                } // end if(ftEnt.mode == "a")

                // buffer used to write to disk
                byte[] writeBuffer = new byte[blockSize];
                // indirect Buffer
                byte[] indirectBuffer = new byte[blockSize];

                int indirectTracker = 0;

                for (int blockTracker = 0; blockTracker < (buffer.length / blockSize); blockTracker++, ftEnt.seekPtr += blockSize) {
                    // for 11 direct pointer
                    if (blockTracker < 11) {
                        ftEnt.iNode.direct[blockTracker] = findFreeBlock();
                        System.arraycopy(buffer, ftEnt.seekPtr, writeBuffer, 0, blockSize);
                        SysLib.write(ftEnt.iNode.direct[blockTracker], writeBuffer);
                        freeBlockTable[ftEnt.iNode.direct[blockTracker]] = false;
                    }
                    // sets Inode.indirect pointer to a free block
                    if (blockTracker == 11) {
                        // setting indirect pointer to a free block
                        ftEnt.iNode.indirect = findFreeBlock();
                        // set freeBlockTable entry to false
                        freeBlockTable[ftEnt.iNode.indirect] = false;
                    }
                    if (blockTracker >= 11) {
                        // find next free block to write from buffer to block indirect points to
                        int newBlock = findFreeBlock();
                        // from buffer to writeBuffer
                        System.arraycopy(buffer, ftEnt.seekPtr, writeBuffer, 0, blockSize);
                        // write to disk
                        SysLib.write(newBlock, writeBuffer);

                        // write address in bytes of new block to indirectBuffer for holding pointers
                        SysLib.int2bytes(newBlock, indirectBuffer, indirectTracker);
                        // increment pointer by 4 for int value in bytes
                        indirectTracker += 4;
                    }
                } // end for()
                // write to disk if using indirect block, write buffer of addresses to block indirect points to
                if ((buffer.length / blockSize) > 11) {
                    SysLib.write(ftEnt.iNode.indirect, indirectBuffer);
                }

                    return ftEnt.seekPtr;
                } // end else
            //  end if(ftEnt.mode == "w" || ftEnt.mode == "w+" || ftEnt.mode == "a")
            // mode "r" or incompatible mode types
            else {
                return -1;
            }
        }
    }

    int delete(String filename)
    {
        short iNumber = directory.namei(filename);
        Inode inode = filetable.getInode(iNumber);
        byte[] killer = new byte [512];

        //FileTableEntry entry = open(filename, "w");
        //short iNumber = directory.namei(filename);
        for( int i = 0; i < 11; i++ )
        {
            if(inode.direct[i] != -1)
            {
                SysLib.write(inode.direct[i], killer);
            }
        }
        if (inode.indirect != -1)
        {
            short blockTracker = 0;
            byte[] address = new byte [512];
            SysLib.rawread(inode.indirect, address);
            for(int i = 0; i < 256; i++, blockTracker +=2)
            {
                short theAddress = SysLib.bytes2short(address, blockTracker);
                if (theAddress > 2)
                {
                    SysLib.rawwrite(theAddress, killer);
                    freeBlockTable[theAddress] = true;
                }
            }
            SysLib.rawwrite(inode.indirect, killer);
            freeBlockTable[inode.indirect] = true;
        }
        if (directory.ifree(directory.namei(filename)))
        {
            return 0;
        }
        else
        {
            return -1;
        }
    }


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
        SysLib.write(target, format);
        return target;
    }

} // end FileSystem class default
