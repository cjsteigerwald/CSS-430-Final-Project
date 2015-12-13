import java.util.Vector;

/**
 * Created by Chris on 12/8/2015.
 */
public class FileTable {

    private Vector <FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory

    public FileTable( Directory directory ) { // constructor
        table = new Vector( );     // instantiate a file (structure) table
        dir = directory;           // receive a reference to the Director
    }                             // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        // allocate a new file (structure) table entry for this file name
        // allocate/retrieve and register the corresponding inode using dir
        // increment this inode's count
        // immediately write back this inode to the disk
        // return a reference to this file (structure) table entry

        short iNumber = -1; // inode number
        Inode inode = null; // holds inode

        while (true) {

            iNumber = (filename.equals("/") ? (short) 0 : dir.namei(filename));

            if (iNumber >= 0) {
                inode = new Inode(iNumber);

                if (mode.equals("r")) {
                    if (inode.usedFlag == 0 || inode.usedFlag == 1 || inode.usedFlag == 2) {
                        inode.usedFlag = 2;
                        break;
                    } else if (inode.usedFlag == 3) {
                        try {
                            wait();
                        } catch (InterruptedException e)
                        {
                            SysLib.cerr("Error while reading");
                        }
                    }

                } else {
                    if (inode.usedFlag == 1 || inode.usedFlag == 0) {
                        inode.usedFlag = 3;
                        break;
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException e) { SysLib.cerr("Error while writing"); }
                    }
                }
            } else if (!mode.equals("r")) {
                iNumber = dir.iAlloc(filename);
                inode = new Inode(iNumber);
                inode.usedFlag = 3;
                break;
            } else {
                return null;
            }
        }
        inode.count++;
        inode.toDisk(iNumber);

        FileTableEntry entry = new FileTableEntry(inode, iNumber, mode);
        table.addElement(entry);
        return entry;
    }

    public synchronized boolean ffree( FileTableEntry entry ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table

        Inode inode = new Inode(entry.iNumber);
        if (table.remove(entry))
        {
            if (inode.usedFlag == 2)
            {
                if (inode.count == 1)
                {
                    notify();
                    inode.usedFlag = 1;
                }
            }
            else if (inode.usedFlag == 3)
            {
                inode.usedFlag = 1;
                notifyAll();
            }
            inode.count--;
            inode.toDisk(entry.iNumber);
            return true;
        }
        return false;
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format

    public Inode getInode(short iNumber)
    {
        boolean found = true;
        for(int i = 0; i < table.size(); i++) {
            if (table.elementAt(i).iNumber == iNumber) {
                return table.elementAt(i).iNode;
            }
        }
        return null;
    }
}