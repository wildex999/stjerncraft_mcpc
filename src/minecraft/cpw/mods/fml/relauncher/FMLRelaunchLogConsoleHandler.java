package cpw.mods.fml.relauncher;

import java.util.logging.ConsoleHandler;

public class FMLRelaunchLogConsoleHandler extends ConsoleHandler {

    public synchronized void flush() {
        super.flush();
    }
}