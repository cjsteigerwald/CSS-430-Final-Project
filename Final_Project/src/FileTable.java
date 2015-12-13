import java.util.Vector;

/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Hunter Grayson, Chris Steigerwald, Michael Voight
 * @last edit: 12/6/2015
 *
 * The file system maintains the file (structure) table shared among all user threads.  When a user thread opens
 * a file, it follows the sequence listed below.
 *  1. The user thread allocates a new entry of the user file descriptor table in its TCB. This entry number itself
 *      becomes a file descriptor number. The entry maintains a reference to a file (structure) table entry.
 *  2. The user thread then requests the file system to allocate a new entry of the system-maintained file
 *      (structure) table. This entry includes the seek pointer of this file, a reference to the inode corresponding
 *      to the file, the inode number, the count to maintain #threads sharing this file depending on the file
 *      access mode.
 *  3. The file system locates the corresponding inode and records it in this file (structure) table entry.
 *  4. The user thread finally registers a reference to this file (structure) table entry in its file descriptor
 *      table entry of the TCB.
 */

public class FileTable
{
    private Vector table;                           // the actual entity of this file table
    private Directory directory;                    // the root directory

    /**
     * FileTable( Directory directory )
     * Overloaded constructor for FileTable() takes in and instantiates a vector for FileTableEntry's and a directory
     * @param directory
     */
    public FileTable( Directory directory )
    {                                               // constructor
        table = new Vector( );                      // instantiate a file (structure) table
        this.directory = directory;                 // receive a reference to the Director
    }                                               // from the file system

    /**
     * fAlloc( String filename, String mode )
     * This method allocates a new file (structure) table entry for this file name
     * Allocate/retrieve and register the corresponding inode using directory
     * Increment this inode's count
     * Immediately writes back this inode to disk
     * @param filename
     * @param mode
     * @return FileTableEntry if success returns a FileTableEntry object, else null if error
     */
    public synchronized FileTableEntry fAlloc( String filename, String mode )
    {
        short iNumber;
        Inode iNode;

        while (true)
        {
            iNumber = (filename.equals("/") ? (short) 0 : directory.namei(filename));

            if (iNumber >= 0)
            {
                iNode = new Inode(iNumber);
                if (mode.equals("r"))
                {
                    if (iNode.usedFlag == 0 || iNode.usedFlag == 1 || iNode.usedFlag == 2)
                    {
                        iNode.usedFlag = 2;
                        break;
                    }
                    else if (iNode.usedFlag == 3)
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e)
                        {
                            SysLib.cerr("Read Error");
                        }
                    }
                }
                else
                {
                    if (iNode.usedFlag == 1 || iNode.usedFlag == 0)
                    {
                        iNode.usedFlag = 3;
                        break;
                    }
                    else
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e)
                        {
                            SysLib.cerr("Write Error");
                        }
                    }
                }
            }
            else if (!mode.equals("r"))
            {
                iNumber = directory.iAlloc(filename);
                iNode = new Inode(iNumber);
                iNode.usedFlag = 3;
                break;
            }
            else
            {
                return null;
            }
        }
        iNode.count++;
        iNode.toDisk(iNumber);
        FileTableEntry entry = new FileTableEntry(iNode, iNumber, mode);
        table.addElement(entry);
        return entry;
    }

    /**
     * fFree( FileTableEntry entry )
     * This method receives a FileTableEntry object reference
     * Saves the corresponding inode to disk
     * Frees this file from table entry
     * @param entry
     * @return return true if FileTableEntry found on table, else return false error
     */
    public synchronized boolean fFree( FileTableEntry entry )
    {
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
}