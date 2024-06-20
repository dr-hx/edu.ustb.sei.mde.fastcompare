package edu.ustb.sei.mde.fastcompare.tests;

import java.util.List;
import java.util.stream.StreamSupport;

import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.junit.Assert;
import org.junit.Test;

import edu.ustb.sei.mde.fastcompare.config.ClassConfigure;
import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.config.MutableFeatureConfigure;
import edu.ustb.sei.mde.fastcompare.ihash.ElementIHasher;
import edu.ustb.sei.mde.fastcompare.match.eobject.EditionDistance;
import edu.ustb.sei.mde.fastcompare.utils.MatchUtil;

public class TestConfigures {

    @Test
    public void testDefaultClassConfigure() {
        MatcherConfigure mc = new MatcherConfigure();
        ClassConfigure cc = mc.getClassConfigure(EcorePackage.Literals.ECLASS);
        System.out.println(cc);
    }

    @Test
    public void testGlobalClassConfigure() {
        MatcherConfigure mc = new MatcherConfigure();
        MutableFeatureConfigure fc = (MutableFeatureConfigure) mc.getOrCreateGlobalFeatureConfigure(EcorePackage.Literals.ENAMED_ELEMENT__NAME);
        fc.setWeight(1000);
        ClassConfigure cc = mc.getClassConfigure(EcorePackage.Literals.ECLASS);
        Assert.assertTrue(StreamSupport.stream(cc.getConcernedFeatures().spliterator(), false).filter(f->f.getKey()==EcorePackage.Literals.ENAMED_ELEMENT__NAME).allMatch(f->f.getValue()==fc));
    }

    @Test
    public void testLocalClassConfigure() {
        MatcherConfigure mc = new MatcherConfigure();
        MutableFeatureConfigure fc = (MutableFeatureConfigure) mc.getOrCreateGlobalFeatureConfigure(EcorePackage.Literals.ENAMED_ELEMENT__NAME);
        fc.setWeight(1000);
        ClassConfigure cc = mc.getClassConfigure(EcorePackage.Literals.ECLASS);
        MutableFeatureConfigure fc2 = new MutableFeatureConfigure(EcorePackage.Literals.ENAMED_ELEMENT__NAME, true);
        fc2.setPosWeight(10000);
        cc.putFeatureConfigure(EcorePackage.Literals.ENAMED_ELEMENT__NAME, fc2);
        Assert.assertTrue(StreamSupport.stream(cc.getConcernedFeatures().spliterator(), false).filter(f->f.getKey()==EcorePackage.Literals.ENAMED_ELEMENT__NAME).allMatch(f->f.getValue()==fc2));
    }

    @Test
    public void testEditionDistance() {
        List<EClass> classes = EcorePackage.eINSTANCE.getEClassifiers().stream().filter(c->c instanceof EClass).map(c->(EClass) c).toList();
        MatcherConfigure matcherConfigure = new MatcherConfigure();
        EditionDistance myDistance = new EditionDistance(matcherConfigure);
        org.eclipse.emf.compare.match.eobject.EditionDistance emfcDistance = org.eclipse.emf.compare.match.eobject.EditionDistance.builder().build();


        Comparison comparison = CompareFactory.eINSTANCE.createComparison();
        Match conMatch = MatchUtil.createMatch();
        conMatch.setLeft(EcorePackage.eINSTANCE);
        comparison.getMatches().add(conMatch);

        for(int l = 0; l < classes.size(); l ++) {
            EClass cl = classes.get(l);
            for(int r = l + 1; r < classes.size(); r ++) {
                EClass cr = classes.get(r);
                double distance = myDistance.distance(comparison, cl, cr, null);
                double eDistance = emfcDistance.distance(comparison, cl, cr);

                if(distance != eDistance)
                    System.out.println("("+cl.getName() +","+cr.getName()+")=("+distance+","+eDistance+")");
            }
        }
    }

    @Test
    public void testIHasher() {
        MatcherConfigure configure = new MatcherConfigure();
        ElementIHasher hasher = new ElementIHasher(configure);
        for(EObject obj : EcorePackage.eINSTANCE.eContents()) {
            System.out.println(obj +"\n\t" + hasher.hash(obj));
        }
    }
}
