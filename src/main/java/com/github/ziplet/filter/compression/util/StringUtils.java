package com.github.ziplet.filter.compression.util;

import java.util.Collection;
import java.util.Iterator;

/**
 * @since 20/06/2014
 */
public class StringUtils {

    public static String collectionToCommaDelimitedString(Collection<?> coll) {
        return collectionToDelimitedString(coll, ",", "", "");
    }
    
    private static String collectionToDelimitedString(Collection<?> coll, String delim, String prefix, String suffix) {
        if (coll == null || coll.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<?> it = coll.iterator();
        while (it.hasNext()) {
            sb.append(prefix).append(it.next()).append(suffix);
            if (it.hasNext()) {
                sb.append(delim);
            }
        }
        return sb.toString();
    }

    public static boolean hasText(String text) {
        return !(text == null || text.isEmpty());
    }
}
