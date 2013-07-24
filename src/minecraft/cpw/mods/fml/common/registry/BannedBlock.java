package cpw.mods.fml.common.registry;

public class BannedBlock {

    public final int blockID;
    public final int meta;

    public BannedBlock(int blockID, int meta)
    {
        this.blockID = blockID;
        this.meta = meta;
    }
}
