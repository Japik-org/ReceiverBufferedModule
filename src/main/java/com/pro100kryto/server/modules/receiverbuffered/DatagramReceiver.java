package com.pro100kryto.server.modules.receiverbuffered;

import com.pro100kryto.server.IStartStopAlive;
import com.pro100kryto.server.StartStopStatus;
import com.pro100kryto.server.logger.ILogger;
import com.pro100kryto.server.utils.datagram.packets.IPacketInProcess;

import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatagramReceiver implements IStartStopAlive {
    private final ICallbackModule callbackModule;
    private final ILogger logger;
    private final BlockingQueue<IPacketInProcess> packetBuffer; // thread-safe ok
    private final int socketTimeout;
    private DatagramSocket datagramSocket;
    private final int port;
    private StartStopStatus status;

    public DatagramReceiver(ICallbackModule callbackModule,
                            int port,
                            int socketTimeout,
                            BlockingQueue<IPacketInProcess> packetBuffer,
                            ILogger logger){

        this.callbackModule = callbackModule;
        this.port = port;
        this.socketTimeout = socketTimeout;
        this.logger = logger;
        this.packetBuffer = packetBuffer;
    }

    @Override
    public void start() throws Throwable {
        if (status!=StartStopStatus.STOPPED) throw new IllegalStateException("Is not stopped");
        status = StartStopStatus.STARTING;

        datagramSocket = new DatagramSocket(port);
        datagramSocket.setSoTimeout(socketTimeout);

        status = StartStopStatus.STARTED;
    }

    @Override
    public void stop(boolean force) throws Throwable{
        datagramSocket.close();
        status = StartStopStatus.STOPPED;
    }

    @Override
    public StartStopStatus getStatus() {
        return status;
    }

    public void tick() throws Throwable{
        IPacketInProcess packetInProcess = callbackModule.getPacketPool().getNextPacket();
        try {
            try {
                packetInProcess.receive(datagramSocket);
                if (!packetBuffer.offer(packetInProcess, socketTimeout, TimeUnit.MILLISECONDS)) {
                    logger.writeWarn("packetBuffer is full");
                }

                return;
            } catch (NullPointerException nullPointerException){
               logger.writeWarn("PacketPool is empty");

            } catch (SocketTimeoutException ignored){
            }

            packetInProcess.recycle();

        } catch (Throwable throwable){
            if (packetInProcess!=null) packetInProcess.recycle();
            throw throwable;
        }
    }
}
