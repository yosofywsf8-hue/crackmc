package dev.crackmc.mod.tunnel;

import dev.crackmc.mod.CrackMCMod;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TunnelManager {

    private static final String DEFAULT_RELAY_HOST = "bore.pub";
    private static final int DEFAULT_RELAY_PORT = 7835;

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static volatile String publicAddress = null;
    private static Socket controlSocket;
    private static ExecutorService pool;

    public static boolean isRunning() {
        return running.get();
    }

    public static String getPublicAddress() {
        return publicAddress;
    }

    public static void startTunnel(int localPort, Consumer<String> onReady, Consumer<String> onError) {
        stopTunnel();
        running.set(true);
        pool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CrackMC-Tunnel");
            t.setDaemon(true);
            return t;
        });

        pool.submit(() -> {
            try {
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(DEFAULT_RELAY_HOST, DEFAULT_RELAY_PORT), 8000);
                controlSocket = sock;

                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
                OutputStream out = sock.getOutputStream();

                // Request port allocation from bore-compatible relay
                String hello = "{\"Hello\":0}\n";
                out.write(hello.getBytes(StandardCharsets.UTF_8));
                out.flush();

                String line = in.readLine();
                int remotePort = DEFAULT_RELAY_PORT;
                if (line != null && line.contains("Granted")) {
                    int idx = line.indexOf("Granted");
                    String sub = line.substring(idx);
                    String digits = sub.replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        remotePort = Integer.parseInt(digits);
                    }
                }

                publicAddress = DEFAULT_RELAY_HOST + ":" + remotePort;
                CrackMCMod.LOGGER.info("[CrackMC] Tunnel established at {}", publicAddress);
                onReady.accept(publicAddress);

                // Keepalive loop until stopped
                while (running.get() && !sock.isClosed()) {
                    String ping = "{\"Heartbeat\":0}\n";
                    out.write(ping.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                if (running.get()) {
                    running.set(false);
                    onError.accept(e.getMessage() != null ? e.getMessage() : "Connection failed");
                }
            }
        });
    }

    public static void stopTunnel() {
        running.set(false);
        publicAddress = null;
        if (controlSocket != null) {
            try {
                controlSocket.close();
            } catch (IOException ignored) {}
            controlSocket = null;
        }
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }
}
