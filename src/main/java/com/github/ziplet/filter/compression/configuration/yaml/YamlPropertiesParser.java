package com.github.ziplet.filter.compression.configuration.yaml;

import com.github.ziplet.filter.compression.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * @author fgaule
 * @since 20/06/2014
 */
public class YamlPropertiesParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(YamlPropertiesParser.class);
    private static final String BRACE_OPEN = "[";
    private static final String BRACE_CLOSE = "]";
    private static final String DOT = ".";
    private static final String EMPTY_STRING = "";

    public Properties parse(File file) {
        Yaml yaml = new Yaml();
        Properties properties = new Properties();

        try {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Loading properties from " + file.getAbsolutePath());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) yaml.load(new FileInputStream(file));
            assignProperties(properties, map, null);

        } catch (IOException e) {
            String errorMessage = "Could not load properties from " + file.getAbsolutePath();

            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(errorMessage + ": " + e.getMessage());
            }

            throw new IllegalArgumentException(errorMessage, e);
        }

        return properties;
    }

    private void assignProperties(Properties properties, Map<String, Object> input, String path) {
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.hasText(path)) {
                if (key.startsWith(BRACE_OPEN)) {
                    key = path + key;
                } else {
                    key = path + DOT + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                addToProperties(properties, key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) value;
                assignProperties(properties, map, key);
            } else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked")
                Collection<Object> collection = (Collection<Object>) value;
                addToProperties(properties, key, StringUtils.collectionToCommaDelimitedString(collection));
                int count = 0;
                for (Object object : collection) {
                    assignProperties(properties, Collections.singletonMap(BRACE_OPEN + (count++) + BRACE_CLOSE, object), key);
                }
            } else {
                addToProperties(properties, key, value == null ? EMPTY_STRING : value);
            }
        }
    }

    protected void addToProperties(Properties props, String key, Object value) {
        props.put(key, value == null ? EMPTY_STRING : value);
    }
}
