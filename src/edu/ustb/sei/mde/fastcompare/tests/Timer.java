package edu.ustb.sei.mde.fastcompare.tests;

import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A timer class helps record time costs, with multithread support
 */
public class Timer {
    private class TimerPair {
        public long start;
        public long total;

        public TimerPair() {
            start = 0;
            total = 0;
        }

        public void commit(long end) {
            if(start != 0) {
                total += (end - start);
                start = 0;
            }
        }
    }

    private boolean outputMilliseconds = true;

    public void setOutputUnit(boolean milliseconds) {
        outputMilliseconds = milliseconds;
    }

    private double formatTime(long nsec) {
        if(outputMilliseconds) return msec(nsec);
        else return sec(nsec);
    }

    private double msec(long nsec) {
        return ((double)nsec) / 1000000.0;
    }

    private double sec(long nsec) {
        return ((double)nsec) / 1000000000.0;
    }

    private Map<Object, Map<Thread, TimerPair>> timers = new HashMap<>();

    private TimerPair getTimerPair(Object key, Thread currentThread) {
        Map<Thread, TimerPair> map = timers.computeIfAbsent(key, (k)->new HashMap<>());
        return map.computeIfAbsent(currentThread, (t)->new TimerPair());
    }

    private TimerPair getTimerPair(Object key) {
        Thread currentThread = Thread.currentThread();
        return getTimerPair(key, currentThread);
    }

    public void tic(Object key) {
        TimerPair pair = getTimerPair(key);
        if(pair.start == 0) {
            pair.start = System.nanoTime();
        }
    }

    public void toc(Object key) {
        TimerPair pair = getTimerPair(key);
        long end = System.nanoTime();
        pair.commit(end);
    }

    public void printTimerInfo(PrintStream out, Object key, Thread thread) {
        TimerPair pair = getTimerPair(key, thread);
        out.print("[");
        out.print(thread.getName());
        out.print("]");
        out.print("\t");
        out.print(key);
        out.print("\t");
        out.print(String.format("%.3f", formatTime(pair.total)));
        if(pair.start != 0) {
            out.println("***");
        } else {
            out.println();
        }
    }

    public void printTimerInfo(PrintStream out, Object key) {
        Map<Thread, TimerPair> allThreads = timers.getOrDefault(key, Collections.emptyMap());
        long total = 0;
        boolean allCompleted = true;
        for(TimerPair pair : allThreads.values()) {
            if(pair.start != 0) allCompleted = false;
            total += pair.total;
        }

        out.print(key);
        out.print("\t");
        out.print(String.format("%.3f", formatTime(total)));
        if(!allCompleted) {
            out.println("***");
        } else {
            out.println();
        }
    }

    public void printTimerInfo(PrintStream out) {
        for(Object key : timers.keySet()) {
            printTimerInfo(out, key);
        }
    }
}
