package engine;

public class StateConfig {
    private final double speedMetersPerSec;
    private final String nextStateWhenFinished;
    private final int framesPerSec;
    private final boolean isLoop;
    private final int frameCount;

    public StateConfig(double speedMetersPerSec, String nextStateWhenFinished,
                      int framesPerSec, boolean isLoop, int frameCount) {
        this.speedMetersPerSec = speedMetersPerSec;
        this.nextStateWhenFinished = nextStateWhenFinished;
        this.framesPerSec = framesPerSec;
        this.isLoop = isLoop;
        this.frameCount = frameCount;
    }

    public double speedMetersPerSec() { return speedMetersPerSec; }
    public String nextStateWhenFinished() { return nextStateWhenFinished; }
    public int framesPerSec() { return framesPerSec; }
    public boolean isLoop() { return isLoop; }
    public int frameCount() { return frameCount; }
}
