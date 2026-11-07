package org.endeavourhealth.core.database.rdbms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeadlockHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DeadlockHandler.class);

    private static final String DEADLOCK_ERR = "Deadlock found when trying to get lock; try restarting transaction";
    private static final String LOCK_ERR = "Lock wait timeout exceeded; try restarting transaction";

    private int maxAttempts;
    private int attempts;
    private List<Pattern> errorMessages = new ArrayList<>();
    private Integer retryDelaySeconds;
    private Integer retryDelayMillis;
    private boolean delayBackOff = false; //whether to increase the delay between attempts as the number of attempts goes up
    private Set<String> matchedMessages = new HashSet<>();

    public DeadlockHandler() {
        this.attempts = 0;
        this.maxAttempts = 5; //allow five attempts by default
        this.retryDelaySeconds = new Integer(1); //default to 1s between attempts
        addErrorMessageToHandler(DEADLOCK_ERR);
        addErrorMessageToHandler(LOCK_ERR);
    }

    public void addErrorMessageToHandler(String regex) {
        Pattern p = Pattern.compile(regex);
        this.errorMessages.add(p);
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = new Integer(retryDelaySeconds);
        this.retryDelayMillis = null; //null the other field
    }

    public void setRetryDelayMillis(int retryDelayMillis) {
        this.retryDelayMillis = new Integer(retryDelayMillis);
        this.retryDelaySeconds = null; //null the other field
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setDelayBackOff(boolean delayBackOff) {
        this.delayBackOff = delayBackOff;
    }

    private long getNextDelayMillis() {

        //if delaying by MS, then it doesn't support the backoff mechanism, so just return the absolute delay
        if (retryDelayMillis != null) {
            return retryDelayMillis;
        }

        if (retryDelaySeconds != null) {
            if (delayBackOff) {
                //if backing off over time, generate the delay from the fibonacci sequence which seems to give
                //a fairly good distribution over time and times by the delay period
                return (long)getFibonacciValue(this.attempts + 1) * (long)retryDelaySeconds * 1000L;

            } else {
                //if not doing the back off thing, then always use the same delay period
                return (long)retryDelaySeconds * 1000L;
            }
        }

        throw new RuntimeException("No delay period set");
    }

    private int getFibonacciValue(int i) {

        if (i <= 2) {
            return 1;

        } else {
            return getFibonacciValue(i-1) + getFibonacciValue(i-2);
        }
    }

    public boolean canHandleError(Exception exc) {

        //if we're out of lives, then we won't handle it
        if (attempts >= maxAttempts) {
            return false;
        }

        boolean throwException = true;
        String matchingMessage = null;

        //check the messages against the ones we're filtering out, making sure to check
        //nested exceptions too
        Throwable t = exc;
        while (t != null && throwException) {
            String msg = t.getMessage();
            if (msg != null) {
                for (Pattern p : errorMessages) {
                    Matcher m = p.matcher(msg);
                    if (m.matches()) {
                        throwException = false;
                        matchingMessage = msg;
                        break;
                    }
                }
            }

            t = t.getCause();
        }

        if (throwException) {
            return false;
        }

        //if the error message has changed since we last tried (e.g. changed from one handled error message
        //to another) then we want to reset the number of attempts
        if (!matchedMessages.isEmpty()
            && !matchedMessages.contains(matchingMessage)) {
            this.attempts = 0;
            LOG.debug("Matching error message has changed since last attempt so resetting lives [" + matchingMessage + "]");
        }
        this.matchedMessages.add(matchingMessage);

        //if it's a matching error, decrease our lives and let it try again
        long delayMs = getNextDelayMillis();
        LOG.error("Error [" + exc.getMessage() + "] when writing to DB - will try again in " + delayMs + "ms (" + (maxAttempts-attempts) + " remaining)");
        attempts++;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            //we should never be interrupted so throw if we get it
            throw new RuntimeException("", ie);
        }

        return true;
    }


    public void handleError(Exception exc) throws Exception {

        boolean throwException = !canHandleError(exc);
        if (throwException) {
            throw exc;
        }
    }
}
