package uk.ac.starlink.vo;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import uk.ac.starlink.vo.TapServiceFinder.Service;
import uk.ac.starlink.vo.TapServiceFinder.Table;

/**
 * TreeModel implementation representing a particular set of tables
 * contained in a list of known TAP services.
 *
 * @author   Mark Taylor
 * @since    30 Jun 2015
 */
public class TapServiceTreeModel implements TreeModel {

    private final String rootLabel_;
    private final Service[] services_;
    private final Map<Service,Table[]> tableMap_;
    private final List<TreeModelListener> listeners_;

    /**
     * Constructor.
     *
     * @param  rootLabel  text label for root element (may be null)
     * @param  services  list of services nodes in tree
     * @param  tableMap  array of tables providing child nodes for each service;
     *                   may be null if no table children are required,
     *                   but if not null must contain an entry for each service
     */
    protected TapServiceTreeModel( String rootLabel, Service[] services,
                                   Map<Service,Table[]> tableMap ) {
        rootLabel_ = rootLabel;
        services_ = services;
        tableMap_ = tableMap;
        listeners_ = new ArrayList<TreeModelListener>();
        if ( tableMap != null ) {
            for ( Service service : services ) {
                if ( tableMap.get( service ) == null ) {
                    throw new IllegalArgumentException( "No table list for "
                                                      + service );
                }
            }
        }
    }

    /**
     * Constructs an instance with no entries.
     *
     * @param  rootLabel  text label for root element (may be null)
     */
    public TapServiceTreeModel( String rootLabel ) {
        this( rootLabel, new Service[ 0 ], null );
    }

    public Object getRoot() {
        return services_;
    }

    public boolean isLeaf( Object node ) {
        return asNode( node ).isLeaf();
    }

    public int getChildCount( Object parent ) {
        Object[] children = asNode( parent ).children_;
        return children == null ? 0 : children.length;
    }

    public Object getChild( Object parent, int index ) {
        return asNode( parent ).children_[ index ];
    }

    public int getIndexOfChild( Object parent, Object child ) {
        if ( parent != null && child != null ) {
            Object[] children = asNode( parent ).children_;
            if ( children != null ) {

                /* Obviously, not very efficient.  I'm not sure when this
                 * method is called, but not I think during normal tree
                 * navigation.  So assume it doesn't matter unless it is
                 * demonstrated otherwise. */
                int nc = children.length;
                for ( int ic = 0; ic < nc; ic++ ) {
                    if ( children[ ic ] == child ) {
                        return ic;
                    }
                }
            }
        }
        return -1;
    }

    public void valueForPathChanged( TreePath path, Object newValue ) {
        assert false : "Tree is not editable from GUI";
    }

    public void addTreeModelListener( TreeModelListener lnr ) {
        listeners_.add( lnr );
    }

    public void removeTreeModelListener( TreeModelListener lnr ) {
        listeners_.remove( lnr );
    }

    /**
     * Returns a tree path which correponds to a TAP service, and which
     * is an ancestor of the supplied path.  The supplied path counts
     * as its own ancestor for these purposes.
     *
     * @param  path   path to examine
     * @return   path corresponding to a sub-path of the supplied one,
     *           for which the terminal element is a TapServiceFinder.Service,
     *           or null if no service appears in the ancestry
     */
    public static TreePath getServicePath( TreePath path ) {
        for ( int i = 0; i < path.getPathCount(); i++ ) {
            if ( path.getPathComponent( i )
                 instanceof TapServiceFinder.Service ) {
                Object[] spath = new Object[ i + 1 ];
                System.arraycopy( path.getPath(), 0, spath, 0, i + 1 );
                return new TreePath( spath );
            }
        }
        return null;
    }

