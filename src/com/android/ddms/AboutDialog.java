package com.android.ddms;

import com.android.ddmlib.Log;
import com.android.ddmuilib.ImageLoader;
import java.io.InputStream;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class AboutDialog extends Dialog {
	private Image logoImage;

	public AboutDialog(Shell parent) {
		this(parent, 67680);
	}

	public AboutDialog(Shell parent, int style) {
		super(parent, style);
	}

	public void open() {
		Shell parent = getParent();
		Shell shell = new Shell(parent, getStyle());
		shell.setText("About...");

		this.logoImage = loadImage(shell, "ddms-128.png");
		createContents(shell);
		shell.pack();

		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		this.logoImage.dispose();
	}

	private Image loadImage(Shell shell, String fileName) {
		String pathName = "/images/" + fileName;

		InputStream imageStream = getClass().getResourceAsStream(pathName);
		if (imageStream == null) {
			Log.w("ddms", "Couldn't load " + pathName);
			Display display = shell.getDisplay();
			return ImageLoader.createPlaceHolderArt(display, 100, 50, display.getSystemColor(9));
		}
		Image img = new Image(shell.getDisplay(), imageStream);
//		if (img == null) {
//			throw new NullPointerException("couldn't load " + pathName);
//		}
		return img;
	}

	private void createContents(final Shell shell) {
		shell.setLayout(new GridLayout(2, false));

		Label logo = new Label(shell, 2048);
		logo.setImage(this.logoImage);

		Composite textArea = new Composite(shell, 0);
		GridLayout layout = new GridLayout(1, true);
		textArea.setLayout(layout);

		Label label = new Label(textArea, 0);
		if ((Main.sRevision != null) && (Main.sRevision.length() > 0)) {
			label.setText("Dalvik Debug Monitor Revision " + Main.sRevision);
		} else {
			label.setText("Dalvik Debug Monitor");
		}
		label = new Label(textArea, 0);

		label.setText("Copyright 2007-2012, The Android Open Source Project");
		label = new Label(textArea, 0);
		label.setText("All Rights Reserved.");

		label = new Label(shell, 0);

		Button ok = new Button(shell, 8);
		ok.setText("OK");
		GridData data = new GridData(128);
		data.widthHint = 80;
		ok.setLayoutData(data);
		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
			}
		});
		shell.pack();

		shell.setDefaultButton(ok);
	}
}