package info.kuee.boxer3.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class NewFolderHandler extends AbstractHandler {

	@SuppressWarnings("restriction")
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		// TODO
		MessageDialog.openInformation(window.getShell(), "", "");

		if (window instanceof org.eclipse.ui.internal.WorkbenchWindow) {
			EModelService modelService = window.getService(EModelService.class);
			MWindow model = ((org.eclipse.ui.internal.WorkbenchWindow) window).getModel();
			MToolControl switcherControl = (MToolControl) modelService.find("PerspectiveSwitcher", model); //$NON-NLS-1$
			if (null != switcherControl) {
				//switcherControl.getTags().remove("HIDEABLE");
				//switcherControl.getTags().remove("SHOW_RESTORE_MENU");
				//switcherControl.getTags().add(IPresentationEngine.HIDDEN_EXPLICITLY);
				switcherControl.getTags().remove(IPresentationEngine.HIDDEN_EXPLICITLY);
//				MWindow mWindow = modelService.getTopLevelWindowFor(switcherControl);
//				List<MTrimElement> trimElements = modelService.findElements(mWindow,
//						null, MTrimElement.class, null);
//				for (MTrimElement trimElement : trimElements) {
//					trimElement.getTags().remove(IPresentationEngine.HIDDEN_EXPLICITLY);
//				}
			}
		}
		return null;
	}

}
