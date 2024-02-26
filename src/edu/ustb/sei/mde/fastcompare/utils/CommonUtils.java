package edu.ustb.sei.mde.fastcompare.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

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


    static public void update(Checksum checksum, long value) {
        int l = (int) (value & 0xFFFFFFFFL);
        int h = (int) (value >>> 32);
        checksum.update(l);
        checksum.update(h);
    }

    static public void update(Checksum checksum, int value) {
        checksum.update(value);
    }

    static public void update(Checksum checksum, double value) {
        update(checksum, Double.doubleToLongBits(value));
    }

    static public void update(Checksum checksum, String str) {
        checksum.update(str.getBytes());
    }

    static private MessageDigest md5;
    static private CRC64 __crc64 = new CRC64();
    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    static public void resetMD5() {
        md5.reset();
    }
    static public long getMD5Value() {
        __crc64.reset();
        __crc64.update(md5.digest());
        return __crc64.getValue();
    }

    static public void updateMD5(long value) {
        int l = (int) (value & 0xFFFFFFFFL);
        int h = (int) (value >>> 32);
        updateMD5(l);
        updateMD5(h);
    }

    static public void updateMD5(int value) {
        md5.update((byte) (value & 0xFF));
        md5.update((byte) ((value >>> 8) & 0xFF));
        md5.update((byte) ((value >>> 16) & 0xFF));
        md5.update((byte) ((value >>> 24) & 0xFF));
    }

    static public void updateMD5(double value) {
        updateMD5(Double.doubleToLongBits(value));
    }

    static public void updateMD5(String str) {
        md5.update(str.getBytes());
    }
}
