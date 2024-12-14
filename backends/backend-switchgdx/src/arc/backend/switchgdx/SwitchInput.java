package arc.backend.switchgdx;

import arc.Input;
import arc.input.InputDevice;
import arc.input.InputProcessor;
import arc.input.KeyCode;

public class SwitchInput extends Input {

    private static final int MAX_TOUCHES = 16;
    
    private static final KeyCode[] SWITCH_KEYS = {
        KeyCode.controllerA, KeyCode.controllerB, KeyCode.controllerX, KeyCode.controllerY,
        KeyCode.buttonThumbL, KeyCode.buttonThumbR,
        KeyCode.buttonL1, KeyCode.buttonR1, KeyCode.buttonL2, KeyCode.buttonR2,
        KeyCode.escape, KeyCode.buttonStart,
        KeyCode.a, KeyCode.w, KeyCode.d, KeyCode.s, 
    };

    private long currentEventTimeStamp;
    private final int[] touchData = new int[MAX_TOUCHES * 3];
    private final int[] previousTouchData = new int[MAX_TOUCHES * 3];
    private final int[] rawTouchIds = new int[MAX_TOUCHES];
    private final int[] touchX = new int[MAX_TOUCHES];
    private final int[] touchY = new int[MAX_TOUCHES];
    private final int[] deltaX = new int[MAX_TOUCHES];
    private final int[] deltaY = new int[MAX_TOUCHES];
    private final boolean[] touched = new boolean[MAX_TOUCHES];
    private boolean wasJustTouched;
    private int prevButtons;
    private final float[] axes = new float[4];

    public SwitchInput () {
        for (int i = 0; i < MAX_TOUCHES; i++) {
            previousTouchData[i * 3] = -1;
            rawTouchIds[i] = -1;
        }
    }
    
    void update() {
        currentEventTimeStamp = System.nanoTime();
        wasJustTouched = false;
        getTouchData(touchData);

        // Todo: There's probably a better way to optimize this

        for (int i = 0; i < MAX_TOUCHES; i++) {
            int rawIndex = touchData[i * 3];
            if (rawIndex == -1)
                continue;
            int previousIndex = -1;
            for (int j = 0; j < MAX_TOUCHES; j++)
                if (previousTouchData[j * 3] == rawIndex) {
                    previousIndex = j;
                    break;
                }
            if (previousIndex == -1) {
                wasJustTouched = true;
                for (int j = 0; j < MAX_TOUCHES; j++)
                    if (rawTouchIds[j] == -1) {
                        touchX[j] = touchData[i * 3 + 1];
                        touchY[j] = touchData[i * 3 + 2];
                        touched[j] = true;
                        rawTouchIds[j] = rawIndex;
                        inputMultiplexer.touchDown(touchData[i * 3 + 1], touchData[i * 3 + 2], j, KeyCode.mouseLeft);
                        break;
                    }
            } else if (touchData[i * 3 + 1] != previousTouchData[previousIndex * 3 + 1] || touchData[i * 3 + 2] != previousTouchData[previousIndex * 3 + 2]) {
                for (int j = 0; j < MAX_TOUCHES; j++)
                    if (rawTouchIds[j] == rawIndex) {
                        deltaX[j] = touchData[i * 3 + 1] - touchX[j];
                        deltaY[j] = touchData[i * 3 + 2] - touchY[j];
                        touchX[j] = touchData[i * 3 + 1];
                        touchY[j] = touchData[i * 3 + 2];
                        inputMultiplexer.touchDragged(touchData[i * 3 + 1], touchData[i * 3 + 2], j);
                        break;
                    }
            }
        }

        for (int i = 0; i < MAX_TOUCHES; i++) {
            int rawPreviousIndex = previousTouchData[i * 3];
            if (rawPreviousIndex == -1)
                continue;
            int index = -1;
            for (int j = 0; j < MAX_TOUCHES; j++)
                if (touchData[j * 3] == rawPreviousIndex) {
                    index = j;
                    break;
                }
            if (index == -1) {
                for (int j = 0; j < MAX_TOUCHES; j++)
                    if (rawTouchIds[j] == rawPreviousIndex) {
                        touchX[j] = previousTouchData[i * 3 + 1];
                        touchY[j] = previousTouchData[i * 3 + 2];
                        deltaX[j] = 0;
                        deltaY[j] = 0;
                        touched[j] = false;
                        rawTouchIds[j] = -1;
                        inputMultiplexer.touchUp(previousTouchData[i * 3 + 1], previousTouchData[i * 3 + 2], j, KeyCode.mouseLeft);
                        break;
                    }
            }
        }

        System.arraycopy(touchData, 0, previousTouchData, 0, MAX_TOUCHES * 3);

        int buttons = getButtons(-1);
        getAxes(-1, axes);
        if (Math.abs(axes[0]) > 0.1f)
            buttons |= axes[0] < 0 ? 1 << 12 : 1 << 14;
        if (Math.abs(axes[1]) > 0.1f)
            buttons |= axes[1] < 0 ? 1 << 13 : 1 << 15;
        for (int i = 0; i < 16; i++) {
            int bit = 1 << i;
            if ((buttons & bit) != 0 && (prevButtons & bit) == 0)
                inputMultiplexer.keyDown(SWITCH_KEYS[i]);
            else if ((buttons & bit) == 0 && (prevButtons & bit) != 0)
                inputMultiplexer.keyUp(SWITCH_KEYS[i]);
        }
        prevButtons = buttons;
    }

    void processDevices(){
        for (InputDevice device : devices) {
            device.postUpdate();
        }
    }
    
    @Override
    public int mouseX() {
        return touchX[0];
    }

    @Override
    public int mouseX(int pointer) {
        return touchX[pointer];
    }

    @Override
    public int deltaX() {
        return deltaX[0];
    }

    @Override
    public int deltaX(int pointer) {
        return deltaX[pointer];
    }

    @Override
    public int mouseY() {
        return touchY[0];
    }

    @Override
    public int mouseY(int pointer) {
        return touchY[pointer];
    }

    @Override
    public int deltaY() {
        return deltaY[0];
    }

    @Override
    public int deltaY(int pointer) {
        return deltaY[pointer];
    }

    @Override
    public boolean isTouched() {
        return touched[0];
    }

    @Override
    public boolean justTouched() {
        return wasJustTouched;
    }

    @Override
    public boolean isTouched(int pointer) {
        return touched[pointer];
    }

    @Override
    public long getCurrentEventTime() {
        return currentEventTimeStamp;
    }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) {
        return peripheral == Peripheral.multitouchScreen;
    }

    @Override
    public native void getTextInput(TextInput input);
    
    private static native void getTouchData(int[] touchData);
    
    private static native int getButtons(int controller);

    private static native void getAxes(int controller, float[] axes);
    
    private static native boolean isConnected(int controller);
    
    private static native void remapControllers(int min, int max, boolean dualJoy, boolean singleMode);
}
