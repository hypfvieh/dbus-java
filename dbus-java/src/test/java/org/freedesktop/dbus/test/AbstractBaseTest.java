package org.freedesktop.dbus.test;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base test class providing logger and common methods.
 * 
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-14
 */
public class AbstractBaseTest extends Assertions {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

}
