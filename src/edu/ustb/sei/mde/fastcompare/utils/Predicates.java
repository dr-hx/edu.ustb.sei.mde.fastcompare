package edu.ustb.sei.mde.fastcompare.utils;

import java.util.function.Predicate;

public class Predicates {
    @SafeVarargs
    static public <T> Predicate<T> and(Predicate<? super T>... preds) {
        return (t)->{
            for(Predicate<? super T> p : preds) {
                if(p.test(t)==false) return false;
            }
            return true;
        };
    }

    @SafeVarargs
    static public <T> Predicate<T> or(Predicate<? super T>... preds) {
        return (t)->{
            for(Predicate<? super T> p : preds) {
                if(p.test(t)==true) return true;
            }
            return false;
        };
    }

    static public <T> boolean forall(Iterable<T> collection, final Predicate<? super T> pred) {
        for(T e : collection) {
            if(pred.test(e)==false) return false;
        }
        return true;
    }

    static public <T> boolean exists(Iterable<T> collection, Predicate<? super T> pred) {
        for(T e : collection) {
            if(pred.test(e)==true) return true;
        }
        return false;
    }

    static public <T> Predicate<T> not(Predicate<? super T> pred) {
        return t->!pred.test(t);
    }
}
