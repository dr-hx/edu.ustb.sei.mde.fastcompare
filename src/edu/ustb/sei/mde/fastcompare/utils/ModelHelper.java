package edu.ustb.sei.mde.fastcompare.utils;

import java.util.function.Consumer;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class ModelHelper {
    @SuppressWarnings("unchecked")
    public static  <T extends EObject> T createEObject(EClass eClass, Consumer<T> initializer) {
        T obj = (T) EcoreUtil.create(eClass);
        initializer.accept(obj);
        return obj;
    }

    public static  <T extends ENamedElement> T createENamedElement(String name, EClass eClass, Consumer<T> initializer) {
        return createEObject(eClass, (it)->{
            it.setName(name);
            initializer.accept(it);
        });
    }

    public static  EPackage createEPackage(String name, Consumer<EPackage> initializer) {
        return createENamedElement(name, EcorePackage.Literals.EPACKAGE, initializer);
    }

    public static  EClass createEClass(String name, Consumer<EClass> initializer) {
        return createENamedElement(name, EcorePackage.Literals.ECLASS, initializer);
    }

    public static  EAttribute createEAttribute(String name, EDataType type, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EATTRIBUTE, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
        });
    }

    public static  EParameter createEParameter(String name, EClassifier type, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EPARAMETER, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
        });
    }

    public static  EOperation createEOperation(String name, EClassifier type, int lower, int upper, Consumer<EOperation> initializer) {
        return createENamedElement(name, EcorePackage.Literals.EOPERATION, (it)->{
            it.setEType(type);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
            initializer.accept(it);
        });
    }

    public static  EReference createEReference(String name, EClass targetType, boolean containment, int lower, int upper) {
        return createENamedElement(name, EcorePackage.Literals.EOPERATION, (it)->{
            it.setEType(targetType);
            it.setLowerBound(lower);
            it.setUpperBound(upper);
            it.setContainment(containment);
        });
    }
}
