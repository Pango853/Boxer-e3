package info.kuee.boxer3.app;

import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@SuppressWarnings("restriction")
	@Override
	public void postWindowCreate() {
		super.postWindowCreate();

		IWorkbenchWindow window = getWindowConfigurer().getWindow();
		if (window instanceof org.eclipse.ui.internal.WorkbenchWindow) {
			EModelService modelService = window.getService(EModelService.class);
			MWindow model = ((org.eclipse.ui.internal.WorkbenchWindow) window).getModel();
			MToolControl switcherControl = (MToolControl) modelService.find("PerspectiveSwitcher", model); //$NON-NLS-1$
			if (null != switcherControl && switcherControl.getTags().contains(IPresentationEngine.HIDDEN_EXPLICITLY)) {
				switcherControl.getTags().remove(IPresentationEngine.HIDDEN_EXPLICITLY);
			}
		}
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(480, 768));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setTitle("Boxer");
	}

}
