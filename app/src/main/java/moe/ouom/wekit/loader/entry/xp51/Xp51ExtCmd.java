package moe.ouom.wekit.loader.entry.xp51;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Objects;

import de.robv.android.xposed.XposedBridge;

public class Xp51ExtCmd {

    public static Object handleQueryExtension(@NonNull String cmd) {
        Objects.requireNonNull(cmd, "cmd");
        return switch (cmd) {
            case "GetXposedBridgeClass" -> XposedBridge.class;
            case "GetLoadPackageParam" -> Xp51HookEntry.getLoadPackageParam();
            case "GetInitZygoteStartupParam" -> Xp51HookEntry.getInitZygoteStartupParam();
            case "GetInitErrors" -> new ArrayList<>();
            default -> null;
        };
    }
}
