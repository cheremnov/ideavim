/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.StripeTable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author vlan
 */
public class VimEmulationConfigurable implements Configurable {
  @NotNull private final VimSettingsPanel myPanel = new VimSettingsPanel();

  @Nls
  @Override
  public String getDisplayName() {
    return "Vim Emulation";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }

  @Override
  public void reset() {
  }

  @Override
  public void disposeUIResources() {
  }

  private static final class VimSettingsPanel extends JPanel {
    @NotNull private final VimShortcutConflictsTable myShortcutConflictsTable = new VimShortcutConflictsTable();

    public VimSettingsPanel() {
      setLayout(new BorderLayout());
      final JScrollPane scrollPane = new JBScrollPane(myShortcutConflictsTable);
      scrollPane.setBorder(new LineBorder(UIUtil.getBorderColor()));
      final JPanel conflictsPanel = new JPanel(new BorderLayout());
      conflictsPanel.setBorder(IdeBorderFactory.createTitledBorder("Shortcut Conflicts", false));
      conflictsPanel.add(scrollPane);
      add(conflictsPanel, BorderLayout.CENTER);
    }
  }

  private static final class VimShortcutConflictsTable extends StripeTable {
    public VimShortcutConflictsTable() {
      super(new TableModel());

      getTableColumn(Column.KEYSTROKE).setPreferredWidth(50);
      getTableColumn(Column.IDE_ACTION).setPreferredWidth(300);
      getTableColumn(Column.OWNER).setPreferredWidth(50);
    }

    @NotNull
    @Override
    public Dimension getMinimumSize() {
      return calcSize(super.getMinimumSize());
    }

    @NotNull
    @Override
    public Dimension getPreferredSize() {
      return calcSize(super.getPreferredSize());
    }

    @NotNull
    private Dimension calcSize(@NotNull Dimension dimension) {
      final Container container = getParent();
      if (container != null) {
        final Dimension size = container.getSize();
        return new Dimension(size.width, dimension.height);
      }
      return dimension;
    }

    @NotNull
    private TableColumn getTableColumn(@NotNull Column column) {
      return getColumnModel().getColumn(column.getIndex());
    }

    private static final class TableModel extends AbstractTableModel {
      @NotNull private final List<Row> myRows = new ArrayList<Row>();

      public TableModel() {
        final KeyParser keyParser = KeyParser.getInstance();
        final Set<KeyStroke> requiredShortcutKeys = keyParser.getRequiredShortcutKeys();
        final Map<KeyStroke, ShortcutOwner> shortcutConflicts = VimPlugin.getShortcutConflicts();
        for (KeyStroke keyStroke : requiredShortcutKeys) {
          if (!VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
            final List<AnAction> conflicts = KeyParser.getKeymapConflicts(keyStroke);
            if (!conflicts.isEmpty()) {
              myRows.add(new Row(keyStroke, conflicts.get(0), shortcutConflicts.get(keyStroke)));
            }
          }
        }
        Collections.sort(myRows);
      }

      @Override
      public int getRowCount() {
        return myRows.size();
      }

      @Override
      public int getColumnCount() {
        return Column.values().length;
      }

      @Nullable
      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        final Column column = Column.fromIndex(columnIndex);
        if (column != null && rowIndex < myRows.size()) {
          final Row row = myRows.get(rowIndex);
          switch (column) {
            case KEYSTROKE:
              return KeymapUtil.getShortcutText(new KeyboardShortcut(row.getKeyStroke(), null));
            case IDE_ACTION:
              return row.getAction().getTemplatePresentation().getText();
            case OWNER:
              return row.getOwner();
          }
        }
        return null;
      }

      @Nullable
      @Override
      public String getColumnName(int index) {
        final Column column = Column.fromIndex(index);
        return column != null ? column.getTitle() : null;
      }
    }

    private static final class Row implements Comparable<Row> {
      @NotNull private final KeyStroke myKeyStroke;
      @NotNull private final AnAction myAction;
      @Nullable private final ShortcutOwner myOwner;

      private Row(@NotNull KeyStroke keyStroke, @NotNull AnAction action, @Nullable ShortcutOwner owner) {
        myKeyStroke = keyStroke;
        myAction = action;
        myOwner = owner;
      }

      @NotNull
      public KeyStroke getKeyStroke() {
        return myKeyStroke;
      }

      @NotNull
      public AnAction getAction() {
        return myAction;
      }

      @Nullable
      public ShortcutOwner getOwner() {
        return myOwner;
      }

      @Override
      public int compareTo(@NotNull Row row) {
        final KeyStroke otherKeyStroke = row.getKeyStroke();
        final int keyCodeDiff = myKeyStroke.getKeyCode() - otherKeyStroke.getKeyCode();
        return keyCodeDiff != 0 ? keyCodeDiff : myKeyStroke.getModifiers() - otherKeyStroke.getModifiers();
      }
    }

    private static enum Column {
      KEYSTROKE(0, "Shortcut"),
      IDE_ACTION(1, "IDE Action"),
      OWNER(2, "Handler");

      @NotNull private static final Map<Integer, Column> ourMembers = new HashMap<Integer, Column>();

      static {
        for (Column column : values()) {
          ourMembers.put(column.myIndex, column);
        }
      }

      private final int myIndex;
      @NotNull private final String myTitle;

      Column(int index, @NotNull String title) {
        myIndex = index;
        myTitle = title;
      }

      @Nullable
      public static Column fromIndex(int index) {
        return ourMembers.get(index);
      }

      public int getIndex() {
        return myIndex;
      }

      @NotNull
      public String getTitle() {
        return myTitle;
      }
    }
  }
}
