package jmri.jmrit.logixng.util;

import jmri.InstanceManager;
import jmri.Memory;
import jmri.MemoryManager;

/**
 * Utility methods to handle references
 */
public class ReferenceUtil {

    private final MemoryManager memoryManager = InstanceManager.getDefault(MemoryManager.class);
    
    /**
     * Checks if the parameter is a reference or not.
     * @param value the string to check
     * @return true if value has a reference. falsw otherwise
     */
    public boolean isReference(String value) {
        // A reference starts with { and ends with }
        return value.startsWith("{") && value.endsWith("}");
    }
    
    private String unescapeString(String value, int startIndex, int endIndex) {
        boolean escaped = false;
        
        StringBuilder sb = new StringBuilder();
        for (int i=startIndex; i < endIndex; i++) {
            if (value.charAt(i) == '\\') escaped = !escaped;
            else escaped = false;
            
            if (! escaped) sb.append(value.charAt(i));
            System.out.format("sb: %s%n", sb.toString());
        }
        
        return sb.toString();
    }
    
    /**
     * Get the value.
     * The value ends either with end of string, or with any of the characters
     * comma, left square bracket, right square bracket or right curly bracket.
     * These characters may be escaped and should then be ignored.
     * @param reference the reference
     * @param startIndex where in the string the value starts, since the
     * reference string may contain several references.
     * @param endIndex index of the end of the value. This is an output parameter.
     * @return the value
     */
    private String getValue(String reference, int startIndex, IntRef endIndex) {
        System.out.format("getValue(%s,%d)%n", reference, startIndex);
        boolean escapeFound = false;
        boolean escaped = false;
        int end = startIndex;
        while (end < reference.length()
                && (escaped ||
                    (reference.charAt(end) != ','
                    && reference.charAt(end) != '['
                    && reference.charAt(end) != ']'
                    && reference.charAt(end) != '{'
                    && reference.charAt(end) != '}'))) {
            if (reference.charAt(end) == '\\') {
                escaped = !escaped;
                escapeFound = true;
            } else {
                escaped = false;
            }
            end++;
        }
        endIndex.v = end;
        System.out.format("getValue(%s,%d): %s, %d%n", reference, startIndex, reference.substring(startIndex, end), end);
        
        if (startIndex == end) throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        if (escapeFound) return unescapeString(reference, startIndex, end);
        else return reference.substring(startIndex, end);
    }
    
    /**
     * Get the reference or the value.
     * The value ends either with end of string, or with any of the characters
     * comma, left square bracket, right square bracket or right curly bracket.
     * These characters may be escaped and should then be ignored.
     * @param value the reference
     * @param startIndex where in the string the value starts, since the
     * reference string may contain several references.
     * @param endIndex index of the end of the value. This is an output parameter.
     * @return the value
     */
    private String getReferenceOrValue(String reference, int startIndex, IntRef endIndex) {
        System.out.format("getReferenceOrValue(%s,%d)%n", reference, startIndex);
        
        // Do we have a new reference?
        if (reference.charAt(startIndex) == '{') {
            return getReference(reference, startIndex, endIndex);
        } else {
            return getValue(reference, startIndex, endIndex);
        }
    }
    
    /**
     * Get the value of a reference
     * @param reference the reference
     * @param startIndex where in the string the reference starts, since the
     * reference string may contain several references.
     * @param endIndex index of the end of the reference. This is an output parameter.
     * @return the value of the reference
     */
    private String getReference(String reference, int startIndex, IntRef endIndex) {
        System.out.format("getReference(%s,%d)%n", reference, startIndex);
        
        // A reference must start with the char {
        if (reference.charAt(startIndex) != '{') {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        String leftValue;
        String column;
        String row;
        
        startIndex++;
        
        leftValue = getReferenceOrValue(reference, startIndex, endIndex);
        
        if (endIndex.v == reference.length()) {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        System.out.format("Reference: '%s', endIndex: %d%n", reference, endIndex.v);
        if ((reference.charAt(endIndex.v) != '}') && (reference.charAt(endIndex.v) != '[')) {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        
        
//        if ((endIndex.v+1 == reference.length()) && (reference.charAt(endIndex.v) == '}')) {
        if ((endIndex.v+1 == reference.length()) || (reference.charAt(endIndex.v) != '[')) {
            System.out.format("getReference: leftValue: '%s'%n", leftValue);
            Memory m = memoryManager.getNamedBean(leftValue);
            if (m != null) {
                if (m.getValue() != null) return m.getValue().toString();
                else throw new IllegalArgumentException("Memory '"+leftValue+"' has no value");
            }
            else throw new IllegalArgumentException("Memory '"+leftValue+"' is not found");
        }
        
        // If we are here, we have a table reference. Find out column and row.
        if (reference.charAt(endIndex.v) != '[') {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        endIndex.v++;
        
        // If we are here, we have a table reference. Find out column and row.
        row = getReferenceOrValue(reference, endIndex.v, endIndex);
        
        System.out.format("endIndex: %d, %c%n", endIndex.v, reference.charAt(endIndex.v));
        if ((endIndex.v+2 == reference.length()
                && (reference.charAt(endIndex.v) == ']')
                && (reference.charAt(endIndex.v+1) == '}'))) {
            return "Testing....";
//            return leftValue[row];
        }
        
        System.out.format("endIndex: %d, %c%n", endIndex.v, reference.charAt(endIndex.v));
        if (endIndex.v+1 == reference.length() || reference.charAt(endIndex.v) != ',') {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        endIndex.v++;
        
        column = getReferenceOrValue(reference, endIndex.v, endIndex);
        if (endIndex.v == reference.length() || reference.charAt(endIndex.v) != ']') {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        System.out.format("endIndex: %d, %c%n", endIndex.v, reference.charAt(endIndex.v));
        if ((endIndex.v+2 == reference.length()
                && (reference.charAt(endIndex.v) == ']')
                && (reference.charAt(endIndex.v+1) == '}'))) {
            return "Testing 222....";
//            return leftValue[row];
        }
        
        throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        
//        throw new UnsupportedOperationException("Table is not yet supported");
    }
    
//    @Nonnull
    public String getReference(String reference) {
        System.out.println();
        System.out.format("getReference(%s)%n", reference);
        if (!isReference(reference)) {
            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        IntRef endIndex = new IntRef();
        String ref = getReference(reference, 0, endIndex);
        
        if (endIndex.v != reference.length()) {
//            throw new IllegalArgumentException("Reference '"+reference+"' is not a valid reference");
        }
        
        return ref;
    }
    
    
    /**
     * Reference to an integer.
     * This class is cheaper to use than AtomicInteger.
     */
    private static class IntRef {
        public int v;
    }
    
}
