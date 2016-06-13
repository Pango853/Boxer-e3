package info.kuee.boxer3.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.kuee.boxer3.auth.AuthUtil;
import info.kuee.boxer3.auth.AuthUtil.AccessKeyPair;
import info.kuee.boxer3.util.Error;

public class AccessKeyDialog extends TitleAreaDialog {
	private static final Logger logger = LoggerFactory.getLogger(AccessKeyDialog.class);

	private FieldEditorPreferencePage prefPage;
	private StringFieldEditor accessKeyField, secretKeyField;

	public AccessKeyDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * ダイアログ・コンテンツの作成
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("Authenication");
		setTitle("Dropbox OAuth");
		setMessage("Input the access key and secret key.");

		Composite area = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) area.getLayout();
		layout.marginTop = 10;
		layout.marginBottom = 10;
		layout.marginLeft = 10;
		layout.marginRight = 10;
		area.layout();

		AccessKeyPair keyPair = AuthUtil.getAccessKeyPair();
		prefPage = new FieldEditorPreferencePage(FieldEditorPreferencePage.GRID) {
			@Override
			public void createControl(Composite parent) {
				noDefaultAndApplyButton();
				super.createControl(parent);
			}

			@Override
			protected void createFieldEditors() {
				accessKeyField = new StringFieldEditor(AuthUtil.PREF_KEY_AUTH_ACCESSKEY, "Access key：", getFieldEditorParent());
				accessKeyField.setStringValue(keyPair.AccessKey);
				addField(accessKeyField);

				secretKeyField = new StringFieldEditor(AuthUtil.PREF_KEY_AUTH_SECRETKEY, "Secret key：", getFieldEditorParent()){
					@Override
					protected void doFillIntoGrid(Composite parent, int numColumns) {
						super.doFillIntoGrid(parent, numColumns);

						getTextControl().setEchoChar('*');
					}
					
				};
				secretKeyField.setStringValue(keyPair.SecretKey);
				addField(secretKeyField);
			}

			@Override
			protected void updateApplyButton() {
				updateButtons(isValid());
				super.updateApplyButton();
			}
		};
		prefPage.createControl(area);
		Control pageControl = prefPage.getControl();
		pageControl.setLayoutData(new GridData(GridData.FILL_BOTH));
		return area;
	}

	/**
	 * ボタン・バーの作成
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		updateButtons(prefPage.isValid());
	}

	/** ダイアログの初期サイズ */
	@Override
	protected Point getInitialSize() {
		return new Point(480, 320);
	}

	@Override
	protected void okPressed() {
		try{
		AuthUtil.storeAccessKeyPair(accessKeyField.getStringValue(), secretKeyField.getStringValue());
			super.okPressed();
		}catch(BackingStoreException e){
			logger.error(Error.INVALID_INPUT, e);
		}
	}

	private void updateButtons(boolean isValid) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isValid);
		}
	}

}