    /**
     * Returns a service in the ancestry of a supplied path.
     * The supplied path counts as its own ancestor for these purposes.
     *
     * @param  path   path to examine
     * @return   service owning the path,
     *           or null if no service appears in the ancestry
     */
    public static TapServiceFinder.Service getService( TreePath path ) {
        if ( path != null ) {
            TreePath servicePath = getServicePath( path );
            if ( servicePath != null ) {
                return (TapServiceFinder.Service)
                       servicePath.getLastPathComponent();
            }
        }
        return null;
    }

    /**
     * Creates a tree model from a list of tables to be displayed,
     * along with a list of services containing at least the owners
     * of the supplied tables.
     *
     * <p>Note that only those services containing at least one of the
     * tables in the supplied list will be displayed.
     * Services without associated tables and tables without associated
     * services will be ignored for the purposes of this model.
     * Tables know which service they belong to.
     *
     * @param   allServices  open-ended list TAP services
     *                       (should contain all parents of given tables,
     *                       but may contain others as well)
     * @param   tables    list of TAP tables for display by this model
     * @return  new tree model
     */
    public static TapServiceTreeModel createModel( Service[] allServices,
                                                   Table[] tables ) {
        Map<String,Service> serviceMap = new LinkedHashMap<String,Service>();
        for ( Service serv : allServices ) {
            serviceMap.put( serv.getId(), serv );
        }
        Map<String,List<Table>> tMap = new LinkedHashMap<String,List<Table>>();
        for ( Table table : tables ) {
            String ivoid = table.getServiceId();
            if ( ! tMap.containsKey( ivoid ) ) {
                tMap.put( ivoid, new ArrayList<Table>() );
            }
            tMap.get( ivoid ).add( table );
        }
        Map<Service,Table[]> tableMap = new LinkedHashMap<Service,Table[]>();
        for ( Map.Entry<String,List<Table>> entry : tMap.entrySet() ) {
            Service service = serviceMap.get( entry.getKey() );
            if ( service != null ) {
                Table[] ts = entry.getValue().toArray( new Table[ 0 ] );
                tableMap.put( service, ts );
            }
        }
        Service[] displayServices =
            tableMap.keySet().toArray( new Service[ 0 ] );
        Arrays.sort( displayServices, byTableCount( tableMap ) );
        String label = "Selected TAP services (" + displayServices.length
                     + "/" + allServices.length + ")";
        return new TapServiceTreeModel( label, displayServices, tableMap );
    }

    /**
     * Creates a tree model from a list of services to be displayed.
     * The services are sorted before presentation.
     *
     * @param  services to form nodes of tree
     * @return  new tree model
     */
    public static TapServiceTreeModel createModel( Service[] services ) {
        Service[] displayServices = services.clone();
        Arrays.sort( displayServices, byTableCount( null ) );
        String label = "All TAP services (" + services.length + ")";
        return new TapServiceTreeModel( label, displayServices, null );
    }

    /**
     * Returns a lightweight facade for an object representing a node
     * in this tree.  The result is an adapter supplying basic tree-like
     * behaviour.  The expectation is that this method will be called
     * frequently as required (perhaps many times for the same object)
     * rather than cached, so this method should be cheap.
     *
     * @param   node  raw data object
     * @return  adapter object representing node data
     */
    private Node asNode( final Object item ) {
        if ( item instanceof Service[] ) {
            return new Node( (Service[]) item ) {
                public String toString() {
                    return rootLabel_;
                }
            };
        }
        else if ( item instanceof Service ) {
            final Service service = (Service) item;
            return new Node( tableMap_ == null ? null
                                               : tableMap_.get( service ) ) {
                public String toString() {
                    String nameTxt = null;
                    if ( nameTxt == null || nameTxt.trim().length() == 0 ) {
                        nameTxt = service.getName();
                    }
                    if ( nameTxt == null || nameTxt.trim().length() == 0 ) {
                        nameTxt = service.getTitle();
                    }
                    if ( nameTxt == null || nameTxt.trim().length() == 0 ) {
                        nameTxt = service.getId();
                    }
                    int ntTotal = service.getTableCount();
                    int ntPresent = children_ == null ? -1 : children_.length;
                    StringBuffer cbuf = new StringBuffer()
                        .append( " (" );
                    if ( ntPresent >= 0 ) {
                         cbuf.append( ntPresent )
                             .append( "/" );
                    }
                    cbuf.append( ntTotal <= 0 ? "?"
                                              : Integer.toString( ntTotal ) )
                        .append( ")" );
                    String countTxt = cbuf.toString();
                    return nameTxt + countTxt;
                }
            };
        }
        else if ( item instanceof Table ) {
            final Table table = (Table) item;
            return new Node( null ) {
                public String toString() {
                    String descrip = table.getDescription();
                    String txt = table.getName();
                    if ( descrip != null ) {
                        txt += " - " + descrip.replaceAll( "\\s+", " " );
                    }
                    return txt;
                }
            };
        }
        else {
            assert false;
            return new Node( null ) {
                public String toString() {
                    return item.toString();
                }
            };
        }
    }

