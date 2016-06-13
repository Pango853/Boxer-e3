package info.kuee.boxer3.dialogs;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
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
import org.osgi.service.prefs.Preferences;

import info.kuee.boxer3.Activator;

public class AuthPreferenceDialog extends TitleAreaDialog {
	public static final String PREF_KEY_AUTH_SECRETKEY = "auth.secret.key";
	public static final String PREF_KEY_AUTH_ACCESSKEY = "auth.access.key";
	private FieldEditorPreferencePage prefPage;
	private String accessKey, secretKey;
	private StringFieldEditor accessKeyField, secretKeyField;

	public AuthPreferenceDialog(Shell parentShell) {
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

		Preferences pref = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		accessKey = pref.get(PREF_KEY_AUTH_ACCESSKEY, "ACCESS_KEY");
		secretKey = pref.get(PREF_KEY_AUTH_SECRETKEY, "SECRET_KEY");

		prefPage = new FieldEditorPreferencePage(FieldEditorPreferencePage.GRID) {
			@Override
			public void createControl(Composite parent) {
				noDefaultAndApplyButton();
				super.createControl(parent);
			}

			@Override
			protected void createFieldEditors() {
				accessKeyField = new StringFieldEditor(PREF_KEY_AUTH_ACCESSKEY, "Access key：", getFieldEditorParent());
				accessKeyField.setStringValue(accessKey);
				addField(accessKeyField);

				secretKeyField = new StringFieldEditor(PREF_KEY_AUTH_SECRETKEY, "Secret key：", getFieldEditorParent()){
					@Override
					protected void doFillIntoGrid(Composite parent, int numColumns) {
						super.doFillIntoGrid(parent, numColumns);

						getTextControl().setEchoChar('*');
					}
					
				};
				secretKeyField.setStringValue(secretKey);
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
		Preferences pref = ConfigurationScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		pref.put(PREF_KEY_AUTH_ACCESSKEY, accessKeyField.getStringValue());
		pref.put(PREF_KEY_AUTH_SECRETKEY, secretKeyField.getStringValue());

		try {
			pref.flush();
			super.okPressed();
		} catch (BackingStoreException e) {
		}
	}

	private void updateButtons(boolean isValid) {
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isValid);
		}
	}

}
