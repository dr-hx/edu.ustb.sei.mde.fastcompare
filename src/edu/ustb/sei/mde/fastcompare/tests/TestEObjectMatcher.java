package edu.ustb.sei.mde.fastcompare.tests;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Iterators;

import static org.eclipse.emf.ecore.EcoreFactory.eINSTANCE;

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
import edu.ustb.sei.mde.fastcompare.match.eobject.ProximityEObjectMatcher;

public class TestEObjectMatcher {

    private MatcherConfigure configure;
    private ProximityEObjectMatcher matcher;
    private EObject left;
    private EObject right;

    @Before
    public void init() {
        buildLeft();
        buildRight();
        configure = new MatcherConfigure();
        configure.setUseSubtreeHash(true);
        matcher = new ProximityEObjectMatcher(configure);
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

    @SuppressWarnings("unchecked")
    private <T extends EObject> T createEObject(EClass eClass, Consumer<T> initializer) {
        T obj = (T) EcoreUtil.create(eClass);
        initializer.accept(obj);
        return obj;
    }

    private <T extends ENamedElement> T createENamedElement(String name, EClass eClass, Consumer<T> initializer) {
        return createEObject(eClass, (it)->{
            it.setName(name);
            initializer.accept(it);
        });
    }

    private EPackage createEPackage(String name, Consumer<EPackage> initializer) {
        return createENamedElement(name, EcorePackage.Literals.EPACKAGE, initializer);
    }

    private EClass createEClass(String name, Consumer<EClass> initializer) {
        return createENamedElement(name, EcorePackage.Literals.ECLASS, initializer);
    }

    private EAttribute createEAttribute(String name, EDataType type, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EATTRIBUTE, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
        });
    }

    private EParameter createEParameter(String name, EClassifier type, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EPARAMETER, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
        });
    }

    private EOperation createEOperation(String name, EClassifier type, int lower, int upper, Consumer<EOperation> initializer) {
        return createENamedElement(name, EcorePackage.Literals.EOPERATION, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
            initializer.accept(it);
        });
    }

    private EReference createEReference(String name, EClass targetType, boolean containment, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EOPERATION, (it)->{
            it.setEType(targetType);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
            it.setContainment(containment);
        });
    }
    

    @Test
    public void test() {
        Comparison comparison = CompareFactory.eINSTANCE.createComparison();
        matcher.createMatches(comparison, Collections.singletonList(left), Collections.singletonList(right), Collections.emptyList());
        System.out.println(comparison);
    }
}
