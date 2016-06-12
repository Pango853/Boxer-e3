package info.kuee.boxer3.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import info.kuee.boxer3.dialogs.MyPreferenceDialog;

public class AuthHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		MyPreferenceDialog dlg = new MyPreferenceDialog(window.getShell());
		int result = dlg.open();
		if (result == Window.OK) {
			// TODO
		}

		return null;
	}

}
