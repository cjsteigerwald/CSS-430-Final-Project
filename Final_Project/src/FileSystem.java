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
