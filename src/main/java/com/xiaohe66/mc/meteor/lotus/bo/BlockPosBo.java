package com.xiaohe66.mc.meteor.lotus.bo;

import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

public class BlockPosBo {
    public int x;
    public int y;
    public int z;

    public BlockPosBo(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void write(Writer writer) throws IOException {
        writer.write(this.x + "," + this.y + "," + this.z + "\n");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BlockPosBo)) {
            return false;
        }
        BlockPosBo that = (BlockPosBo)o;
        return this.x == that.x && this.y == that.y && this.z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }
}
