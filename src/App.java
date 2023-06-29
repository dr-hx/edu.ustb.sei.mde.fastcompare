import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.zip.CRC32;

import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.match.resource.StrategyResourceMatcher;
import org.eclipse.emf.ecore.EcorePackage;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.EditionDistance;
import edu.ustb.sei.mde.fastcompare.shash.Hash64;
import edu.ustb.sei.mde.fastcompare.shash.SHashFunction;

public class App {
    public static void main(String[] args) throws Exception {
        // testHaveSameContainer();
    }

    // private static void testPopcnt() {
    //     Random random = new Random();
    //     long start, end;
    //     long sum1 = 0, sum2 = 0;
    //     int repeat = 100;

    //     for(int i = 0; i<repeat; i++) {
    //         long value = random.nextLong();
    //         start = System.nanoTime();
    //         Hash64.computeBitCounts(value);
    //         end = System.nanoTime();
    //         sum1 += (end - start);
    //     }

    //     for(int i = 0; i<repeat; i++) {
    //         long value = random.nextLong();
    //         start = System.nanoTime();
    //         Hash64.computeBitCounts2(value);
    //         end = System.nanoTime();
    //         sum2 += (end - start);
    //     }

    //     System.out.println("avg. table = "+sum1/1000000.0/repeat+" ms");
    //     System.out.println("avg. fast  = "+sum2/1000000.0/repeat+" ms");
    // }

    // private static void testHaveSameContainer() {
    //     MatcherConfigure matcherConfigure = new MatcherConfigure();
    //     EditionDistance dist = new EditionDistance(matcherConfigure);
    //     Comparison inProgress = CompareFactory.eINSTANCE.createComparison();
    //     long start, end;
    //     long sum1 = 0, sum2 = 0;

    //     int repeat = 1000000;
        
    //     for(int i = 0; i< repeat; i++) {
    //         start = System.nanoTime();
    //         dist.haveSameContainer(inProgress, EcorePackage.Literals.ECLASS, EcorePackage.Literals.ECLASSIFIER);
    //         end = System.nanoTime();
    //         sum1 += (end - start);
    //     }

    //     for(int i =0; i<repeat; i++) {
    //         start = System.nanoTime();
    //         dist._haveSameContainer(inProgress, EcorePackage.Literals.ECLASS, EcorePackage.Literals.ECLASSIFIER);
    //         end = System.nanoTime();
    //         sum2 += (end - start);
    //     }
        


    //     System.out.println("avg. caching = "+sum1/1000000.0/repeat+" ms");
    //     System.out.println("avg. uncaching  = "+sum2/1000000.0/repeat + " ms");
    // }

	private static void testChecksum() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        java.util.zip.CRC32 crc = new CRC32();

        String data = "public int proximity(Iterable<String> aPath, Iterable<String> bPath) {\n		int aSize = 0;\n		int bSize = 0;\n		Iterator<String> itA = aPath.iterator();\n		Iterator<String> itB = bPath.iterator();\n		boolean areSame = true;\n		int commonSegments = 0;\n		int remainingASegments = 0;\n		int remainingBSegments = 0;\n		while (itA.hasNext() && itB.hasNext() && areSame) {\n			String a = itA.next();\n			String b = itB.next();\n			if (a.equals(b)) {\n				commonSegments++;\n			} else {\n				areSame = false;\n			}\n			aSize++;\n			bSize++;\n\n		}\n		if (commonSegments == 0) {\n			return MAX_DISTANCE;\n		}\n		remainingASegments = aSize + Iterators.size(itA) - commonSegments;\n		remainingBSegments = bSize + Iterators.size(itB) - commonSegments;\n\n		int nbSegmentsToGoFromAToB = remainingASegments + remainingBSegments;\n		return (nbSegmentsToGoFromAToB * 10) / (commonSegments * 2 + nbSegmentsToGoFromAToB);\n	}";       
        byte[] bytedata = data.getBytes();

        long start, end;
        long sumcrc = 0, summd = 0;

        for(int i = 0; i< 10; i++) {
            crc.reset();
            start = System.nanoTime();
            crc.update(bytedata);
            crc.getValue();
            end = System.nanoTime();
            System.out.println("crc "+(end-start)/1000000.0+" ms");
            sumcrc += (end - start);
        }

        for(int i =0; i<10; i++) {
            md.reset();
            start = System.nanoTime();
            md.update(bytedata);
            md.digest();
            end = System.nanoTime();
            System.out.println("md "+(end-start)/1000000.0+" ms");
            summd += (end - start);
        }

        System.out.println("avg. crc = "+sumcrc/10000000.0+" ms");
        System.out.println("avg. md  = "+summd/10000000.0+" ms");
    }
}
