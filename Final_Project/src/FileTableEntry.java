/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Chris Steigerwald, Hunter Grayson, Michael Voight
 * @last edit: 12/6/2015
 *
 * This is an Entry for FileTable (struct) that holds the following items:
 *      seekPtr: a file seek pointer
 *      iNode: a reference to its inode
 *      iNumber: this inode number
 *      count: # threads sharing this entry
 *      mode: "r" read, "w" write, "w+" write/read, "a" append to end of file, once set never changes
 */

public class FileTableEntry
{
    public int seekPtr;
    public final Inode iNode;
    public final short iNumber;
    public int count;
    public final String mode;

    /**
     * FileTableEntry ( Inode i, short iNumber, String m )
     * Overloaded FileTableEntry- sets class variables and objects to values passed in.
     * @param iNode
     * @param iNumber
     * @param mode
     */
    public FileTableEntry ( Inode iNode, short iNumber, String mode ) {
        this.seekPtr = 0;
        this.iNode = iNode;
        this.iNumber = iNumber;
        this.count = 1;
        this.mode = mode;
        if ( mode.compareTo( "a" ) == 0 )
            seekPtr = iNode.fileSize;
    }
}