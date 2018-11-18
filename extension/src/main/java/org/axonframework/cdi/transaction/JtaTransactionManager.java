package org.axonframework.cdi.transaction;

import org.axonframework.common.transaction.Transaction;
import org.axonframework.common.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import java.lang.invoke.MethodHandles;


/**
 *
 */
public class JtaTransactionManager implements TransactionManager {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String JBOSS_USER_TRANSACTION_LOCATION
            = "java:jboss/UserTransaction";

    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_LOCATION
            = "java:comp/TransactionSynchronizationRegistry";

    private static final Logger logger = LoggerFactory.getLogger(
            MethodHandles.lookup().lookupClass());

    /**
     * Attempt to obtain an object from the JNDI. If complain is set to true, failure results in an error being logged.
     * If not, a debug message is generated instead with the Exception added. If complain is false and the lookup
     * succeeds, then an info-level message is generated to tell of this. The object is returned if found, null otherwise.
     *
     * @param path The JNDI path.
     * @param desc A description of what kind of object is requested.
     * @param complain True if an error should be logged if the object is not found.
     * @return The object requested, or null if not found.
     */
    private static Object getFromJNDI(final String path, final String desc, boolean complain) {
        Object result = null;
        try {
            logger.debug("Attempting to look up {}.", desc);

            result = new InitialContext().lookup(path);

            if (!complain) {
                logger.info("{} will be used.", desc);
            }
        } catch (NamingException ex1) {
            if (complain) {
                logger.error("Failed to look up {}");
            } else {
                logger.debug("Could not look up {}.", ex1);
            }
        }
        return result;
    }

    private static final class TransactionPath {
        final String path;
        final String description;

        TransactionPath(String path, String description) {
            this.path = path;
            this.description = description;
        }
    }
    private static final TransactionPath userTransactionPaths[] = {
            new TransactionPath(USER_TRANSACTION_LOCATION, "Standard User Transactions"),
            new TransactionPath(JBOSS_USER_TRANSACTION_LOCATION, "JBoss User Transactions")
    };
    private static boolean utLookupDone = false;
    private static TransactionPath jndiPathUT = null;

    /**
     * Get the {@Link javax.transaction.UserTransaction} object if available. The first time we'll iterate through different options, after that
     * we'll reuse this nowledge, or else complain if we found nothing.
     * @return The UserTransaction object if available.
     */
    private UserTransaction getUT() {
        UserTransaction result = null;
        if ((jndiPathTSR == null) && !utLookupDone) { // Only bother if we don't have CMT
            for (TransactionPath tp: userTransactionPaths) {
                result = (UserTransaction) getFromJNDI(tp.path, tp.description, false);

                if (result != null) {
                    jndiPathUT = tp; // First to be found will henceforth be used.
                    break;
                }
            }
            if (result == null) {
                logger.warn("Could not find either standard, nor JBoss proprietary, UserTransaction. BMT Transactions cannot be used!.");
            }
            utLookupDone = true;
        } else if (jndiPathTSR == null ) { // Only bother if we have no alternative
            result = (UserTransaction) getFromJNDI(jndiPathUT.path, "User Transaction", true);
        }
        return result;
    }

    private static final TransactionPath transactionsynchronizationRegistryPaths[] = {
            new TransactionPath(TRANSACTION_SYNCHRONIZATION_REGISTRY_LOCATION, "CMT Transaction Synchronization Registry")
    };
    private static boolean tsrLookupDone = false;
    private static TransactionPath jndiPathTSR = null;

    /**
     * Get the {@Link javax.transaction.TransactionSynchronisationRegistry} object if available. The first time we'll iterate through different options, after that
     * we'll reuse this nowledge, or else complain if we found nothing.
     * @return The TransactionSynchronizationRegistry object if available.
     */
    private TransactionSynchronizationRegistry getTSR() {
        TransactionSynchronizationRegistry result = null;
        if ((jndiPathUT == null) && !tsrLookupDone) { // Only bother if we don't have BMT
            for (TransactionPath tp: transactionsynchronizationRegistryPaths) {
                result = (TransactionSynchronizationRegistry) getFromJNDI(tp.path, tp.description, false);

                if (result != null) {
                    jndiPathTSR = tp; // First to be found will henceforth be used.
                    break;
                }
            }
            if (result == null) {
                logger.error("Could not find a Transaction Synchronization Registry. CMT Transactions cannot be used.");
            }

            tsrLookupDone = true;
        } else if (jndiPathUT == null) { // Only bother if we need CMT
            result = (TransactionSynchronizationRegistry) getFromJNDI(jndiPathTSR.path, "Transaction Synchronization Registry", true);
        }
        return result;
    }

    @Override
    public Transaction startTransaction() {
        return new JtaTransaction(getUT(), getTSR());
    }
}
