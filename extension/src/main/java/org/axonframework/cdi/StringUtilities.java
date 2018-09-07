package org.axonframework.cdi;

import java.util.Optional;

class StringUtilities {

    static String lowerCaseFirstLetter(String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    static Optional<String> createOptional(String value) {
        if ("".equals(value)) {
            return Optional.empty();
        }
        
        return Optional.of(value);
    }
}
