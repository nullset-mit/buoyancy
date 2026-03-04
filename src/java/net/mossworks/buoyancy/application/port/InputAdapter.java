package net.mossworks.buoyancy.application.port;

/**
 * Contract for all input adapters. The composition root calls run() to start
 * whichever adapter is wired (CLI, web, etc.).
 */
public interface InputAdapter {
    void run();
}
