package edu.ustb.sei.mde.fastcompare.ihash;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;

import edu.ustb.sei.mde.fastcompare.config.MatcherConfigure;
import edu.ustb.sei.mde.fastcompare.utils.URIComputer;
import java.util.zip.CRC32;

/**
 * This class is used to compute i-hash
 */
public class ElementIHasher {
    private URIComputer uriComputer;

    public ElementIHasher(MatcherConfigure configure) {
        uriComputer = configure.getUriComputer();
    }

    public long hash(EObject element) {
        builder = new StringBuilder();
        serializeEObject(element);
        String string = builder.toString();
        crc32.reset();
        crc32.update(string.getBytes());
        return crc32.getValue();
    }

    private CRC32 crc32 = new CRC32();

    StringBuilder builder = null;
    void serializePrimitiveValue(Object value) {
        if(value == null) {
            builder.append("null");
        } else {
            Class<?> valueType = value.getClass();
            if (valueType == Integer.class) {
                builder.append(((Integer) value).intValue());
            } else if (valueType == Boolean.class) {
                builder.append(((Boolean) value).booleanValue());
            } else if (valueType == String.class) {
                builder.append(((String) value));
            } else if (valueType == Long.class) {
                builder.append(((Long) value).longValue());
            } else if (valueType == Double.class) {
                builder.append(((Double) value).doubleValue());
            } else if (valueType == Float.class) {
                builder.append(((Float) value).floatValue());
            } else if (valueType == Byte.class) {
                builder.append(((Byte) value).byteValue());
            } else if (valueType == Character.class) {
                builder.append(((Character) value).charValue());
            } else if (valueType == Short.class) {
                builder.append(((Short) value).shortValue());
            } else {
                builder.append(valueType.getName());
            }
        }
    }

    void serializeReferencedEObject(EObject value) {
        Iterable<String> iterables = uriComputer.getOrComputeLocation(value);
        for(String frag : iterables) {
            builder.append(frag);
            builder.append('/');
        }
    }

    void serializeListValue(List<?> value) {
        builder.append('[');
        for(Object v : value) {
            serializeValue(v);
            builder.append(',');
        }
        builder.append(']');
    }

    void serializeFeatureMap(FeatureMap featureMap) {
        builder.append("{<");
        for(FeatureMap.Entry entry : featureMap) {
            builder.append(entry.getEStructuralFeature().getName());
            builder.append("->");
            serializeValue(entry.getValue());
            builder.append(',');
        }
        builder.append(">}");
    }

    void serializeEnumValue(Enumerator value) {
        builder.append(((EEnumLiteral)value).getLiteral());
    }

    void serializeValue(Object value) {
        if(value instanceof Enumerator) {
            serializeEnumValue((Enumerator) value);
        } else if(value instanceof FeatureMap) {
            serializeFeatureMap((FeatureMap) value);
        } else if(value instanceof List) {
            serializeListValue((List<?>) value);
        } else {
            serializePrimitiveValue(value);
        }
    }

    void serializeEValue(Object value) {
        if (value instanceof EList) {
            builder.append('[');
            for (Object v : (EList<?>) value) {
                serializeReferencedEObject((EObject) v);
                builder.append(',');
            }
            builder.append(']');
        } else if (value instanceof EObject) {
            serializeReferencedEObject((EObject) value);
        }
    }

    void serializeEObject(EObject value) {
        builder.append('{');
        EClass clazz = value.eClass();
        for(EStructuralFeature feature : clazz.getEAllStructuralFeatures()) {
            if(value.eIsSet(feature)==false) continue;
            Object val = value.eGet(feature);
            if(feature instanceof EReference) {
                if(((EReference)feature).isContainment() == false) {
                    builder.append(feature.getName());
                    builder.append(':');
                    serializeEValue(val);
                }
            } else {
                builder.append(feature.getName());
                builder.append(':');
                serializeValue(val);
            }
            builder.append(',');
        }
        builder.append('}');
    }
}
