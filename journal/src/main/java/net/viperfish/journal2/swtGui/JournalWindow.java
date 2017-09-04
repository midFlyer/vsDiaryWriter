package net.viperfish.journal2.swtGui;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.nebula.widgets.pagination.collections.PageResultContentProvider;
import org.eclipse.nebula.widgets.pagination.collections.PageResultLoaderList;
import org.eclipse.nebula.widgets.pagination.renderers.navigation.ResultAndNavigationPageLinksRendererFactory;
import org.eclipse.nebula.widgets.pagination.table.PageableTable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import net.viperfish.journal2.core.CopiablePreferencePage;
import net.viperfish.journal2.core.Journal;
import net.viperfish.journal2.core.JournalI18NBundle;
import net.viperfish.journal2.core.JournalService;

public class JournalWindow {
	private JournalService journalService;

	private class SearchJournal {

		private Date datePickerToDate(DateTime dt) {
			Calendar cal = Calendar.getInstance();
			cal.set(dt.getYear(), dt.getMonth(), dt.getDay());
			return cal.getTime();
		}

		private void setPagination(Collection<Journal> data) {
			pgTable.setPageLoader(new PageResultLoaderList<>(new LinkedList<>(data)));
			pgTable.refreshPage(true);
		}

		public void displayAll() {
			tableViewer.setInput(null);
			Date min = null;
			List<Journal> resultList = new LinkedList<>();
			try {
				for (Journal i : journalService.getAll()) {
					if (min == null || min.after(i.getTimestamp())) {
						min = i.getTimestamp();
					}
					resultList.add(i);
				}
			} catch (ExecutionException e) {
				e.printStackTrace();
				displayError();
			}
			setPagination(resultList);
			Calendar cal = Calendar.getInstance();
			if (min != null) {
				cal.setTime(min);
			}
			lowerBound.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
			cal.setTime(new Date());
			cal.add(Calendar.DATE, 1);
			upperBoound.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		}

		public void displayFiltered() {
			tableViewer.setInput(null);
			Date lower = TimeUtils.truncDate(datePickerToDate(lowerBound));
			Date upper = TimeUtils.truncDate(datePickerToDate(upperBoound));
			try {
				setPagination(journalService.getRange(lower, upper));
			} catch (ExecutionException e) {
				e.printStackTrace();
				displayError();
			}
		}

		public void searchJournals() {
			if (searchText.getText().length() == 0) {
				displayFiltered();
				return;
			}
			tableViewer.setInput(null);
			Date lower = TimeUtils.truncDate(datePickerToDate(lowerBound));
			Date upper = TimeUtils.truncDate(datePickerToDate(upperBoound));
			try {
				setPagination(journalService.searchWithinRange(lower, upper, searchText.getText()));
			} catch (ExecutionException e) {
				e.printStackTrace();
				displayError();
			}
		}

		private class SearchSelectionAdapter extends SelectionAdapter {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				searchJournals();
			}

		}

		private class SearchTextChangeAdapter implements ModifyListener {

			@Override
			public void modifyText(ModifyEvent arg0) {
				searchJournals();
			}

		}

		public SelectionListener createSelectAdapter() {
			return new SearchSelectionAdapter();
		}

