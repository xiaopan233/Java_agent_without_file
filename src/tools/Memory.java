package tools;

import sun.misc.Unsafe;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;

public class Memory {
    private RandomAccessFile memoryIO;
    private byte[] codeOverwrite;
    private byte[] codeOriginal;

    public Memory(long jni_getCreatedJavaVMsAddress) throws Exception{
        memoryIO = new RandomAccessFile("/proc/self/mem", "rw");

        byte[] codeInsert1 = new byte[]{
                (byte)0x55,(byte)0x48,(byte)0x89,(byte)0xe5,(byte)0x6a,(byte)0x01,(byte)0x48,(byte)0x83,(byte)0xec,(byte)0x18,(byte)0x48,(byte)0x8d,(byte)0x54,(byte)0x24,(byte)0x18,(byte)0x48,(byte)0xc7,(byte)0xc6,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x48,(byte)0x8d,(byte)0x7c,(byte)0x24,(byte)0x10,(byte)0x48,(byte)0xb8
        };

        byte[] codeInsert2 = new byte[8];
        String hexString = Long.toHexString(jni_getCreatedJavaVMsAddress);
        byte[] codeInsert3 = new byte[]{
                (byte)0xff,(byte)0xd0,(byte)0x48,(byte)0xc7,(byte)0xc2,(byte)0x00,(byte)0x02,(byte)0x01,(byte)0x30,(byte)0x4c,(byte)0x8d,(byte)0x44,(byte)0x24,(byte)0x08,(byte)0x4c,(byte)0x89,(byte)0x04,(byte)0x24,(byte)0x48,(byte)0x8d,(byte)0x34,(byte)0x24,(byte)0x48,(byte)0x8b,(byte)0x7c,(byte)0x24,(byte)0x10,(byte)0x4c,(byte)0x8b,(byte)0x44,(byte)0x24,(byte)0x10,(byte)0x4d,(byte)0x8b,(byte)0x08,(byte)0x4d,(byte)0x8b,(byte)0x41,(byte)0x30,(byte)0x41,(byte)0xff,(byte)0xd0,(byte)0x48,(byte)0x8b,(byte)0x04,(byte)0x24,(byte)0x48,(byte)0x83,(byte)0xc4,(byte)0x20,(byte)0x5d,(byte)0xc3
        };
        int codeInsert2Offset = 0;
        for (int i = hexString.length(); i >= 2; i-=2) {
            String substring = hexString.substring(i - 2, i);
            Integer decode = Integer.decode("0x" + substring);
            byte aByte = decode.byteValue();
            codeInsert2[codeInsert2Offset++] = aByte;
        }

        codeOverwrite = new byte[codeInsert1.length + codeInsert2.length + codeInsert3.length];
        codeOriginal = new byte[codeOverwrite.length];
        System.arraycopy(codeInsert1, 0, codeOverwrite, 0, codeInsert1.length);
        System.arraycopy(codeInsert2, 0, codeOverwrite, codeInsert1.length, codeInsert2.length);
        System.arraycopy(codeInsert3, 0, codeOverwrite, codeInsert1.length+codeInsert2.length, codeInsert3.length);
    }

    /*
    * read to codeOriginal for resume
    * */
    public void read(long offset) throws Exception{
        memoryIO.seek(offset);
        memoryIO.read(codeOriginal);
    }

    /*
     * put codeOverwrite to memory
     * */
    public void write(long offset, boolean overwrite) throws Exception{
        memoryIO.seek(offset);
        if (overwrite)
            memoryIO.write(codeOverwrite);
        else
            memoryIO.write(codeOriginal);
    }

    public long GenerateJPLISAgent(long jvmtiEnv) throws Exception{
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);

        long JPLISAgent = unsafe.allocateMemory(25L);
        //JavaVM*
        unsafe.putLong(JPLISAgent, 0L);

        //_JPLISEnvironment
        unsafe.putLong(JPLISAgent+8L, jvmtiEnv);
        unsafe.putLong(JPLISAgent+16L, JPLISAgent);
        unsafe.putByte(JPLISAgent+24L, (byte)0x1);
        //can_redefine_classes
        unsafe.putByte(jvmtiEnv + 361L, (byte)0x2);
        //jdk11.0.13, jdk12.0.2 377
        //jdk 1.8.202, jdk10.0.2 361
        return JPLISAgent;
    }

    public void close() throws Exception{
        memoryIO.close();
    }
}
