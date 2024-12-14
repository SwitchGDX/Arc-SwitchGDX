package arc.backend.switchgdx;

import arc.Application;
import arc.ApplicationListener;
import arc.Core;
import arc.Settings;
import arc.audio.Audio;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.TaskQueue;

public class SwitchApplication implements Application {
    private final Seq<ApplicationListener> listeners = new Seq<>();
    private final TaskQueue runnables = new TaskQueue();
    private boolean running;

    public static final boolean isSwitch = System.getProperty("os.name").equals("horizon");
    public static final boolean isUWP = System.getProperty("os.name").equals("uwp");
    
    final SwitchGraphics graphics;
    final SwitchInput input;
    final Config config;
    
    public SwitchApplication(ApplicationListener listener, Config config) {
        init(config.vsync);
        
        this.config = config;
        listeners.add(listener);

        Core.app = this;
        Core.files = new SwitchFiles();
        Core.graphics = graphics = new SwitchGraphics();
        Core.input = input = new SwitchInput();
        Core.settings = new Settings();
        Core.audio = new Audio();

        running = true;
        
        try {
            listen(ApplicationListener::init);
            
            int width = 0, height = 0;
            while (running && update()) {
                int currentWidth = graphics.getWidth();
                int currentHeight = graphics.getHeight();
                if (currentWidth != width || currentHeight != height) {
                    width = currentWidth;
                    height = currentHeight;
                    listener.resize(width, height);
                }

                graphics.update();
                input.update();
                defaultUpdate();
                listen(ApplicationListener::update);
                runnables.run();
                input.processDevices();
            }
        } catch (Throwable t) {
            System.out.println("Uncaught Exception:");
            t.printStackTrace();
            System.err.println("Message: " + t.getMessage());
            Throwable cause = t.getCause();
            while (cause != null) {
                System.err.println("Message: " + cause.getMessage());
                cause = cause.getCause();
            }
        }
        
        dispose0();

        System.exit(0);
    }
    
    @Override
    public Seq<ApplicationListener> getListeners() {
        return listeners;
    }

    @Override
    public ApplicationType getType() {
        return ApplicationType.switchGdx;
    }

    @Override
    public String getClipboardText() {
        return "";
    }

    @Override
    public void setClipboardText(String text) {
    }

    @Override
    public void post(Runnable runnable) {
        runnables.post(runnable);
    }

    @Override
    public void exit() {
        post(() -> running = false);
    }

    private void listen(Cons<ApplicationListener> cons){
        synchronized(listeners){
            for(ApplicationListener l : listeners){
                cons.get(l);
            }
        }
    }

    @Override
    public native boolean openURI(String URI);

    private static native void init(boolean vsync);

    private static native boolean update();

    private static native void dispose0();

    public static class Config {

        private boolean vsync = true;
        
        public Config() {
        }

        public Config(boolean vsync) {
            this.vsync = vsync;
        }

        public boolean getVsync() {
            return vsync;
        }

        public void setVsync(boolean vsync) {
            this.vsync = vsync;
        }
    }
}
