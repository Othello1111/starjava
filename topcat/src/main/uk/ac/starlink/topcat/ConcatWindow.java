package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ConcatStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * Window for concatenating two tables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Mar 2004
 */
public class ConcatWindow extends AuxWindow implements ItemListener {

    private final JComboBox t1selector;
    private final JComboBox t2selector;
    private final JScrollPane colScroller;
    private final Action goAct;
    private JComboBox[] colSelectors;

    /**
     * Constructs a new concatenation window.
     *
     * @param  parent  parent window, may be used for window positioning
     */
    public ConcatWindow( Component parent ) {
        super( "Concatenate Tables", parent );

        JComponent main = getMainArea();
        main.setLayout( new BorderLayout() );

        /* Construct base table selection control. */
        t1selector = new JComboBox( new TablesListComboBoxModel() );
        t1selector.setToolTipText( "Table supplying the columns and top rows" );
        t1selector.addItemListener( this );

        /* Construct added table selection control. */
        t2selector = new JComboBox( new TablesListComboBoxModel() );
        t2selector.setToolTipText( "Table supplying the bottom rows" );
        t2selector.addItemListener( this );

        /* Place table selection controls. */
        Box tBox = Box.createVerticalBox();
        main.add( tBox, BorderLayout.NORTH );
        Box line = Box.createHorizontalBox();
        line.add( new JLabel( "Base Table: " ) );
        line.add( t1selector );
        line.add( Box.createHorizontalGlue() );
        tBox.add( line );
        line = Box.createHorizontalBox();
        line.add( new JLabel( "Appended Table: " ) );
        line.add( t2selector );
        line.add( Box.createHorizontalGlue() );
        tBox.add( line );

        /* Place the column correspondance box. */
        colScroller = new JScrollPane();
        colScroller.setPreferredSize( new Dimension( 300, 250 ) );
        colScroller.setBorder( makeTitledBorder( "Column Assignments" ) );
        main.add( colScroller, BorderLayout.CENTER );

        /* Place go button. */
        goAct = new ConcatAction( "Concatenate", null,
                                  "Create new concatenated table" );
        Box controlBox = Box.createHorizontalBox();
        getControlPanel().add( controlBox );
        controlBox.add( Box.createHorizontalGlue() );
        controlBox.add( new JButton( goAct ) );
        controlBox.add( Box.createHorizontalGlue() );

        /* Add standard help actions. */
        addHelp( "ConcatWindow" );

        /* Initialise state. */
        updateDisplay();

        /* Make the component visible. */
        pack();
        setVisible( true );
    }

    /**
     * Returns the selected base table.
     *
     * @return  base topcat model
     */
    private TopcatModel getBaseTable() {
        return (TopcatModel) t1selector.getSelectedItem();
    }

    /**
     * Returns the selected table for adding.
     *
     * @return  added topcat model
     */
    private TopcatModel getAddedTable() {
        return (TopcatModel) t2selector.getSelectedItem();
    }

    /**
     * Ensures the display shows the right thing for a given base and
     * additional table.
     */
    public void updateDisplay() {
        TopcatModel tc1 = getBaseTable();
        TopcatModel tc2 = getAddedTable();
        JPanel colPanel = new JPanel( new GridLayout( 0, 2 ) ) {
            public Dimension getMaximumSize() {
                return new Dimension( super.getMaximumSize().width, 
                                      super.getPreferredSize().height );
            }
        };
        Box colBox = Box.createVerticalBox();
        colBox.add( colPanel );
        colBox.add( Box.createVerticalGlue() );
        colScroller.setViewportView( colBox );
        if ( tc1 != null && tc2 != null ) {

            /* Title. */
            JLabel tl1 = new JLabel( "Base Table", SwingConstants.CENTER );
            JLabel tl2 = new JLabel( "Appended Table", SwingConstants.CENTER );
            tl1.setBorder( BorderFactory
                          .createBevelBorder( BevelBorder.RAISED ) );
            tl2.setBorder( BorderFactory
                          .createBevelBorder( BevelBorder.RAISED ) );
            colPanel.add( tl1 );
            colPanel.add( tl2 );

            /* One selector for each column. */
            TableColumnModel colModel1 = tc1.getColumnModel();
            TableColumnModel colModel2 = tc2.getColumnModel();
            int ncol = colModel1.getColumnCount();
            colSelectors = new JComboBox[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo cinfo = 
                    ((StarTableColumn) colModel1.getColumn( icol ))
                   .getColumnInfo();
                colPanel.add( new JLabel( cinfo.getName() + ": " ) );
                ColumnComboBoxModel comboModel =
                    RestrictedColumnComboBoxModel
                   .makeClassColumnComboBoxModel( colModel2, true, 
                                                  cinfo.getContentClass() );
                JComboBox combo = comboModel.makeComboBox();
                guessColumn( comboModel, cinfo );
                colSelectors[ icol ] = combo;
                colPanel.add( combo );
            }
        }
    }

