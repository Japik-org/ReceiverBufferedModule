package com.pro100kryto.server.modules.receiverbuffered.connection;

import com.pro100kryto.server.module.IModuleConnection;
import com.pro100kryto.server.utils.datagram.packets.IPacket;
import com.sun.istack.Nullable;

public interface IReceiverBufferedModuleConnection extends IModuleConnection {
    boolean hasNextPacket();
    @Nullable
    IPacket getNextPacket();
}
