package tools;

import java.io.RandomAccessFile;

public class MapsParser {
    private RandomAccessFile mapsReader;

    public MapsParser() throws Exception{
        mapsReader = new RandomAccessFile("/proc/self/maps", "r");
    }

    public long getLibMemoryAddress(String libname) throws Exception{
        mapsReader.seek(0L);
        long libMemeryAddress = 0L;
        String procSelfMem;

        while((procSelfMem = mapsReader.readLine()) != null) {
            if (procSelfMem.contains(libname)) {
                String[] address = procSelfMem.split(" ");
                String[] addressArr1 = address[0].split("-");
                libMemeryAddress = Long.valueOf(addressArr1[0], 16);
                break;
            }
        }
        if (libMemeryAddress == 0L){
            throw new Exception("[-] maps parser error!");
        }
        return libMemeryAddress;
    }

    public void close() throws Exception{
        mapsReader.close();
    }
}
