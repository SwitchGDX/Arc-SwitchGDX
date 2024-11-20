package arc.backend.switchgdx;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.graphics.GL20;
import arc.graphics.GL30;
import arc.graphics.Pixmap;
import arc.graphics.gl.GLVersion;

public class SwitchGraphics extends Graphics {

    private final BufferFormat bufferFormat;
    private final GLVersion glVersion;
    private final String extensions;

    private float time;
    private float deltaTime;
    private long frameId = -1;
    private int frames;
    private long previousTime;
    private int fps;

    public SwitchGraphics () {
        Core.gl = Core.gl20 = new SwitchGL();

        String versionString = Core.gl.glGetString(GL20.GL_VERSION);
        String vendorString = Core.gl.glGetString(GL20.GL_VENDOR);
        String rendererString = Core.gl.glGetString(GL20.GL_RENDERER);
        glVersion = new GLVersion(Application.ApplicationType.desktop, versionString, vendorString, rendererString);

        extensions = Core.gl.glGetString(GL20.GL_EXTENSIONS);

        bufferFormat = new BufferFormat(8, 8, 8, 8, 24, 8, 0, false);

        previousTime = System.currentTimeMillis();
    }
    
    void update() {
        long timestamp = System.currentTimeMillis();
        deltaTime = (Math.max(timestamp - previousTime, 1)) / 1000.f;
        previousTime = timestamp;
        time += deltaTime;
        frameId++;
        frames++;
        if (time > 1) {
            fps = frames;
            time = 0;
            frames = 0;
        }
    }
    
    @Override
    public GL20 getGL20() {
        return Core.gl20;
    }

    @Override
    public void setGL20(GL20 gl20) {
    }

    @Override
    public GL30 getGL30() {
        return null;
    }

    @Override
    public void setGL30(GL30 gl30) {
    }

    @Override
    public native int getWidth ();

    @Override
    public native int getHeight ();

    @Override
    public int getBackBufferWidth () {
        return getWidth();
    }

    @Override
    public int getBackBufferHeight () {
        return getHeight();
    }

    @Override
    public long getFrameId () {
        return frameId;
    }

    @Override
    public float getDeltaTime () {
        return deltaTime;
    }

    @Override
    public int getFramesPerSecond () {
        return fps;
    }

    @Override
    public GLVersion getGLVersion () {
        return glVersion;
    }

    @Override
    public float getPpiX () {
        return 160;
    }

    @Override
    public float getPpiY () {
        return 160;
    }

    @Override
    public float getPpcX () {
        return 63;
    }

    @Override
    public float getPpcY () {
        return 63;
    }

    @Override
    public float getDensity() {
        return 1;
    }

    @Override
    public boolean setWindowedMode(int width, int height) {
        return false;
    }

    @Override
    public void setTitle(String title) {
    }

    @Override
    public void setBorderless(boolean undecorated) {
    }

    @Override
    public void setResizable(boolean resizable) {
    }

    @Override
    public void setVSync(boolean vsync) {
    }

    @Override
    public BufferFormat getBufferFormat() {
        return bufferFormat;
    }

    @Override
    public boolean supportsExtension(String extension) {
        return extensions.contains(extension);
    }

    @Override
    public boolean isContinuousRendering() {
        return true;
    }

    @Override
    public void setContinuousRendering(boolean isContinuous) {
    }

    @Override
    public void requestRendering() {
    }

    @Override
    public boolean isFullscreen() {
        return true;
    }

    @Override
    public Cursor newCursor(Pixmap pixmap, int xHotspot, int yHotspot) {
        return null;
    }

    @Override
    protected void setCursor(Cursor cursor) {
    }

    @Override
    protected void setSystemCursor(Cursor.SystemCursor systemCursor) {
    }
}
