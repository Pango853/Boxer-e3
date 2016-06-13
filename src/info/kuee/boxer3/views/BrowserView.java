package info.kuee.boxer3.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import info.kuee.boxer3.auth.AuthServer;

public class BrowserView extends ViewPart {
	private Browser browser;

	public BrowserView() {}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		browser = new Browser(parent, SWT.NONE);
		browser.setUrl(AuthServer.getAuthURL());
	}

	@Override
	public void setFocus() {
		browser.setFocus();
	}
}
