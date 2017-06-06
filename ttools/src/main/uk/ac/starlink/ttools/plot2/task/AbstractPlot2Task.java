package uk.ac.starlink.ttools.plot2.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowSequence;
import uk.ac.starlink.table.WrapperStarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.OutputStreamParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.func.Strings;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Captioner;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decoration;
import uk.ac.starlink.ttools.plot2.Gang;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.LegendEntry;
import uk.ac.starlink.ttools.plot2.LegendIcon;
import uk.ac.starlink.ttools.plot2.Navigator;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotPlacement;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxis;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.SingleGanger;
import uk.ac.starlink.ttools.plot2.SubCloud;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.ZoneContent;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.KeySet;
import uk.ac.starlink.ttools.plot2.config.LoggingConfigMap;
import uk.ac.starlink.ttools.plot2.config.RampKeySet;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperType;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plottask.PaintMode;
import uk.ac.starlink.ttools.plottask.PaintModeParameter;
import uk.ac.starlink.ttools.plottask.Painter;
import uk.ac.starlink.ttools.plottask.SwingPainter;
import uk.ac.starlink.ttools.task.AddEnvironment;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.ttools.task.DoubleArrayParameter;
import uk.ac.starlink.ttools.task.DynamicTask;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.StringMultiParameter;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * Abstract superclass for tasks performing plot2 plots using STILTS.
 * Concrete subclasses must supply the PlotType (perhaps from the
 * environment), and may customise the visible task parameter set.
 *
 * @author   Mark Taylor
 * @since    22 Aug 2014
 */
public abstract class AbstractPlot2Task implements Task, DynamicTask {

    private final boolean allowAnimate_;
    private final GangerFactory gangerFact_;
    private final IntegerParameter xpixParam_;
    private final IntegerParameter ypixParam_;
    private final PaddingParameter paddingParam_;
    private final PaintModeParameter painterParam_;
    private final DataStoreParameter dstoreParam_;
    private final StringMultiParameter seqParam_;
    private final BooleanParameter legendParam_;
    private final BooleanParameter legborderParam_;
    private final BooleanParameter legopaqueParam_;
    private final StringMultiParameter legseqParam_;
    private final BooleanParameter bitmapParam_;
    private final Parameter<Compositor> compositorParam_;
    private final InputTableParameter animateParam_;
    private final FilterParameter animateFilterParam_;
    private final IntegerParameter parallelParam_;
    private final Parameter[] basicParams_;

    public static final String LAYER_PREFIX = "layer";
    public static final String ZONE_PREFIX = "zone";
    private static final String TABLE_PREFIX = "in";
    private static final String FILTER_PREFIX = "icmd";
    public static final String EXAMPLE_LAYER_SUFFIX = "N";
    public static final String EXAMPLE_ZONE_SUFFIX = "Z";
    public static final String DOC_ZONE_SUFFIX = "";
    private static final GraphicExporter[] EXPORTERS =
        GraphicExporter.getKnownExporters( PlotUtil.LATEX_PDF_EXPORTER );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.plot2.task" );

    /**
     * Constructor with explicit animation capability.
     *
     * @param  allowAnimate  true iff animation options should be provided
     * @param  gangerFact    controls how plots can be grouped
     */
    protected AbstractPlot2Task( boolean allowAnimate,
                                 GangerFactory gangerFact ) {
        allowAnimate_ = allowAnimate;
        gangerFact_ = gangerFact;
        List<Parameter> plist = new ArrayList<Parameter>();

        paddingParam_ = new PaddingParameter( "insets" );

        xpixParam_ = new IntegerParameter( "xpix" );
        xpixParam_.setPrompt( "Total horizontal size in pixels" );
        xpixParam_.setDescription( new String[] {
            "<p>Size of the output image in the X direction in pixels.",
            "This includes space for any axis labels, padding",
            "and other decoration outside the plot area itself.",
            "See also <code>" + paddingParam_.getName() + "</code>.",
            "</p>",
        } );
        xpixParam_.setIntDefault( 500 );
        xpixParam_.setMinimum( 1 );
        plist.add( xpixParam_ );

        ypixParam_ = new IntegerParameter( "ypix" );
        ypixParam_.setPrompt( "Total vertical size in pixels" );
        ypixParam_.setDescription( new String[] {
            "<p>Size of the output image in the Y direction in pixels.",
            "This includes space for any axis labels, padding",
            "and other decoration outside the plot area itself.",
            "See also <code>" + paddingParam_.getName() + "</code>.",
            "</p>",
        } );
        ypixParam_.setIntDefault( 400 );
        ypixParam_.setMinimum( 1 );
        plist.add( ypixParam_ );

        paddingParam_.setPrompt( "Space outside plotting area" );
        paddingParam_.setDescription( new String[] {
            "<p>Defines the amount of space in pixels around the",
            "actual plotting area.",
            "This space is used for axis labels, and other decorations",
            "and any left over forms an empty border.",
            "</p>",
            "<p>The size and position of the actual plotting area",
            "is determined by this parameter along with", 
            "<code>" + xpixParam_ + "</code> and",
            "<code>" + ypixParam_ + "</code>.",
            "</p>",
            "<p>The value of this parameter is 4 comma separated integers:",
            "<code>&lt;top&gt;,&lt;left&gt;,&lt;bottom&gt;,&lt;right&gt;"
                + "</code>.",
            "Any or all of these values may be left blank,",
            "in which case the corresponding margin will be calculated",
            "automatically according to how much space is required.",
            "</p>",
        } );
        plist.add( paddingParam_ );

        painterParam_ = createPaintModeParameter();
        plist.add( painterParam_ );

        dstoreParam_ = new DataStoreParameter( "storage" );
        dstoreParam_.setDescription( new String[] {
           dstoreParam_.getDescription(),
           "<p>The default value is",
           "<code>" + dstoreParam_.getName( dstoreParam_
                                           .getDefaultForCaching( true ) )
                    + "</code>",
           "if a live plot is being generated",
           "(<code>" + painterParam_.getName() + "="
                     + painterParam_.getName( PaintMode.SWING_MODE )
                     + "</code>),",
           "since in that case the plot needs to be redrawn every time",
           "the user performs plot navigation actions or resizes the window,",
           "or if animations are being produced.",
           "Otherwise (e.g. output to a graphics file) the default is",
           "<code>" + dstoreParam_.getName( dstoreParam_
                                           .getDefaultForCaching( false ) )
                    + "</code>.",
           "</p>",
        } );
        plist.add( dstoreParam_ );

        seqParam_ = new StringMultiParameter( "seq", ',' );
        seqParam_.setUsage( "<suffix>[,...]" );
        seqParam_.setPrompt( "Order in which to plot layers" );
        String osfix = "&lt;" + EXAMPLE_LAYER_SUFFIX + "&gt;";
        seqParam_.setDescription( new String[] {
            "<p>Contains a comma-separated list of layer suffixes",
            "to determine the order in which layers are drawn on the plot.",
            "This can affect which symbol are plotted on top of,",
            "and so potentially obscure, which other ones.",
            "</p>",
            "<p>When specifying a plot, multiple layers may be specified,",
            "each introduced by a parameter",
            "<code>" + LAYER_PREFIX + osfix + "</code>,",
            "where <code>" + osfix + "</code> is a different (arbitrary)",
            "suffix labelling the layer,",
            "and is appended to all the parameters",
            "specific to defining that layer.",
            "</p>",
            "<p>By default the layers are drawn on the plot in the order",
            "in which the <code>" + LAYER_PREFIX + "*</code> parameters",
            "appear on the command line.",
            "However if this parameter is specified, each comma-separated",
            "element is interpreted as a layer suffix,",
            "giving the ordered list of layers to plot.",
            "Every element of the list must be a suffix with a corresponding",
            "<code>" + LAYER_PREFIX + "</code> parameter,",
            "but missing or repeated elements are allowed.",
            "</p>",
        } );
        seqParam_.setNullPermitted( true );
        plist.add( seqParam_ );

        legendParam_ = new BooleanParameter( "legend" );
        legendParam_.setPrompt( "Show legend?" );
        legendParam_.setDescription( new String[] {
            "<p>Whether to draw a legend or not.",
            "If no value is supplied, the decision is made automatically:",
            "a legend is drawn only if it would have more than one entry.",
            "</p>",
        } );
        legendParam_.setNullPermitted( true );
        plist.add( legendParam_ );

        legborderParam_ = new BooleanParameter( "legborder" );
        legborderParam_.setPrompt( "Border around legend?" );
        legborderParam_.setDescription( new String[] {
            "<p>If true, a line border is drawn around the legend.",
            "</p>",
        } );
        legborderParam_.setBooleanDefault( true );
        plist.add( legborderParam_ );

        legopaqueParam_ = new BooleanParameter( "legopaque" );
        legopaqueParam_.setPrompt( "Legend background opaque?" );
        legopaqueParam_.setDescription( new String[] {
            "<p>If true, the background of the legend is opaque,",
            "and the legend obscures any plot components behind it.",
            "Otherwise, it's transparent.",
            "</p>",
        } );
        legopaqueParam_.setBooleanDefault( true );
        plist.add( legopaqueParam_ );

        legseqParam_ = new StringMultiParameter( "legseq", ',' );
        legseqParam_.setUsage( "<suffix>[,...]" );
        legseqParam_.setPrompt( "Order in which to add layers to legend" );
        legseqParam_.setDescription( new String[] {
            "<p>Determines which layers are represented in the legend",
            "(if present) and in which order they appear.", 
            "The legend has a line for each layer label",
            "(as determined by the",
            "<code>" + createLabelParameter( EXAMPLE_LAYER_SUFFIX ) + "</code>",
            "parameter).",
            "If multiple layers have the same label,",
            "they will contribute to the same entry in the legend,",
            "with style icons plotted over each other.",
            "The value of this parameter is a sequence of layer suffixes,",
            "which determines the order in which the legend entries appear.",
            "Layers with suffixes missing from this list",
            "do not show up in the legend at all.",
            "</p>",
            "<p>If no value is supplied (the default),",
            "the sequence is the same as the layer plotting sequence",
            "(see <code>" + seqParam_.getName() + "</code>).",
            "</p>",
        } );
        legseqParam_.setNullPermitted( true );
        plist.add( legseqParam_ );

        plist.add( createLegendPositionParameter( DOC_ZONE_SUFFIX ) );
        plist.add( createTitleParameter( DOC_ZONE_SUFFIX ) );

        plist.addAll( getZoneKeyParams( StyleKeys.AUX_RAMP .getKeys() ) );
        plist.addAll( getZoneKeyParams( new ConfigKey[] {
            StyleKeys.SHADE_LOW,
            StyleKeys.SHADE_HIGH,
        } ) );

        plist.add( createAuxLabelParameter( DOC_ZONE_SUFFIX ) );
        plist.add( createAuxCrowdParameter( DOC_ZONE_SUFFIX ) );
        plist.add( createAuxWidthParameter( DOC_ZONE_SUFFIX ) );
        plist.add( createAuxVisibleParameter( DOC_ZONE_SUFFIX ) );

        bitmapParam_ = new BooleanParameter( "forcebitmap" );
        bitmapParam_.setPrompt( "Force non-vector graphics output?" );
        bitmapParam_.setDescription( new String[] {
            "<p>Affects whether rendering of the data contents of a plot",
            "(though not axis labels etc) is always done to",
            "an intermediate bitmap rather than, where possible,",
            "being painted using graphics primitives.",
            "This is a rather arcane setting that may nevertheless",
            "have noticeable effects on the appearance and",
            "size of an output graphics file, as well as plotting time.",
            "For some types of plot",
            "(e.g. <code>" + ShapeFamilyLayerType.SHADING_PREFIX
                           + EXAMPLE_LAYER_SUFFIX + "="
                           + ShapeMode.AUTO.getModeName() + "</code>",
            "or    <code>" + ShapeFamilyLayerType.SHADING_PREFIX
                           + EXAMPLE_LAYER_SUFFIX + "="
                           + ShapeMode.DENSITY.getModeName() + "</code>)",
            "it will have no effect, since this kind of rendering",
            "happens in any case.",
            "</p>",
            "<p>When writing to vector graphics formats (PDF and PostScript),",
            "setting it true will force the data contents to be bitmapped.",
            "This may make the output less beautiful",
            "(round markers will no longer be perfectly round),",
            "but it may result in a much smaller file",
            "if there are very many data points.",
            "</p>",
            "<p>When writing to bitmapped output formats",
            "(PNG, GIF, JPEG, ...),",
            "it fixes shapes to be the same as seen on the screen",
            "rather than be rendered at the mercy of the graphics system,",
            "which sometimes introduces small distortions.",
            "</p>",
        } );
        bitmapParam_.setBooleanDefault( false );
        plist.add( bitmapParam_ );

        compositorParam_ = new CompositorParameter( "compositor" );
        plist.add( compositorParam_ );

        if ( allowAnimate ) {
            animateParam_ = new InputTableParameter( "animate" );
            animateParam_.setNullPermitted( true );
            animateParam_.setTableDescription( "the animation control table" );
            animateParam_.setDescription( new String[] {
                "<p>If not null, this parameter causes the command",
                "to create a sequence of plots instead of just one.",
                "The parameter value is a table with one row for each",
                "frame to be produced.",
                "Columns in the table are interpreted as parameters",
                "which may take different values for each frame;",
                "the column name is the parameter name,",
                "and the value for a given frame is its value from that row.",
                "Animating like this is considerably more efficient",
                "than invoking the STILTS command in a loop.",
                "</p>",
                animateParam_.getDescription(),
            } );
            plist.add( animateParam_ );
            plist.add( animateParam_.getFormatParameter() );
            plist.add( animateParam_.getStreamParameter() );
            animateFilterParam_ = new FilterParameter( "acmd" );
            animateFilterParam_
               .setTableDescription( "the animation control table",
                                     animateParam_, Boolean.TRUE );
            plist.add( animateFilterParam_ );
            parallelParam_ = new IntegerParameter( "parallel" );
            parallelParam_.setPrompt( "Parallelism for animation frames" );
            parallelParam_.setDescription( new String[] {
                "<p>Determines how many threads will run in parallel",
                "if animation output is being produced.",
                "Only used if the <code>" + animateParam_.getName() + "</code>",
                "parameter is supplied.",
                "The default value is the number of processors apparently",
                "available to the JVM.",
                "</p>",
            } );
            parallelParam_.setMinimum( 1 );
            parallelParam_.setIntDefault( Runtime.getRuntime()
                                         .availableProcessors() );
            plist.add( parallelParam_ );
        }
        else {
            animateParam_ = null;
            animateFilterParam_ = null;
            parallelParam_ = null;
        }
        basicParams_ = plist.toArray( new Parameter[ 0 ] );
    }

