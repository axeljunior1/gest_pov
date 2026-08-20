package com.gestpov.desktop.util;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Exécute un appel hors thread JavaFX, puis revient sur l'UI.
 */
public final class FxAsync {

    private FxAsync() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Thread thread = new Thread(() -> {
            try {
                T result = work.call();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Throwable error) {
                Platform.runLater(() -> onError.accept(error));
            }
        }, "gest-pov-async");
        thread.setDaemon(true);
        thread.start();
    }

    public static void runVoid(ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        run(() -> {
            work.run();
            return Boolean.TRUE;
        }, ignored -> onSuccess.run(), onError);
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
