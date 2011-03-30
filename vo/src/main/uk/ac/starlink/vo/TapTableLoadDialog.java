package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoader;

/**
 * Load dialogue for TAP services.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapTableLoadDialog extends DalTableLoadDialog {

    private final Map<String,TapQueryPanel> tqMap_;
    private JTabbedPane tabber_;
    private JComponent tqContainer_;
    private TapQueryPanel tqPanel_;
    private UwsJobListPanel jobsPanel_;
    private ResumeTapQueryPanel resumePanel_;
    private CaretListener adqlListener_;
    private int tqTabIndex_;
    private int jobsTabIndex_;
    private int resumeTabIndex_;

    // This is an expression designed to pick up things that the user might
    // have entered as an upload table identifier.  It intentionally includes
    // illegal TAP upload strings, so that the getUploadTable method
    // has a chance to emit a helpful error message.
    private static final Pattern UPLOAD_REGEX =
        Pattern.compile( "TAP_UPLOAD\\.([^ ()*+-,/;<=>&?|\t\n\r]*)" );

    /**
     * Constructor.
     */
    public TapTableLoadDialog() {
        super( "TAP", "Query remote databases using SQL-like language",
               Capability.TAP, false, false );
        tqMap_ = new HashMap<String,TapQueryPanel>();
        setIconUrl( TapTableLoadDialog.class.getResource( "tap.gif" ) );
    }

    protected Component createQueryComponent() {

        /* Prepare a panel to search the registry for TAP services. */
        final Component searchPanel = super.createQueryComponent();

        /* Prepare a panel for monitoring running jobs. */
        jobsPanel_ = new UwsJobListPanel() {
            public void addJob( UwsJob job, boolean select ) {
                super.addJob( job, select );
                tabber_.setEnabledAt( jobsTabIndex_, true );
            }
            public void removeJob( UwsJob job ) {
                super.removeJob( job );
                if ( getJobs().length == 0 ) {
                    tabber_.setEnabledAt( jobsTabIndex_, false );
                    tabber_.setSelectedIndex( tqTabIndex_ );
                }
            }
        };

        /* Prepare a panel for resuming previously started jobs. */
        resumePanel_ = new ResumeTapQueryPanel( this );

        /* Prepare a tabbed panel to contain the components. */
        tabber_ = new JTabbedPane();
        tabber_.add( "Select Service", searchPanel );
        tqContainer_ = new JPanel( new BorderLayout() );
        String tqTitle = "Enter Query";
        tabber_.add( tqTitle, tqContainer_ );
        tqTabIndex_ = tabber_.getTabCount() - 1;
        tabber_.add( "Resume Job", resumePanel_ );
        resumeTabIndex_ = tabber_.getTabCount() - 1;
        tabber_.add( "Running Jobs", jobsPanel_ );
        jobsTabIndex_ = tabber_.getTabCount() - 1;

        /* Provide a button to move to the query tab.
         * Placing it near the service selector makes it more obvious that
         * that is what you need to do after selecting a TAP service. */
        final Action tqAct = new AbstractAction( tqTitle ) {
            public void actionPerformed( ActionEvent evt ) {
                tabber_.setSelectedIndex( tqTabIndex_ );
            }
        };
        tqAct.putValue( Action.SHORT_DESCRIPTION,
                        "Go to " + tqTitle
                      + " tab to prepare and execute TAP query" );
        Box buttLine = Box.createHorizontalBox();
        buttLine.add( Box.createHorizontalGlue() );
        buttLine.add( new JButton( tqAct ) );
        getControlBox().add( buttLine );

        /* Only enable the query tab if a valid service URL has been
         * selected. */
        tqAct.setEnabled( false );
        tabber_.setEnabledAt( tqTabIndex_, false );
        tabber_.setEnabledAt( jobsTabIndex_, false );
        getServiceUrlField().addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                boolean hasUrl;
                try {
                    checkUrl( getServiceUrl() );
                    hasUrl = true;
                }
                catch ( RuntimeException e ) {
                    hasUrl = false;
                }
                tabber_.setEnabledAt( tqTabIndex_, hasUrl );
                tqAct.setEnabled( hasUrl );
            }
        } );

        /* Arrange for the table query panel to get updated when it becomes
         * the visible tab. */
        tabber_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                if ( tabber_.getSelectedIndex() == tqTabIndex_ ) {
                    setSelectedService( getServiceUrl() );
                }
                updateReady();
            }
        } );

        /* Arrange that the TAP query submit action's enabledness status
         * can be sensitive to the content of the ADQL entry field. */
        adqlListener_ = new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        };

        /* It's big. */
        tabber_.setPreferredSize( new Dimension( 600, 550 ) );

        /* Return the tabbed pane which is the main query component. */
        return tabber_;
    }

    /**
     * Returns a table named by an upload specifier in an ADQL query.
     * The TapTableLoadDialog implementation of this throws an exception,
     * but subclasses may override this if they are capable of providing
     * uploadable tables.
     * If no table named by the given label is available, it is good
     * practice to throw an IllegalArgumentException with an informative
     * message, though returning null is also acceptable.
     *
     * @param  upLabel  name part of an uploaded table specification,
     *                  that is the part following the "TAP_UPLOAD." part
     * @return  table named by <code>upLabel</code>
     */
    protected StarTable getUploadTable( String upLabel ) {
        throw new IllegalArgumentException( "Upload tables not supported" );
    }

    public TableLoader createTableLoader() {
        int itab = tabber_.getSelectedIndex();
        if ( itab == tqTabIndex_ ) {
            return createQueryPanelLoader();
        }
        else if ( itab == resumeTabIndex_ ) {
            return resumePanel_.createTableLoader();
        }
        else {
            return null;
        }
    }

    /**
     * Adds a running TAP query to the list of queries this dialogue
     * is currently aware of.
     *
     * @param  tapQuery  query to add
     */
    public void addRunningQuery( TapQuery tapQuery ) {
        jobsPanel_.addJob( tapQuery.getUwsJob(), true );
        tabber_.setSelectedIndex( jobsTabIndex_ );
    }

    /**
     * Returns a new query TableLoader for the case when the QueryPanel
     * is the currently visible tab.
     *
     * @return   new loader
     */
    private TableLoader createQueryPanelLoader() {
        final URL serviceUrl = checkUrl( getServiceUrl() );
        final String adql = tqPanel_.getAdql();
        final String summary = TapQuery.summarizeAdqlQuery( serviceUrl, adql );
        final Map<String,StarTable> uploadMap =
            new LinkedHashMap<String,StarTable>();
        TapCapabilityPanel tcapPanel = tqPanel_.getCapabilityPanel();
        long rowUploadLimit = tcapPanel.getUploadLimit( TapLimit.ROWS );
        final long byteUploadLimit = tcapPanel.getUploadLimit( TapLimit.BYTES );
        Set<String> uploadLabels = getUploadLabels( adql );
        for ( String upLabel : uploadLabels ) {
            StarTable upTable = getUploadTable( upLabel );
            if ( upTable != null ) {
                long nrow = upTable.getRowCount();
                if ( rowUploadLimit >= 0 && nrow > rowUploadLimit ) {
                    throw new IllegalArgumentException(
                        "Table " + upLabel + " too many rows for upload "
                      + " (" + nrow + ">" + rowUploadLimit + ")" );
                }
                uploadMap.put( upLabel, upTable );
            }
            else {
                throw new IllegalArgumentException( "No known table \"" 
                                                  + upLabel + "\" for upload" );
            }
        }
        final Map<String,String> extraParams =
            new LinkedHashMap<String,String>();
        String language = tcapPanel.getQueryLanguage();
        if ( language != null && language.trim().length() > 0 ) {
            extraParams.put( "LANG", language );
        }
        long maxrec = tcapPanel.getMaxrec();
        if ( maxrec > 0 ) {
            extraParams.put( "MAXREC", Long.toString( maxrec ) );
        }
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                final TapQuery tapQuery =
                    TapQuery.createAdqlQuery( serviceUrl, adql, uploadMap,
                                              extraParams, byteUploadLimit,
                                              tfact.getStoragePolicy() );
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        addRunningQuery( tapQuery );
                    }
                } );
                List<DescribedValue> metaList =
                    new ArrayList<DescribedValue>();
                metaList.addAll( Arrays.asList( tapQuery.getQueryMetadata() ) );
                metaList.addAll( Arrays
                                .asList( getResourceMetadata( serviceUrl
                                                             .toString() ) ) );
                DescribedValue[] metas =
                    metaList.toArray( new DescribedValue[ 0 ] );
                return createTableSequence( tfact, tapQuery, metas );
            }
            public String getLabel() {
                return summary;
            }
        };
    }

    /**
     * Returns a table sequence constructed from a given TAP query.
     * This method marks each TapQuery for deletion on JVM shutdown,
     * or if the query fails.
     * Subclass implementations may override this method to perform
     * different job deletion behaviour.
     *
     * @param   tfact  table factory
     * @param   tapQuery  TAP query
     * @param   tapMeta  metadata describing the query suitable for
     *          decorating the resulting table
     * @return  table sequence suitable for a successful return from
     *          this dialog's TableLoader
     */
    protected TableSequence createTableSequence( StarTableFactory tfact,
                                                 TapQuery tapQuery,
                                                 DescribedValue[] tapMeta )
            throws IOException {
        UwsJob uwsJob = tapQuery.getUwsJob();
        uwsJob.setDeleteOnExit( true );
        tapQuery.start();
        StarTable st;
        try {
            st = tapQuery.waitForResult( tfact, 4000 );
        }
        catch ( InterruptedException e ) {
            uwsJob.attemptDelete();
            throw (IOException)
                  new InterruptedIOException( "Interrupted" )
                 .initCause( e );
        }
        catch ( IOException e ) {
            uwsJob.attemptDelete();
            throw e;
        }
        st.getParameters().addAll( Arrays.asList( tapMeta ) );
        return Tables.singleTableSequence( st );
    }

    public boolean isReady() {
        if ( tqPanel_ == null || tabber_.getSelectedIndex() != tqTabIndex_ ) {
            return false;
        }
        else {
            String adql = tqPanel_.getAdql();
            return super.isReady() && adql != null && adql.trim().length() > 0;
        }
    }

    /**
     * Returns a set of table identifers which are required for upload.
     * This is the set of any [identifier]s in the query of the form
     * TAP_UPLOAD.[identifier].
     *
     * @param   adql  ADQL/S text
     * @return   collection of upload identifiers
     */
    private static Set<String> getUploadLabels( String adql ) {

        /* Use of a regex for this partial parse is not bulletproof,
         * but you would have to have quite contrived ADQL to get a
         * false match here. */
        Set<String> labelSet = new HashSet<String>();
        Matcher matcher = UPLOAD_REGEX.matcher( adql );
        while ( matcher.find() ) {
            labelSet.add( matcher.group( 1 ) );
        }
        return labelSet;
    }

    /**
     * Configure this dialogue to use a TAP service with a given service URL.
     *
     * @param  serviceUrl  service URL for TAP service
     */
    private void setSelectedService( String serviceUrl ) {

        /* We have to install a TapQueryPanel for this service in the 
         * appropriate tab of the tabbed pane.
         * First remove any previously installed query panel. */
        if ( tqPanel_ != null ) {
            tqContainer_.remove( tqPanel_ );
            tqPanel_.getAdqlPanel().removeCaretListener( adqlListener_ );
            tqPanel_ = null;
        }
        if ( serviceUrl != null ) {

            /* Construct, configure and cache a suitable query panel
             * if we haven't seen this service URL before now. */
            if ( ! tqMap_.containsKey( serviceUrl ) ) {
                TapQueryPanel tqPanel = new TapQueryPanel();
                tqPanel.setServiceHeading( getServiceHeading( serviceUrl ) );
                tqPanel.setServiceUrl( serviceUrl );
                tqMap_.put( serviceUrl, tqPanel );
            }

            /* Get the panel from the cache, now guaranteed present. */
            tqPanel_ = tqMap_.get( serviceUrl );

            /* Install ready for use. */
            tqPanel_.getAdqlPanel().addCaretListener( adqlListener_ );
            tqContainer_.add( tqPanel_, BorderLayout.CENTER );
        }
        updateReady();
    }

    /**
     * Returns a line of text describing the given service URL.
     * This is intended to be as human-readable as possible, and will be
     * taken from the currently selected resource if it appears to be
     * appropriate for the given URL.
     *
     * @param  serviceUrl  service URL of TAP service to find a heading for
     * @return  human-readable description of service
     */
    private String getServiceHeading( String serviceUrl ) {
        if ( serviceUrl == null && serviceUrl.trim().length() == 0 ) {
            return "";
        }
        RegistryPanel regPanel = getRegistryPanel();
        RegResource[] resources = regPanel.getSelectedResources();
        RegCapabilityInterface[] caps = regPanel.getSelectedCapabilities();
        if ( caps.length == 1 && resources.length == 1 ) {
            String acref = caps[ 0 ].getAccessUrl();
            if ( serviceUrl.equals( caps[ 0 ].getAccessUrl() ) ) {
                String heading = getResourceHeading( resources[ 0 ] );
                if ( heading != null && heading.trim().length() > 0 ) {
                    return heading;
                }
            }
        }
        return serviceUrl;
    }

    /**
     * Returns a line of text describing the given registry resource.
     * This is intended to be as human-readable as possible.
     * If the resource contains no appropriate fields however,
     * null may be returned.
     *
     * @param  resource  resourse to describe
     * @return  human-readable description of resource, or null
     */
    private static String getResourceHeading( RegResource resource ) {
        String title = resource.getTitle();
        if ( title != null && title.trim().length() > 0 ) {
            return title;
        }
        String shortName = resource.getShortName();
        if ( shortName != null && shortName.trim().length() > 0 ) {
            return shortName;
        }
        String ident = resource.getIdentifier();
        if ( ident != null && ident.trim().length() > 0 ) {
            return ident;
        }
        return null;
    }
}
