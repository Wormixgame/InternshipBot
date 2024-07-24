package tech.reliab.kaiten.kaiten;


import java.time.Instant;

public abstract class KaitenTask {
    protected abstract void performTask(Instant lastTaskEndedAt);
    public Instant execute(Instant lastTaskEndedAt) {
        performTask(lastTaskEndedAt);
        return Instant.now();
    }
}
