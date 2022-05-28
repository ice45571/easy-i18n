package de.marhali.easyi18n.tabs;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.table.JBTable;

import de.marhali.easyi18n.InstanceManager;
import de.marhali.easyi18n.dialog.EditDialog;
import de.marhali.easyi18n.listener.ReturnKeyListener;
import de.marhali.easyi18n.listener.DeleteKeyListener;
import de.marhali.easyi18n.listener.PopupClickListener;
import de.marhali.easyi18n.model.TranslationData;
import de.marhali.easyi18n.model.action.TranslationDelete;
import de.marhali.easyi18n.model.KeyPath;
import de.marhali.easyi18n.model.Translation;
import de.marhali.easyi18n.model.TranslationValue;
import de.marhali.easyi18n.model.bus.FilteredBusListener;
import de.marhali.easyi18n.tabs.renderer.TableRenderer;
import de.marhali.easyi18n.tabs.mapper.TableModelMapper;
import de.marhali.easyi18n.util.KeyPathConverter;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;

/**
 * Shows translation state as table.
 * @author marhali
 */
public class TableView implements FilteredBusListener {

    private final JBTable table;

    private final Project project;

    private KeyPathConverter converter;

    private JPanel rootPanel;
    private JPanel containerPanel;

    public TableView(Project project) {
        this.project = project;

        table = new JBTable();
        table.getEmptyText().setText(ResourceBundle.getBundle("messages").getString("view.empty"));
        table.addMouseListener(new PopupClickListener(e -> showEditPopup(table.rowAtPoint(e.getPoint()))));
        table.addKeyListener(new ReturnKeyListener(() -> showEditPopup(table.getSelectedRow())));
        table.addKeyListener(new DeleteKeyListener(this::deleteSelectedRows));
        table.setDefaultRenderer(String.class, new TableRenderer());

        containerPanel.add(new JBScrollPane(table));
    }

    private void showEditPopup(int row) {
        if (row < 0) {
            return;
        }

        KeyPath fullPath = this.converter.fromString(String.valueOf(this.table.getValueAt(row, 0)));
        TranslationValue value = InstanceManager.get(project).store().getData().getTranslation(fullPath);

        if (value != null) {
            new EditDialog(project, new Translation(fullPath, value)).showAndHandle();
        }
    }

    private void deleteSelectedRows() {
        Set<KeyPath> batchDelete = new HashSet<>();

        for (int selectedRow : table.getSelectedRows()) {
            batchDelete.add(this.converter.fromString(String.valueOf(table.getValueAt(selectedRow, 0))));
        }

        for (KeyPath key : batchDelete) {
            InstanceManager.get(project).processUpdate(new TranslationDelete(new Translation(key, null)));
        }
    }

    @Override
    public void onUpdateData(@NotNull TranslationData data) {
        this.converter = new KeyPathConverter(project);

        table.setModel(new TableModelMapper(data, this.converter, update ->
                InstanceManager.get(project).processUpdate(update)));
    }

    @Override
    public void onFocusKey(@NotNull KeyPath key) {
        String concatKey = this.converter.toString(key);
        int row = -1;

        for (int i = 0; i < table.getRowCount(); i++) {
            if (table.getValueAt(i, 0).equals(concatKey)) {
                row = i;
            }
        }

        if (row > -1) { // Matched @key
            table.getSelectionModel().setSelectionInterval(row, row);
            table.scrollRectToVisible(new Rectangle(table.getCellRect(row, 0, true)));
        }
    }

    @Override
    public void onExpandAll() {
        // table-view never collapse any rows
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    public JBTable getTable() {
        return table;
    }
}