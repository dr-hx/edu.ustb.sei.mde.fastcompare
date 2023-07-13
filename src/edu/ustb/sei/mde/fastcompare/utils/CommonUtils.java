package edu.ustb.sei.mde.fastcompare.utils;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.CRC32;

import com.google.common.collect.FluentIterable;


public class CommonUtils {
    static public void checkNotNull(Object o) {
        if(o==null) throw new RuntimeException("An non-nullable object is null");
    }
    static public boolean ObjectEqual(Object left, Object right) {
        if (left == right)
            return true;
        else if (left == null || right == null)
            return false;
        else
            return left.equals(right);
    }
    
    public static <T extends Object> T getFirst(
      Iterable<? extends T> iterable, T defaultValue) {
        Iterator<? extends T> it = iterable.iterator();
        if(it.hasNext()) {
            return it.next();
        } else return defaultValue;
    }
    
    public final static <T> Iterator<T> filter(Iterator<T> iterator, Predicate<? super T> retainIfTrue) {
        return new Iterator<T>() {
            private T next = null;

            @Override
            public boolean hasNext() {
                if(next != null) return true;
                else {
                    while(iterator.hasNext()) {
                        T value = iterator.next();
                        if(retainIfTrue.test(value)) {
                            next = value;
                            return true;
                        }
                    }
                    return false;
                }
            }

            @Override
            public T next() {
                return next;
            }
            
        };
    }

    public final static <T> Spliterator<T> filter(Spliterator<T> spliterator, Predicate<? super T> retainIfTrue) {
        return new Spliterator<T>() {

            @Override
            public int characteristics() {
                return spliterator.characteristics();
            }

            @Override
            public long estimateSize() {
                return spliterator.estimateSize();
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                return spliterator.tryAdvance(e->{
                    if(retainIfTrue.test(e)) {
                        action.accept(e);
                    }
                });
            }

            @Override
            public Spliterator<T> trySplit() {
               return spliterator.trySplit();
            }
            
        };
    }

    public static <T extends Object> Iterable<T> filter(
            final Iterable<T> unfiltered, final Predicate<? super T> retainIfTrue) {
        return new FluentIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return CommonUtils.filter(unfiltered.iterator(), retainIfTrue);
            }


            @Override
            public void forEach(Consumer<? super T> action) {
                unfiltered.forEach(
                        (T a) -> {
                            if (retainIfTrue.test(a)) {
                                action.accept(a);
                            }
                        });
            }

            @Override
            public Spliterator<T> spliterator() {
                return CommonUtils.filter(unfiltered.spliterator(), retainIfTrue);
            }
        };
    }


    static public void update(CRC32 crc32, long value) {
        int h = (int) (value >>> 32);
        int l = (int) (value & 0xFFFFFFFFL);
        crc32.update(h);
        crc32.update(l);
    }

    static public void update(CRC32 crc32, int value) {
        crc32.update(value);
    }

    static public void update(CRC32 crc32, double value) {
        update(crc32, Double.doubleToLongBits(value));
    }

    static public void update(CRC32 crc32, String str) {
        crc32.update(str.getBytes());
    }
}
