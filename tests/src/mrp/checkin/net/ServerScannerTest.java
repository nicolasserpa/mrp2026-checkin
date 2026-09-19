package mrp.checkin.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ServerScannerTest {

    @Test
    public void hostsCoverSubnetFor24() {
        List<String> hosts = ServerScanner.hostsInNetwork("10.162.200.55");
        assertEquals(254, hosts.size());
        assertEquals("10.162.200.1", hosts.get(0));
        assertEquals("10.162.200.254", hosts.get(253));
    }

    @Test
    public void hostsRejectMalformedInput() {
        assertTrue(ServerScanner.hostsInNetwork(null).isEmpty());
        assertTrue(ServerScanner.hostsInNetwork("not-an-ip").isEmpty());
        assertTrue(ServerScanner.hostsInNetwork("1.2.3.999").isEmpty());
        assertTrue(ServerScanner.hostsInNetwork("1.2.3").isEmpty());
    }

    @Test
    public void scanStopsAtFirstMatch() throws Exception {
        final AtomicInteger probes = new AtomicInteger();
        final AtomicReference<String> found = new AtomicReference<String>(null);
        final ServerScanner scanner = new ServerScanner();
        // Lista pequena: 3 miss + 1 hit no final (índice 3)
        List<String> hosts = Arrays.asList(
                "10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.99");
        final CountDownLatch done = new CountDownLatch(1);

        final Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                scanner.scan(hosts, new ServerScanner.HostChecker() {
                    @Override
                    public boolean isServer(String host) {
                        probes.incrementAndGet();
                        return "10.0.0.99".equals(host);
                    }
                }, new ServerScanner.Listener() {
                    @Override
                    public void onProgress(int checked, int total) {
                    }

                    @Override
                    public void onFound(String host) {
                        found.set(host);
                        done.countDown();
                    }

                    @Override
                    public void onNothing() {
                        done.countDown();
                    }
                });
            }
        });
        runner.start();
        assertTrue("scan deve achar o host", done.await(30, TimeUnit.SECONDS));
        assertEquals("10.0.0.99", found.get());
        assertTrue("não deve varrer tudo quando acha cedo",
                probes.get() <= 4);
        runner.join(2000);
    }

    @Test
    public void scanReportsNothingWhenNoMatch() throws Exception {
        final AtomicReference<Boolean> nothing = new AtomicReference<Boolean>(false);
        final ServerScanner scanner = new ServerScanner();
        // 3 hosts rápidos sem servidor
        List<String> hosts = Arrays.asList("10.0.0.1", "10.0.0.2", "10.0.0.3");
        final CountDownLatch done = new CountDownLatch(1);

        final Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                scanner.scan(hosts, new ServerScanner.HostChecker() {
                    @Override
                    public boolean isServer(String host) {
                        return false;
                    }
                }, new ServerScanner.Listener() {
                    @Override
                    public void onProgress(int checked, int total) {
                    }

                    @Override
                    public void onFound(String host) {
                        done.countDown();
                    }

                    @Override
                    public void onNothing() {
                        nothing.set(true);
                        done.countDown();
                    }
                });
            }
        });
        runner.start();
        assertTrue("scan deve terminar", done.await(30, TimeUnit.SECONDS));
        assertTrue(nothing.get());
        runner.join(2000);
    }
}