    /**
     * Returns a comparator to sort services by the number of tables.
     * It sorts first by the number of children it has in this tree,
     * and as a tie-breaker by the total number of tables in the service.
     *
     * @return  service comparator
     */
    private static Comparator<Service>
            byTableCount( final Map<Service,Table[]> tableMap ) {
        return new Comparator<Service>() {
            public int compare( Service s1, Service s2 ) {
                if ( tableMap != null ) {
                    int dc = tableMap.get( s2 ).length
                           - tableMap.get( s1 ).length;
                    if ( dc != 0 ) {
                        return dc;
                    }
                }
                int dt = s2.getTableCount() - s1.getTableCount();
                if ( dt != 0 ) {
                    return dt;
                }
                int da = s2.hashCode() - s1.hashCode();
                return da;
            }
        };
    }

    /**
     * Returns a cell renderer suitable for rendering nodes of a JTree
     * using a model of this class.
     *
     * @return  tree cell renderer
     */
    public static TreeCellRenderer createCellRenderer() {
        return new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent( JTree tree,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean isExpanded,
                                                           boolean isLeaf,
                                                           int irow,
                                                           boolean hasFocus ) {
                TreeModel model = tree.getModel();

                /* Prepare text for labelling the node. */
                String text =
                      model instanceof TapServiceTreeModel
                    ? ((TapServiceTreeModel) model).asNode( value ).toString()
                    : value.toString();

                /* Adjust presentation for nodes that are present in the
                 * selection model, but which don't represent services
                 * (these are probably tables).
                 * They should look less prominent, since 'selecting' them
                 * doesn't have any effect on the rest of the GUI.
                 * Currently, make them look focussed but unselected - this
                 * is just a hack to give them some L&F-friendly appearance 
                 * that's (hopefully) visible but not the same as normal
                 * selection.  Could probably be done better, but hard to
                 * get something that's guaranteed to match the L&F. */
                if ( isSelected && ! ( value instanceof Service ) ) {
                    hasFocus = true;
                    isSelected = false;
                }

                /* Configure the renderer (this object) and return. */
                Component comp =
                    super.getTreeCellRendererComponent( tree, value, isSelected,
                                                        isExpanded, isLeaf,
                                                        irow, hasFocus );
                if ( comp instanceof JLabel ) {
                    ((JLabel) comp).setText( text );
                }
                return comp;
            }
        };
    }

    /**
     * Defines the basic behaviour required from a node in this tree.
     */
    private static abstract class Node {
        final Object[] children_;

        /**
         * Constructor.
         *
         * @return   child nodes, null for tree leaves
         */
        Node( Object[] children ) {
            children_ = children;
        }

        /**
         * Indicates whether this node is a leaf.
         *
         * @return  true for leaf, false for branch
         */
        boolean isLeaf() {
            return children_ == null;
        }

        /**
         * This method is used to provide the text of the node as rendered
         * by this class's custom TreeCellRenderer.
         */
        @Override
        public abstract String toString();
    }
}