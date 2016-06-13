package info.kuee.boxer3.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.dropbox.core.DbxException;
import com.dropbox.core.v1.DbxAccountInfo;

import info.kuee.boxer3.auth.AuthUtil;
 
public class AccountView extends ViewPart {
	public AccountView() {}

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);

		AuthUtil.loginIfNot(site.getShell());
	}

	private void addTextRow(Composite comp, String label, String value){
		new Label(comp, SWT.LEFT).setText(label);
		new Label(comp, SWT.CENTER).setText(":");
		Text valueTxt = new Text(comp, SWT.LEFT | SWT.READ_ONLY | SWT.BORDER);
		valueTxt.setText(value);
		valueTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	private void addLinkRow(Composite comp, String label, String value){
		new Label(comp, SWT.LEFT).setText(label);
		new Label(comp, SWT.CENTER).setText(":");
		Link valueLink = new Link(comp, SWT.LEFT | SWT.BORDER);
		valueLink.setText("<A HREF=\"" + value + "\">" + value + "</A>");
		valueLink.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
	}
	private void addAccountInfoRows(Composite comp, DbxAccountInfo accountInfo){
		addTextRow(comp, "ID", String.valueOf(accountInfo.userId));
		addTextRow(comp, "Name", accountInfo.displayName);
		addLinkRow(comp, "Link", accountInfo.referralLink);
		addTextRow(comp, "Quota", accountInfo.quota.toString());
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite infoComp = new Composite(parent, SWT.NONE);
		// GridLayout
		GridLayout layout = new GridLayout(3, false);
		infoComp.setLayout(layout);

		try {
			addAccountInfoRows(infoComp, AuthUtil.getClient().getAccountInfo());

			// TODO
			//Button logoutBtn = new Button(infoComp, SWT.NONE);
			//logoutBtn.setText("Logout");
			//logoutBtn.addSelectionListener(new SelectionAdapter() {

			//	@Override
			//	public void widgetSelected(SelectionEvent e) {
			//		super.widgetSelected(e);
			//		AuthUtil.forgetToken();
			//	}
			//});
			//GridData gridData = new GridData(SWT.CENTER, GridData.VERTICAL_ALIGN_CENTER, false, false, 2, 1);
			//logoutBtn.setLayoutData(gridData);
		} catch (DbxException e) {
			addTextRow(infoComp, "N/A", "N/A");
		}
	}

	@Override
	public void setFocus() {
	}
}
