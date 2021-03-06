package org.ovirt.engine.core.bll;

import java.util.ArrayList;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;

public abstract class SortedMultipleActionsRunnerBase extends MultipleActionsRunner {
    public SortedMultipleActionsRunnerBase(VdcActionType actionType,
                                           ArrayList<VdcActionParametersBase> parameters, boolean isInternal) {
        super(actionType, parameters, isInternal);
    }

    protected abstract void sortCommands();

    @Override
    protected void runCommands() {
        sortCommands();
        super.runCommands();
    }
}
