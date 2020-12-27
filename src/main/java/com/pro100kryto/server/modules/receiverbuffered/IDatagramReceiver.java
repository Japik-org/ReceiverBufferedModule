package com.pro100kryto.server.modules.receiverbuffered;

import com.pro100kryto.server.IStartStopAlive;
import com.pro100kryto.server.logger.ILogger;
import com.pro100kryto.server.utils.datagram.packets.IPacketInProcess;

public interface IDatagramReceiver extends IStartStopAlive {
    ILogger getLogger();
    void putPacket(IPacketInProcess packet) throws IllegalStateException;
    boolean canPutPacket();
    boolean hasNext();
    IPacketInProcess getNext();
}
