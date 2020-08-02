package jmri.jmrix.xpa;

/**
 * Defines the interface for listening to traffic sent to an XpressNet based
 * Command Station via an XPA and a modem.
 *
 * @author Paul Bender Copyright (C) 2004
 */
public interface XpaListener extends java.util.EventListener {

    void message(XpaMessage m);

    void reply(XpaMessage m);
}
