package org.ovirt.engine.ui.webadmin.section.main.view.tab;

import java.util.List;

import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VMStatus;
import org.ovirt.engine.ui.common.CommonApplicationConstants;
import org.ovirt.engine.ui.common.idhandler.ElementIdHandler;
import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.action.ActionButtonDefinition;
import org.ovirt.engine.ui.common.widget.action.CommandLocation;
import org.ovirt.engine.ui.common.widget.table.column.EnumColumn;
import org.ovirt.engine.ui.common.widget.table.column.TextColumnWithTooltip;
import org.ovirt.engine.ui.uicommonweb.ReportInit;
import org.ovirt.engine.ui.uicommonweb.UICommand;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmListModel;
import org.ovirt.engine.ui.webadmin.ApplicationConstants;
import org.ovirt.engine.ui.webadmin.ApplicationResources;
import org.ovirt.engine.ui.webadmin.section.main.presenter.tab.MainTabVirtualMachinePresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.AbstractMainTabWithDetailsTableView;
import org.ovirt.engine.ui.webadmin.uicommon.ReportActionsHelper;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminImageButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.action.WebAdminMenuBarButtonDefinition;
import org.ovirt.engine.ui.webadmin.widget.table.column.CommentColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.PercentColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.UptimeColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VmStatusColumn;
import org.ovirt.engine.ui.webadmin.widget.table.column.VmTypeColumn;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class MainTabVirtualMachineView extends AbstractMainTabWithDetailsTableView<VM, VmListModel> implements MainTabVirtualMachinePresenter.ViewDef {

    interface ViewIdHandler extends ElementIdHandler<MainTabVirtualMachineView> {
        ViewIdHandler idHandler = GWT.create(ViewIdHandler.class);
    }

    private final CommonApplicationConstants commonConstants;

    @Inject
    public MainTabVirtualMachineView(MainModelProvider<VM, VmListModel> modelProvider,
            ApplicationResources resources, ApplicationConstants constants,
            CommonApplicationConstants commonConstants) {
        super(modelProvider);

        this.commonConstants = commonConstants;

        ViewIdHandler.idHandler.generateAndSetIds(this);
        initTable(resources, constants);
        initWidget(getTable());
    }

    void initTable(ApplicationResources resources, ApplicationConstants constants) {
        getTable().enableColumnResizing();

        getTable().addColumn(new VmStatusColumn<VM>(), constants.empty(), "30px"); //$NON-NLS-1$

        getTable().addColumn(new VmTypeColumn(), constants.empty(), "30px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> nameColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getName();
            }
        };
        getTable().addColumn(nameColumn, constants.nameVm(), "120px"); //$NON-NLS-1$

        CommentColumn<VM> commentColumn = new CommentColumn<VM>();
        getTable().addColumnWithHtmlHeader(commentColumn, commentColumn.getHeaderHtml(), "30px"); //$NON-NLS-1$


        TextColumnWithTooltip<VM> hostColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getRunOnVdsName();
            }
        };
        getTable().addColumn(hostColumn, constants.hostVm(), "120px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> ipColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getVmIp();
            }
        };
        getTable().addColumn(ipColumn, constants.ipVm(), "120px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> fqdnColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getVmFQDN();
            }

        };
        getTable().addColumn(fqdnColumn, constants.fqdn(), "120px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> clusterColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getVdsGroupName();
            }
        };
        getTable().addColumn(clusterColumn, constants.clusterVm(), "120px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> dcColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getStoragePoolName();
            }
        };
        getTable().addColumn(dcColumn, constants.dcVm(), "120px"); //$NON-NLS-1$

        PercentColumn<VM> memoryColumn = new PercentColumn<VM>() {
            @Override
            public Integer getProgressValue(VM object) {
                return object.getUsageMemPercent();
            }
        };
        getTable().addColumn(memoryColumn, constants.memoryVm(), "60px"); //$NON-NLS-1$

        PercentColumn<VM> cpuColumn = new PercentColumn<VM>() {
            @Override
            public Integer getProgressValue(VM object) {
                return object.getUsageCpuPercent();
            }
        };
        getTable().addColumn(cpuColumn, constants.cpuVm(), "60px"); //$NON-NLS-1$

        PercentColumn<VM> networkColumn = new PercentColumn<VM>() {
            @Override
            public Integer getProgressValue(VM object) {
                return object.getUsageNetworkPercent();
            }
        };
        getTable().addColumn(networkColumn, constants.networkVm(), "60px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> displayColumn = new EnumColumn<VM, DisplayType>() {
            @Override
            protected DisplayType getRawValue(VM object) {
                return object.getDisplayType();
            }

            @Override
            public String getValue(VM object) {
                if ((object.getStatus() == VMStatus.Down) || (object.getStatus() == VMStatus.ImageLocked))
                    return ""; //$NON-NLS-1$
                else
                    return renderer.render(getRawValue(object));
            }
        };
        getTable().addColumn(displayColumn, constants.displayVm(), "80px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> statusColumn = new EnumColumn<VM, VMStatus>() {
            @Override
            public VMStatus getRawValue(VM object) {
                switch (object.getStatus()) {
                case PreparingForHibernate: return VMStatus.SavingState;
                default:                    return object.getStatus();
                }
            }
        };
        getTable().addColumn(statusColumn, constants.statusVm(), "80px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> uptimeColumn = new UptimeColumn<VM>() {
            @Override
            public Double getRawValue(VM object) {
                return object.getRoundedElapsedTime();
            }
        };
        getTable().addColumn(uptimeColumn, constants.uptimeVm(), "120px"); //$NON-NLS-1$

        TextColumnWithTooltip<VM> descriptionColumn = new TextColumnWithTooltip<VM>() {
            @Override
            public String getValue(VM object) {
                return object.getDescription();
            }
        };
        getTable().addColumn(descriptionColumn, constants.description(), "150px"); //$NON-NLS-1$

        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.newVm()) {

            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getNewVmCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.editVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.removeVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRemoveCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.runOnceVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRunOnceCommand();
            }
        });
        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.runVm(),
                resources.runVmImage(), resources.runVmDisabledImage()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRunCommand();
            }
        });
        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.suspendVm(),
                resources.suspendVmImage(), resources.suspendVmDisabledImage()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getPauseCommand();
            }
        });
        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.shutDownVm(),
                resources.stopVmImage(), resources.stopVmDisabledImage()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getShutdownCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.powerOffVm(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getStopCommand();
            }
        });
        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.rebootVm(),
                resources.rebootImage(), resources.rebootDisabledImage()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getRebootCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.consoleVm(),
                resources.consoleImage(), resources.consoleDisabledImage()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getConsoleConnectCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(commonConstants.consoleOptions(),
                CommandLocation.OnlyFromContext) { //$NON-NLS-1$
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEditConsoleCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.migrateVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getMigrateCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.cancelMigrationVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getCancelMigrateCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.makeTemplateVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getNewTemplateCommand();
            }
        });
        // TODO: separator
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.exportVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getExportCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.createSnapshotVM()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getCreateSnapshotCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.cheangeCdVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getChangeCdCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.assignTagsVm()) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getAssignTagsCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.enableGlobalHaMaintenanceVm(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getEnableGlobalHaMaintenanceCommand();
            }
        });
        getTable().addActionButton(new WebAdminButtonDefinition<VM>(constants.disableGlobalHaMaintenanceVm(), CommandLocation.OnlyFromContext) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getDisableGlobalHaMaintenanceCommand();
            }
        });

        if (ReportInit.getInstance().isReportsEnabled()) {
            List<ActionButtonDefinition<VM>> resourceSubActions =
                    ReportActionsHelper.getInstance().getResourceSubActions("VM", getModelProvider()); //$NON-NLS-1$
            if (resourceSubActions != null && resourceSubActions.size() > 0) {
                getTable().addActionButton(new WebAdminMenuBarButtonDefinition<VM>(constants.showReportVm(),
                        resourceSubActions));
            }
        }

        getTable().addActionButton(new WebAdminImageButtonDefinition<VM>(constants.guideMeVm(),
                resources.guideSmallImage(), resources.guideSmallDisabledImage(), true) {
            @Override
            protected UICommand resolveCommand() {
                return getMainModel().getGuideCommand();
            }
        });
    }
}
