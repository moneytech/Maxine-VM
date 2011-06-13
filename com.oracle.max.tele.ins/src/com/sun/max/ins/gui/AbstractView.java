/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;

import javax.swing.*;

import com.sun.cri.ci.*;
import com.sun.max.ins.*;
import com.sun.max.ins.InspectionSettings.AbstractSaveSettingsListener;
import com.sun.max.ins.InspectionSettings.SaveSettingsEvent;
import com.sun.max.ins.InspectionSettings.SaveSettingsListener;
import com.sun.max.ins.view.InspectionViews.ViewKind;
import com.sun.max.ins.view.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * An interactive visual presentation of some aspect of VM state.
 * <p>
 * This presentation creates some kind of view (the visual representation of some piece
 * of VM state) and puts it into an {@link InspectorFrame} for realization in the
 * window system.
 * <p>
 * <b>Event Notification</b>:
 * This abstract class ensures that every view listens for {@linkplain InspectionListener Inspection Events}
 * as well as {@linkplain ViewFocusListener Focus Events}.  Any implementation
 * that wishes to receive such notifications must do so by overriding the appropriate
 * methods in interfaces {@link InspectionListener} and {@link ViewFocusListener},
 * for which empty methods are provided in this abstract class.</p>
 */
public abstract class AbstractView<View_Type extends AbstractView> extends AbstractInspectionHolder implements InspectionListener, ViewFocusListener {

    private static final int TRACE_VALUE = 1;

    private static final ImageIcon DEFAULT_MENU_ICON = InspectorImageIcon.createDownTriangle(16, 16);

    public enum MenuKind {
        // Standard menu, of which every menu bar will have a subset.
        // They should be created in this order for consistency, as that
        // is the order in which they will appear.
        DEFAULT_MENU(""),
        EDIT_MENU("Edit"),
        MEMORY_MENU("Memory"),
        OBJECT_MENU("Object"),
        CODE_MENU("Code "),
        DEBUG_MENU("Debug"),
        VIEW_MENU("View"),
        HELP_MENU("Help");

        private final String label;

        private MenuKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

    }

    /**
     * Creates a set of standard menu items for this view which are
     * appropriate to one of the standard menu kinds.
     *
     * @param menuKind the kind of menu for which the standard items are intended
     * @return a new set of menu items
     */
    protected final InspectorMenuItems defaultMenuItems(MenuKind menuKind) {
        return defaultMenuItems(menuKind, AbstractView.this);
    }

    /**
     * Creates a set of standard menu items for a View which are
     * appropriate to one of the standard menu kinds.
     *
     * @param view the view for which view-specific items will operate
     * @param menuKind the kind of menu for which the standard items are intended
     * @return a new set of menu items
     */
    protected final InspectorMenuItems defaultMenuItems(MenuKind menuKind, final AbstractView view) {

        switch(menuKind) {
            case DEFAULT_MENU:
                return new AbstractInspectorMenuItems(inspection()) {
                    public void addTo(InspectorMenu menu) {
                        menu.add(getCloseViewAction());
                        menu.add(views().deactivateOtherViewsAction(view));
                        menu.addSeparator();
                        menu.add(gui().moveToMiddleAction(view));
                        menu.add(gui().resizeToFitAction(view));
                        menu.add(gui().resizeToFillAction(view));
                        menu.add(gui().restoreDefaultGeometryAction(view));
                        menu.addSeparator();
                        menu.add(getPrintAction());
                    }
                };
            case EDIT_MENU:
                break;
            case MEMORY_MENU:
                return new AbstractInspectorMenuItems(inspection()) {
                    public void addTo(InspectorMenu menu) {
                        menu.addSeparator();
                        menu.add(actions().genericMemoryMenuItems());
                    }
                };
            case OBJECT_MENU:
                return new AbstractInspectorMenuItems(inspection()) {

                    public void addTo(InspectorMenu menu) {
                        menu.addSeparator();
                        menu.add(actions().genericObjectMenuItems());
                    }
                };
            case CODE_MENU:
                return new AbstractInspectorMenuItems(inspection()) {

                    public void addTo(InspectorMenu menu) {
                        menu.addSeparator();
                        menu.add(actions().genericCodeMenuItems());
                    }
                };
            case DEBUG_MENU:
                return new AbstractInspectorMenuItems(inspection()) {

                    public void addTo(InspectorMenu menu) {
                        menu.addSeparator();
                        menu.add(actions().genericBreakpointMenuItems());
                        menu.add(views().activateSingletonViewAction(ViewKind.BREAKPOINTS));
                        if (vm().watchpointManager() != null) {
                            menu.add(actions().genericWatchpointMenuItems());
                            menu.add(views().activateSingletonViewAction(ViewKind.WATCHPOINTS));
                        }
                    }
                };
            case VIEW_MENU:
                return new AbstractInspectorMenuItems(inspection()) {

                    public void addTo(InspectorMenu menu) {
                        menu.add(getViewOptionsAction());
                        menu.add(getRefreshAction());
                        menu.addSeparator();
                        menu.add(actions().genericViewMenuItems());
                    }
                };
            case HELP_MENU:
                break;
        }
        // Empty set of menu items
        return new AbstractInspectorMenuItems(inspection()) {

            public void addTo(InspectorMenu menu) {
            }
        };
    }