		public ModifyListener createModifyAdapter() {
			return new SearchTextChangeAdapter();
		}
	}

	private void displayError() {
		MessageDialog.openError(shell, JournalI18NBundle.getString("label.error"),
				JournalI18NBundle.getString("journal2.error.generic"));
	}

	private Text searchText;
	private Display display;
	private Shell shell;
	private ToolBar operationBar;
	private ToolItem newJournal;
	private ToolItem deleteJournal;
	private SearchJournal search;

	private Table searchResults;
	private TableViewer tableViewer;

	private Label recentLabel;
	private DateTime lowerBound;
	private DateTime upperBoound;
	private PageableTable pgTable;
	private CopiablePreferencePage[] page;

	public JournalWindow(JournalService service, CopiablePreferencePage... page) {
		this.journalService = service;
		this.page = page;
	}

	private PreferenceManager createPreferences() {
		PreferenceManager manager = new PreferenceManager();
		for (CopiablePreferencePage p : page) {
			manager.addToRoot(new PreferenceNode(p.getTitle(), p.copy()));
		}
		return manager;
	}

	private void newJournal() {
		Journal result = new JournalEditor().open(new Journal());
		if (result == null) {
			return;
		}
		try {
			journalService.add(result);
		} catch (ExecutionException e) {
			e.printStackTrace();
			displayError();
		}
		search.searchJournals();
	}

	private void deleteJournal() {
		StructuredSelection selected = (StructuredSelection) tableViewer.getSelection();
		if (selected.isEmpty()) {
			return;
		}
		boolean toDelete = MessageDialog.openConfirm(shell,
				JournalI18NBundle.getString("journal2.main.deleteWarning.title"),
				JournalI18NBundle.getString("journal2.main.deleteWarning"));
		if (toDelete) {
			Journal s = (Journal) selected.getFirstElement();
			try {
				journalService.remove(s.getId());
			} catch (ExecutionException e) {
				e.printStackTrace();
				displayError();
			}
			search.searchJournals();
		}
	}

	/**
	 * Open the window.
	 * 
	 * @wbp.parser.entryPoint
	 */
	public void open() {
		display = Display.getDefault();
		shell = new Shell();
		// shell.setImage(SWTResourceManager.getImage(JournalWindow.class,
		// "/logo.ico"));
		shell.setSize(495, 480);
		shell.setText("vsDiary - 6.0.0");
		shell.setLayout(new GridLayout(8, false));
		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				System.exit(0);
			}
		});

		searchText = new Text(shell, SWT.BORDER);
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 8, 1));

		operationBar = new ToolBar(shell, SWT.FLAT | SWT.RIGHT);
		operationBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 8, 1));

		newJournal = new ToolItem(operationBar, SWT.NONE);
		newJournal.setText(JournalI18NBundle.getString("label.add"));

		deleteJournal = new ToolItem(operationBar, SWT.NONE);
		deleteJournal.setText(JournalI18NBundle.getString("label.delete"));

		recentLabel = new Label(shell, SWT.NONE);
		recentLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		recentLabel.setText(JournalI18NBundle.getString("label.range"));

		lowerBound = new DateTime(shell, SWT.DROP_DOWN);

		Label lblTo = new Label(shell, SWT.NONE);
		lblTo.setText(JournalI18NBundle.getString("label.to"));

		upperBoound = new DateTime(shell, SWT.DROP_DOWN);

		search = new SearchJournal();

		lowerBound.addSelectionListener(search.createSelectAdapter());
		upperBoound.addSelectionListener(search.createSelectAdapter());
		searchText.addModifyListener(search.createModifyAdapter());
		newJournal.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				newJournal();
			}

		});

		deleteJournal.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				deleteJournal();
			}

		});

		Menu mainMenu = new Menu(shell, SWT.BAR);
		shell.setMenuBar(mainMenu);

		MenuItem fileMenu = new MenuItem(mainMenu, SWT.CASCADE);
		fileMenu.setText(JournalI18NBundle.getString("label.file"));

		Menu menu = new Menu(fileMenu);
		fileMenu.setMenu(menu);

		MenuItem newEntryMenu = new MenuItem(menu, SWT.NONE);
		newEntryMenu.setText(JournalI18NBundle.getString("label.add"));
		newEntryMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				newJournal();
			}

		});

		MenuItem exit = new MenuItem(menu, SWT.NONE);
		exit.setText(JournalI18NBundle.getString("label.exit"));
		exit.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				shell.dispose();
			}

		});

		MenuItem editMenu = new MenuItem(mainMenu, SWT.CASCADE);
		editMenu.setText(JournalI18NBundle.getString("label.edit"));

		Menu menu_1 = new Menu(editMenu);
		editMenu.setMenu(menu_1);

		MenuItem deleteEntryMenu = new MenuItem(menu_1, SWT.NONE);
		deleteEntryMenu.setText(JournalI18NBundle.getString("label.delete"));
		deleteEntryMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				deleteJournal();
			}

		});

		MenuItem showAllMenu = new MenuItem(menu_1, SWT.NONE);
		showAllMenu.setText(JournalI18NBundle.getString("label.showAll"));
		showAllMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				search.displayAll();
			}

		});

		MenuItem preferenceMenu = new MenuItem(mainMenu, SWT.CASCADE);
		preferenceMenu.setText(JournalI18NBundle.getString("label.settings"));

		Menu settingMenu = new Menu(preferenceMenu);
		preferenceMenu.setMenu(settingMenu);

		MenuItem reCryptMenuItem = new MenuItem(settingMenu, SWT.NONE);
		reCryptMenuItem.setText(JournalI18NBundle.getString("label.recrypt"));
		reCryptMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				try {
					journalService.reCrypt();
				} catch (IOException e) {
					e.printStackTrace();
					JournalWindow.this.displayError();
				}
			}

		});

		MenuItem passwordMenu = new MenuItem(settingMenu, SWT.NONE);
		passwordMenu.setText(JournalI18NBundle.getString("label.changePasswd"));
		passwordMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				super.widgetSelected(arg0);
				ChangePasswordPrompt prompt = new ChangePasswordPrompt(journalService);
				prompt.open();
			}

		});

		MenuItem changeConfigMenu = new MenuItem(settingMenu, SWT.NONE);
		changeConfigMenu.setText(JournalI18NBundle.getString("label.preferences"));
		changeConfigMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				super.widgetSelected(arg0);
				new PreferenceDialog(shell, createPreferences()).open();
			}

		});

		MenuItem helpMenu = new MenuItem(mainMenu, SWT.CASCADE);
		helpMenu.setText(JournalI18NBundle.getString("label.help"));

		Menu menu_2 = new Menu(helpMenu);
		helpMenu.setMenu(menu_2);

		MenuItem aboutMenu = new MenuItem(menu_2, SWT.NONE);
		aboutMenu.setText(JournalI18NBundle.getString("label.about"));
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);
		new Label(shell, SWT.NONE);

		pgTable = new PageableTable(shell, SWT.NONE, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL, 10,
				PageResultContentProvider.getInstance(), null,
				ResultAndNavigationPageLinksRendererFactory.getFactory());
		GridData gd_pgTable = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd_pgTable.horizontalSpan = 8;
		pgTable.setLayoutData(gd_pgTable);

		tableViewer = pgTable.getViewer();
		searchResults = tableViewer.getTable();
		searchResults.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableViewer.setContentProvider(new ArrayContentProvider());
		searchResults.setHeaderVisible(true);
		searchResults.setLinesVisible(true);
		final TableViewerColumn titles = new TableViewerColumn(tableViewer, SWT.NONE);
		titles.getColumn().setWidth(200);
		titles.getColumn().setText(JournalI18NBundle.getString("journal.title"));
		titles.getColumn().setResizable(true);
		titles.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Journal j = (Journal) element;
				return j.getSubject();
			}
		});
		final TableViewerColumn dates = new TableViewerColumn(tableViewer, SWT.NONE);
		dates.getColumn().setWidth(200);
		dates.getColumn().setResizable(true);
		dates.getColumn().setText(JournalI18NBundle.getString("journal.timestamp"));
		dates.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				Journal j = (Journal) element;
				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
				return df.format(j.getTimestamp());
			}
		});

		tableViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent arg0) {
				StructuredSelection selected = (StructuredSelection) arg0.getSelection();
				if (selected.isEmpty()) {
					return;
				}
				Journal pointer = (Journal) selected.getFirstElement();
				Journal result = new JournalEditor().open(pointer);
				if (result == null) {
					return;
				}
				try {
					journalService.update(pointer.getId(), pointer);
				} catch (ExecutionException e) {
					e.printStackTrace();
					displayError();
				}
				search.searchJournals();
			}
		});
		aboutMenu.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				super.widgetSelected(e);
				MessageDialog.openInformation(shell, JournalI18NBundle.getString("label.about"),
						JournalI18NBundle.getString("journa2.main.about"));
			}

		});

		shell.addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(ControlEvent arg0) {
				// TODO Auto-generated method stub
				super.controlResized(arg0);
				Rectangle area = shell.getClientArea();
				Point preferredSize = searchResults.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - searchResults.getBorderWidth() * 2;
				if (preferredSize.y > area.height + searchResults.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = searchResults.getVerticalBar().getSize();
					width -= vBarSize.x;
				}
				Point oldSize = searchResults.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns
					// smaller first and then resize the table to
					// match the client area width
					titles.getColumn().setWidth(width / 2);
					dates.getColumn().setWidth(width - titles.getColumn().getWidth() - 25);
				} else {
					// table is getting bigger so make the table
					// bigger first and then make the columns wider
					// to match the client area width
					titles.getColumn().setWidth(width / 2);
					dates.getColumn().setWidth(width - titles.getColumn().getWidth() - 25);
				}
			}

		});

		search.displayAll();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

	}
}