    /**
     * Constructor with default animation capability.
     *
     * @param  gangerFact    controls how plots can be grouped
     */
    protected AbstractPlot2Task( GangerFactory gangerFact ) {
        this( true, gangerFact );
    }

    /**
     * Concrete subclasses must implement this method to provide
     * the PlotType and other information from the environment
     * that may not be available at construction time.
     *
     * @param  env  execution environment
     * @return  context
     */
    public abstract PlotContext getPlotContext( Environment env )
            throws TaskException;

    /**
     * May provide a default value for a given config parameter that is
     * sensitive to the content of the execution environment.
     * This is here to provide a hook for subclasses to set up defaults
     * for some config parameters on the basis of what layers are present.
     *
     * @param   env  execution environment
     * @param   key  config key for which a parameter is required
     * @param   suffixes  ordered list of the plot layer suffixes
     *          in use for the plot being performed
     * @return  default for parameter getting value for <code>key</code>,
     *          or null if none is obvious
     */
    protected abstract <T> String getConfigParamDefault( Environment env,
                                                         ConfigKey<T> key,
                                                         String[] suffixes )
            throws TaskException;

    /**
     * Returns the list of parameters supplied by the AbstractPlot2Task
     * implementation.  Subclasses should include these alongside any
     * they want to add for presentation to the user.
     *
     * @return  basic parameter list
     */
    public final Parameter[] getBasicParameters() {
        return basicParams_;
    }

