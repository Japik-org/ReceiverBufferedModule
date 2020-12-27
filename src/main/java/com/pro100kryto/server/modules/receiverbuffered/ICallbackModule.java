package com.pro100kryto.server.modules.receiverbuffered;

import com.pro100kryto.server.modules.packetpool.connection.IPacketPoolModuleConnection;

public interface ICallbackModule {
    IPacketPoolModuleConnection getPacketPool();
}