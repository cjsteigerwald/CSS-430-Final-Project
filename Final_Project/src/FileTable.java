import java.util.Vector;

/**
 * Created by Chris on 12/8/2015.
 */
public class FileTable {

    private Vector table;         // the actual entity of this file table
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
        FileTableEntry entry;
        short Inumber = dir.namei(filename);
        // Inode exists because its value is not -1
        if (Inumber != -1)
        {
            Inode iNodeBuffer = new Inode(Inumber);
            iNodeBuffer.count ++;
            entry = new FileTableEntry(iNodeBuffer, Inumber, mode);
            table.add(entry);
            return entry;
        } // end if (Inumber != -1)
        // Inode does not exist in FileTable
        else
        {
            if (mode == "r")
            {
                return null;
            }
            else
            {
                Inumber = dir.findNextInode();
                Inode iNodeBuffer = new Inode(Inumber);
                iNodeBuffer.count ++;
                entry = new FileTableEntry(iNodeBuffer, Inumber, mode);
                table.add(entry);
                return entry;
            }
        } // end else
    } // end public synchronized FileTableEntry falloc( String filename, String mode )

    public synchronized boolean ffree( FileTableEntry entry ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
        entry.iNode.toDisk(entry.iNumber);
        return table.remove(entry);
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( );  // return if table is empty
    }                            // should be called before starting a format
}