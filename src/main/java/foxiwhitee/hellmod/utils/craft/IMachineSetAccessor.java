package foxiwhitee.hellmod.utils.craft;

import appeng.api.networking.IGridHost;
import appeng.me.MachineSet;

public interface IMachineSetAccessor {
    default MachineSet create(Class<? extends IGridHost> m) {
        return null;
    }
}
