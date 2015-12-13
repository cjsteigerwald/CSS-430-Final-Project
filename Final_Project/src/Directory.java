/**
 * @Project: ${PACKAGE_NAME}
 * @file: ${FILE_NAME}
 * @author: Chris Steigerwald, Hunter Grayson, Chris Steigerwald, Michael Voight
 * @last edit: 12/6/2015
 *
 * The "/" root directory maintains each file in a different directory entry that contains its file name
 * (in maximum 30 characters = in max. 60 bytes in Java) and the corresponding Inode number. The directory
 * receives the maximum number of Inodes to be created, (i.e., thus the max. number of files to be created)
 * and keeps track of which Inode numbers (iNumbers) are in use. Since the directory itself is considered as
 * a file, its contents are maintained by an Inode, specifically saying Inode 0. This can be located in the
 * first 32 bytes of the disk block 1.
 *
 * Upon a boot, the file system instantiates the following Directory class as the root directory through its
 * constructor, reads the file from the disk that can be found at Inode 0 (the first 32 bytes of the disk block 1),
 * and initializes the Directory instance with the file contents. Upon shutdown, the file system must write back
 * the Directory information onto the disk. The bytes2directory() method will initialize the Directory instance
 * with a byte array read from the disk and the directory2bytes() method converts the Directory instance into a
 * byte array that can be written back to the disk.
 *
 */

/**
 * Directory class
 */
public class Directory
{
    private static int maxChars = 30; // max characters of each file name
    private int fileSize[];
    private char fileName[][];

    /**
     * Directory( int maxInumber )
     * Default constructor for Directory class takes in the maximum number of inodes and instantiates two arrays:
     *          fileSize: an array to hold size of files read in
     *          fileName: an array for holding file name
     * These arrays correspond to the inode number
     * @param maxInumber
     */
    public Directory( int maxInumber )
    {
        fileSize = new int[maxInumber];
        for ( int i = 0; i < maxInumber; i++ )
            fileSize[i] = 0;
        fileName = new char[maxInumber][maxChars];
        String root = "/";
        fileSize[0] = root.length( );
        root.getChars( 0, fileSize[0], fileName[0], 0 );
    }

    /**
     * bytes2directory( byte byteData[] )
     * Assumes data[] received directory information from disk initializes the Directory instance with this data
     * @param byteData
     * @return int returns 0 upon success
     */
    public int bytes2directory( byte byteData[] )
    {
        int set = 0;
        for (int i = 0; i < fileName.length; i++)
        {
            set = i * 4;
            fileSize[i] = SysLib.bytes2int(byteData, set);
        }
        set += 4;
        for (int i = 0; i < fileName.length; i++)
        {
            String name = new String(byteData, set + (i * (2 * maxChars)), 2 * maxChars);
            for (int j = 0; j < fileSize[i]; j++)
                fileName[i][j] = name.charAt(j);
        }
        return 0;
    }

    /**
     * directory2bytes()
     * Converts Directory information into a plain bute array and returns it
     * this byte array will be written back to disk
     * note: only meaning ful directory information should be converted into bytes
     * @return byte[] array of byte data that is the directory
     */
    public byte[] directory2bytes( )
    {
        byte[] directoryBytes = new byte[(4 * fileName.length) + (fileName.length * (2 * maxChars))];
        int offset = 0;
        for (int i = 0; i < fileName.length; i++)
        {
            SysLib.int2bytes(fileSize[i], directoryBytes, offset);
            offset += 4;
        }
        for (int i = 0; i < fileName.length; i++)
        {
            String theName = new String(fileName[i], 0, fileSize[i]);
            byte[] nameHolder = theName.getBytes();
            System.arraycopy(nameHolder, 0, directoryBytes, offset, nameHolder.length);
            offset += (maxChars * 2);
        }
        return directoryBytes;
    }

    /**
     * The name is the filename of a file to be created. Allocates a new inode number for this filename
     * @param name
     * @return short returns inode number associated with filename upon success, else 0 is failure
     */
    public short iAlloc( String name )
    {
        for (int i = 0; i < fileName.length; i++) {
            if (fileSize[i] == 0) {
                fileSize[i] = name.length();
                for (int j = 0; j < fileSize[i]; j++)
                {
                    fileName[i][j] = name.charAt(j);
                }
                return (short)i;
            }
        }
        return 0;
    }

    /**
     * iFree(short iNumber)
     * This method frees an inode from disk allowing it to be reused by the system
     * @param iNumber
     * @return boolean True is inode freed, else false is failure
     */
    public boolean iFree( short iNumber )
    {
        if (iNumber > fileSize.length || iNumber < 0 || fileSize[iNumber] == 0)
        {
            return false;
        }
        else
        {
            fileSize[iNumber] = 0;
            fileName[iNumber][0] = '0';
            for (int i = 0; i < maxChars; i++)
            {
                fileName[iNumber][i] = '0';
            }
            return true;
        }
    }

    /**
     * namei(String name)
     * returns the inumber corresponding to this filename
     * @param name
     * @return
     */
    public short namei( String name )
    {
        for (short i = 0; i < fileName.length; i++) {
            String testedName = new String(fileName[i], 0, fileSize[i]);
            if (fileSize[i] > 0 && name.equalsIgnoreCase(testedName))
            {
                return i;
            }
        }
        return -1;
    }
}