package com.bytezone.diskbrowser.gui;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;

import com.bytezone.common.EnvironmentAction;
import com.bytezone.common.FontAction;
import com.bytezone.common.QuitAction.QuitListener;
import com.bytezone.diskbrowser.applefile.HiResImage;
import com.bytezone.diskbrowser.applefile.Palette;
import com.bytezone.diskbrowser.applefile.PaletteFactory;
import com.bytezone.diskbrowser.applefile.VisicalcFile;
import com.bytezone.diskbrowser.disk.DataDisk;
import com.bytezone.diskbrowser.disk.FormattedDisk;

public class MenuHandler
    implements DiskSelectionListener, FileSelectionListener, QuitListener
{
  static final String PREFS_LINE_WRAP = "line wrap";
  private static final String PREFS_SHOW_CATALOG = "show catalog";
  private static final String PREFS_SHOW_LAYOUT = "show layout";
  private static final String PREFS_SHOW_FREE_SECTORS = "show free sectors";
  private static final String PREFS_COLOUR_QUIRKS = "colour quirks";
  private static final String PREFS_MONOCHROME = "monochrome";
  //  private static final String PREFS_DEBUGGING = "debugging";
  private static final String PREFS_PALETTE = "palette";

  FormattedDisk currentDisk;

  JMenuBar menuBar = new JMenuBar ();
  JMenu fileMenu = new JMenu ("File");
  JMenu formatMenu = new JMenu ("Format");
  JMenu colourMenu = new JMenu ("Colours");
  JMenu helpMenu = new JMenu ("Help");

  // File menu items
  JMenuItem rootItem = new JMenuItem ("Set root folder...");
  JMenuItem refreshTreeItem = new JMenuItem ("Refresh current tree");
  JMenuItem executeDiskItem;
  JMenuItem printItem = new JMenuItem ("Print output panel...");
  JMenuItem closeTabItem = new JMenuItem ();
  JMenuItem duplicateItem = new JMenuItem ();
  FontAction fontAction;

  // Format menu items
  final JMenuItem lineWrapItem = new JCheckBoxMenuItem ("Line wrap");
  final JMenuItem showLayoutItem = new JCheckBoxMenuItem ("Show layout panel");
  JMenuItem showCatalogItem = new JCheckBoxMenuItem ("Show catalog panel");
  JMenuItem showFreeSectorsItem = new JCheckBoxMenuItem ("Show free sectors");

  JMenuItem sector256Item = new JRadioButtonMenuItem ("256 byte sectors");
  JMenuItem sector512Item = new JRadioButtonMenuItem ("512 byte blocks");
  JMenuItem interleave0Item = new JRadioButtonMenuItem (new InterleaveAction (0));
  JMenuItem interleave1Item = new JRadioButtonMenuItem (new InterleaveAction (1));
  JMenuItem interleave2Item = new JRadioButtonMenuItem (new InterleaveAction (2));
  JMenuItem interleave3Item = new JRadioButtonMenuItem (new InterleaveAction (3));

  JMenuItem colourQuirksItem = new JCheckBoxMenuItem ("Colour quirks");
  JMenuItem monochromeItem = new JCheckBoxMenuItem ("Monochrome");
  JMenuItem debuggingItem = new JCheckBoxMenuItem ("Debugging");
  JMenuItem nextPaletteItem = new JMenuItem ("Next Palette");
  JMenuItem prevPaletteItem = new JMenuItem ("Previous Palette");

  ButtonGroup paletteGroup = new ButtonGroup ();

  public MenuHandler (Preferences prefs)
  {
    menuBar.add (fileMenu);
    menuBar.add (formatMenu);
    menuBar.add (colourMenu);
    menuBar.add (helpMenu);

    fileMenu.add (rootItem);
    fileMenu.addSeparator ();
    fileMenu.add (refreshTreeItem);

    addLauncherMenu ();

    fileMenu.add (printItem);
    fileMenu.addSeparator ();
    fileMenu.add (closeTabItem);

    fontAction = new FontAction ();
    JMenuItem fontItem = new JMenuItem (fontAction);
    fileMenu.add (fontItem);
    fontAction.setSampleText ("120  FOR Z = 14 TO 24:\n" + "  VTAB 5:\n" + "  HTAB Z:\n"
        + "  PRINT AB$:\n" + "  FOR TI = 1 TO 50:\n" + "  NEXT :\n" + "  POKE 0,Z + 40:\n"
        + "  POKE 1,9:\n" + "  CALL MU:\n" + "  VTAB 5:\n" + "  HTAB Z:\n"
        + "  PRINT SPC(12):\n" + "NEXT :\n" + "VTAB 5:\n" + "HTAB 24:\n" + "PRINT AB$\n");

    fileMenu.add (duplicateItem);
    fileMenu.add (debuggingItem);

    formatMenu.add (lineWrapItem);
    formatMenu.add (showCatalogItem);
    formatMenu.add (showLayoutItem);
    formatMenu.add (showFreeSectorsItem);

    formatMenu.addSeparator ();

    formatMenu.add (interleave0Item);
    formatMenu.add (interleave1Item);
    formatMenu.add (interleave2Item);
    formatMenu.add (interleave3Item);

    formatMenu.addSeparator ();

    formatMenu.add (sector256Item);
    formatMenu.add (sector512Item);

    // set placeholders for the palettes
    List<Palette> palettes = HiResImage.getPalettes ();
    for (int i = 0; i < palettes.size (); i++)
    {
      JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem ("x");
      paletteGroup.add (menuItem);
      colourMenu.add (menuItem);
    }

    colourMenu.addSeparator ();
    colourMenu.add (colourQuirksItem);
    colourMenu.add (monochromeItem);
    colourMenu.addSeparator ();
    colourMenu.add (nextPaletteItem);
    colourMenu.add (prevPaletteItem);

    helpMenu.add (new JMenuItem (new EnvironmentAction ()));

    sector256Item.setActionCommand ("256");
    sector256Item.setAccelerator (KeyStroke.getKeyStroke ("alt 4"));
    sector512Item.setActionCommand ("512");
    sector512Item.setAccelerator (KeyStroke.getKeyStroke ("alt 5"));

    ButtonGroup sectorGroup = new ButtonGroup ();
    ButtonGroup interleaveGroup = new ButtonGroup ();

    sectorGroup.add (sector256Item);
    sectorGroup.add (sector512Item);
    interleaveGroup.add (interleave0Item);
    interleaveGroup.add (interleave1Item);
    interleaveGroup.add (interleave2Item);
    interleaveGroup.add (interleave3Item);
  }

  void addHelpMenuAction (Action action, String functionName)
  {
    //    if (Platform.MAC)
    //    {
    //      try
    //      {
    //        if (functionName.equals ("about"))
    //          OSXAdapter.setAboutHandler (action,
    //              action.getClass ().getDeclaredMethod (functionName, (Class[]) null));
    //        else if (functionName.equals ("prefs"))
    //          OSXAdapter.setPreferencesHandler (action,
    //              action.getClass ().getDeclaredMethod (functionName, (Class[]) null));
    //      }
    //      catch (Exception e)
    //      {
    //        e.printStackTrace ();
    //      }
    //    }
    //    else
    //    {
    helpMenu.add (new JMenuItem (action));
    //    }
  }

  private void addLauncherMenu ()
  {
    if (!Desktop.isDesktopSupported ())
      return;

    boolean openSupported = false;
    for (Desktop.Action action : Desktop.Action.values ())
      if (action.toString ().equals ("OPEN"))
      {
        openSupported = true;
        break;
      }
    if (!openSupported)
      return;

    executeDiskItem = new JMenuItem (new ExecuteDiskAction (this));
    fileMenu.add (executeDiskItem);
    fileMenu.addSeparator ();
  }

  @Override
  public void quit (Preferences prefs)
  {
    prefs.putBoolean (PREFS_LINE_WRAP, lineWrapItem.isSelected ());
    prefs.putBoolean (PREFS_SHOW_LAYOUT, showLayoutItem.isSelected ());
    prefs.putBoolean (PREFS_SHOW_CATALOG, showCatalogItem.isSelected ());
    prefs.putBoolean (PREFS_SHOW_FREE_SECTORS, showFreeSectorsItem.isSelected ());
    prefs.putBoolean (PREFS_COLOUR_QUIRKS, colourQuirksItem.isSelected ());
    prefs.putBoolean (PREFS_MONOCHROME, monochromeItem.isSelected ());
    //    prefs.putBoolean (PREFS_DEBUGGING, debuggingItem.isSelected ());
    prefs.putInt (PREFS_PALETTE,
        HiResImage.getPaletteFactory ().getCurrentPaletteIndex ());
  }

  @Override
  public void restore (Preferences prefs)
  {
    lineWrapItem.setSelected (prefs.getBoolean (PREFS_LINE_WRAP, true));
    showLayoutItem.setSelected (prefs.getBoolean (PREFS_SHOW_LAYOUT, true));
    showCatalogItem.setSelected (prefs.getBoolean (PREFS_SHOW_CATALOG, true));
    showFreeSectorsItem.setSelected (prefs.getBoolean (PREFS_SHOW_FREE_SECTORS, false));
    colourQuirksItem.setSelected (prefs.getBoolean (PREFS_COLOUR_QUIRKS, false));
    monochromeItem.setSelected (prefs.getBoolean (PREFS_MONOCHROME, false));
    //    debuggingItem.setSelected (prefs.getBoolean (PREFS_DEBUGGING, false));

    int paletteIndex = prefs.getInt (PREFS_PALETTE, 0);
    PaletteFactory paletteFactory = HiResImage.getPaletteFactory ();
    paletteFactory.setCurrentPalette (paletteIndex);
    Palette palette = paletteFactory.getCurrentPalette ();
    Enumeration<AbstractButton> enumeration = paletteGroup.getElements ();
    while (enumeration.hasMoreElements ())
    {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem) enumeration.nextElement ();
      if (item.getText ().equals (palette.getName ()))
      {
        item.setSelected (true);
        break;
      }
    }

    HiResImage.setDefaultColourQuirks (colourQuirksItem.isSelected ());
    HiResImage.setDefaultMonochrome (monochromeItem.isSelected ());
    VisicalcFile.setDefaultDebug (debuggingItem.isSelected ());
  }

  @Override
  public void diskSelected (DiskSelectedEvent event)
  {
    currentDisk = event.getFormattedDisk ();
    adjustMenus (currentDisk);
  }

  @Override
  public void fileSelected (FileSelectedEvent event)
  {
    // This can happen if a file is selected from a dual-dos disk
    if (event.file.getFormattedDisk () != currentDisk)
    {
      currentDisk = event.file.getFormattedDisk ();
      adjustMenus (currentDisk);
    }
  }

  private void adjustMenus (final FormattedDisk disk)
  {
    if (disk != null)
    {
      sector256Item.setSelected (disk.getDisk ().getBlockSize () == 256);
      sector512Item.setSelected (disk.getDisk ().getBlockSize () == 512);
      interleave0Item.setSelected (disk.getDisk ().getInterleave () == 0);
      interleave1Item.setSelected (disk.getDisk ().getInterleave () == 1);
      interleave2Item.setSelected (disk.getDisk ().getInterleave () == 2);
      interleave3Item.setSelected (disk.getDisk ().getInterleave () == 3);
    }

    boolean isDataDisk = (disk instanceof DataDisk);

    sector256Item.setEnabled (isDataDisk);
    sector512Item.setEnabled (isDataDisk);
    interleave0Item.setEnabled (isDataDisk);
    interleave1Item.setEnabled (isDataDisk);
    interleave2Item.setEnabled (isDataDisk);
    interleave3Item.setEnabled (isDataDisk);

    if (isDataDisk)
    {
      // make this an action too
      ActionListener sectorListener = new ActionListener ()
      {
        @Override
        public void actionPerformed (ActionEvent e)
        {
          int size = Integer.parseInt (e.getActionCommand ());
          disk.getDisk ().setBlockSize (size);
        }
      };

      sector256Item.addActionListener (sectorListener);
      sector512Item.addActionListener (sectorListener);

      ((InterleaveAction) interleave0Item.getAction ()).setDisk (currentDisk);
      ((InterleaveAction) interleave1Item.getAction ()).setDisk (currentDisk);
      ((InterleaveAction) interleave2Item.getAction ()).setDisk (currentDisk);
      ((InterleaveAction) interleave3Item.getAction ()).setDisk (currentDisk);
    }
  }
}