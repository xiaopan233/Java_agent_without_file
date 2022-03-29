package domain;

public class SymbolTableDo {
    public byte[] st_name = new byte[4];
    public byte[] st_info= new byte[1];
    public byte[] st_other = new byte[1];
    public byte[] st_shndx = new byte[2];
    public byte[] st_value = new byte[8];
    public byte[] st_size = new byte[8];

    /*
    * custom
    * */
    public long symbolTableName;
    public long type; /*2 FUNC*/
}
