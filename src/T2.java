import com.Demo;
import domain.SymbolTableDo;
import tools.ElfParser;
import tools.LibPathFinder;
import tools.MapsParser;
import tools.Memory;

import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;

public class T2 {
    public static void main(String[] args) throws Exception{
        MapsParser mapsParser = new MapsParser();
        long libjvmBaseAddress = mapsParser.getLibMemoryAddress("libjvm.so");
        long libjavaBaseAddress = mapsParser.getLibMemoryAddress("libjava.so");
        mapsParser.close();

        String libPath = LibPathFinder.getLibPath();
        ElfParser libjvmElfParser = new ElfParser(libPath + "/server/libjvm.so");
        SymbolTableDo jni_getCreatedJavaVMsSymbolTableDo = libjvmElfParser.findSymbolTable("JNI_GetCreatedJavaVMs", ".dynsym");
        long jni_getCreatedJavaVMsOffset = ElfParser.byteToLong(jni_getCreatedJavaVMsSymbolTableDo.st_value, true);
        long jni_getCreatedJavaVMsAddress = libjvmBaseAddress + jni_getCreatedJavaVMsOffset;
        System.out.println("[+] libjvm.so JNI_GetCreatedJavaVMs address is: 0x" + Long.toHexString(jni_getCreatedJavaVMsAddress));

        ElfParser libjavaElfParser = new ElfParser(libPath + "/libjava.so");
        SymbolTableDo java_java_io_fileInputStream_skip0SymbolTableDo = libjavaElfParser.findSymbolTable("Java_java_io_FileInputStream_skip0", ".symtab");
        long java_java_io_fileInputStream_skip0SymbolTableDoOffset = ElfParser.byteToLong(java_java_io_fileInputStream_skip0SymbolTableDo.st_value, true);
        long java_java_io_fileInputStream_skip0SymbolTableDoAddress = libjavaBaseAddress + java_java_io_fileInputStream_skip0SymbolTableDoOffset;
        System.out.println("[+] libjava.so Java_java_io_FileInputStream_skip0 address is: 0x" + Long.toHexString(java_java_io_fileInputStream_skip0SymbolTableDoAddress));

        Memory memory = new Memory(jni_getCreatedJavaVMsAddress);
        memory.read(java_java_io_fileInputStream_skip0SymbolTableDoAddress);
        memory.write(java_java_io_fileInputStream_skip0SymbolTableDoAddress, true);

        FileInputStream fileInputStream = new FileInputStream("/proc/self/maps");
        long jvmtiEnvPointer = fileInputStream.skip(1L);

        memory.write(java_java_io_fileInputStream_skip0SymbolTableDoAddress, false);
        memory.close();

        System.out.println("[+] jvmtiEnvPointer: 0x" + Long.toHexString(jvmtiEnvPointer));

        long JPLISAgent = memory.GenerateJPLISAgent(jvmtiEnvPointer);

        /*
        * Test no file Java Agent
        * */
        Demo demo = new Demo();
        demo.print();

        URL demoClassURL = T2.class.getResource("/Demo.class");
        String demoClassURLPathath = demoClassURL.getPath();
        byte[] evilClassClassBytes = readFile(demoClassURLPathath);

        /*
        * InstrumentationImpl.redefineClasses()
        * */
        Class instrumentationImplClass = Class.forName("sun.instrument.InstrumentationImpl");
        Constructor instrumentationImplConstructor = instrumentationImplClass.getDeclaredConstructor(long.class, boolean.class, boolean.class);
        instrumentationImplConstructor.setAccessible(true);
        Object instrumentationImpl = instrumentationImplConstructor.newInstance(JPLISAgent, true, false);

        ClassDefinition[] classDefinitions = new ClassDefinition[1];
        classDefinitions[0] = new ClassDefinition(com.Demo.class, evilClassClassBytes);

        Method redefineClassesMethod = instrumentationImplClass.getDeclaredMethod("redefineClasses", ClassDefinition[].class);
        redefineClassesMethod.setAccessible(true);
        redefineClassesMethod.invoke(instrumentationImpl, new Object[]{classDefinitions});

        demo.print();
    }

    private static byte[] readFile(String path) throws Exception{
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] b = new byte[(int)file.length()];
        int off = 0;
        int step = 200;
        int tmp = 0;
        while (true){
            if(off+step > b.length){
                tmp = b.length - off;
                fileInputStream.read(b,off,tmp);
                break;
            }
            tmp=fileInputStream.read(b,off,step);
            off += tmp;
        }
        return b;
    }
}
