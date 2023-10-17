package it.cnr.ilc.texto.manager;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 *
 * @author oakgen
 */
@Component
public class MonitorManager extends Manager {

    private final Map<Thread, String> threads = new ConcurrentHashMap<>();
    private final Map<String, Monitor> monitors = new HashMap<>();

    public void startRequest(HttpServletRequest request) throws Exception {
        String uuid = request.getHeader("uuid");
        if (uuid != null) {
            threads.put(Thread.currentThread(), uuid);
            monitors.put(uuid, new Monitor());
        }
    }

    public void endRequest() throws Exception {
        String uuid = threads.remove(Thread.currentThread());
        if (uuid != null) {
            monitors.remove(uuid);
        }
    }

    public Monitor getMonitor(String uuid) {
        return monitors.get(uuid);
    }

    public void stopMonitor(String uuid) {
        Monitor monitor = monitors.remove(uuid);
        if (monitor != null) {
            threads.remove(monitor.thread);
            monitor.thread.interrupt();
        }
    }

    public void setMax(int max) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        String uuid = threads.get(Thread.currentThread());
        if (uuid != null) {
            Monitor monitor = monitors.get(uuid);
            if (max < 0) {
                throw new IndexOutOfBoundsException();
            }
            monitor.max = max;
            monitor.current = 0;
        }
    }

    public void addMax(int add) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        String uuid = threads.get(Thread.currentThread());
        if (uuid != null) {
            Monitor monitor = monitors.get(uuid);
            if (monitor.max + add < 0) {
                throw new IndexOutOfBoundsException();
            }
            monitor.max = add;
        }
    }

    public void next(int value) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        String uuid = threads.get(Thread.currentThread());
        if (uuid != null) {
            Monitor monitor = monitors.get(uuid);
            if (monitor.current + value > monitor.max) {
                throw new IndexOutOfBoundsException();
            }
            monitor.current += value;
        }
    }

    public void next() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException();
        }
        String uuid = threads.get(Thread.currentThread());
        if (uuid != null) {
            Monitor monitor = monitors.get(uuid);
            if (monitor.current == monitor.max) {
                throw new IndexOutOfBoundsException();
            }
            monitor.current += 1;
        }
    }

    public void setResult(Object result) {
        String uuid = threads.get(Thread.currentThread());
        if (uuid != null) {
            Monitor monitor = monitors.get(uuid);
            monitor.result = result;
        }
    }

    public static class Monitor {

        private Thread thread = Thread.currentThread();
        private int current;
        private int max;
        private Object result;

        public int getMax() {
            return max;
        }

        public int getCurrent() {
            return current;
        }

        public Object getResult() {
            return result;
        }

    }
}
