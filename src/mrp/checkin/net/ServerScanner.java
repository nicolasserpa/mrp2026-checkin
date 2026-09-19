package mrp.checkin.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Varre a sub-rede local atrás de um servidor da API. Bloqueante: deve ser
 * chamado fora da UI thread. Progresso e resultado chegam via {@link Listener}.
 *
 * <p>A varredura é cooperativamente cancelável ({@link #stop()}): se o usuário
 * sair da tela, o scan termina no próximo host.</p>
 *
 * <p>Algoritmo testável na JVM: {@link #hostsInNetwork(String)} (enumeração pura
 * dos IPs do /24) está separado do probing de rede ({@link HostChecker});
 * os testes injetam um checker fake.</p>
 */
public final class ServerScanner {
    /** Diz se um host (ex.: "192.168.0.12") é o servidor da API. */
    public interface HostChecker {
        /** Bloqueante. Deve respeitar o próprio timeout curto. */
        boolean isServer(String host);
    }

    public interface Listener {
        /** Chamada a cada host verificado. host -> "10.162.200.x". */
        void onProgress(int checked, int total);

        /** Servidor encontrado. */
        void onFound(String host);

        /** Nenhum encontrado (ou varredura interrompida). */
        void onNothing();
    }

    private static final int WORKERS = 16;
    private static final long HOST_TIMEOUT_MS = 1500;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Enumera os hosts do mesmo /24 de um IPv4. Ex.: "10.162.200.55" produz
     * "10.162.200.1".."10.162.200.254" (pula .0 e .255). Não depende de rede.
     */
    public static List<String> hostsInNetwork(String localIp) {
        List<String> out = new ArrayList<String>();
        if (localIp == null) {
            return out;
        }
        String[] parts = localIp.split("\\.");
        if (parts.length != 4) {
            return out;
        }
        for (int i = 0; i < 4; i++) {
            int v;
            try {
                v = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return out;
            }
            if (v < 0 || v > 255) {
                return out;
            }
        }
        String prefix = parts[0] + "." + parts[1] + "." + parts[2] + ".";
        for (int h = 1; h <= 254; h++) {
            out.add(prefix + h);
        }
        return out;
    }

    /** IPv4 da interface ativa (a 1ª com endereço global não-loopback). */
    public static String localIpv4() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis)) {
                if (!ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                for (InetAddress a : Collections.list(addrs)) {
                    if (a.isLoopbackAddress() || a.isLinkLocalAddress()
                            || a.isMulticastAddress() || !(a instanceof java.net.Inet4Address)) {
                        continue;
                    }
                    return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Coopera, não corta: o host em andamento termina no timeout. */
    public void stop() {
        stopRequested.set(true);
    }

    public void scan(List<String> hosts, final HostChecker checker, final Listener listener) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        listener.onProgress(0, hosts.size());
        ExecutorService pool = newDaemonPool(WORKERS);
        try {
            CompletionService<String> cs = new ExecutorCompletionService<String>(pool);
            int submitted = 0;
            for (final String host : hosts) {
                if (stopRequested.get()) {
                    break;
                }
                cs.submit(new Callable<String>() {
                    @Override
                    public String call() {
                        if (stopRequested.get()) {
                            return null;
                        }
                        return checkHost(host, checker) ? host : null;
                    }
                });
                submitted++;
            }
            int done = 0;
            while (done < submitted) {
                if (stopRequested.get()) {
                    break;
                }
                try {
                    Future<String> f = cs.poll(200, TimeUnit.MILLISECONDS);
                    if (f == null) {
                        continue;
                    }
                    done++;
                    String host = f.get();
                    listener.onProgress(done, hosts.size());
                    if (host != null) {
                        listener.onFound(host);
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            listener.onNothing();
        } finally {
            pool.shutdownNow();
            stopRequested.set(false);
            running.set(false);
        }
    }

    /** Probe com timeout próprio: nunca prende o worker além de HOST_TIMEOUT_MS. */
    private boolean checkHost(String host, HostChecker checker) {
        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicBoolean result = new AtomicBoolean(false);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result.set(checker.isServer(host));
                } catch (Exception ignored) {
                } finally {
                    done.set(true);
                }
            }
        }, "netprobe-" + host);
        t.setDaemon(true);
        t.start();
        long deadline = System.currentTimeMillis() + HOST_TIMEOUT_MS;
        while (!done.get() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return done.get() ? result.get() : false;
    }

    private static ExecutorService newDaemonPool(int n) {
        return Executors.newFixedThreadPool(n, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "netscanner-worker");
                t.setDaemon(true);
                return t;
            }
        });
    }
}