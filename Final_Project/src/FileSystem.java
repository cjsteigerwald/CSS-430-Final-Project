/**
 * Created by Chris Steigerwald, Hunter Grayson, Michael Voight on 12/6/2015.
 */
public class FileSystem {
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;

    public FileSystem(int diskBlocks) {
        // create superblock, and format disk with 64 inodes in default
        superblock = new SuperBlock(diskBlocks);

        // create directory, and register "/" in directory entry 0
        directory = new Directory(superblock.inodeBlocks);

        // file table is created, and store directory in the file table
        filetable = new FileTable(directory);

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
            int address = 4;
            // temp buffer for rawread to hold a block, and passed as source to arraycopy
            byte readBuffer[] = new byte[blockSize];

            //byte addBuffer[] = new byte[address];
                    //int maxBufferSize = buffer.length;     // 136,704 bytes max size
            // byte tracker for keeping track of position
            int spaceTracker = 0;
                    //int readSize = 0;
            // for loop for iterating through iNode direct pointers
            // i = 3 because the first pointer are for stdin, stdout, and err
            for (int i = 3; i < ftEnt.iNode.direct.length; i++, spaceTracker += blockSize)
            {
                // if iNode's direct pointer points to block, read in block and copy to buffer
                if(ftEnt.iNode.direct[i] != -1)
                {
                    SysLib.rawread(ftEnt.iNode.direct[i], buffer);
                    System.arraycopy(readBuffer, 0, buffer, spaceTracker, blockSize);
                    spaceTracker += blockSize;
                }
                else
                {
                    return spaceTracker;
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
                for (int i = 0; i < addInBlock; i += addressLength, spaceTracker += blockSize)
                {
                    System.arraycopy(addressBuffer, i, readAddress, 0, addressLength);
                    addressPointer = SysLib.bytes2int(readAddress, 0);
                    // address block is now equal 0 indicating no more indirect addresses
                    if (addressPointer == 0)
                    {
                        return spaceTracker;
                    }
                    // // reads addressPointer buffer into readBuffer to prepare to be added to buffer
                    SysLib.rawread(addressPointer, readBuffer);
                    // uses arraycopy to copy readBuffer onto buffer at placement spaceTracker
                    System.arraycopy(readBuffer, 0, buffer, spaceTracker, blockSize);
                }
                return spaceTracker;
            }
        } // end else statement
        return -1;
    }

    int write(FileTableEntry ftEnt, byte[] buffer)
    {
        // size of block
        int blockSize = 512;
        // buffer used to write to disk
        byte[] writeBuffer = new byte[blockSize];
        // pointer to keep track of location
        int spaceTrakcer;

        return -1;
    }

    int delete(String filename)
    {
        //FileTableEntry entry = open(filename, "w");
        //short iNumber = directory.namei(filename);
        if (directory.ifree(directory.namei(filename)))
        {
            return 0;
        }
        else
        {
            return -1;
        }
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 0;
    private final int SEEK_END = 0;

    int seek(FileTableEntry ftEnt, int offset, int whence)
    {
<<<<<<< Updated upstream
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
=======
        return -1;
>>>>>>> Stashed changes
    }

} // end FileSystem class default
