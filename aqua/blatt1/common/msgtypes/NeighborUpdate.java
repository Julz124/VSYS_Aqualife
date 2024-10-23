package aqua.blatt1.common.msgtypes;

import aqua.blatt1.common.Direction;

import java.io.Serializable;
import java.net.InetSocketAddress;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {
    private final InetSocketAddress neighbor;
    private final Direction direction;

    public NeighborUpdate(InetSocketAddress neighbor, Direction direction) {
        this.neighbor = neighbor;
        this.direction = direction;
    }

    public InetSocketAddress getAddress() {
        return this.neighbor;
    }

    public Direction getDirection() {
        return this.direction;
    }
}
