package domain;

public class ElfFileHeaderDO {
    public byte[] e_ident = new byte[16];
    public byte[] e_type = new byte[2];
    public byte[] e_machine = new byte[2];
    public byte[] e_version = new byte[4];
    public byte[] e_entry = new byte[8];
    public byte[] e_phoff = new byte[8];
    public byte[] e_shoff = new byte[8];
    public byte[] e_flags = new byte[4];
    public byte[] e_ehsize = new byte[2];
    public byte[] e_phentsize = new byte[2];
    public byte[] e_phnum = new byte[2];
    public byte[] e_shentsize = new byte[2];
    public byte[] e_shnum = new byte[2];
    public byte[] e_shstrndx = new byte[2];
    /*
    * cunstom data
    * */
    public long sectionHeaderSectionNameIndex;
    public long sectionHeaderNumber;
    public long sectionHeaderOffset;
}
