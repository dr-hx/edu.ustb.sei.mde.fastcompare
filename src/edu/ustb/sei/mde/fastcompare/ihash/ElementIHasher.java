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
        reset();
        serializeEObject(element);
        return crc32.getValue();
    }

    private CRC32 crc32 = new CRC32();
    private void reset() {
        crc32.reset();
    }

    static private final byte[] byteCache = new byte[8];
    private void append(int value) {
        byteCache[0] = (byte) (value & 0xFF);
        byteCache[1] = (byte) ((value >>> 8) & 0xFF);
        byteCache[2] = (byte) ((value >>> 16) & 0xFF);
        byteCache[3] = (byte) ((value >>> 24) & 0xFF);
        crc32.update(byteCache, 0, 4);
    }

    private void append(boolean value) {
        if(value) crc32.update("true".getBytes());
        else  crc32.update("false".getBytes());
    }

    private void append(String value) {
        crc32.update(value.getBytes());
    }

    private void append(long value) {
        append((int)(value & 0xFFFFFFFFL));
        append((int)((value >>> 32) & 0xFFFFFFFFL));
    }

    private void append(double value) {
        append(Double.doubleToLongBits(value));
    }

    private void append(float value) {
        append(Float.floatToIntBits(value));
    }

    private void append(byte value) {
        byteCache[0] = value;
        crc32.update(byteCache, 0, 1);
    }

    private void append(char value) {
        byteCache[0] = (byte) (value & 0xFF);
        byteCache[1] = (byte) ((value >>> 8) & 0xFF);
        crc32.update(byteCache, 0, 2);
    }

    private void append(short value) {
        byteCache[0] = (byte) (value & 0xFF);
        byteCache[1] = (byte) ((value >>> 8) & 0xFF);
        crc32.update(byteCache, 0, 2);
    }

    private void serializePrimitiveValue(Object value) {
        if(value == null) {
            append("null");
        } else {
            Class<?> valueType = value.getClass();
            if (valueType == Integer.class) {
                append(((Integer) value).intValue());
            } else if (valueType == Boolean.class) {
                append(((Boolean) value).booleanValue());
            } else if (valueType == String.class) {
                append(((String) value));
            } else if (valueType == Long.class) {
                append(((Long) value).longValue());
            } else if (valueType == Double.class) {
                append(((Double) value).doubleValue());
            } else if (valueType == Float.class) {
                append(((Float) value).floatValue());
            } else if (valueType == Byte.class) {
                append(((Byte) value).byteValue());
            } else if (valueType == Character.class) {
                append(((Character) value).charValue());
            } else if (valueType == Short.class) {
                append(((Short) value).shortValue());
            } else {
                append(valueType.getName());
            }
        }
    }

    private void serializeReferencedEObject(EObject value) {
        Iterable<String> iterables = uriComputer.getOrComputeLocation(value);
        for(String frag : iterables) {
            append(frag);
            append('/');
        }
    }

    private void serializeListValue(List<?> value) {
        append('[');
        for(Object v : value) {
            serializeValue(v);
            append(',');
        }
        append(']');
    }

    private void serializeFeatureMap(FeatureMap featureMap) {
        append("{<");
        for(FeatureMap.Entry entry : featureMap) {
            append(entry.getEStructuralFeature().getName());
            append("->");
            serializeValue(entry.getValue());
            append(',');
        }
        append(">}");
    }

    private void serializeEnumValue(Enumerator value) {
        append(((EEnumLiteral)value).getLiteral());
    }

    private void serializeValue(Object value) {
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

    private void serializeEValue(Object value) {
        if (value instanceof EList) {
            append('[');
            for (Object v : (EList<?>) value) {
                serializeReferencedEObject((EObject) v);
                append(',');
            }
            append(']');
        } else if (value instanceof EObject) {
            serializeReferencedEObject((EObject) value);
        }
    }

    private void serializeEObject(EObject value) {
        append('{');
        EClass clazz = value.eClass();
        for(EStructuralFeature feature : clazz.getEAllStructuralFeatures()) {
            if(value.eIsSet(feature)==false) continue;
            Object val = value.eGet(feature);
            if(feature instanceof EReference) {
                if(((EReference)feature).isContainment() == false) {
                    append(feature.getName());
                    append(':');
                    serializeEValue(val);
                }
            } else {
                append(feature.getName());
                append(':');
                serializeValue(val);
            }
            append(',');
        }
        append('}');
    }
}
