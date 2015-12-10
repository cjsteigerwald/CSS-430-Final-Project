/**
 * Created by Chris on 12/6/2015.
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
            for (int i = 0; i < ftEnt.iNode.direct.length; i++, spaceTracker += blockSize)
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
                // buffer for holding address of block from indirect block addresses
                byte[] addressBuffer = new byte[blockSize];
                // holds address being read from addressBuffer for indirect links
                byte[] readAddress = new byte[4];
                //
                SysLib.rawread(ftEnt.iNode.indirect, addressBuffer);
                for (int i = 0; i < addInBlock; i += 4, spaceTracker += blockSize)
                {
                    System.arraycopy(addressBuffer, i, readAddress, 0, 4);
                    addressPointer = SysLib.bytes2int(readAddress, 0);
                    if (addressPointer == 0)
                    {
                        return spaceTracker;
                    }

                    SysLib.rawread(addressPointer, readBuffer);
                    System.arraycopy(readBuffer, 0, buffer, spaceTracker, blockSize);
                }
                return spaceTracker;
            }
        } // end else statement
        return -1;
    }

    int write(FileTableEntry ftEnt, byte[] buffer)
    {

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

    }

} // end FileSystem class default
