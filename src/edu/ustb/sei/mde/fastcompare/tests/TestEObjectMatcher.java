package edu.ustb.sei.mde.fastcompare.tests;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Consumer;

import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.match.eobject.EditionDistance;
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;

import static edu.ustb.sei.mde.fastcompare.utils.ModelHelper.*;

@SuppressWarnings("all")
public class TestEObjectMatcher {

    private MatcherConfigure configure;
    private ProximityEObjectMatcher matcher;
    private EObject left;
    private EObject right;
    private EObject origin;

    @Before
    public void init() {
        buildLeft();
        buildRight();
        buildOriginal();
        configure = new MatcherConfigure();
        configure.setUseSubtreeHash(true);
        configure.setUseSimHash(false);
        matcher = new ProximityEObjectMatcher(configure);
    }

    @After
    public void dump() {
        ((EditionDistance)configure.getDistanceFunction()).dump();
    }

    private void buildOriginal() {
        origin = createEPackage("pkg1", (pkg)->{
            pkg.getEClassifiers().add(createEClass("cls1", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.ESTRING, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls2", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.ESTRING, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls3", (cls)->{
                cls.getEAttributes().add(createEAttribute("att3", EcorePackage.Literals.EINT, 1, 1));
                cls.getEAttributes().add(createEAttribute("att4", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));
        });
    }

    private void buildLeft() {
        left = createEPackage("pkg1", (pkg)->{
            pkg.getEClassifiers().add(createEClass("cls1", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.ESTRING, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls2", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.ESTRING, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls3", (cls)->{
                cls.getEAttributes().add(createEAttribute("att3", EcorePackage.Literals.EINT, 1, 1));
                cls.getEAttributes().add(createEAttribute("att4", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));
        });
    }

    private void buildRight() {
        right = createEPackage("pkg1", (pkg)->{
            pkg.getEClassifiers().add(createEClass("cls1", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.EINT, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls2", (cls)->{
                cls.getEAttributes().add(createEAttribute("att1", EcorePackage.Literals.ESTRING, 1, 1));
                cls.getEAttributes().add(createEAttribute("att2", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));

            pkg.getEClassifiers().add(createEClass("cls4", (cls)->{
                cls.getEAttributes().add(createEAttribute("att3", EcorePackage.Literals.EINT, 1, 1));
                cls.getEAttributes().add(createEAttribute("att4", EcorePackage.Literals.EBOOLEAN, 1, 1));
            }));
        });
    }

    @Test
    public void test() {
        Comparison comparison = CompareFactory.eINSTANCE.createComparison();
        matcher.createMatches(comparison, Collections.singletonList(left), Collections.singletonList(right), Collections.singletonList(origin));
        System.out.println(comparison);
    }
}