    /**
     * Creates a new StarTable based on the selections made by the user.
     *
     * @return  concatenated table
     */
    private StarTable makeTable() {
        int ncol = colSelectors.length;
        int[] colMap = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            TableColumn tcol = (TableColumn) colSelectors[ icol ]
                               .getSelectedItem();
            colMap[ icol ] = tcol instanceof StarTableColumn 
                           ? ((StarTableColumn) tcol).getModelIndex()
                           : -1;
        }
        return new ConcatStarTable( getBaseTable().getApparentStarTable(),
                                    getAddedTable().getApparentStarTable(),
                                    colMap );
    }

    /**
     * Update the display of columns if the selected tables change.
     */
    public void itemStateChanged( ItemEvent evt ) {
        updateDisplay();
    }

    /**
     * Sets the initial selection on a column comboox model to match 
     * a given column info.  It matches names and UCDs and so on to try
     * to find some that look like they go together.  Could probably be
     * improved.
     *
     * @param  comboModel  combobox model containing the possible choices.
     *         Option 0 is some kind of null option
     * @param  cinfo1      column info which the selection should try to match
     */
    private static void guessColumn( ComboBoxModel comboModel,
                                     ColumnInfo cinfo1 ){
        int iSel = 0;
        String name1 = cinfo1.getName();
        String ucd1 = cinfo1.getUCD();

        /* If there is only one possible selection which matches the name
         * or the UCD of the column, use that.  Otherwise, use the null 
         * option. */
        for ( int i = 1; i < comboModel.getSize(); i++ ) {
            ColumnInfo cinfo2 = ((StarTableColumn) comboModel.getElementAt( i ))
                               .getColumnInfo();
            boolean match =
                name1 != null && name1.equalsIgnoreCase( cinfo2.getName() ) ||
                ucd1 != null && ucd1.equals( cinfo2.getUCD() );
            if ( match ) {
                if ( iSel == 0 ) {
                    iSel = i;
                }
                else {
                    iSel = 0;
                    break;
                }
            }
        }
        comboModel.setSelectedItem( comboModel.getElementAt( iSel ) );
    }

    /**
     * Action definisions for ConcatWindow.
     */
    private class ConcatAction extends BasicAction {
        ConcatAction( String name, Icon icon, String description ) {
            super( name, icon, description );
        }
        public void actionPerformed( ActionEvent evt ) {
            Component parent = ConcatWindow.this;
            if ( this == goAct ) {
                Object msg;
                int msgType;
                String title;
                boolean ok;
                try {
                    TopcatModel tcModel = 
                        ControlWindow.getInstance()
                                     .addTable( makeTable(), "concatenated",
                                                true );
                    title = "Tables Concatenated";
                    msg = "New concatenated table " + tcModel + " created";
                    msgType = JOptionPane.INFORMATION_MESSAGE;
                    ok = true;
                }
                catch ( Exception e ) {
                    title = "No Concatenation";
                    msg = e.getMessage();
                    msgType = JOptionPane.WARNING_MESSAGE;
                    ok = false;
                }
                JOptionPane.showMessageDialog( parent, msg, title, msgType );
                if ( ok ) {
                    dispose();
                }
            }
            else {
                throw new AssertionError();
            }
        }
    }
}
