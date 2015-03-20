package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.gui.ArrayTableColumn;
import uk.ac.starlink.util.gui.ArrayTableModel;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Displays the metadata from an array of SchemaMeta objects.
 * These can be acquired from a TableSet XML document as exposed
 * by VOSI and TAP interfaces or from interrogating TAP_SCHEMA tables.
 *
 * @author   Mark Taylor
 * @since    21 Jan 2011
 */
public class TableSetPanel extends JPanel {

    private final JTree tTree_;
    private final JTextField searchField_;
    private final JLabel countLabel_;
    private final TreeSelectionModel selectionModel_;
    private final JTable colTable_;
    private final JTable foreignTable_;
    private final ArrayTableModel colTableModel_;
    private final ArrayTableModel foreignTableModel_;
    private final MetaColumnModel colColModel_;
    private final MetaColumnModel foreignColModel_;
    private final SchemaMetaPanel schemaPanel_;
    private final TableMetaPanel tablePanel_;
    private final JTabbedPane detailTabber_;
    private final int itabSchema_;
    private final int itabTable_;
    private final int itabCol_;
    private final int itabForeign_;
    private final JComponent metaPanel_;
    private final JSplitPane metaSplitter_;
    private TapMetaReader metaReader_;
    private ExecutorService metaExecutor_;
    private SchemaMeta[] schemas_;

