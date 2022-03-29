package domain;

public class SectionHeaderDo {
    public byte[] sh_name = new byte[4];
    public byte[] sh_type = new byte[4];
    public byte[] sh_flags = new byte[8];
    public byte[] sh_addr = new byte[8];
    public byte[] sh_offset = new byte[8];
    public byte[] sh_size = new byte[8];
    public byte[] sh_link = new byte[4];
    public byte[] sh_info = new byte[4];
    public byte[] sh_addralign = new byte[8];
    public byte[] sh_entsize = new byte[8];
    /*
    * custom
    * */
    public long sectionHeaderName;
    public long sectionHeaderOffset;
    public long sectionHeaderSize;

}
