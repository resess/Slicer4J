package com.fasterxml.jackson.databind.exc;

import java.util.*;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Specialized {@link JsonMappingException} sub-class specifically used
 * to indicate problems due to encountering a JSON property that could
 * not be mapped to an Object property (via getter, constructor argument
 * or field).
 */
public class UnrecognizedPropertyException
    extends PropertyBindingException
{
    private static final long serialVersionUID = 1L;

    public UnrecognizedPropertyException(String msg, JsonLocation loc,
            Class<?> referringClass, String propName,
            Collection<Object> propertyIds)
    {
        super(msg, loc, referringClass, propName, propertyIds);
    }

    /**
     * Factory method used for constructing instances of this exception type.
     * 
     * @param jp Underlying parser used for reading input being used for data-binding
     * @param fromObjectOrClass Reference to either instance of problematic type (
     *    if available), or if not, type itself
     * @param propertyName Name of unrecognized property
     * @param propertyIds (optional, null if not available) Set of properties that
     *    type would recognize, if completely known: null if set can not be determined.
     */
    public static UnrecognizedPropertyException from(JsonParser jp,
            Object fromObjectOrClass, String propertyName,
            Collection<Object> propertyIds)
    {
        if (fromObjectOrClass == null) {
            throw new IllegalArgumentException();
        }
        Class<?> ref;
        if (fromObjectOrClass instanceof Class<?>) {
            ref = (Class<?>) fromObjectOrClass;
        } else {
            ref = fromObjectOrClass.getClass();
        }
        String msg = "Unrecognized field \""+propertyName+"\" (class "+ref.getName()+"), not marked as ignorable";
        UnrecognizedPropertyException e = new UnrecognizedPropertyException(msg,
                jp.getCurrentLocation(), ref, propertyName, propertyIds);
        // but let's also ensure path includes this last (missing) segment
        e.prependPath(fromObjectOrClass, propertyName);
        return e;
    }

    /**
     * @deprecated Since 2.3, use {@link #getPropertyName} instead.
     */
    @Deprecated // since 2.3
    public String getUnrecognizedPropertyName() {
        return getPropertyName();
    }    
}
