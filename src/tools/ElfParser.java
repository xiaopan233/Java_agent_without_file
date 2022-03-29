package tools;

import domain.ElfFileHeaderDO;
import domain.SectionHeaderDo;
import domain.SymbolTableDo;

import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class ElfParser {
    private String fileName;
    private RandomAccessFile randomAccessFile;
    private ElfFileHeaderDO elfFileHeaderDO;
    private HashMap<String, SectionHeaderDo> sectionHeaderDoHashMap;
    private boolean encoding; /* true little-endian; false big-endian */

    private byte[] sectionTableString;
    private byte[] symStrTableString;
    private byte[] dynSymTableString;

    public ElfParser(String fileName) throws Exception{
        this.fileName = fileName;
        this.randomAccessFile = new RandomAccessFile(fileName, "r");
        this.elfFileHeaderDO = new ElfFileHeaderDO();
        this.sectionHeaderDoHashMap = new HashMap<>();
        //parse file header
        fileHeaderParser();
        sectionHeaderParser();

        //parse symbol table names string and dynsym table names string
        stringTableParser(sectionHeaderDoHashMap.get(".strtab"), "symStrTableString");
        stringTableParser(sectionHeaderDoHashMap.get(".dynstr"), "dynSymTableString");
    }

    private void fileHeaderParser() throws Exception{
        randomAccessFile.seek(0);
        randomAccessFile.read(elfFileHeaderDO.e_ident);
        randomAccessFile.read(elfFileHeaderDO.e_type);
        randomAccessFile.read(elfFileHeaderDO.e_machine);
        randomAccessFile.read(elfFileHeaderDO.e_version);
        randomAccessFile.read(elfFileHeaderDO.e_entry);
        randomAccessFile.read(elfFileHeaderDO.e_phoff);
        randomAccessFile.read(elfFileHeaderDO.e_shoff);
        randomAccessFile.read(elfFileHeaderDO.e_flags);
        randomAccessFile.read(elfFileHeaderDO.e_ehsize);
        randomAccessFile.read(elfFileHeaderDO.e_phentsize);
        randomAccessFile.read(elfFileHeaderDO.e_phnum);
        randomAccessFile.read(elfFileHeaderDO.e_shentsize);
        randomAccessFile.read(elfFileHeaderDO.e_shnum);
        randomAccessFile.read(elfFileHeaderDO.e_shstrndx);

        encoding = elfFileHeaderDO.e_ident[5] == 0x1;

        elfFileHeaderDO.sectionHeaderSectionNameIndex = byteToLong(elfFileHeaderDO.e_shstrndx, encoding);
        elfFileHeaderDO.sectionHeaderOffset = byteToLong(elfFileHeaderDO.e_shoff, encoding);
        elfFileHeaderDO.sectionHeaderNumber = byteToLong(elfFileHeaderDO.e_shnum, encoding);
    }

    private void sectionHeaderParser() throws Exception{
        long filePoint = elfFileHeaderDO.sectionHeaderOffset;
        ArrayList<SectionHeaderDo> sectionHeaderDoArrayList = new ArrayList<>();

        for (int i = 1; i <= elfFileHeaderDO.sectionHeaderNumber; i++){
            SectionHeaderDo sectionHeader = new SectionHeaderDo();
            randomAccessFile.seek(filePoint);

            randomAccessFile.read(sectionHeader.sh_name);
            randomAccessFile.read(sectionHeader.sh_type);
            randomAccessFile.read(sectionHeader.sh_flags);
            randomAccessFile.read(sectionHeader.sh_addr);
            randomAccessFile.read(sectionHeader.sh_offset);
            randomAccessFile.read(sectionHeader.sh_size);
            randomAccessFile.read(sectionHeader.sh_link);
            randomAccessFile.read(sectionHeader.sh_info);
            randomAccessFile.read(sectionHeader.sh_addralign);
            randomAccessFile.read(sectionHeader.sh_entsize);

            //byte[] to long
            sectionHeader.sectionHeaderOffset = byteToLong(sectionHeader.sh_offset, encoding);
            sectionHeader.sectionHeaderSize = byteToLong(sectionHeader.sh_size, encoding);
            sectionHeader.sectionHeaderName = byteToLong(sectionHeader.sh_name, encoding);

            filePoint = randomAccessFile.getFilePointer();
            sectionHeaderDoArrayList.add(sectionHeader);
        }

        //Get section names table
        SectionHeaderDo sectionNamesTable = sectionHeaderDoArrayList.get((int) elfFileHeaderDO.sectionHeaderSectionNameIndex);
        stringTableParser(sectionNamesTable, "sectionTableString");

        //Re-composer section tables with names
        for (SectionHeaderDo sectionHeaderDo : sectionHeaderDoArrayList) {
            String tableNameString = addressAssociateString(sectionHeaderDo.sectionHeaderName, "sectionTableString");
            sectionHeaderDoHashMap.put(tableNameString, sectionHeaderDo);
        }
    }



    public SymbolTableDo findSymbolTable(String symbolName, String tableName) throws Exception{
        String strTableName;
        String strTableString;
        if (tableName.equals(".symtab")){
            strTableName = ".strtab";
            strTableString = "symStrTableString";
        }else if (tableName.equals(".dynsym")){
            strTableName = ".dynstr";
            strTableString = "dynSymTableString";
        }else{
            throw new Exception("[-] Symtable name error");
        }
        SectionHeaderDo symbolTableSectionHeader = sectionHeaderDoHashMap.get(tableName);
        SectionHeaderDo stringTableSectionHeader = sectionHeaderDoHashMap.get(strTableName);
        SymbolTableDo resSymbolTableDo = new SymbolTableDo();

        //size and offset
        long symbolTableOffset = byteToLong(symbolTableSectionHeader.sh_offset, encoding);
        long number = byteToLong(symbolTableSectionHeader.sh_size, encoding) / byteToLong(symbolTableSectionHeader.sh_entsize, encoding);
        randomAccessFile.seek(symbolTableOffset);

        for (int i = 1; i <= number; i++) {
            randomAccessFile.read(resSymbolTableDo.st_name);
            randomAccessFile.read(resSymbolTableDo.st_info);
            randomAccessFile.read(resSymbolTableDo.st_other);
            randomAccessFile.read(resSymbolTableDo.st_shndx);
            randomAccessFile.read(resSymbolTableDo.st_value);
            randomAccessFile.read(resSymbolTableDo.st_size);
            resSymbolTableDo.symbolTableName = byteToLong(resSymbolTableDo.st_name, encoding);

            //Just parse FUNC
            long type = byteToLong(resSymbolTableDo.st_info, encoding) & 0xf;
            if (type != 2L){
                continue;
            }
            String name = addressAssociateString(resSymbolTableDo.symbolTableName, strTableString);
            if (symbolName.equals(name)){
                break;
            }
        }
        return resSymbolTableDo;
    }

    private void stringTableParser(SectionHeaderDo namesTable, String tableString) throws Exception{
        //section names string
        byte[] sectionTableString = new byte[(int) namesTable.sectionHeaderSize];
        randomAccessFile.seek(namesTable.sectionHeaderOffset);
        randomAccessFile.read(sectionTableString);

        Field sectionTableStringField = this.getClass().getDeclaredField(tableString);
        sectionTableStringField.setAccessible(true);
        sectionTableStringField.set(this, sectionTableString);
    }


    public static long byteToLong(byte[] b0, boolean byteEncoding){
        byte[] b;
        if (b0.length < 8){

            b = new byte[8];
        }else{
            b = new byte[b0.length];
        }
        System.arraycopy(b0, 0, b, 0, b0.length);

        ByteBuffer wrap = ByteBuffer.wrap(b);
        ByteBuffer order;

        //little-endian
        if (byteEncoding){
            order = wrap.order(ByteOrder.LITTLE_ENDIAN);
        }
        //big-endian
        else {
            order = wrap.order(ByteOrder.BIG_ENDIAN);
        }
        return order.getLong();
    }

    private String addressAssociateString(long stringOffset, String tableString) throws Exception{
        int readOffset = (int) stringOffset;
        StringBuilder stringBuilder = new StringBuilder();

        //which table name string
        Field tableStringField = this.getClass().getDeclaredField(tableString);
        tableStringField.setAccessible(true);
        byte[] tableStringBytes = (byte[]) tableStringField.get(this);

        while (true){
            byte b = tableStringBytes[readOffset];
            if (b == 0x00){
                break;
            }
            char c = (char) b;
            stringBuilder.append(c);
            readOffset++;
        }
        return stringBuilder.toString();
    }
}
