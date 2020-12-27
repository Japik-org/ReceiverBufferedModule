package com.pro100kryto.server.modules;

import com.pro100kryto.server.StartStopStatus;
import com.pro100kryto.server.logger.ILogger;
import com.pro100kryto.server.module.AModuleConnection;
import com.pro100kryto.server.module.IModuleConnectionSafe;
import com.pro100kryto.server.module.Module;
import com.pro100kryto.server.module.ModuleConnectionSafe;
import com.pro100kryto.server.modules.packetpool.connection.IPacketPoolModuleConnection;
import com.pro100kryto.server.modules.protocollitenetlib.connection.IProtocolModuleConnection;
import com.pro100kryto.server.modules.protocollitenetlib.connection.exceptions.AProtocolException;
import com.pro100kryto.server.modules.receiverbuffered.DatagramReceiver;
import com.pro100kryto.server.modules.receiverbuffered.ICallbackModule;
import com.pro100kryto.server.modules.receiverbuffered.connection.IReceiverBufferedModuleConnection;
import com.pro100kryto.server.service.IServiceControl;
import com.pro100kryto.server.utils.datagram.packets.IPacket;
import com.pro100kryto.server.utils.datagram.packets.IPacketInProcess;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ReceiverBufferedModule extends Module implements ICallbackModule {
    private DatagramReceiver receiver;
    private IModuleConnectionSafe<IProtocolModuleConnection> protocolModuleConnection;
    private IModuleConnectionSafe<IPacketPoolModuleConnection> packetPoolModuleConnection;

    private BlockingQueue<IPacketInProcess> packetBuffer;


    public ReceiverBufferedModule(IServiceControl service, String name) {
        super(service, name);
    }

    @Override
    protected void startAction() throws Throwable {
        try {
            if (moduleConnection == null) moduleConnection = new ReceiverModuleConnection(logger);
        } catch (Throwable throwable){
            logger.writeError("failed create ReceiverModuleConnection");
            throw throwable;
        }

        String protocolModuleName = settings.getOrDefault("protocol-module-name", "Protocol");
        protocolModuleConnection = new ModuleConnectionSafe<>(service, protocolModuleName);

        String packetPoolModuleName = settings.getOrDefault("packetpool-module-name", "PacketPool");
        packetPoolModuleConnection = new ModuleConnectionSafe<>(service, packetPoolModuleName);

        int socketTimeout = Integer.parseInt(
                settings.getOrDefault("socket-timeout", "1500"));
        int port = Integer.parseInt(
                settings.getOrDefault("socket-port", "49300"));
        int packetBufferSize = Integer.parseInt(
                settings.getOrDefault("packetbuffer-size", "128"));

        packetBuffer = new ArrayBlockingQueue<>(packetBufferSize);

        receiver = new DatagramReceiver(this,
                port, socketTimeout,
                packetBuffer, logger);
        receiver.start();
    }

    @Override
    protected void stopAction(boolean force) throws Throwable {
        receiver.stop(force);
        receiver = null;

        for (IPacketInProcess packetInProcess : packetBuffer)
            if (!packetInProcess.isRecycled())
                packetInProcess.recycle();

        protocolModuleConnection = null;
        packetPoolModuleConnection = null;

        packetBuffer = null;
    }

    @Override
    public void tick() throws Throwable {
        receiver.tick();
    }

    // -------- callback

    @Override
    public IPacketPoolModuleConnection getPacketPool() {
        return packetPoolModuleConnection.getModuleConnection();
    }

    // ---------

    private class ReceiverModuleConnection extends AModuleConnection implements IReceiverBufferedModuleConnection {

        public ReceiverModuleConnection(ILogger logger){
            super(logger, name, type);
        }

        @Override
        public boolean isAliveModule() {
            return getStatus() == StartStopStatus.STARTED;
        }

        // ---------------

        @Override
        public boolean hasNextPacket() {
            try {
                return !packetBuffer.isEmpty();
            } catch (NullPointerException ignored){
            }
            return false;
        }

        @Override
        public IPacket getNextPacket() {
            try{
                IPacketInProcess packetInProcess = packetBuffer.poll();
                Objects.requireNonNull(packetInProcess);
                protocolModuleConnection.getModuleConnection().processPacketOnReceive(packetInProcess);
                return packetInProcess.convertToFinalPacket();

            } catch (AProtocolException protocolException){
                return getNextPacket();

            } catch (Throwable ignored){
            }

            return null;
        }
    }
}
