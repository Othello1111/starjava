package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.ProcessingStep;
import uk.ac.starlink.ttools.mode.ProcessingMode;

/**
 * Task which maps one or more input tables to an output table.
 * This class provides methods to acquire the table sources and sink;
 * any actual transformation work is done by a separate 
 * {@link TableMapper} object.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public abstract class MapperTask implements Task {

    private final TableMapper mapper_;
    private final FilterParameter outFilterParam_;
    private final ProcessingMode outMode_;
    private Parameter[] params_;

    /**
     * Constructor.
     *
     * @param   mapper   object which defines mapping transformation
     * @param   outMode  processing mode which determines the destination of
     *          the processed table
     * @param   useOutFilter allow specification of filters for output table
     */
    public MapperTask( TableMapper mapper, ProcessingMode outMode,
                       boolean useOutFilter ) {
        mapper_ = mapper;
        outMode_ = outMode;
        List paramList = new ArrayList();

        /* Processing parameters. */
        paramList.addAll( Arrays.asList( mapper.getParameters() ) );

        /* Output filter. */
        if ( useOutFilter ) {
            outFilterParam_ = new FilterParameter( "ocmd" );
            outFilterParam_.setPrompt( "Processing command(s) " 
                                     + "for output table" );
            outFilterParam_.setDescription( new String[] {
                "Commands to operate on the output table,",
                "after all other processing has taken place.",
                outFilterParam_.getDescription(),
            } );
            paramList.add( outFilterParam_ );
        }
        else {
            outFilterParam_ = null;
        }

        /* Set output parameter list. */
        paramList.addAll( Arrays.asList( outMode.getAssociatedParameters() ) );
        setParameters( (Parameter[]) paramList.toArray( new Parameter[ 0 ] ) );
    }

    public Parameter[] getParameters() {
        return params_;
    }

    /**
     * Sets the list of parameters for this task.
     *
     * @param   params  parameter array
     */
    protected void setParameters( Parameter[] params ) {
        params_ = params;
    }

    /**
     * Returns an array of InputSpec objects describing the input tables
     * used by this task.
     *
     * @return   input table specifiers
     */
    protected abstract InputSpec[] getInputSpecs();

    public Executable createExecutable( Environment env ) throws TaskException {

        InputSpec[] inSpecs = getInputSpecs();
        final int nIn = inSpecs.length;

        /* Get raw input tables. */
        final StarTable[] inTables = new StarTable[ nIn ];
        for ( int i = 0; i < nIn; i++ ) {
            inTables[ i ] = inSpecs[ i ].getTableParameter().tableValue( env );
        }

        /* Get a sequence of pre-processing steps for each input table. */
        final ProcessingStep[][] inSteps = new ProcessingStep[ nIn ][];
        for ( int i = 0; i < nIn; i++ ) {
            FilterParameter fp = inSpecs[ i ].getFilterParameter();
            inSteps[ i ] = fp != null ? fp.stepsValue( env )
                                      : new ProcessingStep[ 0 ];
        }

        /* Get the mapping which defines the actual processing done by
         * this task. */
        final TableMapping mapping = mapper_.createMapping( env );

        /* Get a sequence of post-processing steps for the output table. */
        final ProcessingStep[] outSteps = outFilterParam_ != null
                                        ? outFilterParam_.stepsValue( env )
                                        : new ProcessingStep[ 0 ];

        /* Get the table consumer, which defines the output table's final
         * destination. */
        final TableConsumer baseConsumer = outMode_.createConsumer( env );

        /* Construct a consumer which will combine the post-processing
         * and the final disposal. */
        final TableConsumer consumer = new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                for ( int i = 0; i < outSteps.length; i++ ) {
                    table = outSteps[ i ].wrap( table );
                }
                baseConsumer.consume( table );
            }
        };

        /* Check unused arguments for things we can write helpful messages
         * about. */
        checkUnused( env );

        /* Construct and return an executable which will do all the work. */
        return new Executable() {
            public void execute() throws IOException, TaskException {

                /* Perform any required pre-filtering of input tables. */
                for ( int i = 0; i < nIn; i++ ) {
                    for ( int j = 0; j < inSteps[ i ].length; j++ ) {
                        inTables[ i ] = inSteps[ i ][ j ].wrap( inTables[ i ] );
                    }
                }

                /* Finally, execute the pipeline. */
                mapping.mapTables( inTables, new TableConsumer[] { consumer } );
            }
        };
    }

    /**
     * Returns this task's Mapper object.
     *
     * @return  mapper
     */
    public TableMapper getMapper() {
        return mapper_;
    }

    /**
     * Returnst this task's output mode.
     *
     * @return  output mode
     */
    public ProcessingMode getOutputMode() {
        return outMode_;
    }

    /**
     * Checks the unused words in the environment in case we can write any
     * useful messages.
     *
     * @param  env  execution environment
     * @throws  TaskException   if there's trouble
     */
    private void checkUnused( Environment env ) throws TaskException {
        if ( env instanceof LineEnvironment ) {
            String[] unused = ((LineEnvironment) env).getUnused();
            for ( int i = 0; i < unused.length; i++ ) {
                String word = unused[ i ];
                if ( word.startsWith( "out=" ) || word.startsWith( "ofmt=" ) ) {
                    throw new UsageException(
                        word + ": out and ofmt parameters can only be used " +
                        "when omode=out" );
                }
            }
        }
    }

    /**
     * Struct-type class which provides specifications for a single 
     * input table for a task.
     */
    protected static class InputSpec {

        private final InputTableParameter tableParam_;
        private final FilterParameter filterParam_;

        /**
         * Constructor.
         *
         * @param  tableParam  input table parameter
         * @param  filterParam  input filter parameter
         */
        public InputSpec( InputTableParameter tableParam,
                          FilterParameter filterParam ) {
            tableParam_ = tableParam;
            filterParam_ = filterParam;
        }

        /**
         * Returns input table parameter.
         *
         * @param  input table parameter
         */
        public InputTableParameter getTableParameter() {
            return tableParam_;
        }

        /**
         * Returns the input filter parameter.
         *
         * @param   input filter parameter (may be null)
         */
        public FilterParameter getFilterParameter() {
            return filterParam_;
        }
    }
}