    /** Number of nodes below which tree nodes are expanded. */
    private static final int TREE_EXPAND_THRESHOLD = 100;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     */
    public TableSetPanel() {
        super( new BorderLayout() );
        tTree_ = new JTree();
        tTree_.setRootVisible( false );
        tTree_.setShowsRootHandles( true );
        tTree_.setExpandsSelectedPaths( true );
        selectionModel_ = tTree_.getSelectionModel();
        selectionModel_.setSelectionMode( TreeSelectionModel
                                         .SINGLE_TREE_SELECTION );
        selectionModel_.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent evt ) {
                updateForSelection();
            }
        } );

        searchField_ = new JTextField();
        searchField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateTree( false );
            }
        } );
        JLabel searchLabel = new JLabel( "Find: " );
        String searchTip = "Enter one or more strings to restrict the content "
                         + "of the schema display tree";
        searchField_.setToolTipText( searchTip );
        searchLabel.setToolTipText( searchTip );

        countLabel_ = new JLabel();

        colTableModel_ = new ArrayTableModel( createColumnMetaColumns(),
                                              new ColumnMeta[ 0 ] );
        colTable_ = new JTable( colTableModel_ );
        colTable_.setColumnSelectionAllowed( false );
        colTable_.setRowSelectionAllowed( false );
        colColModel_ =
            new MetaColumnModel( colTable_.getColumnModel(), colTableModel_ );
        colTable_.setColumnModel( colColModel_ );

        foreignTableModel_ = new ArrayTableModel( createForeignMetaColumns(),
                                                  new ColumnMeta[ 0 ] );
        foreignTable_ = new JTable( foreignTableModel_ );
        foreignTable_.setColumnSelectionAllowed( false );
        foreignTable_.setRowSelectionAllowed( false );
        foreignColModel_ =
            new MetaColumnModel( foreignTable_.getColumnModel(),
                                 foreignTableModel_ );
        foreignTable_.setColumnModel( foreignColModel_ );

        tablePanel_ = new TableMetaPanel();
        schemaPanel_ = new SchemaMetaPanel();

        detailTabber_ = new JTabbedPane();
        int itab = 0;
        detailTabber_.addTab( "Schema", schemaPanel_ );
        itabSchema_ = itab++;
        detailTabber_.addTab( "Table", tablePanel_ );
        itabTable_ = itab++;
        detailTabber_.addTab( "Columns", new JScrollPane( colTable_ ) );
        itabCol_ = itab++;
        detailTabber_.addTab( "Foreign Keys",
                              new JScrollPane( foreignTable_ ) );
        itabForeign_ = itab++;

        JComponent treePanel = new JPanel( new BorderLayout() );
        treePanel.add( new JScrollPane( tTree_ ), BorderLayout.CENTER );
        treePanel.add( countLabel_, BorderLayout.SOUTH );
        JComponent searchLine = Box.createHorizontalBox();
        searchLine.add( searchLabel );
        searchLine.add( searchField_ );
        searchLine.setBorder( BorderFactory.createEmptyBorder( 0, 0, 5, 0 ) );
        treePanel.add( searchLine, BorderLayout.NORTH );

        metaSplitter_ = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        metaSplitter_.setBorder( BorderFactory.createEmptyBorder() );
        treePanel.setMinimumSize( new Dimension( 100, 100 ) );
        detailTabber_.setMinimumSize( new Dimension( 100, 100 ) );
        metaSplitter_.setLeftComponent( treePanel );
        metaSplitter_.setRightComponent( detailTabber_ );

        metaPanel_ = new JPanel( new BorderLayout() );
        metaPanel_.add( metaSplitter_, BorderLayout.CENTER );
        add( metaPanel_, BorderLayout.CENTER );
        setSchemas( null, null );
    }

    /**
     * Returns a new menu for controlling which columns are visible in
     * the column display table.
     *
     * @param  name  menu name
     */
    public JMenu makeColumnDisplayMenu( String name ) {
        return colColModel_.makeCheckBoxMenu( name );
    }

    /**
     * Installs an object that knows how to acquire TAP schema metadata.
     * If the supplied reader is non-null, calling this method
     * initiates an asynchronous read of the metadata, which will
     * be displayed in this panel when it arrives.
     *
     * @param  metaReader   new metadata reader; may be null to clear display
     */
    public void setMetaReader( final TapMetaReader metaReader ) {
        if ( metaReader_ != null ) {
            metaExecutor_.shutdownNow();
            metaExecutor_ = null;
        }
        metaReader_ = metaReader;
        setSchemas( metaReader, null );
        if ( metaReader_ == null ) {
            return;
        }
        showFetchProgressBar( metaReader, "Fetching Table Metadata" );
        Runnable schemaWorker = new Runnable() {
            public void run() {
                if ( metaReader != metaReader ) {
                    return;
                }
                final SchemaMeta[] schemas;
                try {
                    schemas = metaReader.readSchemas();
                }
                catch ( final Exception error ) {
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            showFetchFailure( metaReader, error );
                        }
                    } );
                    return;
                }
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        setSchemas( metaReader, schemas );
                    }
                } );
            }
        };
        metaExecutor_ = Executors.newSingleThreadExecutor( new ThreadFactory() {
            public Thread newThread( Runnable r ) {
                Thread th = new Thread( r, "TAP Metadata Query" );
                th.setDaemon( true );
                return th;
            }
        } );
        metaExecutor_.submit( schemaWorker );
    }

    /**
     * Returns the current table metadata set.
     * May be null if the read is still in progress.
     *
     * @return   current schema metadata array, may be null
     */
    public SchemaMeta[] getSchemas() {
        return schemas_;
    }

    /**
     * Sets the data model for the metadata displayed by this panel,
     * and updates the display.
     * The data is in the form of an array of schema metadata objects.
     *
     * @param  metaReader  reader supplying the schemas; if it doesn't match
     *                     the currently installed one, the call is ignored
     * @param  schemas  schema metadata objects, null if no metadata available
     */
    private void setSchemas( TapMetaReader metaReader, SchemaMeta[] schemas ) {
        if ( metaReader != metaReader_ ) {
            return;
        }
        if ( schemas != null ) {
            checkSchemasPopulated( schemas );
        }
        schemas_ = schemas;
        TreeModel treeModel =
            new TapMetaTreeModel( schemas_ == null ? new SchemaMeta[ 0 ]
                                                   : schemas_ );
        tTree_.setModel( new MaskTreeModel( treeModel ) );
        searchField_.setText( null );
        selectionModel_.setSelectionPath( null );
        updateTree( true );

        final String countTxt;
        if ( schemas == null ) {
            countTxt = "no metadata";
        }
        else {
            int nTable = 0;
            for ( SchemaMeta schema : schemas ) {
                nTable += schema.getTables().length;
            }
            countTxt = schemas.length + " schemas, " + nTable + " tables";
        }
        countLabel_.setText( countTxt );

        metaPanel_.removeAll();
        metaPanel_.add( metaSplitter_ );
        metaPanel_.revalidate();
        updateForSelection();
        repaint();
    }

    /**
     * Displays a progress bar to indicate that metadata fetching is going on.
     *
     * @param  metaReader  reader in use; if it doesn't match
     *                     the currently installed one, the call is ignored
     * @param  message  message to display
     */
    private void showFetchProgressBar( TapMetaReader metaReader,
                                       String message ) {
        if ( metaReader != metaReader_ ) {
            return;
        }
        JProgressBar progBar = new JProgressBar();
        progBar.setIndeterminate( true );
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.add( Box.createHorizontalGlue() );
        msgLine.add( new JLabel( message ) );
        msgLine.add( Box.createHorizontalGlue() );
        JComponent progLine = Box.createHorizontalBox();
        progLine.add( Box.createHorizontalGlue() );
        progLine.add( progBar );
        progLine.add( Box.createHorizontalGlue() );
        JComponent workBox = Box.createVerticalBox();
        workBox.add( Box.createVerticalGlue() );
        workBox.add( msgLine );
        workBox.add( Box.createVerticalStrut( 5 ) );
        workBox.add( progLine );
        workBox.add( Box.createVerticalGlue() );
        JComponent workPanel = new JPanel( new BorderLayout() );
        workPanel.add( workBox, BorderLayout.CENTER );
        metaPanel_.removeAll();
        metaPanel_.add( workPanel, BorderLayout.CENTER );
        metaPanel_.revalidate();
    }

    /**
     * Displays an indication that metadata fetching failed.
     * 
     * @param  metaReader  reader in use; if it doesn't match
     *                     the currently installed one, the call is ignored
     * @param  error   error that caused the failure
     */
    private void showFetchFailure( TapMetaReader metaReader, Throwable error ) {
        if ( metaReader != metaReader_ ) {
            return;
        }
        ErrorDialog.showError( this, "Table Metadata Error", error );
        JComponent msgLine = Box.createHorizontalBox();
        msgLine.setAlignmentX( 0 );
        msgLine.add( new JLabel( "No table metadata available" ) );
        JComponent srcLine = Box.createHorizontalBox();
        srcLine.setAlignmentX( 0 );
        srcLine.add( new JLabel( "Metadata Source: " ) );
        JTextField srcField = new JTextField( metaReader.getSource() );
        srcField.setEditable( false );
        srcField.setBorder( BorderFactory.createEmptyBorder() );
        srcLine.add( new ShrinkWrapper( srcField ) );
        JComponent errLine = Box.createHorizontalBox();
        errLine.setAlignmentX( 0 );
        errLine.add( new JLabel( "Error: " ) );
        String errtxt = error.getMessage();
        if ( errtxt == null || errtxt.trim().length() == 0 ) {
            errtxt = error.toString();
        }
        JTextField errField = new JTextField( errtxt );
        errField.setEditable( false );
        errField.setBorder( BorderFactory.createEmptyBorder() );
        errLine.add( new ShrinkWrapper( errField ) );
        JComponent linesVBox = Box.createVerticalBox();
        linesVBox.add( Box.createVerticalGlue() );
        linesVBox.add( msgLine );
        linesVBox.add( Box.createVerticalStrut( 15 ) );
        linesVBox.add( srcLine );
        linesVBox.add( errLine );
        linesVBox.add( Box.createVerticalGlue() );
        JComponent linesHBox = Box.createHorizontalBox();
        linesHBox.add( Box.createHorizontalGlue() );
        linesHBox.add( linesVBox );
        linesHBox.add( Box.createHorizontalGlue() );
        JComponent panel = new JPanel( new BorderLayout() );
        panel.add( linesHBox, BorderLayout.CENTER );
        metaPanel_.removeAll();
        metaPanel_.add( panel, BorderLayout.CENTER );
        metaPanel_.revalidate();
    }

    /**
     * Checks that all the schemas are populated with lists of their tables.
     * The SchemaMeta and TapMetaReader interface permit unpopulated schemas,
     * but this GUI relies in some places on the assumption that schemas
     * are always populated, so things will probably go wrong if it's not
     * the case.  Log a warning in case of unpopulated schemas
     * to give a clue what's wrong.
     *
     * @param  schemas  schemas to test
     */
    private void checkSchemasPopulated( SchemaMeta[] schemas ) {
        for ( SchemaMeta smeta : schemas ) {
            if ( smeta.getTables() == null ) {
                logger_.warning( "Schema metadata object(s) not populated"
                               + " with tables, probably will cause trouble"
                               + "; use a different TapMetaReader?" );
                return;
            }
        }
    }

    /**
     * Returns the table which is currently selected for metadata display.
     *
     * @return   selected table, may be null
     */
    public TableMeta getSelectedTable() {
        return TapMetaTreeModel.getTable( selectionModel_.getSelectionPath() );
    }

    /**
     * Invoked when table selection may have changed.
     */
    private void updateForSelection() {
        TreePath path = selectionModel_.getSelectionPath();
        final TableMeta table = TapMetaTreeModel.getTable( path );
        SchemaMeta schema = TapMetaTreeModel.getSchema( path );
        ColumnMeta[] cols = table == null ? new ColumnMeta[ 0 ]
                                          : table.getColumns();
        ForeignMeta[] fkeys = table == null ? new ForeignMeta[ 0 ]
                                            : table.getForeignKeys();
        if ( cols == null ) {
            metaExecutor_.submit( new Runnable() {
                public void run() {
                    populateColumns( table );
                }
            } );
            cols = new ColumnMeta[ 0 ];
        }
        if ( fkeys == null ) {
            metaExecutor_.submit( new Runnable() {
                public void run() {
                    populateForeignKeys( table );
                }
            } );
            fkeys = new ForeignMeta[ 0 ];
        }
        schemaPanel_.setSchema( schema );
        detailTabber_.setIconAt( itabSchema_, activeIcon( schema != null ) );
        tablePanel_.setTable( table );
        detailTabber_.setIconAt( itabTable_, activeIcon( table != null ) );
        displayColumns( table, cols );
        displayForeignKeys( table, fkeys );
    }

    /**
     * Updates the display if required for the columns of a table.
     *
     * @param  table  table
     * @param  cols  columns
     */
    private void displayColumns( TableMeta table, ColumnMeta[] cols ) {
        if ( table == getSelectedTable() ) {
            colTableModel_.setItems( cols );
            detailTabber_.setIconAt( itabCol_, activeIcon( cols != null &&
                                                           cols.length > 0 ) );
            if ( table != null ) {
                configureColumnWidths( colTable_ );
            }
        }
    }

    /**
     * Updates the display if required for the foreign keys of a table.
     *
     * @param  table  table
     * @param  fkeys  foreign keys
     */
    private void displayForeignKeys( TableMeta table, ForeignMeta[] fkeys ) {
        if ( table == getSelectedTable() ) {
            foreignTableModel_.setItems( fkeys );
            detailTabber_.setIconAt( itabForeign_,
                                     activeIcon( fkeys != null &&
                                                 fkeys.length > 0 ) );
            if ( table != null ) {
                configureColumnWidths( foreignTable_ );
            }
        }
    }

    /**
     * Configures the columns widths of a JTable in the tabbed pane
     * to correspond to its current contents.
     *
     * @param  jtable   table to update
     */
    private void configureColumnWidths( final JTable jtable ) {
        Runnable configer = new Runnable() {
            public void run() {
                StarJTable.configureColumnWidths( jtable, 360, 9999 );
            }
        };
        if ( metaSplitter_.getSize().width > 0 ) {
            configer.run();
        }
        else {
            SwingUtilities.invokeLater( configer );
        }
    }

    /**
     * Performs the work for asynchronous acquisition of column metadata
     * for a given table.  On completion, it updates the TableMeta object
     * and the display.
     * Do not call from the event dispatch thread.
     *
     * @param  tmeta   table currently lacking column metadata
     */
    private void populateColumns( final TableMeta tmeta ) {
        ColumnMeta[] cmetas;
        try {
            cmetas = metaReader_.readColumns( tmeta );
        }
        catch ( IOException e ) {
            logger_.warning( "Failed to read column metadata for table "
                           + tmeta.getName() );
            cmetas = new ColumnMeta[ 0 ];
        }
        final ColumnMeta[] cmetas0 = cmetas;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                tmeta.setColumns( cmetas0 );
                displayColumns( tmeta, cmetas0 );
            }
        } );
    }

    /**
     * Performs the work for asynchronous acquisition of foreign key metadata
     * for a given table.  On completion, it updates the TableMeta object
     * and the display.
     * Do not call from the event dispatch thread.
     *
     * @param  tmeta   table currently lacking column metadata
     */
    private void populateForeignKeys( final TableMeta tmeta ) {
        ForeignMeta[] fmetas;
        try {
            fmetas = metaReader_.readForeignKeys( tmeta );
        }
        catch ( IOException e ) {
            logger_.warning( "Failed to read foreign key metadata for table "
                           + tmeta.getName() );
            fmetas = new ForeignMeta[ 0 ];
        }
        final ForeignMeta[] fmetas0 = fmetas;
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                tmeta.setForeignKeys( fmetas0 );
                displayForeignKeys( tmeta, fmetas0 );
            }
        } );
    }

    /**
     * Called if the schema information in the JTree or its presentation
     * rules may have changed.
     *
     * @param  dataChanged  true iff this update includes a change of
     *         the schema array underlying the tree model
     */
    private void updateTree( boolean dataChanged ) {

        /* We should have a MaskTreeModel, unless maybe there's no data. */
        TreeModel treeModel = tTree_.getModel();
        if ( ! ( treeModel instanceof MaskTreeModel ) ) {
            return;
        }
        MaskTreeModel mModel = (MaskTreeModel) treeModel;

        /* Get a node mask object from the text entry field. */
        String text = searchField_.getText();
        MaskTreeModel.Mask mask = text == null || text.trim().length() == 0
                                ? null
                                : new TextMask( text );

        /* We will be changing the mask, which will cause a
         * treeStructureChanged TreeModelEvent to be sent to listeners,
         * more or less wiping out any state of the JTree view.
         * So store the view state we want to preserve (information about
         * selections and node expansion) here, so we can restore it after
         * the model has changed. */
        Object root = mModel.getRoot();
        TreePath[] selections = tTree_.getSelectionPaths();
        List<TreePath> expandedList = new ArrayList<TreePath>();
        for ( Enumeration<TreePath> tpEn =
                  tTree_.getExpandedDescendants( new TreePath( root ) );
              tpEn.hasMoreElements(); ) {
            expandedList.add( tpEn.nextElement() );
        }
        TreePath[] oldExpanded = expandedList.toArray( new TreePath[ 0 ] );
        int oldCount = mModel.getNodeCount();

        /* Update the model. */
        mModel.setMask( mask );

        /* Apply node expansions in the JTree view.  This is a bit ad hoc.
         * If we've just cut the tree down from huge to manageable,
         * expand all the top-level (schema) nodes.  Conversely,
         * if we've just grown it from manageable to huge, collapse
         * all the top-level nodes.  Otherwise, try to retain (restore)
         * the previous expansion state.  */
        int newCount = mModel.getNodeCount();
        int ne = TREE_EXPAND_THRESHOLD;
        final TreePath[] newExpanded;
        if ( ( dataChanged || oldCount > ne ) && newCount < ne ) {
            int nc = mModel.getChildCount( root );
            newExpanded = new TreePath[ nc ];
            for ( int ic = 0; ic < nc; ic++ ) {
                Object child = mModel.getChild( root, ic );
                newExpanded[ ic ] =
                    new TreePath( new Object[] { root, child } );
            }
        }
        else if ( dataChanged || ( oldCount < ne && newCount > ne ) ) {
            newExpanded = new TreePath[ 0 ];
        }
        else {
            newExpanded = oldExpanded;
        }
        for ( TreePath expTp : newExpanded ) {
            tTree_.expandPath( expTp );
        }

        /* Try to restore previous selections (only one, probably).
         * If the old selections are no longer in the tree, chuck them out. */
        if ( mask != null && selections != null ) {
            selections = sanitiseSelections( selections, mModel );
        }

        /* If for whatever reason we have no selections, select something.
         * This logic will probably pick the first leaf node (table metadata,
         * rather than schema metadata).  But if it can't find it where it
         * expects, it will just pick the very first node in the tree. */
        if ( selections == null || selections.length == 0 ) {
            TreePath tp0 = tTree_.getPathForRow( 0 );
            TreePath tp1 = tTree_.getPathForRow( 1 );
            TreePath tp = tp1 != null && tp0 != null
                       && mModel.isLeaf( tp1.getLastPathComponent() )
                       && ! mModel.isLeaf( tp0.getLastPathComponent() )
                     ? tp1
                     : tp0;
            selections = tp != null ? new TreePath[] { tp }
                                    : new TreePath[ 0 ];
        }
        tTree_.setSelectionPaths( selections );
    }

    /**
     * Get a list of tree paths based on a given list, but making sure
     * they exist in a given tree model.
     *
     * @param   selections  initial selection paths
     * @param   model   tree model within which result selections must exist
     * @return  list of selections that are in the model (may be empty)
     */
    private static TreePath[] sanitiseSelections( TreePath[] selections,
                                                  TreeModel model ) {
        List<TreePath> okPaths = new ArrayList<TreePath>();
        Object root = model.getRoot();

        /* Tackle each input path one at a time. */
        for ( TreePath path : selections ) {
            if ( path.getPathComponent( 0 ) != root ) {
                assert false;
            }
            else {

                /* Try to add each element of the input path to the result path.
                 * If any of the nodes in the chain is missing, just stop
                 * there, leaving an ancestor of the input path. */
                int nel = path.getPathCount();
                List<Object> els = new ArrayList<Object>( nel );
                els.add( root );
                for ( int i = 1; i < nel; i++ ) {
                    Object pathEl = path.getPathComponent( i );
                    if ( model.getIndexOfChild( els.get( i - 1 ), pathEl )
                         >= 0 ) {
                        els.add( pathEl );
                    }
                    else {
                        break;
                    }
                }

                /* Add it to the result, but only if it's not just the
                 * root node (and if we don't already have it). */
                TreePath okPath =
                    new TreePath( els.toArray( new Object[ 0 ] ) );
                if ( okPath.getPathCount() > 1 &&
                     ! okPaths.contains( okPath ) ) {
                    okPaths.add( okPath );
                }
            }
        }
        return okPaths.toArray( new TreePath[ 0 ] );
    }

    /**
     * Returns a small icon indicating whether a given tab is currently
     * active or not.
     *
     * @param   isActive  true iff tab has content
     * @return  little icon
     */
    private Icon activeIcon( boolean isActive ) {
        return HasContentIcon.getIcon( isActive );
    }

    /**
     * Returns the ColumnMeta object associated with a given item
     * in the column metadata table model.  It's just a cast.
     *
     * @param   item  table cell contents
     * @return   column metadata object associated with <code>item</code>
     */
    private static ColumnMeta getCol( Object item ) {
        return (ColumnMeta) item;
    }

    /**
     * Returns the ForeignMeta object associated with a given item
     * in the foreign key table model.  It's just a cast.
     *
     * @param  item   table cell contents
     * @return   foreign key object associated with <code>item</code>
     */
    private static ForeignMeta getForeign( Object item ) {
        return (ForeignMeta) item;
    }

    /**
     * Utility method to return a string representing the length of an array.
     *
     * @param  array  array object, or null
     * @return  string giving length of array, or null for null input
     */
    private static String arrayLength( Object[] array ) {
        return array == null ? null : Integer.toString( array.length );
    }

    /**
     * Constructs an array of columns which define the table model
     * to use for displaying the column metadata.
     *
     * @return   column descriptions
     */
    private static ArrayTableColumn[] createColumnMetaColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Name", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getName();
                }
            },
            new ArrayTableColumn( "DataType", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getDataType();
                }
            },
            new ArrayTableColumn( "Indexed", Boolean.class ) {
                public Object getValue( Object item ) {
                    return Boolean
                          .valueOf( Arrays.asList( getCol( item ).getFlags() )
                                          .indexOf( "indexed" ) >= 0 );
                }
            },
            new ArrayTableColumn( "Unit", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUnit();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getDescription();
                }
            },
            new ArrayTableColumn( "UCD", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUcd();
                }
            },
            new ArrayTableColumn( "Utype", String.class ) {
                public Object getValue( Object item ) {
                    return getCol( item ).getUtype();
                }
            },
            new ArrayTableColumn( "Flags", String.class ) {
                public Object getValue( Object item ) {
                    String[] flags = getCol( item ).getFlags();
                    if ( flags != null && flags.length > 0 ) {
                        StringBuffer sbuf = new StringBuffer();
                        for ( int i = 0; i < flags.length; i++ ) {
                            if ( i > 0 ) {
                                sbuf.append( ' ' );
                            }
                            sbuf.append( flags[ i ] );
                        }
                        return sbuf.toString();
                    }
                    else {
                        return null;
                    }
                }
            },
        };
    }

    /**
     * Constructs an array of columns which define the table model
     * to use for displaying foreign key information.
     *
     * @return  column descriptions
     */
    private static ArrayTableColumn[] createForeignMetaColumns() {
        return new ArrayTableColumn[] {
            new ArrayTableColumn( "Target Table", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getTargetTable();
                }
            },
            new ArrayTableColumn( "Links", String.class ) {
                public Object getValue( Object item ) {
                    ForeignMeta.Link[] links = getForeign( item ).getLinks();
                    StringBuffer sbuf = new StringBuffer();
                    for ( int i = 0; i < links.length; i++ ) {
                        ForeignMeta.Link link = links[ i ];
                        if ( i > 0 ) {
                            sbuf.append( "; " );
                        }
                        sbuf.append( link.getFrom() )
                            .append( "->" )
                            .append( link.getTarget() );
                    }
                    return sbuf.toString();
                }
            },
            new ArrayTableColumn( "Description", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getDescription();
                }
            },
            new ArrayTableColumn( "Utype", String.class ) {
                public Object getValue( Object item ) {
                    return getForeign( item ).getUtype();
                }
            },
        };
    }

    /**
     * Tree node mask that selects on simple matches of node name strings
     * to one or more space-separated words entered in the search field.
     */
    private static class TextMask implements MaskTreeModel.Mask {
        private final Set<String> lwords_;

        /**
         * Constructor.
         *
         * @param  txt  entered text
         */
        TextMask( String txt ) {
            lwords_ = new HashSet<String>( Arrays.asList( txt.trim()
                                          .toLowerCase().split( "\\s+" ) ) );
        }

        public boolean isIncluded( Object node ) {
            if ( node != null && node.toString() != null ) {
                String nodeTxt = node.toString().toLowerCase();
                for ( String lword : lwords_ ) {
                    if ( nodeTxt.indexOf( lword ) >= 0 ) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return lwords_.hashCode();
        }

        @Override
        public boolean equals( Object other ) {
            return other instanceof TextMask
                && this.lwords_.equals( ((TextMask) other).lwords_ );
        }
    }

    /**
     * MetaPanel subclass for displaying table metadata.
     */
    private static class TableMetaPanel extends MetaPanel {
        private final JTextComponent nameField_;
        private final JTextComponent ncolField_;
        private final JTextComponent nfkField_;
        private final JTextComponent descripField_;
        private TableMeta table_;

        /**
         * Constructor.
         */
        TableMetaPanel() {
            nameField_ = addLineField( "Name" );
            ncolField_ = addLineField( "Columns" );
            nfkField_ = addLineField( "Foreign Keys" );
            descripField_ = addMultiLineField( "Description" );
        }

        /**
         * Configures this component to display metadata for a given table.
         *
         * @param  table  table metadata to display
         */
        public void setTable( TableMeta table ) {
            if ( table != table_ ) {
                table_ = table;
                nameField_.setText( table == null ? null : table.getName() );
                nameField_.setCaretPosition( 0 );
                descripField_.setText( table == null ? null
                                                     : table.getDescription() );
                descripField_.setCaretPosition( 0 );
                ColumnMeta[] cols = table == null ? null : table.getColumns();
                ncolField_.setText( arrayLength( cols ) );
                ForeignMeta[] fks = table == null ? null
                                                  : table.getForeignKeys();
                nfkField_.setText( arrayLength( fks ) );
            }
        }
    }

    /**
     * MetaPanel subclass for displaying schema metadata.
     */
    private static class SchemaMetaPanel extends MetaPanel {
        private final JTextComponent nameField_;
        private final JTextComponent ntableField_;
        private final JTextComponent descripField_;
        private SchemaMeta schema_;

        /**
         * Constructor.
         */
        SchemaMetaPanel() {
            nameField_ = addLineField( "Name" );
            ntableField_ = addLineField( "Tables" );
            descripField_ = addMultiLineField( "Description" );
        }

        /**
         * Configures this component to display metadata for a given schema.
         *
         * @param  schema  schema metadata to display
         */
        public void setSchema( SchemaMeta schema ) {
            if ( schema != schema_ ) {
                schema_ = schema;
                nameField_.setText( schema == null ? null : schema.getName() );
                nameField_.setCaretPosition( 0 );
                descripField_.setText( schema == null
                                     ? null
                                     : schema.getDescription() );
                descripField_.setCaretPosition( 0 );
                TableMeta[] tables = schema == null ? null : schema.getTables();
                ntableField_.setText( arrayLength( tables ) );
            }
        }
    }
}
