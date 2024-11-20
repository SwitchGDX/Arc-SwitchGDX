package arc.backend.switchgdx;

import arc.Core;
import arc.Files;
import arc.files.Fi;
import arc.util.ArcRuntimeException;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SwitchFiles implements Files {
    @Override
    public Fi get(String path, FileType type) {
        if (type == FileType.classpath) {
            type = FileType.internal;
            path = "classpath/" + path;
        }
        return new SwitchFi(path, type);
    }
    
    @Override
    public String getExternalStoragePath () {
        return SwitchApplication.isSwitch ? "sdmc:" : "sdmc";
    }

    @Override
    public boolean isExternalStorageAvailable () {
        return true;
    }

    @Override
    public native String getLocalStoragePath ();

    @Override
    public boolean isLocalStorageAvailable () {
        return true;
    }
    
    public static class SwitchFi extends Fi {
        public SwitchFi (String fileName, Files.FileType type) {
            super(fileName, type);
        }

        public SwitchFi (File file, Files.FileType type) {
            super(file, type);
        }

        @Override
        public Fi child (String name) {
            if (file.getPath().length() == 0) return new SwitchFi(new File(name), type);
            return new SwitchFi(new File(file, name), type);
        }

        @Override
        public Fi sibling (String name) {
            if (file.getPath().length() == 0) throw new ArcRuntimeException("Cannot get the sibling of the root.");
            return new SwitchFi(new File(file.getParent(), name), type);
        }

        @Override
        public Fi parent () {
            File parent = file.getParentFile();
            if (parent == null) {
                if (type == Files.FileType.absolute)
                    parent = new File("/");
                else
                    parent = new File("");
            }
            return new SwitchFi(parent, type);
        }

        @Override
        public File file () {
            if (type == FileType.internal)
                return new File(SwitchApplication.isSwitch ? "romfs:" : (SwitchApplication.isUWP ? "" : "romfs"), file.getPath());
            if (type == FileType.local)
                return new File(Core.files.getLocalStoragePath(), file.getPath());
            return super.file();
        }

        @Override
        public boolean isDirectory() {
            if (path().startsWith("classpath/"))
                return false;
            return super.isDirectory();
        }

        @Override
        public ByteBuffer map (FileChannel.MapMode mode) {
            throw new ArcRuntimeException("Cannot map files in Switch backend");
        }
    }
}