    public Executable createExecutable( final Environment env )
            throws TaskException {
        final PlotContext context = getPlotContext( env );
        final Painter painter = painterParam_.painterValue( env );
        final boolean isSwing = painter instanceof SwingPainter;
        final TableProducer animateProducer =
              allowAnimate_
            ? ConsumerTask.createProducer( env, animateFilterParam_,
                                           animateParam_ )
            : null;
        boolean isAnimate = animateProducer != null;
        dstoreParam_.setDefaultCaching( isSwing || isAnimate );

        /* Single frame: prepare operation and return an executable that
         * has no reference to the environment. */
        if ( ! isAnimate ) {
            final PlotExecutor executor = createPlotExecutor( env, context );
            return new Executable() {
                public void execute() throws IOException {
                    DataStore dataStore;
                    try {
                        dataStore = executor.createDataStore( null );
                    }
                    catch ( InterruptedException e ) {
                        Thread.currentThread().isInterrupted();
                        return;
                    }

                    /* For an active display, create and post a component
                     * which will draw the requested plot on demand,
                     * including resizing when appropriate. */
                    if ( isSwing ) {
                        PlotCaching caching = PlotCaching.createFullyCached();
                        final JComponent panel =
                            executor.createPlotComponent( dataStore,
                                                          true, caching );
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                ((SwingPainter) painter).postComponent( panel );
                            }
                        } );
                    }

                    /* For a static plot, generate and plot
                     * the fixed icon here. */
                    else {
                        Icon plot = executor.createPlotIcon( dataStore );
                        long start = System.currentTimeMillis();
                        painter.paintPicture( PlotUtil.toPicture( plot ) );
                        PlotUtil.logTime( logger_, "Plot", start );
                    }
                }
            };
        }

        /* Animation.  This works by reading a row from a supplied table
         * for each output frame.  Each column of the table represents
         * (and is named as) one of the parameters of this task that
         * can change between frames.  This means the task operates
         * in a rather non-standard way: most of the execution
         * environment is kept, modified, and interrogated during the task
         * execution (Executable.execute() method) rather than being
         * interrogated and thrown away after the Executable is created. */
        else {

            /* First, read the animation table for later use. */
            final StarTable animateTable;
            final ColumnInfo[] infos;
            final long nrow;
            Object[] row0;
            Environment env0;
            try {
                Row0Table atable = new Row0Table( animateProducer.getTable() );
                animateTable = atable;
                infos = Tables.getColumnInfos( animateTable );
                nrow = animateTable.getRowCount();

                /* We also read the first row, for preparing a dummy frame. */
                row0 = atable.getRow0();
                env0 = createFrameEnvironment( env, infos, row0, 0, nrow );
            }
            catch ( IOException e ) {
                throw new ExecutionException( "Error reading animation table: "
                                            + e, e );
            }

            /* This line prepares to paint a dummy frame, but doesn't do it
             * (the created executor is just discarded).
             * The purpose of this is to read the variables from the execution
             * environment whose values are required to specify a frame of
             * the animation.  In this way, any parameter errors can be
             * identified now and passed back to the user, rather than
             * showing up during the actual execution.  For related reasons,
             * if we didn't do this, the parameter system would complain
             * that there are unused parameters in the environment. */
            createPlotExecutor( env0, context );

            /* Screen animation. */
            if ( isSwing ) {
                return new Executable() {
                    public void execute() throws IOException, TaskException {
                        try {
                            animateSwing( env, context, animateTable );
                        }
                        catch ( InterruptedException e ) {
                            Thread.currentThread().isInterrupted();
                            return;
                        }
                    }
                };
            }

            /* File output animation. */
            else {
                final String out0 = getPainterOutputName( env0 );
                final int parallel = parallelParam_.intValue( env );
                return new Executable() {
                    public void execute() throws IOException, TaskException {
                        try {
                            animateOutput( env, context, animateTable,
                                           parallel, out0 );
                        }
                        catch ( InterruptedException e ) {
                            Thread.currentThread().isInterrupted();
                            return;
                        }
                    }
                };
            }
        }
    }

    /**
     * Paints a sequence of animation frames under control of a parameter
     * table, outputting the result to a sequence of files.
     *
     * @param  baseEnv  base execution environment
     * @param  context  plot context
     * @param  animateTable  table providing per-frame adjustments
     *                       to environment
     * @param  parallel  thread count for calculations
     * @param  out0  name of first output frame
     */
    private void animateOutput( Environment baseEnv, PlotContext context,
                                StarTable animateTable, int parallel,
                                String out0 )
            throws TaskException, IOException, InterruptedException {
        ColumnInfo[] infos = Tables.getColumnInfos( animateTable );
        long nrow = animateTable.getRowCount();
        int nthr = parallel;
        ExecutorService paintService =
            new ThreadPoolExecutor( nthr, nthr, 60, TimeUnit.SECONDS,
                                    new ArrayBlockingQueue<Runnable>( nthr ),
                                    new ThreadPoolExecutor.CallerRunsPolicy() );
        RowSequence aseq = animateTable.getRowSequence();
        DataStore lastDataStore = null;
        String lastOutName = null;
        try {
            for ( long irow = 0; aseq.next(); irow++ ) {
                Environment frameEnv =
                    createFrameEnvironment( baseEnv, infos, aseq.getRow(),
                                            irow, nrow );
                final PlotExecutor executor =
                    createPlotExecutor( frameEnv, context );
                final Painter painter = getPainter( frameEnv );
                final DataStore dstore =
                    executor.createDataStore( lastDataStore );
                final String outName = getPainterOutputName( frameEnv );
                paintService.submit( new Callable<Void>() {
                    public Void call() throws IOException {
                        long start = System.currentTimeMillis();
                        Icon plot = executor.createPlotIcon( dstore );
                        painter.paintPicture( PlotUtil.toPicture( plot ) );
                        PlotUtil.logTime( logger_, "Plot " + outName, start );
                        return null;
                    }
                } );
                lastOutName = outName;
                lastDataStore = dstore;
            }
        }
        finally {
            aseq.close();
        }
        paintService.shutdown();
        paintService.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
        logger_.warning( "Wrote " + nrow + " frames, "
                       + out0 + " .. " + lastOutName );
    }

    /**
     * Paints a sequence of animation frames under control of a parameter
     * table, displaying the results in a screen component.
     *
     * @param  baseEnv  base execution environment
     * @param  context  plot context
     * @param  animateTable  table providing per-frame adjustments
     *                       to environment
     */
    private void animateSwing( Environment baseEnv, PlotContext context,
                               StarTable animateTable )
            throws TaskException, IOException, InterruptedException {
        final SwingPainter painter =
            (SwingPainter) createPaintModeParameter().painterValue( baseEnv );
        ColumnInfo[] infos = Tables.getColumnInfos( animateTable );
        long nrow = animateTable.getRowCount(); 
        RowSequence aseq = animateTable.getRowSequence();
        final JComponent holder = new JPanel( new BorderLayout() );
        PlotCaching caching = PlotCaching.createFullyCached();
        DataStore dataStore = null;

        /* The swing animation is not parallelised, but should be.
         * It's like this.  The work is not (mostly) done by creating the
         * PlotDisplay component plot component, it's done when that
         * component paints itself, i.e. on the event dispatch thread,
         * so I can't easily do parallel plotting and feed the results
         * to the EDT to display.
         * To parallelise it, I should cause that painting work to be
         * done before the component is posted, and cached within the
         * PlotDisplay object ready for fast plotting when it becomes visible.
         * PlotDisplay is more or less set up to do this, but it would
         * need to know its dimensions to know how to do the plot,
         * which requires a bit of additional plumbing. */
        try {
            for ( long irow = 0; aseq.next(); irow++ ) {
                Environment frameEnv =
                    createFrameEnvironment( baseEnv, infos, aseq.getRow(),
                                            irow, nrow );
                PlotExecutor executor = createPlotExecutor( frameEnv, context );
                dataStore = executor.createDataStore( dataStore );
                final JComponent panel =
                    executor.createPlotComponent( dataStore, true, caching );
                final boolean init = irow == 0;

                /* It's necessary to use invokeAndWait here, since the
                 * display painting is slow.  If invokeLater is used,
                 * most frames are never seen.  If I fix it so that
                 * most of the work is done outside the EDT,
                 * I can change this back to invokeLater. */
                try {
                    SwingUtilities.invokeAndWait( new Runnable() {
                        public void run() {
                            holder.removeAll();
                            holder.add( panel, BorderLayout.CENTER );
                            holder.revalidate();
                            holder.repaint();
                            if ( init ) {
                                painter.postComponent( holder );
                            }                   
                        }
                    } );
                }
                catch( InvocationTargetException e ) {
                    throw new TaskException( "Painting error: " + e, e );
                }
            }
        }
        finally {
            aseq.close();
        }
    }

    /**
     * Returns an execution environment based on a given static environment,
     * but augmented by values from a row of an animation table.
     *
     * @param  baseEnv  base environment
     * @param  colInfos  columns of animation table named as task parameters
     * @param  row    single row of animation table to augment environment
     * @param  irow   row index
     * @param  nrow   total row count (-1 if not known)
     * @return  environment for single animation frame
     */
    private Environment createFrameEnvironment( Environment baseEnv,
                                                ColumnInfo[] colInfos,
                                                Object[] row, long irow,
                                                long nrow )
            throws IOException, TaskException {
        OutputStreamParameter outParam = painterParam_.getOutputParameter();
        boolean hasPaintout = false;

        /* Prepare a map containing the values in the animation table row,
         * keyed by column name (=parameter name). */
        Map<String,String> map = new HashMap<String,String>();
        int ncol = colInfos.length;
        for ( int ic = 0; ic < ncol; ic++ ) {
            String pname = colInfos[ ic ].getName();
            hasPaintout = hasPaintout || pname.equals( outParam.getName() );
            Object cell = row[ ic ];
            if ( cell != null ) {
                map.put( pname, cell.toString() );
            }
        }

        /* Mangle the output filename if present and if explicit values are
         * not present in the animation table; append a frame number. */
        if ( ! hasPaintout &&
             ! ( painterParam_.painterValue( baseEnv )
                 instanceof SwingPainter ) ) {
            String baseOut = outParam.stringValue( baseEnv );
            int numpos = baseOut.lastIndexOf( '.' );
            if ( numpos < 0 ) {
                numpos = baseOut.length() - 1;
            }
            StringBuffer frameOut = new StringBuffer( baseOut );
            int ndigit = nrow > 0 ? (int) Math.ceil( Math.log10( nrow + 1 ) )
                                  : 3;
            String snum = "-" + Strings.padWithZeros( irow + 1, ndigit );
            frameOut.insert( numpos, snum );
            map.put( outParam.getName(), frameOut.toString() );
        }

        /* Return an environment with these additions. */
        return AddEnvironment.createAddEnvironment( baseEnv, map );
    }

    /**
     * Returns an Icon that paints the plot described
     * by a value-bearing execution environment.
     * This utility method is not used for executing this class.
     *
     * @param  env  execution environment
     * @return  plot icon
     */
    public Icon createPlotIcon( Environment env )
            throws TaskException, IOException, InterruptedException {
        dstoreParam_.setDefaultCaching( false );
        PlotExecutor executor =
            createPlotExecutor( env, getPlotContext( env ) );
        return executor.createPlotIcon( executor.createDataStore( null ) );
    }

    /**
     * Prepares a plot for this task as specified by a given environment,
     * but does not run any of the actual plotting code.
     * If this utility method completes without error, there is a good chance
     * that the specified plot will also run without error.
     *
     * @param  env  populated environment
     * @throws   TaskException   in case of error
     */
    public void testEnv( Environment env ) throws TaskException {
        createPlotExecutor( env, getPlotContext( env ) );
    }

    /**
     * Returns a graphical component that displays an interactive view of
     * the plot described by a value-bearing execution environment.
     * This utility method is not used for executing the task defined by
     * this class.
     *
     * @param  env  execution environment
     * @param  caching  whether data and plot should be cached or re-read
     *                  at every repaint
     * @return  active plot view component
     */
    public PlotDisplay createPlotComponent( Environment env, boolean caching )
            throws TaskException, IOException, InterruptedException {
        dstoreParam_.setDefaultCaching( caching );
        PlotExecutor executor =
            createPlotExecutor( env, getPlotContext( env ) );
        PlotCaching plotCaching = caching ? PlotCaching.createFullyCached()
                                          : PlotCaching.createUncached();
        return executor.createPlotComponent( executor.createDataStore( null ),
                                             true, plotCaching );
    }

    public Parameter[] getContextParameters( Environment env )
            throws TaskException {

        /* Initialise list with non-context-sensitive parameters. */
        List<Parameter> paramList = new ArrayList<Parameter>();
        paramList.addAll( Arrays.asList( getParameters() ) );

        /* Go through each layer that has been set in the environment
         * (by a layerN setting).  Get all the parameters associated
         * with that layer type and suffix. */
        PlotContext context = getPlotContext( env );
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType layer = entry.getValue();

            /* Add an entry for the layer parameter itself, with a fixed
             * value. */
            LayerTypeParameter layerParam =
                createLayerTypeParameter( suffix, context );
            layerParam.setUsage( layerParam.stringifyOption( layer ) );
            paramList.add( layerParam );

            /* Add entries for the parameters associated with that
             * layer type. */
            for ( ParameterFinder finder :
                  getLayerParameterFinders( env, context, layer, suffix ) ) {
                paramList.add( finder.createParameter( suffix ) );
            }
        }
        return paramList.toArray( new Parameter[ 0 ] );
    }

    public Parameter getParameterByName( Environment env, String paramName )
            throws TaskException {

        /* Check if the parameter is a layer parameter itself. */
        if ( paramName.toLowerCase()
                      .startsWith( LAYER_PREFIX.toLowerCase() ) ) {
            String suffix = paramName.substring( LAYER_PREFIX.length() );
            PlotContext context = getPlotContext( env );
            return createLayerTypeParameter( suffix, context );
        }

        /* Otherwise, find each layer that has been set in the environment
         * (by a layerN setting).  Find its layer type and suffix.
         * Then it's a case of going through all the parameters that
         * come with that layer type to see if any of them match the
         * requested on by name. */
        PlotContext context = getPlotContext( env );
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType layer = entry.getValue();
            for ( ParameterFinder finder :
                  getLayerParameterFinders( env, context, layer, suffix ) ) {
                Parameter p = finder.findParameterByName( paramName, suffix );
                if ( p != null ) {
                    return p;
                }
            }
        }

        /* No luck. */
        return null;
    }

    /**
     * Returns a list of parameter finders for parameters specific to
     * a given layer.  These can be used to find all the parameters
     * which are only present in virtue of the existence of a given
     * plot layer.
     *
     * @param   env  execution environment
     * @param   context  plot context
     * @param   layer   plot layer for which parameters are required
     * @param   suffix  suffix associated with layer
     * @return  array of plot finder for layer-specific parameters
     */
    private ParameterFinder[]
            getLayerParameterFinders( Environment env,
                                      final PlotContext context,
                                      final LayerType layer,
                                      final String suffix )
            throws TaskException {
        List<ParameterFinder> finderList = new ArrayList<ParameterFinder>();

        /* Layer type associated parameters. */
        int nassoc = layer.getAssociatedParameters( "dummy" ).length;
        for ( int ia = 0; ia < nassoc; ia++ ) {
            final int iassoc = ia;
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return layer.getAssociatedParameters( sfix )[ iassoc ];
                }
            } );
        }

        /* Layer positional parameters. */
        int npos = layer.getPositionCount();
        DataGeom geom = context.getGeom( env, suffix );
        Coord[] posCoords = geom.getPosCoords();
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            final String posSuffix = npos > 1
                                   ? PlotUtil.getIndexSuffix( ipos )   
                                   : "";
            for ( Coord coord : posCoords ) {
                for ( final Input input : coord.getInputs() ) {
                    finderList.add( new ParameterFinder<Parameter>() {
                        public Parameter createParameter( String sfix ) {
                            return createDataParameter( input, posSuffix + sfix,
                                                        true );
                        }
                    } );
                }
            }
        }

        /* Layer geometry-specific parameters. */
        Parameter[] geomParams = context.getGeomParameters( suffix );
        for ( int igp = 0; igp < geomParams.length; igp++ ) {
            final int igp0 = igp;
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return context.getGeomParameters( sfix )[ igp0 ];
                }
            } );
        }

        /* Layer non-positional parameters. */
        Coord[] extraCoords = layer.getExtraCoords();
        for ( Coord coord : extraCoords ) {
            for ( final Input input : coord.getInputs() ) {
                finderList.add( new ParameterFinder<Parameter>() {
                    public Parameter createParameter( String sfix ) {
                        return createDataParameter( input, sfix, true );
                    }
                } );
            }
        }

        /* Layer style parameters. */
        ConfigKey[] styleKeys = layer.getStyleKeys();
        for ( final ConfigKey key : styleKeys ) {
            finderList.add( new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return ConfigParameter
                          .createLayerSuffixedParameter( key, sfix, true );
                }
            } );
        }

        /* Now try shading parameters if appropriate. */
        if ( layer instanceof ShapeFamilyLayerType ) {
            final ShapeFamilyLayerType shadeLayer =
                (ShapeFamilyLayerType) layer;
            ShapeMode shapeMode = new ParameterFinder<Parameter<ShapeMode>>() {
                public Parameter<ShapeMode> createParameter( String sfix ) {
                    return shadeLayer.createShapeModeParameter( sfix );
                }
            }.getParameter( env, suffix )
             .objectValue( env );

            /* Shading coordinate parameters. */
            Coord[] shadeCoords = shapeMode.getExtraCoords();
            for ( Coord coord : shadeCoords ) {
                for ( final Input input : coord.getInputs() ) {
                    finderList.add( new ParameterFinder<Parameter>() {
                        public Parameter createParameter( String sfix ) {
                            return createDataParameter( input, sfix, true );
                        }
                    } );
                }
            }

            /* Shading config parameters. */
            ConfigKey[] shadeKeys = shapeMode.getConfigKeys();
            for ( final ConfigKey key : shadeKeys ) {
                finderList.add( new ParameterFinder<Parameter>() {
                    public Parameter createParameter( String sfix ) {
                        return ConfigParameter
                              .createLayerSuffixedParameter( key, sfix, true );
                    }
                } ); 
            }
        }
        return finderList.toArray( new ParameterFinder[ 0 ] );
    }

    /**
     * Gets a painter value from an environment.
     *
     * The implementation should be trivial (paintModeParam.painterValue(env))
     * but instead it requires a hack.
     *
     * @param  env    execution environment
     * @return   painter object
     */
    private Painter getPainter( Environment env ) throws TaskException {
        PaintModeParameter paintModeParam = createPaintModeParameter();

        /* The following line is the ghastly hack.  We have to force the
         * output parameter associated with the paint mode parameter to
         * acquire its value from the given environment.  Because of the
         * opaque and nasty way that Environment is specified, it's not
         * possible to write an Environment implementation that properly
         * delegates to another one for resolving associated variables. */
        env.acquireValue( paintModeParam.getOutputParameter() );
        return paintModeParam.painterValue( env );
    }

    /**
     * Returns the filename associated with the graphics output file from
     * a given environment.
     *
     * @param  env    execution environment
     * @return  output graphics filename
     */
    private String getPainterOutputName( Environment env )
            throws TaskException {
        OutputStreamParameter outParam =
            createPaintModeParameter().getOutputParameter();

        /* Hack - see getPainter method. */
        env.acquireValue( outParam );
        return outParam.stringValue( env );
    }

    /**
     * Turns an execution environment (containing value-bearing parameters)
     * into an object capable of performing a static or interactive plot.
     *
     * @param  env  execution environment
     * @param  context   plot context
     * @return   plot executor
     */
    private PlotExecutor createPlotExecutor( Environment env,
                                             PlotContext context )
            throws TaskException {

        /* What kind of plot? */
        PlotType plotType = context.getPlotType();
        final SurfaceFactory surfFact = plotType.getSurfaceFactory();
        final PaperTypeSelector ptSel = plotType.getPaperTypeSelector();

        /* Set up generic configuration. */
        final int xpix = xpixParam_.intValue( env );
        final int ypix = ypixParam_.intValue( env );
        final boolean forceBitmap = bitmapParam_.booleanValue( env );
        final DataStoreFactory storeFact = dstoreParam_.objectValue( env );
        final Compositor compositor = compositorParam_.objectValue( env );
        Padding padding = paddingParam_.objectValue( env );
        final Ganger ganger = gangerFact_.createGanger( padding );

        /* Gather the defined plot layers from the environment. */
        Map<String,PlotLayer> layerMap = createLayerMap( env, context );

        /* Get the sequence of layers, then of legend entries, to plot. */
        String[] layerSeq = seqParam_.stringsValue( env );
        if ( layerSeq.length == 0 ) {
            layerSeq = layerMap.keySet().toArray( new String[ 0 ] );
        }
        String[] legendSeq = legseqParam_.stringsValue( env );
        if ( legendSeq.length == 0 ) {
            legendSeq = layerSeq;
        }

        /* Get an ordered list of layers to plot. */
        final PlotLayer[] layers = getLayerSequence( layerMap, layerSeq );

        /* Assemble the list of DataSpecs required for the plot. */
        int nl = layers.length;
        final DataSpec[] dataSpecs = new DataSpec[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            dataSpecs[ il ] = layers[ il ] == null
                            ? null
                            : layers[ il ].getDataSpec();
        }

        /* Prepare lists of config keys. */
        ConfigKey[] profileKeys = surfFact.getProfileKeys();
        ConfigKey[] aspectKeys = surfFact.getAspectKeys();
        ConfigKey[] shadeKeys = { StyleKeys.SHADE_LOW, StyleKeys.SHADE_HIGH };

        /* Prepare to get the navigator, but don't do it yet since we won't
         * need one if the display is not interactive. */
        final ConfigMap navConfig =
            createBasicConfigMap( env, surfFact.getNavigatorKeys() );

        /* Get information about which zones are defined. */
        Map<String,String[]> zoneSuffixMap = getZoneSuffixMap( env, layerSeq );
        String[] zoneSuffixes =
            zoneSuffixMap.keySet().toArray( new String[ 0 ] );

        /* Prepare parallel arrays of per-zone information for the plotting. */
        final int nz = zoneSuffixes.length;
        final ZoneContent[] contents = new ZoneContent[ nz ];
        final Object[] profiles = PlotUtil.createProfileArray( surfFact, nz );
        final ConfigMap[] aspectConfigs = new ConfigMap[ nz ];
        final ShadeAxisFactory[] shadeFacts = new ShadeAxisFactory[ nz ];
        final Range[] shadeFixRanges = new Range[ nz ];
        for ( int iz = 0; iz < nz; iz++ ) {
            String zoneSuffix = zoneSuffixes[ iz ];

            /* Work out which layers will participate in the current zone. */
            String[] zoneLayerSuffixes = zoneSuffixMap.get( zoneSuffix );
            int nzl = zoneLayerSuffixes.length;
            PlotLayer[] zoneLayers = new PlotLayer[ nzl ];
            for ( int il  = 0; il < nzl; il++ ) {
                zoneLayers[ il ] = layerMap.get( zoneLayerSuffixes[ il ] );
            }

            /* Get legend for the current zone. */
            List<String> zoneLegendList = new ArrayList<String>();
            for ( String l : legendSeq ) {
                if ( Arrays.asList( zoneLayerSuffixes ).contains( l ) ) {
                    zoneLegendList.add( l );
                }
            }
            Icon legend =
                createLegend( env, layerMap,
                              zoneLegendList.toArray( new String[ 0 ] ) );

            /* Get profile for the current zone. */
            ConfigMap profileConfig =
                createZoneConfigMap( env, profileKeys,
                                     zoneSuffix, zoneLayerSuffixes );
            Object profile = surfFact.createProfile( profileConfig );

            /* Prepare to calculate aspect for the current zone. */
            ConfigMap aspectConfig =
                createZoneSuffixedConfigMap( env, aspectKeys, zoneSuffix );

            /* Prepare to specify the shade axis for the current zone. */
            ConfigMap shadeConfig =
                createZoneSuffixedConfigMap( env, shadeKeys, zoneSuffix );
            Range shadeFixRange =
                new Range( shadeConfig.get( StyleKeys.SHADE_LOW ),
                           shadeConfig.get( StyleKeys.SHADE_HIGH ) );
            ShadeAxisFactory shadeFact =
                createShadeAxisFactory( env, zoneLayers, zoneSuffix );

            /* Get the legend position for the current zone. */
            final float[] legPos;
            if ( legend == null ) {
                legPos = null;
            }
            else {
                legPos = new ParameterFinder<DoubleArrayParameter>() {
                    public DoubleArrayParameter createParameter( String sfix ) {
                        return createLegendPositionParameter( sfix );
                    }
                }.getParameter( env, zoneSuffix )
                 .floatsValue( env );
            }

            /* Get the plot title for the current zone. */
            String title = new ParameterFinder<Parameter<String>>() {
                public Parameter<String> createParameter( String sfix ) {
                    return createTitleParameter( sfix );
                }
            }.getParameter( env, zoneSuffix )
             .stringValue( env );

            /* Populate per-zone arrays. */
            contents[ iz ] =
                new ZoneContent( zoneLayers, legend, legPos, title );
            profiles[ iz ] = profile;
            aspectConfigs[ iz ] = aspectConfig;
            shadeFacts[ iz ] = shadeFact;
            shadeFixRanges[ iz ] = shadeFixRange;
        }

        /* We have all we need.  Construct and return the object
         * that can do the plot. */
        return new PlotExecutor() {

            public DataStore createDataStore( DataStore prevStore )
                    throws IOException, InterruptedException {
                long t0 = System.currentTimeMillis();
                DataStore store =
                    storeFact.readDataStore( dataSpecs, prevStore );
                PlotUtil.logTime( logger_, "Data", t0 );
                return store;
            }

            public PlotDisplay createPlotComponent( DataStore dataStore,
                                                    boolean navigable,
                                                    PlotCaching caching ) {
                Navigator navigator = navigable
                                    ? surfFact.createNavigator( navConfig )
                                    : null;
                PlotDisplay panel =
                    PlotDisplay
                   .createGangDisplay( ganger, surfFact, nz, contents,
                                       profiles, aspectConfigs,
                                       shadeFacts, shadeFixRanges, navigator,
                                       ptSel, compositor, dataStore, caching );
                panel.setPreferredSize( new Dimension( xpix, ypix ) );
                return panel;
            }

            public Icon createPlotIcon( DataStore dataStore ) {
                Object[] aspects = new Object[ nz ];
                long t0 = System.currentTimeMillis();
                for ( int iz = 0; iz < nz; iz++ ) {
                    ZoneContent content = contents[ iz ];
                    Object profile = profiles[ iz ];
                    ConfigMap config = aspectConfigs[ iz ];
                    PlotLayer[] layers = content.getLayers();
                    Range[] ranges =
                          surfFact.useRanges( profile, config )
                        ? surfFact.readRanges( profile, layers, dataStore )
                        : null;
                    aspects[ iz ] =
                        surfFact.createAspect( profile, config, ranges );
                }
                return AbstractPlot2Task
                      .createPlotIcon( ganger, surfFact,
                                       nz, contents, profiles, aspects,
                                       shadeFacts, shadeFixRanges,
                                       ptSel, compositor, dataStore,
                                       xpix, ypix, forceBitmap );
            }
        };
    }

    /**
     * Obtains a list of the PlotLayers specified by parameters in
     * the execution environment for a given PlotContext.
     *
     * @param   env  execution environment
     * @param   context  plot context
     * @return   suffix->layer map for all the layers specified
     *           by the environment
     */
    private Map<String,PlotLayer> createLayerMap( Environment env,
                                                  PlotContext context )
            throws TaskException {

        /* Work out what plotters/layers are requested. */
        Map<String,Plotter> plotterMap = getPlotters( env, context );

        /* For each plotter, create a PlotLayer based on it using the
         * appropriately suffix-coded parameters in the environment.
         * In this step we deliberately create all the specified layers
         * though some might not be used in future.
         * It's important to do it that way, so that the parameter system
         * does not report specified but unplotted layer parameters as unused.
         * Creating layer objects is in any case cheap. */
        Map<String,PlotLayer> layerMap = new LinkedHashMap<String,PlotLayer>();
        for ( Map.Entry<String,Plotter> entry : plotterMap.entrySet() ) {
            String suffix = entry.getKey();
            Plotter plotter = entry.getValue();
            DataGeom geom = plotter.getCoordGroup().getPositionCount() > 0
                          ? context.getGeom( env, suffix )
                          : null;
            PlotLayer layer = createPlotLayer( env, suffix, plotter, geom );
            layerMap.put( suffix, layer );
        }
        return layerMap;
    }

    /**
     * Turns the map of defined layers into an ordered sequence of layers
     * to plot.
     *
     * @param  layerMap  suffix->layer map for all defined layers
     * @return   ordered list of layers to be actually plotted
     */
    private PlotLayer[] getLayerSequence( Map<String,PlotLayer> layerMap,
                                          String[] suffixSeq )
            throws TaskException {
        int nl = suffixSeq.length;
        PlotLayer[] layers = new PlotLayer[ nl ];
        for ( int il = 0; il < nl; il++ ) {
            String suffix = suffixSeq[ il ];
            PlotLayer layer = layerMap.get( suffix );
            if ( layer == null ) {
                String msg = new StringBuffer()
                    .append( "No specification for layer \"" )
                    .append( suffix )
                    .append( "\"" )
                    .append( "; known layers: " )
                    .append( layerMap.keySet() )
                    .toString();
                throw new ParameterValueException( seqParam_, msg );
            }
            else {
                layers[ il ] = layer;
            }
        }
        return layers;
    }

    /**
     * Determines from the environment which layers are to be plotted
     * in which zones.  The result is an ordered map in which each key
     * is a zone suffix, and each value is a non-empty list of layer 
     * suffixes from the input array that will appear in that zone.
     * Unnamed zones are represented by the empty string.
     *
     * <p>If multi-zone plots are not supported, the result will be
     * a single entry map, with the sole key being the empty string.
     *
     * @param   env  execution environment
     * @param  layerSuffixes   suffixes for layers that will be plotted
     * @return   map from zone suffix to list of layer suffixes
     */
    private Map<String,String[]> getZoneSuffixMap( Environment env,
                                                   String[] layerSuffixes )
            throws TaskException {

        /* If no ganging, just group all the layer suffixes under
         * a single key. */
        if ( ! gangerFact_.isMultiZone() ) {
            Map<String,String[]> map = new HashMap<String,String[]>();
            map.put( "", layerSuffixes );
            return map;
        }

        /* Otherwise we need to group under zone suffixes. */
        else {    
            Map<String,List<String>> zoneMap =
                new LinkedHashMap<String,List<String>>();

            /* Iterate over known layers. */
            for ( String layerSuffix : layerSuffixes ) {

                /* Identify explicitly stated or implicit zone suffix. */
                String zoneSuffix = getZoneSuffix( env, layerSuffix );

                /* Add layer suffix to zone map entry, initialising entry
                 * first if required. */
                if ( ! zoneMap.containsKey( zoneSuffix ) ) {
                    zoneMap.put( zoneSuffix, new ArrayList<String>() );
                }
                zoneMap.get( zoneSuffix ).add( layerSuffix );
            }

            /* Recast map and return. */
            Map<String,String[]> zmap = new LinkedHashMap<String,String[]>();
            for ( Map.Entry<String,List<String>> entry : zoneMap.entrySet() ) {
                zmap.put( entry.getKey(),
                          entry.getValue().toArray( new String[ 0 ] ) );
            }
            return zmap;
        }
    }

    /**
     * Turns the map of defined layers into a legend icon.
     *
     * @param  env  execution environment
     * @param  layerMap  suffix->layer map for all defined layers
     * @param  suffixSeq  ordered array of suffixes for layers to be plotted
     * @return  legend icon, may be null
     */
    private Icon createLegend( Environment env, Map<String,PlotLayer> layerMap,                                String[] suffixSeq )
            throws TaskException {

        /* Make a map from layer labels to arrays of styles.
         * Each entry in this map will correspond to a legend entry. */
        Map<List<SubCloud>,String> cloudMap =
            new LinkedHashMap<List<SubCloud>,String>();
        Map<String,List<Style>> labelMap =
            new LinkedHashMap<String,List<Style>>();
        for ( String suffix : suffixSeq ) {
            PlotLayer layer = layerMap.get( suffix );
            if ( layer == null ) {
                String msg = new StringBuffer()
                    .append( "No specification for layer \"" )
                    .append( suffix )
                    .append( "\"" )
                    .append( "; known layers: " )
                    .append( layerMap.keySet() )
                    .toString();
                throw new ParameterValueException( legseqParam_, msg );
            }

            /* If a legend label has been explicitly given for this layer,
             * use that. */
            String label = new ParameterFinder<Parameter<String>>() {
                public Parameter<String> createParameter( String sfix ) {
                    return createLabelParameter( sfix );
                }
            }.getParameter( env, suffix )
             .objectValue( env );

            /* Otherwise, work one out.  We don't give every layer its own
             * entry, but instead group layers together by their data
             * coordinates, since you may well have multiple layers with the
             * same data overplotted to achieve some visual effect.
             * The SubCloud is an object which can tell (testing by equality)
             * whether layers have the same positional coordinates. */
            if ( label == null ) {
                List<SubCloud> dataClouds = getPointClouds( layer );
                if ( ! cloudMap.containsKey( dataClouds ) ) {
                    String suffixLabel = suffix.length() == 0 ? "data" : suffix;
                    cloudMap.put( dataClouds, suffixLabel );
                }
                label = cloudMap.get( dataClouds );
            }
            if ( ! labelMap.containsKey( label ) ) {
                labelMap.put( label, new ArrayList<Style>() );
            }
            labelMap.get( label ).add( layer.getStyle() );
        }

        /* Turn the map into a list of LegendEntry objects. */
        List<LegendEntry> entryList = new ArrayList<LegendEntry>();
        for ( Map.Entry<String,List<Style>> entry : labelMap.entrySet() ) {
            String label = entry.getKey();
            Style[] styles = entry.getValue().toArray( new Style[ 0 ] );
            entryList.add( new LegendEntry( label, styles ) );
        }
        LegendEntry[] legEntries = entryList.toArray( new LegendEntry[ 0 ] );
 
        /* Work out if we are actually going to use a legend. */
        Boolean hasLegObj = legendParam_.objectValue( env );
        boolean hasLegend = hasLegObj == null ? legEntries.length > 1
                                              : hasLegObj.booleanValue();

        /* Construct and return the legend, or return null, as required. */
        if ( hasLegend ) {
            Captioner captioner = getCaptioner( env );
            boolean hasBorder = legborderParam_.booleanValue( env );
            boolean isOpaque = legopaqueParam_.booleanValue( env );
            Color bgColor = isOpaque ? Color.WHITE : null;
            return new LegendIcon( legEntries, captioner, hasBorder, bgColor );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a list of point clouds representing the positional coordinates
     * used to plot a layer.  The result is an object that can be compared
     * for equality to test whether layers are representing the same
     * positional data set.
     *
     * @return  layer  plot layer
     * @return   list of positional dataset identifiers
     */
    private static List<SubCloud> getPointClouds( PlotLayer layer ) {
        DataSpec dataSpec = layer.getDataSpec();
        DataGeom geom = layer.getDataGeom();
        CoordGroup cgrp = layer.getPlotter().getCoordGroup();
        int npos = cgrp.getPositionCount();
        List<SubCloud> cloudList = new ArrayList<SubCloud>( npos );
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            int iposCoord = cgrp.getPosCoordIndex( ipos, geom );
            cloudList.add( new SubCloud( geom, dataSpec, iposCoord ) );
        }
        return cloudList;
    }

    /**
     * Returns the zone suffix associated with a given layer.
     *
     * @param   env  execution environment
     * @param   layerSuffix  identifier for a layer
     * @return   zone identifier for layer, may be empty string but not null
     */
    private static String getZoneSuffix( Environment env, String layerSuffix )
            throws TaskException {
        String zoneSuffix = new ParameterFinder<Parameter<String>>() {
            public Parameter<String> createParameter( String sfix ) {
                return createZoneParameter( sfix );
            }
        }.getParameter( env, layerSuffix )
         .objectValue( env ); 
        return zoneSuffix == null ? "" : zoneSuffix;
    }

    /**
     * Returns a map of suffix strings to LayerType objects.
     * Each suffix string is appended to parameters associated with the
     * relevant LayerType as a namespacing device on the command line.
     *
     * @param  env  execution environment
     * @param  context  plot context
     * @return  mapping from suffixes to layer types for the environment
     */
    private static Map<String,LayerType> getLayers( Environment env,
                                                    PlotContext context )
            throws TaskException {
        String prefix = LAYER_PREFIX;
        Map<String,LayerType> map = new LinkedHashMap<String,LayerType>();
        for ( String pname : env.getNames() ) {
            if ( pname != null &&
                 pname.toLowerCase().startsWith( prefix.toLowerCase() ) ) {
                String suffix = pname.substring( prefix.length() );
                LayerType ltype = createLayerTypeParameter( suffix, context )
                                 .objectValue( env );
                map.put( suffix, ltype );
            }
        }
        return map;
    }

    /**
     * Returns a map of suffix strings to Plotter objects.
     * Each suffix string is appended to parameters associated with the
     * relevant plotter as a namespacing device on the command line.
     *
     * @param  env  execution environment
     * @param  context  plot context
     * @return  mapping from suffixes to plotters for the environment
     */
    private Map<String,Plotter> getPlotters( Environment env,
                                             PlotContext context )
            throws TaskException {
        Map<String,Plotter> plotterMap = new LinkedHashMap<String,Plotter>();
        for ( Map.Entry<String,LayerType> entry :
              getLayers( env, context ).entrySet() ) {
            String suffix = entry.getKey();
            LayerType ltype = entry.getValue();
            plotterMap.put( suffix, ltype.getPlotter( env, suffix ) );
        }
        return plotterMap;
    }

    /**
     * Acquires a captioner from the environment.
     * At present, a single captioner is used for labelling the legend
     * and any axes; this is also the one that will be used by default
     * for any label-type plot layers.  However you could refine it to
     * use different parameter sets for different purposes.
     *
     * @param   env  execution environment
     * @return   captioner
     */
    private Captioner getCaptioner( Environment env ) throws TaskException {
        KeySet<Captioner> capKeys = StyleKeys.CAPTIONER;
        ConfigMap capConfig = createBasicConfigMap( env, capKeys.getKeys() );
        return capKeys.createValue( capConfig );
    }

    /**
     * Acquires a ShadeAxisFactory from the environment for a given plot zone.
     *
     * @param  env  execution environment
     * @param  layers  layers that will be plotted
     * @param  zoneSuffix   identifier for zone whose shader is to be calculated
     * @return   shade axis factory, may be null
     */
    private ShadeAxisFactory createShadeAxisFactory( Environment env,
                                                     PlotLayer[] layers,
                                                     String zoneSuffix )
            throws TaskException {

        /* Locate the first layer that references the aux colour scale. */
        AuxScale scale = AuxScale.COLOR;
        PlotLayer scaleLayer = getFirstAuxLayer( layers, scale );

        /* Work out whether to display the colour ramp at all. */
        Boolean auxvis = new ParameterFinder<BooleanParameter>() {
            public BooleanParameter createParameter( String sfix ) {
                return createAuxVisibleParameter( sfix );
            }
        }.getParameter( env, zoneSuffix )
         .objectValue( env );
        boolean hasAux = auxvis == null ? scaleLayer != null
                                        : auxvis.booleanValue();
        if ( ! hasAux ) {
            return null;
        }

        /* Find a suitable label for the colour ramp. */
        StringParameter auxlabelParam = new ParameterFinder<StringParameter>() {
            public StringParameter createParameter( String sfix ) {
                return createAuxLabelParameter( sfix );
            }
        }.getParameter( env, zoneSuffix );
        if ( scaleLayer != null ) {
            auxlabelParam.setStringDefault( getAuxLabel( scaleLayer, scale ) );
        }
        String label = auxlabelParam.objectValue( env );

        /* Get axis crowding. */
        double crowd = new ParameterFinder<DoubleParameter>() {
            public DoubleParameter createParameter( String sfix ) {
                return createAuxCrowdParameter( sfix );
            }
        }.getParameter( env, zoneSuffix )
         .doubleValue( env );

        /* Get axis colour ramp width in pixels. */
        int rampWidth = new ParameterFinder<IntegerParameter>() {
            public IntegerParameter createParameter( String sfix ) {
                return createAuxWidthParameter( sfix );
            }
        }.getParameter( env, zoneSuffix )
         .intValue( env );

        /* Configure and return a shade axis accordingly. */
        RampKeySet rampKeys = StyleKeys.AUX_RAMP;
        Captioner captioner = getCaptioner( env );
        ConfigMap auxConfig =
            createZoneSuffixedConfigMap( env, rampKeys.getKeys(), zoneSuffix );
        RampKeySet.Ramp ramp = rampKeys.createValue( auxConfig );
        return RampKeySet.createShadeAxisFactory( ramp, captioner, label,
                                                  crowd, rampWidth );
    }

    /**
     * Returns a config map based on given keys with values derived from
     * the execution environment.  There is no funny business with appending
     * suffixes etc.
     *
     * @param  env  execution environment
     * @param  keys  config keys
     * @return  config map
     */
    private ConfigMap createBasicConfigMap( Environment env, ConfigKey[] keys )
            throws TaskException {
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T> getParameter( Environment env,
                                                        ConfigKey<T> key ) {
                return new ConfigParameter<T>( key );
            }
        };
        return createConfigMap( env, keys, cpFact );
    }

    /**
     * Creates a PlotLayer from the environment given a plotter, suffix
     * and geom.
     *
     * @param   env  execution environment containing parameter assignments
     * @param   suffix  parameter suffix for this layer
     * @param   plotter  plotter object for this layer
     * @param   geom   data position geometry
     * @return  plot layer
     */
    private <S extends Style>
            PlotLayer createPlotLayer( Environment env, String suffix,
                                       Plotter plotter, DataGeom geom )
            throws TaskException {

        /* Get basic and additional coordinate specifications. */
        CoordGroup cgrp = plotter.getCoordGroup();
        DataSpec dataSpec =
            createDataSpec( env, suffix, geom, cgrp.getPositionCount(),
                            cgrp.getExtraCoords() );

        /* Prepare a config map with entries for all the config keys
         * required by the plotter.  All the config keys reported
         * by the plotter are included.  We also add keys for the
         * aux axis, if used, since these are global (per zone not per layer).
         * This is a bit questionable, the plotter is using unreported
         * config options, but if it didn't, it would report per-layer
         * aux axis options (colour maps etc) which the plot surface
         * decorations can't reflect (there's only one aux colour ramp
         * displayed).  Maybe look at this again one day. */
        ConfigMap layerConfig =
            createLayerSuffixedConfigMap( env, plotter.getStyleKeys(), suffix );
        ConfigMap auxConfig =
            createZoneSuffixedConfigMap( env, StyleKeys.AUX_RAMP.getKeys(),
                                         getZoneSuffix( env, suffix ) );
        ConfigMap config = new ConfigMap();
        config.putAll( auxConfig );
        config.putAll( layerConfig );

        /* Work out the requested style. */
        @SuppressWarnings("unchecked")
        Plotter<S> splotter = (Plotter<S>) plotter;
        S style;
        try {
            style = splotter.createStyle( config );
        }
        catch ( ConfigException e ) {
            throw new UsageException( e.getConfigKey().getMeta().getShortName()
                                    + ": " + e.getMessage(), e );
        }

        /* Return a layer based on these. */
        return splotter.createLayer( geom, dataSpec, style );
    }

    /**
     * Generates a DataSpec for a given list of Coords from the environment.
     *
     * @param  env  execution environment
     * @param  suffix   parameter suffix of interest
     * @param  geom  defines positional coordinate groups
     * @param  npos  number of positional coordinate groups
     * @param  extraCoords   non-positional coordinates
     * @return   data spec from environment
     */
    private DataSpec createDataSpec( Environment env, String suffix,
                                     DataGeom geom, int npos,
                                     Coord[] extraCoords )
            throws TaskException {
        if ( npos == 0 && extraCoords.length == 0 ) {
            return null;
        }
        StarTable table = getInputTable( env, suffix );
        List<CoordValue> cvlist = new ArrayList<CoordValue>();
        Coord[] posCoords = geom == null ? new Coord[ 0 ] : geom.getPosCoords();
        for ( int ipos = 0; ipos < npos; ipos++ ) {
            String posSuffix = npos > 1 ? PlotUtil.getIndexSuffix( ipos ) : "";
            for ( int ic = 0; ic < posCoords.length; ic++ ) {
                cvlist.add( getCoordValue( env, posCoords[ ic ],
                                           posSuffix + suffix ) );
            }
        }
        for ( int ic = 0; ic < extraCoords.length; ic++ ) {
            cvlist.add( getCoordValue( env, extraCoords[ ic ], suffix ) );
        }
        CoordValue[] coordVals = cvlist.toArray( new CoordValue[ 0 ] );
        return new JELDataSpec( table, null, coordVals );
    }

    /**
     * Turns a coord into a CoordValue.
     *
     * @param   env  execution environment
     * @param   coord  coordinate definition
     * @param   suffix  suffix to append to parameter name
     * @return   coordinate with expression values acquired from environment
     */
    private CoordValue getCoordValue( Environment env, Coord coord,
                                      String suffix ) throws TaskException {
        Input[] inputs = coord.getInputs();
        int ni = inputs.length;
        String[] exprs = new String[ ni ];
        for ( int ii = 0; ii < ni; ii++ ) {
            final Input input = inputs[ ii ];
            Parameter param = new ParameterFinder<Parameter>() {
                public Parameter createParameter( String sfix ) {
                    return createDataParameter( input, sfix, true );
                }
            }.getParameter( env, suffix );
            param.setNullPermitted( ! coord.isRequired() );
            exprs[ ii ] = param.stringValue( env );
        }
        return new CoordValue( coord, exprs );
    }

    /**
     * Returns a ConfigMap specific to a given layer derived from the
     * assignments made in a given execution environment,
     * based on config keys and a layer identifier.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  layer-specific configuration keys to find values for
     * @param  layerSuffix  layer identifier string appended to config key
     *                      shortnames to make env parameter names
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createLayerSuffixedConfigMap( Environment env,
                                                    ConfigKey[] configKeys,
                                                    String layerSuffix )
            throws TaskException {
        return createSuffixedConfigMap( env, configKeys, layerSuffix, false );
    }

    /**
     * Returns a ConfigMap specific to a given zone derived from the
     * assignments made in a given execution environment,
     * based on config keys and a zone identifier.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  zone-specific configuration keys to find values for
     * @param  zoneSuffix   zone identifier string appended to config key
     *                      shortnames to make env parameter names
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createZoneSuffixedConfigMap( Environment env,
                                                   ConfigKey[] configKeys,
                                                   String zoneSuffix )
            throws TaskException {
        return createSuffixedConfigMap( env, configKeys, zoneSuffix, true );
    }

    /**
     * Returns a ConfigMap derived from the assignments made in a given
     * execution environment, with a layer- or zone-related suffix.
     * Parameter names specified in the environment
     * are the config names plus a supplied zone suffix,
     * but parameters named with the suffix partly or completely omitted
     * are used if they exist and the suffixed one does not.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  configuration keys to find values for
     * @param  suffix  identifier to append to config key shortnames
     *                 to make env parameter names
     * @param  isZone  true for zone suffixes, false for layer suffixes
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createSuffixedConfigMap( Environment env,
                                               ConfigKey[] configKeys,
                                               final String suffix,
                                               final boolean isZone )
            throws TaskException {
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T>
                    getParameter( Environment env, final ConfigKey<T> key ) {
                return new ParameterFinder<ConfigParameter<T>>() {
                    public ConfigParameter<T> createParameter( String sfix ) {
                        return isZone
                             ? ConfigParameter
                              .createZoneSuffixedParameter( key, sfix, true )
                             : ConfigParameter
                              .createLayerSuffixedParameter( key, sfix, true );
                    }
                }.getParameter( env, suffix );
            }
        };
        return createConfigMap( env, configKeys, cpFact );
    }

    /**
     * Constructs from the environment a config map with requested entries
     * that are specific (or can be treated as specific) to a given zone.
     *
     * @param  env  execution environment
     * @param  zoneConfigKeys   keys for which an entry in the output map
     *                          is required
     * @param  zoneSuffix  suffix identifying the zone in question
     * @param  zoneLayerSuffixes   list of suffixes identifying layers
     *                             that will be plotted on the zone of interest
     * @return  config map containing entries specific to chosen zone
     */
    private ConfigMap createZoneConfigMap( Environment env,
                                           ConfigKey[] zoneConfigKeys,
                                           final String zoneSuffix,
                                           final String[] zoneLayerSuffixes )
            throws TaskException {
        ConfigParameterFactory cpFact = new ConfigParameterFactory() {
            public <T> ConfigParameter<T> getParameter( Environment env,
                                                        final ConfigKey<T> key )
                    throws TaskException {

                /* Identify a parameter that will acquire the value of the
                 * chosen config key for the chosen zone. */
                ConfigParameter<T> param =
                        new ParameterFinder<ConfigParameter<T>>() {
                    public ConfigParameter<T> createParameter( String sfix ) {
                        return ConfigParameter
                              .createZoneSuffixedParameter( key, sfix, true );
                    }
                }.getParameter( env, zoneSuffix );

                /* This can use subclass functionality to default axis names
                 * from data coordinate parameter values. */
                String dflt =
                    getConfigParamDefault( env, key, zoneLayerSuffixes );
                if ( dflt != null ) {
                    param.setStringDefault( dflt );
                }
                return param;
            }
        };
        return createConfigMap( env, zoneConfigKeys, cpFact );
    }

    /**
     * Returns a ConfigMap derived from the assignments made in a given
     * execution environment.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  configKeys  configuration keys to find values for
     * @param  cpFact  turns config keys into Parameters
     * @return  config map with values for the supplied keys
     */
    private ConfigMap createConfigMap( Environment env, ConfigKey[] configKeys,
                                       ConfigParameterFactory cpFact )
            throws TaskException {
        Level level = Level.CONFIG;
        ConfigMap config = new ConfigMap();
        if ( Logger.getLogger( getClass().getName() ).isLoggable( level ) ) {
            config = new LoggingConfigMap( config, level );
        }
        for ( ConfigKey<?> key : configKeys ) {
            putConfigValue( env, key, cpFact, config );
        }
        return config;
    }

    /**
     * Extracts a parameter corresponding to a given config key from a
     * given execution environment, and puts the result into a config map.
     *
     * @param  env  execution environment bearing the parameter values
     * @param  key   key to find value for
     * @param  cpFact  turns config keys into Parameters
     * @param  map   map into which key/value pair will be written
     */
    private <T> void putConfigValue( Environment env, ConfigKey<T> key,
                                     ConfigParameterFactory cpFact,
                                     ConfigMap map )
            throws TaskException {
        ConfigParameter<T> param = cpFact.getParameter( env, key );
        T value = param.objectValue( env );
        if ( key.getValueClass().equals( Double.class ) && value == null ) {
            value = key.cast( Double.NaN );
        }
        map.put( key, value );
    }

    /**
     * Returns a table from the environment.
     *
     * @param   env  execution environment
     * @param   suffix   parameter suffix
     * @return   table
     */
    private StarTable getInputTable( Environment env, String suffix )
            throws TaskException {
        FilterParameter filterParam = new ParameterFinder<FilterParameter>() {
            public FilterParameter createParameter( String sfix ) {
                return createFilterParameter( sfix, null );
            }
        }.getParameter( env, suffix );
        InputTableParameter tableParam =
                new ParameterFinder<InputTableParameter>() {
            public InputTableParameter createParameter( String sfix ) {
                return createTableParameter( sfix );
            }
        }.getParameter( env, suffix );

        /* Note that tables produced by this call which have the same
         * input specifications (text of input and filter parameters)
         * will be equal in the sense of Object.equals().  That's good,
         * since it means that in the common case where the same table
         * contributes to multiple layers, the DataStore only has to
         * scan the table once. */
        TableProducer producer =
            ConsumerTask.createProducer( env, filterParam, tableParam );
        try {
            return producer.getTable();
        }
        catch ( IOException e ) {
            throw new ExecutionException( "Table processing error", e );
        }
    }

    /**
     * Returns a parameter for acquiring a data table.
     *
     * @param  suffix  layer-specific suffix
     * @return   table parameter
     */
    public static InputTableParameter createTableParameter( String suffix ) {
        return new InputTableParameter( TABLE_PREFIX + suffix );
    }

    /**
     * Returns a parameter for acquiring a filter applied to the table input
     * for a given layer.
     *
     * @param  suffix  layer-specific suffix
     * @param  tableParam input table parameter associated with the layer
     * @return   filter parameter
     */
    public static FilterParameter
            createFilterParameter( String suffix,
                                   InputTableParameter tableParam ) {
        FilterParameter param = new FilterParameter( FILTER_PREFIX + suffix );
        param.setTableDescription( "the layer " + suffix + " input table",
                                   tableParam, null );
        return param;
    }

    /**
     * Returns a parameter to get a textual label corresponding to the layer
     * identified by a given layer suffix.  This label is displayed in
     * the legend.
     *
     * @param  suffix  layer suffix
     * @return  parameter to get legend label for layer
     */
    public static Parameter<String> createLabelParameter( String suffix ) {
        StringParameter param = new StringParameter( "leglabel" + suffix );
        param.setUsage( "<text>" );
        param.setPrompt( "Legend label for layer " + suffix );
        param.setDescription( new String[] { 
            "<p>Sets the presentation label for the layer with a given suffix.",
            "This is the text which is displayed in the legend, if present.",
            "Multiple layers may use the same label, in which case",
            "they will be combined to form a single legend entry.",
            "</p>",
            "<p>If no value is supplied (the default),",
            "the suffix itself is used as the label.",
            "</p>",
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter to get a plot title for the zone identified
     * by a given zone suffix.
     *
     * @param  suffix  zone suffix, or either null or empty string for all zones
     * @return  parameter to get plot title for zone
     */
    private Parameter<String> createTitleParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "title";
        StringParameter param =
            new StringParameter( baseName + ( suffix == null ? "" : suffix ) );
        param.setPrompt( "Title for plot"
                       + ( suffix == null ? "" : " zone " + suffix ) );
        param.setDescription( new String[] {
            "<p>Text of a title to be displayed at the top of",
            ( suffix == null ? "the plot" : "plot zone " + suffix ) + ".",
            "If null, the default, no title is shown",
            "and there's more space for the graphics.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter for acquiring the aux ramp label.
     *
     * @param   suffix  zone suffix
     * @return   parameter
     */
    private StringParameter createAuxLabelParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "auxlabel";
        StringParameter param =
            new StringParameter( baseName + ( suffix == null ? ""
                                                             : suffix ) );
        param.setUsage( "<text>" );
        param.setPrompt( "Label for aux axis"
                       + ( suffix == null ? "" : " for zone " + suffix ) );
        param.setDescription( new String[] {
            "<p>Sets the label used to annotate the aux axis"
            + ( suffix == null ? ","
                               : " for zone " + suffix + "," ),
            "if it is visible.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter for determining whether the aux colour ramp
     * is painted for a given plot zone.
     *
     * @param  suffix  zone suffix
     * @return   parameter
     */
    private BooleanParameter createAuxVisibleParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "auxvisible";
        BooleanParameter param =
            new BooleanParameter( baseName + ( suffix == null ? "" : suffix ) );
        param.setPrompt( "Display aux colour ramp"
                       + ( suffix == null ? ""
                                          : " for zone " + suffix )
                       + "?" );
        param.setDescription( new String[] {
            "<p>Determines whether the aux axis colour ramp",
            "is displayed alongside the plot"
            + ( suffix == null ? "."
                               : " for zone " + suffix + "." ),
            "</p>",
            "<p>If not supplied (the default),",
            "the aux axis will be visible when aux shading is used",
            "in any of the plotted layers.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter for determining crowding on the aux axis.
     *
     * @param  suffix  zone suffix
     * @return   parameter
     */
    private DoubleParameter createAuxCrowdParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "auxcrowd";
        DoubleParameter param =
            new DoubleParameter( baseName + ( suffix == null ? ""
                                                             : suffix ) );
        param.setUsage( "<factor>" );
        param.setPrompt( "Tick crowding on aux axis"
                       + ( suffix == null ? "" : " for zone " + suffix ) );
        param.setDescription( new String[] {
            "<p>Determines how closely the tick marks are spaced on",
            "the Aux axis" + ( suffix == null ? ","
                                              : " for zone " + suffix + "," ),
            "if visible.",
            "The default value is 1, meaning normal crowding.",
            "Larger values result in more ticks,",
            "and smaller values fewer ticks.",
            "Tick marks will not however be spaced so closely that",
            "the labels overlap each other,",
            "so to get very closely spaced marks you may need to",
            "reduce the font size as well.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setDoubleDefault( 1 );
        return param;
    }

    /**
     * Returns a parameter for determining aux colour ramp lateral size
     * in pixels.
     *
     * @param  suffix  zone suffix
     * @return  parameter
     */
    private IntegerParameter createAuxWidthParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "auxwidth";
        IntegerParameter param =
            new IntegerParameter( baseName + ( suffix == null ? "" : suffix ) );
        param.setUsage( "<pixels>" );
        param.setPrompt( "Width of aux axis ramp"
                       + ( suffix == null ? "" : " for zone " + suffix ) );
        param.setDescription( new String[] {
            "<p>Determines the lateral size of the aux colour ramp,",
            "if visible, in pixels.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setIntDefault( 15 );
        return param;
    }

    /**
     * Returns a parameter to get the legend position for the zone identified
     * by a given zone suffix.
     *
     * @param  suffix  zone suffix, or either null or empty string for all zones
     * @return   parameter to get legend position for zone
     */
    private DoubleArrayParameter
            createLegendPositionParameter( String suffix ) {
        if ( "".equals( suffix ) ) {
            suffix = null;
        }
        String baseName = "legpos";
        DoubleArrayParameter param =
            new DoubleArrayParameter( baseName + ( suffix == null ? ""
                                                                  : suffix ),
                                      2 );
        param.setUsage( "<xfrac,yfrac>" );
        param.setPrompt( "X,Y fractional internal legend position"
                       + ( suffix == null ? "" : " for zone " + suffix ) );
        param.setDescription( new String[] {
            "<p>Determines the internal position of the legend on",
            ( suffix == null ? "the plot" : "plot zone " + suffix ) + ".",
            "The value is a comma-separated pair of values giving the",
            "X and Y positions of the legend within the plotting bounds,",
            "so for instance \"<code>0.5,0.5</code>\" will put the legend",
            "right in the middle of the plot.",
            "If no value is supplied, the legend will appear outside",
            "the plot boundary.",
            "</p>",
            getZoneDoc( baseName, suffix ),
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns XML text documenting the zone-specific use of a 
     * zone-suffixed parameter.  If zones are not in use in this task,
     * an empty string is returned.
     *
     * @param   baseName  base name of parameter
     * @param   suffix   actual zone suffix of the parameter in question
     * @return  non-null string containing zero or more XML &lt;p&gt; elements
     */
    private String getZoneDoc( String baseName, String suffix ) {
        if ( ! gangerFact_.isMultiZone() ) {
            return "";
        }
        else if ( suffix != null && suffix.length() > 0 ) {
            return new StringBuffer()
               .append( "<p>This parameter affects only zone " )
               .append( "<code>" )
               .append( suffix )
               .append( "</code>.\n" )
               .append( "</p>" )
               .toString();
        }
        else {
            return new StringBuffer()
               .append( "<p>If a zone suffix is appended " )
               .append( "to the parameter name, " )
               .append( "only that zone is affected,\n" )
               .append( "e.g. " )
               .append( "<code>" )
               .append( baseName )
               .append( EXAMPLE_ZONE_SUFFIX )
               .append( "</code>" )
               .append( " affects only zone " )
               .append( "<code>" )
               .append( EXAMPLE_ZONE_SUFFIX )
               .append( "</code>" )
               .append( "." )
               .append( "</p>\n" )
               .toString();
        }
    }

    /**
     * Returns a parameter for acquiring a plotter.
     *
     * @param   suffix  parameter name suffix
     * @param   context  plot context
     * @return   plotter parameter
     */
    public static LayerTypeParameter
            createLayerTypeParameter( String suffix, PlotContext context ) {
        return new LayerTypeParameter( LAYER_PREFIX, suffix, context );
    }

    /**
     * Returns a parameter for associating a zone identifier with a given
     * layer.  The <em>value</em> acquired by this parameter is the zone suffix.
     *
     * @param  layerSuffix  identifier for the layer whose zone is to be
     *                      determined
     * @return   zone suffix parameter
     */
    public static Parameter<String> createZoneParameter( String layerSuffix ) {
        StringParameter param =
            new StringParameter( ZONE_PREFIX + layerSuffix );
        param.setUsage( "<text>" );
        param.setPrompt( "Plot zone for layer " + layerSuffix );
        param.setDescription( new String[] {
            "<p>Defines which plot zone the layer with suffix " + layerSuffix,
            "will appear in.",
            "This only makes sense for multi-zone plots.",
            "The actual value of the parameter is not significant,",
            "it just serves as a label,",
            "but different layers will end up in the same plot zone",
            "if they give the same values for this parameter.",
            "</p>",
        } );
        param.setNullPermitted( true );
        return param;
    }

    /**
     * Returns a parameter for acquiring a column of data.
     *
     * @param  input  specifies input value required from user
     * @param  suffix  layer-specific suffix
     * @param  fullDetail  if true, extra detail is appended to the description
     * @return   data parameter
     */
    public static StringParameter createDataParameter( Input input,
                                                       String suffix,
                                                       boolean fullDetail ) {
        InputMeta meta = input.getMeta();
        boolean hasSuffix = suffix.length() > 0;
        String cName = meta.getShortName();
        Class cClazz = input.getValueClass();
        final String typeTxt;
        final String typeUsage;
        if ( cClazz.equals( String.class ) ) {
            typeTxt = "string";
            typeUsage = "txt";
        }
        else if ( cClazz.equals( Integer.class ) ||
                  cClazz.equals( Long.class ) ) {
            typeTxt = "integer";
            typeUsage = "int";
        }
        else if ( Number.class.isAssignableFrom( cClazz ) ) {
            typeTxt = "numeric";
            typeUsage = "num";
        }
        else {
            typeTxt = "<code>" + cClazz.getSimpleName() + "</code>";
            typeUsage = null;
        }
        StringParameter param = new StringParameter( cName + suffix );
        String prompt = meta.getShortDescription();
        if ( fullDetail ) {
            prompt += hasSuffix ? ( " for layer " + suffix )
                                : " for plot layers";
        }
        param.setPrompt( prompt );
        StringBuffer dbuf = new StringBuffer()
            .append( meta.getXmlDescription() );
        dbuf.append( "<p>" );
        if ( fullDetail ) {
            dbuf.append( "This parameter gives a column name, " )
                .append( "fixed value, or algebraic expression for the\n" )
                .append( "<code>" )
                .append( cName )
                .append( "</code> coordinate\n" );
            if ( hasSuffix ) {
                dbuf.append( "for layer <code>" )
                    .append( suffix )
                    .append( "</code>" );
            }
            else {
                dbuf.append( "for all plot layers" );
            }
            dbuf.append( ".\n" );
        }
        dbuf.append( "The value is a " )
            .append( typeTxt )
            .append( " algebraic expression based on column names\n" )
            .append( "as described in <ref id='jel'/>.\n" )
            .append( "</p>\n" );
        param.setDescription( dbuf.toString() );
        String vUsage = meta.getValueUsage();
        if ( vUsage == null ) {
            vUsage = typeUsage;
        }
        param.setUsage( vUsage == null ? "<expr>"
                                       : "<" + vUsage + "-expr>" );
        return param;
    }

    /**
     * Returns a parameter for specifying a paint mode.
     *
     * @return   paint mode parameter
     */
    private static PaintModeParameter createPaintModeParameter() {
        return new PaintModeParameter( "omode", EXPORTERS );
    }

    /**
     * Creates an icon which will paint the content of a plot.
     * This icon is expected to be painted once and then discarded,
     * so it's not cached.
     *
     * @param  ganger  defines plot surface grouping
     * @param  surfFact   surface factory
     * @param  nz   number of plot zones in gang
     * @param  contents   zone contents (nz-element array)
     * @param  aspects    plot surface aspects by zone (nz-element array)
     * @param  shadeFacts   shader axis factories by zone (nz-element array),
     *                      elements may be null if not required
     * @param  shadeFixRanges  fixed shader ranges by zone (nz-element array)
     *                         elements may be null for auto-range or if no
     *                         shade axis
     * @param  ptSel    paper type selector
     * @param  compositor  compositor for pixel composition
     * @param  dataStore   data storage object
     * @param  xpix    horizontal size of icon in pixels
     * @param  ypix    vertical size of icon in pixels
     * @param  forceBitmap   true to force bitmap output of vector graphics,
     *                       false to use default behaviour
     * @return  icon  icon for plotting
     */
    public static <P,A> Icon
            createPlotIcon( Ganger<P,A> ganger,
                            final SurfaceFactory<P,A> surfFact,
                            final int nz, final ZoneContent[] contents,
                            final P[] profiles, final A[] aspects,
                            ShadeAxisFactory[] shadeFacts,
                            Range[] shadeFixRanges,
                            final PaperTypeSelector ptSel,
                            final Compositor compositor,
                            final DataStore dataStore,
                            final int xpix, final int ypix,
                            final boolean forceBitmap ) {
        final Rectangle extBox = new Rectangle( 0, 0, xpix, ypix );
        final boolean cached = false;
        final Object[] planArray = null;
        final Set<Object> planSet = null;
        final boolean withScroll = false;

        /* Acquire nominal plot bounds that are good enough for working
         * out aux data ranges. */
        Gang approxGang =
            ganger.createGang( extBox, surfFact, nz, contents, profiles,
                               aspects, new ShadeAxis[ nz ], withScroll );

        /* Calculate aux ranges if required.
         * Although this should maybe belong with the aspect determination
         * since it involves ranging, it's convenient to do it here
         * because we will need the aux ranges anyway. */
        final ShadeAxis[] shadeAxes = new ShadeAxis[ nz ];
        final List<Map<AuxScale,Range>> auxRangeList =
            new ArrayList<Map<AuxScale,Range>>();
        long start = System.currentTimeMillis();
        for ( int iz = 0; iz < nz; iz++ ) {
            ZoneContent content = contents[ iz ];
            P profile = profiles[ iz ];
            ShadeAxisFactory shadeFact = shadeFacts[ iz ];
            Surface approxSurf =
                surfFact.createSurface( approxGang.getZonePlotBounds( iz ),
                                        profiles[ iz ], aspects[ iz ] );
            Map<AuxScale,Range> auxRanges =
                PlotDisplay.getAuxRanges( content.getLayers(), approxSurf,
                                          shadeFixRanges[ iz ], shadeFact,
                                          planArray, dataStore );
            auxRangeList.add( auxRanges );
            Range shadeRange = auxRanges.get( AuxScale.COLOR );
            if ( shadeFact != null && shadeRange != null ) {
                shadeAxes[ iz ] = shadeFact.createShadeAxis( shadeRange );
            }
        }
        PlotUtil.logTime( logger_, "Range", start );

        /* Work out plot bounds. */
        final Gang gang =
            ganger.createGang( extBox, surfFact, nz, contents, profiles,
                               aspects, shadeAxes, withScroll );

        /* Construct and return an icon that paints all the zones. */
        return new Icon() {
            public int getIconWidth() {
                return xpix;
            }
            public int getIconHeight() {
                return ypix;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                g.translate( x, y );
                Shape clip = g.getClip();
                for ( int iz = 0; iz < nz; iz++ ) {
                    ZoneContent content = contents[ iz ];
                    PlotLayer[] layers = content.getLayers();
                    Surface surface =
                        surfFact.createSurface( gang.getZonePlotBounds( iz ),
                                                profiles[ iz ], aspects[ iz ] );
                    Decoration[] decs =
                        PlotPlacement
                       .createPlotDecorations( surface, content.getLegend(),
                                               content.getLegendPosition(),
                                               content.getTitle(),
                                               shadeAxes[ iz ] );
                    PlotPlacement placer =
                        new PlotPlacement( extBox, surface, decs );
                    LayerOpt[] opts = PaperTypeSelector.getOpts( layers );
                    PaperType paperType =
                          forceBitmap
                        ? ptSel.getPixelPaperType( opts, compositor, null )
                        : ptSel.getVectorPaperType( opts );
                    if ( clip != null &&
                         ! clip.intersects( surface.getPlotBounds() ) ) {
                        layers = new PlotLayer[ 0 ];
                    }
                    Icon zicon =
                        PlotUtil
                       .createPlotIcon( placer, layers, auxRangeList.get( iz ),
                                        dataStore, paperType, cached, planSet );
                    zicon.paintIcon( c, g, 0, 0 );
                }
                g.translate( -x, -y );
            }
        };
    }

    /**
     * Returns a list of parameters suffixed by zone based on a list of
     * ConfigKeys.
     *
     * @param  keys  config keys
     * @return  parameters for acquiring config key values
     */
    public final List<Parameter> getZoneKeyParams( ConfigKey[] keys ) {
        boolean isMultiZone = gangerFact_.isMultiZone();
        List<Parameter> plist = new ArrayList<Parameter>();
        for ( int ik = 0; ik < keys.length; ik++ ) {
            plist.add( ConfigParameter
                      .createZoneSuffixedParameter( keys[ ik ], DOC_ZONE_SUFFIX,
                                                    isMultiZone ) );
        }
        return plist;
    }

    /**
     * Identifies and returns the first layer in a given list that
     * appears to make use of a given AuxScale.
     * The test is whether it does ranging for the scale.
     *
     * @param   layers  list of known layers
     * @param   scale   target scale
     * @return  one of the layers in the given list that appears to use
     *          <code>scale</code>, or null if none do
     */
    private static PlotLayer getFirstAuxLayer( PlotLayer[] layers,
                                               AuxScale scale ) {
        for ( PlotLayer layer : layers ) {
            if ( layer.getAuxRangers().containsKey( scale ) ) {
                return layer;
            }
        }
        return null;
    }

    /**
     * Tries to return the name of an input data quantity used in plotting
     * a given layer that corresponds to a given AuxScale.
     *
     * @param   layer   plot layer
     * @param   scale   scale
     * @return  user-readable name for data corresponding to
     *          <code>scale</code> in <code>layer</code>,
     *          or null if there's no obvious answer
     */
    private static String getAuxLabel( PlotLayer layer, AuxScale scale ) {
        AuxReader rdr = layer.getAuxRangers().get( scale );
        if ( rdr != null ) {
            int icAux = rdr.getCoordIndex();
            if ( icAux >= 0 ) {
                DataSpec dataSpec = layer.getDataSpec();
                assert dataSpec == null || dataSpec instanceof JELDataSpec;
                if ( dataSpec instanceof JELDataSpec ) {
                    String[] exprs =
                        ((JELDataSpec) dataSpec).getCoordExpressions( icAux );
                    if ( exprs.length == 1 ) {
                        return exprs[ 0 ];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Object that can turn a ConfigKey into a Parameter.
     */
    private interface ConfigParameterFactory {

        /**
         * Produces a parameter to find the value for a given config key.
         *
         * @param   key  config key
         * @param   env  execution environment
         * @return   parameter that can get a value for <code>key</code>
         */
        <T> ConfigParameter<T> getParameter( Environment env, ConfigKey<T> key )
                 throws TaskException;
    }

    /**
     * StarTable wrapper class which caches the first row of the table.
     * It uses a row sequence to do this, which it saves for later use.
     * This means that it doesn't need to use a separate row sequence
     * for the initial row, but it's only a good idea if you know you're
     * going to be calling getRowSequence at least once.
     */
    private static class Row0Table extends WrapperStarTable {
        final Object[] row0_;
        RowSequence rseq_;

        /**
         * Constructor.
         *
         * @param   base   table to which most calls are delegated
         */
        Row0Table( StarTable base ) throws IOException {
            super( base );
            rseq_ = base.getRowSequence();
            rseq_.next();
            row0_ = rseq_.getRow();
        }

        /**
         * Returns the first row.
         *
         * @return  row
         */
        public Object[] getRow0() {
            return row0_;
        }

        @Override
        public synchronized RowSequence getRowSequence() throws IOException {
            if ( rseq_ == null ) {
                return super.getRowSequence();
            }
            else {
                RowSequence baseSeq = rseq_;
                rseq_ = null;
                return new WrapperRowSequence( baseSeq ) {
                    long irow = -1;
                    @Override
                    public boolean next() throws IOException {
                        return ++irow == 0 || super.next();
                    }
                    @Override
                    public Object getCell( int icol ) throws IOException {
                        return irow == 0 ? row0_[ icol ]
                                         : super.getCell( icol );
                    }
                    @Override
                    public Object[] getRow() throws IOException {
                        return irow == 0 ? row0_ : super.getRow();
                    }
                };
            }
        }
    }

    /**
     * Object capable of executing a static or interactive plot.
     * All configuration options are contained.
     */
    private interface PlotExecutor {

        /**
         * Creates a data store suitable for use with this object.
         *
         * @param     prevStore  previously obtained data store, may be null
         * @return    object containing plot data
         */
        DataStore createDataStore( DataStore prevStore )
                throws IOException, InterruptedException;

        /**
         * Generates an interactive plot component.
         *
         * @param  dataStore  object containing plot data
         * @param  navigable  if true, standard pan/zoom mouse listeners
         *                   will be installed
         * @param  caching   plot caching policy
         */
        PlotDisplay createPlotComponent( DataStore dataStore,
                                         boolean navigable,
                                         PlotCaching caching );

        /**
         * Generates an icon which will draw the plot.
         * This may be slow to paint.
         *
         * @param  dataStore  object containing plot data
         */
        Icon createPlotIcon( DataStore dataStore );
    }
}
