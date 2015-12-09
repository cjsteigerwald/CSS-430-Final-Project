
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    // Directory entries
    private int fsize[];        // each element stores a different file size.
    private char fnames[][];    // each element stores a different file name.

    public Directory( int maxInumber ) { // directory constructor
        fsize = new int[maxInumber];     // maxInumber = max files
        for ( int i = 0; i < maxInumber; i++ )
            fsize[i] = 0;                 // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";                // entry(inode) 0 is "/"
        fsize[0] = root.length( );        // fsize[0] is the size of "/".
        root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
    }

    public int bytes2directory( byte data[] ) {
        // assumes data[] received directory information from disk
        // initializes the Directory instance with this data[]
        int spaceTracker;
        int i = 0;
        for (spaceTracker = 0; spaceTracker < fsize.length; spaceTracker += 4, i++)
        {
            fsize[i] = SysLib.bytes2int(data, spaceTracker);
        }
        i = 0;
        String theString;
        for ( ; spaceTracker < data.length; spaceTracker += maxChars, i++ )
        {
            theString = new String(data,spaceTracker, maxChars);
            fnames[i] = theString.toCharArray();
        }
        return 0;
    }

    public byte[] directory2bytes( ) {
        // converts and return Directory information into a plain byte array
        // this byte array will be written back to disk
        // note: only meaningfull directory information should be converted
        // into bytes.
        byte[] data = new byte [(fsize.length * 4) + (fnames.length * 30)];
        byte[] fileSize = new byte[4];
        int spaceTracker = 0;
        for (int i = 0; i < fsize.length; i++, spaceTracker += 4)
        {
            SysLib.int2bytes(fsize[i], data, spaceTracker);
        }
        for (int i = 0; i < fnames.length; i++, spaceTracker += maxChars)
        {
            String theString = new String(fnames[i], 0, fsize[i]);
            byte[] string2Bytes = theString.getBytes();
            System.arraycopy(data, spaceTracker, string2Bytes, 0, maxChars);
        }
        return data;
    }

    public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        if (namei(filename) == -1)
        {
            return findNextInode();
        }
        else
            return -1;
    }

    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if (iNumber > fsize.length || iNumber < 0 || fsize[iNumber] == 0)
        {
            return false;
        }
        else
        {
            fsize[iNumber] = 0;
            fnames[iNumber][0] = '0';
            for (int i = 0; i < maxChars; i++)
            {
                fnames[iNumber][i] = '0';
            }
            return true;
        }


    }

    public short namei( String filename ) {
        // returns the inumber corresponding to this filename
        String testString;
        for (short i = 0; i < fnames.length; i++)
        {
            testString = new String(fnames[i]);
            if (filename.equals(testString))
            {
                return i;
            }
        }
        return -1;
    }

    // returns next available iNode that is not used by iterating through the fsize looking for '0' indicating
    // an empty slot.
    // returns short : value of iNode
    public short findNextInode ()
    {
        for (short i = 0; i < fnames.length; i++)
        {
            if (fsize[i] == 0)
            {
                return i;
            }
        }
        return -1;
    } // end findNextInode
}