    private final ViewKind viewKind;

    /**
     * A listener that saves the current frame geometry (location, size)
     * whenever a "save" event takes place.  Newly created frames are initialized
     * to saved geometry settings, if present, as identified by the view's key.
     */
    private final SaveSettingsListener saveGeometrySettingsListener;

    private InspectorFrame frame;

    private final TimedTrace updateTracer;

    private InspectorAction showViewAction = null;
    private InspectorAction closeViewAction = null;

    private Set<ViewEventListener> viewEventListeners = CiUtil.newIdentityHashSet();

    /**
     * Abstract constructor for all views.
     *
     * @param viewKind the kind of view being created
     * @param geometrySettingsKey if non-null, makes the size and location of this
     * view persistent across sessions.
     */
    protected AbstractView(Inspection inspection, ViewKind viewKind, String geometrySettingsKey) {
        super(inspection);
        this.viewKind = viewKind;
        saveGeometrySettingsListener = (geometrySettingsKey == null) ? null : new AbstractSaveSettingsListener(geometrySettingsKey, this) {

            @Override
            public Rectangle defaultGeometry() {
                return AbstractView.this.defaultGeometry();
            }

            public void saveSettings(SaveSettingsEvent saveSettingsEvent) {
            }
        };
        this.updateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "refresh");
    }

    /**
     * Adds a listener for changes in {@link AbstractView} window state.
     */
    public void addViewEventListener(ViewEventListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "adding view event listener: " + listener);
        viewEventListeners.add(listener);
    }

    /**
     * Removes a listener for changes in {@link AbstractView} window state.
     */
    public void removeViewEventListener(ViewEventListener listener) {
        Trace.line(TRACE_VALUE, tracePrefix() + "removing view event listener: " + listener);
        viewEventListeners.remove(listener);
    }

    /**
     * @return the manager for kind of view
     */
    public final ViewManager viewManager() {
        return viewKind.viewManager();
    }

    /**
     * @return the component in which the view displays its content.
     */
    public final JComponent getJComponent() {
        return frame.getJComponent();
    }

    /**
     * Gets a default location for a view.  For singletons, these tend to be statically defined by the
     * view geometry preferences.  For other views, the default might be the location at which it
     * was created originally.
     *
     * @return default geometry for this view, to be used if no prior settings; null if no default specified.
     */
    protected Rectangle defaultGeometry() {
        return inspection().geometry().preferredFrameGeometry(viewKind);
    }

    /**
     * @return the current geometry for this view in the main frame
     */
    protected final Rectangle getGeometry() {
        return getJComponent().getBounds();
    }

    /**
     * Sets the geometry of the view in the main frame.
     *
     * @param rectangle the new geometry for the view
     */
    public final void setGeometry(Rectangle rectangle) {
        getJComponent().setBounds(rectangle);
    }

    /**
     * Sets the size of the view in the main frame.
     *
     * @param rectangle the new geometry for the view
     */
    public final void setSize(int width, int height) {
        getJComponent().setSize(width, height);
    }


    protected void setWarning() {
        //highlight();
    }

    /**
     * @return the string currently appearing in the title or tab of the view's window frame
     */
    public String getTitle() {
        return frame.getTitle();
    }

    /**
     * Gets from subclasses the currently appropriate title for this view's display frame.
     *
     * @return a short string suitable for appearing in the window frame of an view.
     * If this text is expected to change dynamically, a call to {@link #setTitle()}
     * will cause this to be called again and the result assigned to the frame.
     */
    public abstract String getTextForTitle();

    protected final void setTitle(String title) {
        frame.setTitle(title == null ? getTextForTitle() : title);
    }

    /**
     * Sets the display frame title for this view to the string provided by
     * the abstract method {@link #getTextForTitle()}.
     */
    protected final void setTitle() {
        setTitle(null);
    }

    /**
     * @return the visible table for views with table-based displays; null if none.
     */
    protected InspectorTable getTable() {
        return null;
    }

    /**
     * Creates the content that will be inserted into the View's frame.
     */
    protected abstract void createViewContent();

    /**
     * Creates a simple frame for the view and:
     * <ul>
     * <li>calls {@link createView()} to populate it;</li>
     * <li>adds this view to the collection of update listeners; and</li>
     * <li>makes it all visible in the window system.</li>
     * </ul>
     * <p>
     * The geometry (size and location) of the frame will be saved across sessions
     * if a non-null key was provided in the constructor.
     * if no key is provided, or if no settings from previous sessions have been saved,
     * then the initial geometry will be taken from a specification created by
     * implementations of the {@link InspectorGeometry} interface.
     *
     * @param addMenuBar should a menu bar be added to the frame.
     * @see InspectorGeometry#preferredFrameGeometry(ViewKind)
     */
    protected InspectorFrame createFrame(boolean addMenuBar) {
        frame = new InspectorInternalFrame(this, addMenuBar);
        setTitle();
        createViewContent();
        frame.pack();
        gui().addView(this);
        inspection().addInspectionListener(this);
        focus().addListener(this);
        if (saveGeometrySettingsListener != null) {
            inspection().settings().addSaveSettingsListener(saveGeometrySettingsListener);
        }
        return frame;
    }

    /**
     * Creates a tabbed frame for the view and:
     * <ul>
     * <li>calls {@link #createViewContent()} to populate it;</li>
     * <li>adds this view to the collection of update listeners; and</li>
     * <li>makes it all visible in the window system.</li>
     * </ul>
     * <p>
     * Note that these frames only exist in side a tabbed container, and thus
     * persistent geometry is not supported for them.
     *
     * @param addMenuBar should a menu bar be added to the frame.
     */
    protected InspectorFrame createTabFrame(TabbedView parent) {
        frame = new InspectorRootPane(this, parent, true);
        setTitle();
        createViewContent();
        frame.pack();
        gui().addView(this);
        inspection().addInspectionListener(this);
        focus().addListener(this);
        return frame;
    }

    /**
     * Each view optionally re-reads, and updates any state caches if needed
     * from the VM.  The expectation is that some views may cache and
     * update selectively, but the argument can override this.
     *
     * @param force suspend caching behavior; read state unconditionally.
     */
    protected abstract void refreshState(boolean force);

    /**
     * Refreshes any state needed from the VM and then ensures that the visual
     * display is completely updated.
     *
     * @param force  force suspend caching behavior; read state unconditionally.
     */
    private void refresh(boolean force) {
        refreshState(force);
        frame.refresh(force);
        frame.invalidate();
        frame.repaint();
    }

    /**
     * Unconditionally forces a full refresh of this view.
     */
    protected final void forceRefresh() {
        refresh(true);
    }

    /**
     * Rebuilds the content of the view, much more
     * expensive than {@link #refresh(boolean)}, but necessary when the parameters or
     * configuration of the view changes enough to require creating a new one.
     */
    protected final void reconstructView() {
        final Dimension size = frame.getSize();
        createViewContent();
        frame.setPreferredSize(size);
        frame.pack();
    }

    public void setContentPane(Container contentPane) {
        frame.setContentPane(contentPane);
    }

    public Container getContentPane() {
        return frame.getContentPane();
    }

    public void setLayeredPane(JLayeredPane layeredPane) {
        frame.setLayeredPane(layeredPane);
    }

    public JLayeredPane getLayeredPane() {
        return frame.getLayeredPane();
    }

    public void setGlassPane(Component glassPane) {
        frame.setGlassPane(glassPane);
    }

    public Component getGlassPane() {
        return frame.getGlassPane();
    }

    public boolean isVisible() {
        return frame.isVisible();
    }

    protected void moveToFront() {
        frame.moveToFront();
    }

    protected boolean isSelected() {
        return frame.isSelected();
    }

    protected void setSelected() {
        frame.setSelected();
    }

    protected void setStateColor(Color color) {
        frame.setStateColor(color);
    }

    public void pack() {
        frame.pack();
    }

    public void flash() {
        frame.flash(style().frameBorderFlashColor());
    }

    /**
     * Calls this view to the users attention:  move to front, select, and flash.
     */
    public void highlight() {
        gui().moveToExposeDefaultMenu(this);
        frame.moveToFront();
        setSelected();
        flash();
    }

    /**
     * If not already visible and selected, calls this view to the users attention:  move to front, select, and flash.
     */
    protected void highlightIfNotVisible() {
        frame.moveToFront();
        if (!isSelected()) {
            setSelected();
            frame.flash(style().frameBorderFlashColor());
        }
    }

    /**
     * Explicitly closes a particular view, but
     * many are closed implicitly by a window system
     * event on the frame.  Start the closure by
     * notifying the frame, which will then close
     * the view.
     */
    public final void dispose() {
        frame.dispose();
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    protected void viewGetsWindowFocus() {
    }

    /**
     * Receives notification that the the frame has acquired focus in the window system.
     */
    protected void viewLosesWindowFocus() {
    }

    /**
     * Receives notification that the window system is closing this view and cleans
     * up.
     * <ul>
     * <li>Removes the view's inspection, focus, and save settings listeners;</li>
     * <li>Notifies the view's view manager that the view is closing; and</li>
     * <li>Triggers a save event</li>
     * </ul>
     */
    protected void viewClosing() {
        inspection().removeInspectionListener(this);
        focus().removeListener(this);
        if (saveGeometrySettingsListener != null) {
            inspection().settings().removeSaveSettingsListener(saveGeometrySettingsListener);
        }
        inspection().settings().save();
        for (ViewEventListener listener : viewEventListeners) {
            listener.viewClosing(this);
        }
        // don't try to recompute the title, just get the one that's been in use
        Trace.line(1, tracePrefix() + " closing view " + getTitle());
    }

    public void vmStateChanged(boolean force) {
        final String title = getTitle();
        updateTracer.begin(title);
        refresh(force);
        updateTracer.end(title);
    }

    public void breakpointStateChanged() {
    }

    public void watchpointSetChanged() {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Default behavior for any change in view configuration is to just build
     * the view again from the start.  Concrete view types should override
     * this method if that doesn't work (although it probably ought to be made
     * to work always).
     */
    public void viewConfigurationChanged() {
        reconstructView();
    }

    public void vmProcessTerminated() {
    }

    public void inspectionEnding() {
    }

    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
    }

    public void threadFocusSet(MaxThread oldMaxThread, MaxThread maxeThread) {
    }

    public void frameFocusChanged(MaxStackFrame oldStackFrame, MaxStackFrame stackFrame) {
    }

    public void addressFocusChanged(Address oldAddress, Address address) {
    }

    public void memoryRegionFocusChanged(MaxMemoryRegion oldMemoryRegion, MaxMemoryRegion memoryRegion) {
    }

    public void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint) {
    }

    public void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint) {
    }

    public void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject) {
    }

    /**
     * @return an action that makes visible the view and highlights it.
     */
    public final InspectorAction getShowViewAction() {
        // Only need one, but maybe not even that one; create lazily.
        if (showViewAction == null) {
            showViewAction = new InspectorAction(inspection(), getTextForTitle()) {

                @Override
                protected void procedure() {
                    highlight();
                }
            };
        }
        return showViewAction;
    }

    /**
     * @return an action that closes the view.
     */
    public final InspectorAction getCloseViewAction() {
        // Only need one, but maybe not even that one; create lazily.
        if (closeViewAction == null) {
            closeViewAction = new InspectorAction(inspection(), "Close") {

                @Override
                protected void procedure() {
                    dispose();
                }
            };
        }
        return closeViewAction;
    }

    /**
     * @return an action that will present a dialog that enables selection of view options;
     * returns a disabled dummy action if not overridden.
     */
    public InspectorAction getViewOptionsAction() {
        final InspectorAction dummyViewOptionsAction = new InspectorAction(inspection(), "View Options") {
            @Override
            protected void procedure() {
            }
        };
        dummyViewOptionsAction.setEnabled(false);
        return dummyViewOptionsAction;
    }

    /**
     * @return an action that will refresh any state from the VM.
     */
    public InspectorAction getRefreshAction() {
        return new InspectorAction(inspection(), "Refresh") {
            @Override
            protected void procedure() {
                Trace.line(TRACE_VALUE, "Refreshing view: " + AbstractView.this);
                forceRefresh();
            }
        };
    }

    /**
     * @return the default print action for table-based views; depends on an overridden
     * {@link #getTable()} method to provide the table.
     */
    protected final InspectorAction getDefaultPrintAction() {
        return new InspectorAction(inspection(), "Print") {
            @Override
            public void procedure() {
                final MessageFormat footer = new MessageFormat(vm().entityName() + ": " + getTextForTitle() + "  Printed: " + new Date() + " -- Page: {0, number, integer}");
                try {
                    final InspectorTable inspectorTable = getTable();
                    assert inspectorTable != null;
                    inspectorTable.print(JTable.PrintMode.FIT_WIDTH, null, footer);
                } catch (PrinterException printerException) {
                    gui().errorMessage("Print failed: " + printerException.getMessage());
                }
            }
        };
    }

    /**
     * @return an action that will present a print dialog for printing the contents of the view;
     * returns a disabled dummy action if not overridden.
     */
    public InspectorAction getPrintAction() {
        final InspectorAction dummyPrintAction = new InspectorAction(inspection(), "Print") {
            @Override
            protected void procedure() {
            }
        };
        dummyPrintAction.setEnabled(false);
        return dummyPrintAction;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ":  " + getTitle();
    }

}