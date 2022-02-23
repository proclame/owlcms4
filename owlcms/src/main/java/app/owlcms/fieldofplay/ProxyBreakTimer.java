/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers
 * associated with each screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
/**
 * @author owlcms
 *
 */
public class ProxyBreakTimer implements IProxyTimer, IBreakTimer {

    private Integer breakDuration;
    private BreakType breakType;

    private LocalDateTime end;
    private FieldOfPlay fop;
    private boolean indefinite;
    private long lastStop;
    final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyBreakTimer.class);
    private Object origin;
    private boolean running = false;
    private long startMillis;
    private long stopMillis;
    private int timeRemaining;
    private int timeRemainingAtLastStop;
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Instantiates a new break timer proxy.
     *
     * @param fop the fop
     */
    public ProxyBreakTimer(FieldOfPlay fop) {
        this.setFop(fop);
    }

    @Override
    public void finalWarning(Object origin) {
        // ignored
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#getBreakDuration()
     */
    @Override
    public Integer getBreakDuration() {
        return breakDuration;
    }

    public BreakType getBreakType() {
        return breakType;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Object getOrigin() {
        return origin;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemaining()
     */
    @Override
    public int getTimeRemaining() {
        return timeRemaining;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemainingAtLastStop()
     */
    @Override
    public int getTimeRemainingAtLastStop() {
        return timeRemainingAtLastStop;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#initialWarning(java.lang.Object)
     */
    @Override
    public void initialWarning(Object origin) {
        // ignored
    }

    /**
     * @return the indefinite
     */
    @Override
    public boolean isIndefinite() {
        return indefinite;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#isRunning()
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Compute time elapsed since start.
     */
    @Override
    public int liveTimeRemaining() {
        if (end != null) {
            int until = (int) LocalDateTime.now().until(end, ChronoUnit.MILLIS);
            logger.debug("liveTimeRemaining target {} {}",
                    until >= 0 ? DurationFormatUtils.formatDurationHMS(until) : until,
                    LoggerUtils.whereFrom());
            return until;
        } else if (isRunning()) {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            int tr = (int) (getTimeRemaining() - elapsed);
            logger.debug("liveTimeRemaining running {} {}", tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
                    LoggerUtils.whereFrom());
            return tr;
        } else {
            int tr = getTimeRemaining();
            logger.debug("liveTimeRemaining stopped {} {}", tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
                    LoggerUtils.whereFrom());
            return tr;
        }
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setBreakDuration(java.lang.Integer)
     */
    @Override
    public void setBreakDuration(Integer breakDuration) {
        this.breakDuration = breakDuration;
    }

    public void setBreakType(BreakType breakType) {
        this.breakType = breakType;
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setEnd(java.time.LocalDateTime)
     */
    @Override
    public void setEnd(LocalDateTime targetTime) {
        indefinite = false;
        // end != null overrides duration computation
        logger.debug("setting end time = {}", targetTime);
        this.end = targetTime;
    }

    /**
     * @param fop the fop to set
     */
    @Override
    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setIndefinite()
     */
    @Override
    public void setIndefinite() {
        indefinite = true;
        logger.warn("setting breaktimer indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
        this.setTimeRemaining(0);
        this.setEnd(null);
        getFop().pushOut(
                new UIEvent.BreakSetTime(getFop().getBreakType(), getFop().getCountdownType(), getTimeRemaining(), null,
                        true, this));
        setRunning(false);
        indefinite = true;
    }

    @Override
    public void setOrigin(Object origin) {
        this.origin = origin;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
     *
     */
    @Override
    public void setTimeRemaining(int timeRemaining2) {
        indefinite = false;
        this.timeRemaining = timeRemaining2;
        setRunning(false);
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        startMillis = System.currentTimeMillis();
        BreakType breakType = getFop().getBreakType();
        this.breakType = breakType;
        UIEvent.BreakStarted event = new UIEvent.BreakStarted(isIndefinite() ? null : getMillis(), getOrigin(), false,
                breakType, getFop().getCountdownType(), LoggerUtils.stackTrace());
        logger.debug("posting {}", event);
        getFop().pushOut(event);
        setRunning(true);
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#stop()
     */
    @Override
    public void stop() {
        if (isRunning()) {
            computeTimeRemaining();
        }
        setRunning(false);
        timeRemainingAtLastStop = timeRemaining;
        logger.debug("***stopping Break -- timeRemaining = {} [{}]", getTimeRemaining(), LoggerUtils.whereFrom());
        timeRemainingAtLastStop = getTimeRemaining();
        logger.warn("break stop = {} [{}]", liveTimeRemaining(), LoggerUtils.whereFrom());
        UIEvent.BreakPaused event = new UIEvent.BreakPaused(isIndefinite() ? null : getTimeRemaining(), getOrigin(),
                false,
                getFop().getBreakType(), getFop().getCountdownType());
        getFop().pushOut(event);
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
     */
    @Override
    public void timeOver(Object origin) {
        if (isRunning() && !isIndefinite()) {
            long now = System.currentTimeMillis();
            if (now - lastStop > 1000) {
                // ignore rash of timers all signaling break over
                lastStop = System.currentTimeMillis();
                this.stop();
            } else {
                return;
            }
        } else {
            // we've already signaled time over.
            return;
        }
        logger.debug("break {} {} timeover = {} [{}]", isRunning(), isIndefinite(), getTimeRemaining(),
                LoggerUtils.whereFrom());

        // should emit sound at end of break
        getFop().pushOut(new UIEvent.BreakDone(origin, getFop().getBreakType()));
        getFop().fopEventPost(new FOPEvent.BreakDone(getFop().getBreakType(), origin));
//        BreakType breakType = getFop().getBreakType();
//        if (breakType == BreakType.FIRST_SNATCH || breakType == BreakType.FIRST_CJ) {
//            fop.fopEventPost(new FOPEvent.StartLifting(origin));
//        } else if (breakType == BreakType.BEFORE_INTRODUCTION) {
//            fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.DURING_INTRODUCTION, CountdownType.INDEFINITE, null,
//                    null, origin));
//        }
    }

    /**
     * @return the fop
     */
    FieldOfPlay getFop() {
        return fop;
    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private int computeTimeRemaining() {
        if (end != null) {
            setTimeRemaining((int) LocalDateTime.now().until(end, ChronoUnit.MILLIS));
        } else {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            setTimeRemaining((int) (getTimeRemaining() - elapsed));
        }
        return getTimeRemaining();
    }

    private int getMillis() {
        return (int) (this.getEnd() != null ? LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS)
                : getTimeRemaining());
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

}
