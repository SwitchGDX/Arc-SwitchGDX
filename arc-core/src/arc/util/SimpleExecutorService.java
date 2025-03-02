package arc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class SimpleExecutorService implements ExecutorService, Runnable {
    private volatile boolean shuttingDown;
    private volatile boolean terminated;
    private final ArrayList<Thread> threads = new ArrayList<>();
    private final ArrayList<Task> tasks = new ArrayList<>();

    public SimpleExecutorService(String name, int threadCount) {
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(this, name + " " + (threadCount + 1));
            threads.add(thread);
            thread.start();
        }
    }
    
    @Override
    public void run() {
        while (!terminated) {
            Task task = null;
            synchronized (this) {
                if (!tasks.isEmpty())
                    task = tasks.remove(0);
            }
            if (task == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
                continue;
            }
            task.run();
        }
        terminated = true;
    }

    @Override
    public void shutdown() {
        shuttingDown = true;
    }

    @Override
    public synchronized List<Runnable> shutdownNow() {
        shuttingDown = true;
        for (Task task : tasks)
            task.cancel(true);
        tasks.clear();
        return new ArrayList<>();
    }

    @Override
    public boolean isShutdown() {
        return shuttingDown;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        long timeout = System.currentTimeMillis() + timeUnit.toMillis(l);
        while (true) {
            if (terminated)
                return true;
            if (System.currentTimeMillis() > timeout)
                return false;
            Thread.sleep(1);
        }
    }

    @Override
    public synchronized <T> Future<T> submit(Callable<T> callable) {
        Task task = new Task(callable);
        tasks.add(task);
        return (Future<T>) task;
    }

    @Override
    public synchronized <T> Future<T> submit(Runnable runnable, T t) {
        Task task = new Task(runnable, t);
        tasks.add(task);
        return (Future<T>) task;
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return submit(runnable, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection) throws InterruptedException {
        ArrayList<Future<T>> futures = new ArrayList<>();
        for (Callable<T> callable : collection)
            futures.add(submit(callable));
        while (true) {
            boolean done = true;
            for (Future<T> future : futures)
                if (!future.isDone())
                    done = false;
            if (done)
                return futures;
            Thread.sleep(1);
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException {
        long timeout = System.currentTimeMillis() + timeUnit.toMillis(l);
        ArrayList<Future<T>> futures = new ArrayList<>();
        for (Callable<T> callable : collection)
            futures.add(submit(callable));
        while (true) {
            boolean done = true;
            for (Future<T> future : futures)
                if (!future.isDone())
                    done = false;
            if (done || System.currentTimeMillis() > timeout)
                return futures;
            Thread.sleep(1);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection) throws InterruptedException, ExecutionException {
        if (collection.isEmpty())
            throw new IllegalArgumentException("No callables specified");
        ArrayList<Future<T>> futures = new ArrayList<>();
        for (Callable<T> callable : collection)
            futures.add(submit(callable));
        while (true) {
            for (Future<T> future : futures) {
                if (!future.isDone())
                    continue;
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    if (futures.size() == 1)
                        throw e;
                    futures.remove(future);
                }
                break;
            }
            Thread.sleep(1);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> collection, long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        long timeout = System.currentTimeMillis() + timeUnit.toMillis(l);
        if (collection.isEmpty())
            throw new IllegalArgumentException("No callables specified");
        ArrayList<Future<T>> futures = new ArrayList<>();
        for (Callable<T> callable : collection)
            futures.add(submit(callable));
        while (true) {
            for (Future<T> future : futures) {
                if (!future.isDone())
                    continue;
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    if (futures.size() == 1)
                        throw e;
                    futures.remove(future);
                }
                break;
            }
            if (System.currentTimeMillis() > timeout)
                throw new TimeoutException();
            Thread.sleep(1);
        }
    }

    @Override
    public void execute(Runnable runnable) {
        submit(runnable);
    }
    
    public static class Task implements java.util.concurrent.Future<Object> {
        private final Runnable runnable;
        private final Callable<?> callable;
        private Object retVal;
        private volatile boolean done;
        private volatile boolean cancelled;
        private Throwable exception;
        
        public Task(Runnable runnable, Object retVal) {
            this.runnable = runnable;
            this.callable = null;
            this.retVal = retVal;
        }

        public Task(Callable<?> callable) {
            this.runnable = null;
            this.callable = callable;
        }
        
        public void run() {
            if (done || cancelled)
                return;
            try {
                if (runnable != null) {
                    runnable.run();
                } else {
                    retVal = callable.call();
                }
            } catch (Throwable exception) {
                this.exception = exception;
            }
            done = true;
        }
        
        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            boolean success = !cancelled;
            cancelled = true;
            return success;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done || cancelled;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            while (true) {
                if (done) {
                    if (exception != null)
                        throw new ExecutionException(exception);
                    return retVal;
                }
                Thread.sleep(1);
            }
        }

        @Override
        public Object get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
            long timeout = System.currentTimeMillis() + timeUnit.toMillis(l);
            while (true) {
                if (done) {
                    if (exception != null)
                        throw new ExecutionException(exception);
                    return retVal;
                }
                if (System.currentTimeMillis() > timeout)
                    throw new TimeoutException();
                Thread.sleep(1);
            }
        }
    }
}
