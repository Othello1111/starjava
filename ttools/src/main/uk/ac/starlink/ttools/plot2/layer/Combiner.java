package uk.ac.starlink.ttools.plot2.layer;

import java.util.Arrays;
import java.util.BitSet;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the combination mode for accumulating values into a bin.
 *
 * <p>Note that the {@link #SUM} mode is usually sensible for unweighted values,
 * but if the values are weighted it may be more revealing to use
 * one of the others (like {@link #MEAN}).
 *
 * @author    Mark Taylor
 * @since     20 Sep 2015
 */
@Equality
public abstract class Combiner {

    private final String name_;
    private final String description_;

    /** Report the number of submitted values. */
    public static final Combiner COUNT;

    /** Calculate the sum of all submitted values. */
    public static final Combiner SUM;

    /** Calculate the mean of all submitted values. */
    public static final Combiner MEAN;

    /** Calculate the minimum of all submitted values. */
    public static final Combiner MIN;

    /** Calculate the maximum of all submitted values. */
    public static final Combiner MAX;

    /** Return 1 if any value submitted, 0 otherwise. */
    public static final Combiner HIT;

    private static final Combiner[] COMBINERS = new Combiner[] {
        SUM = new SumCombiner(),
        MEAN = new MeanCombiner(),
        COUNT = new CountCombiner(),
        MIN = new MinCombiner(),
        MAX = new MaxCombiner(),
        HIT = new HitCombiner(),
    };

    /**
     * Constructor.
     *
     * @param   name  name
     * @param   description  short textual description
     */
    protected Combiner( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Creates an object which can be used to accumulate values.
     *
     * <p><strong>Note:</strong> Since many container instances may
     * by generated (when using a HashBinList) it is desirable to
     * keep the returned objects as small as possible.
     * In particular, it's a good idea to make the returned objects
     * instances of a static class, to avoid an unncecessary reference
     * to the owner object, unless there's a really compelling reason
     * to do otherwise.
     *
     * @return  new container
     */
    public abstract Container createContainer();

    /**
     * May be able to create a bin list suitable for non-sparse,
     * moderate-sized index ranges.
     * If a combiner implementation is able to provide an ArrayBinList
     * implementation that should be significantly more efficient
     * than a HashBinList, this method should return it.
     * If not, it can return null.
     *
     * @param  size   index range of required bin list
     * @return   array-based bin list, or null
     */
    public abstract BinList createArrayBinList( int size );

    /**
     * Returns this combiner's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short textual description of this combiner.
     *
     * @return  short description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of the known general purpose instances of this class.
     *
     * @return   combiner list
     */
    public static Combiner[] getKnownCombiners() {
        return COMBINERS.clone();
    }

    /**
     * Defines an object that can be used to accumulate values and
     * retrieve a result.
     */
    public interface Container {

        /**
         * Submits a new numeric value for accumulation to the result.
         * In general, NaN values should not be submitted.
         *
         * @param  datum  new value to accumulate
         */
        void submit( double datum );

        /**
         * Returns the combined result of all the values submitted so far.
         * In general, if no values have been submitted,
         * a NaN should be returned.
         *
         * @return  combined value of all submitted data
         */
        double getResult();
    }

    /**
     * Combiner implementation that calculates the mean.
     */
    private static class MeanCombiner extends Combiner {

        /**
         * Constructor.
         */
        public MeanCombiner() {
            super( "mean", "the mean of the combined values" );
        }

        public BinList createArrayBinList( int size ) {
            final int[] counts = new int[ size ];
            final double[] sums = new double[ size ];
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double value ) {
                    counts[ index ]++;
                    sums[ index ] += value;
                }
                public double getBinResultInt( int index ) {
                    int count = counts[ index ];
                    return count == 0 ? Double.NaN
                                      : sums[ index ] / (double) count;
                }
            };
        }

        public Container createContainer() {
            return new MeanContainer();
        }

        /**
         * Container that holds a count and a sum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MeanContainer implements Container {
            int count_;
            double sum_;
            public void submit( double datum ) {
                count_++;
                sum_ += datum;
            }
            public double getResult() {
                return count_ == 0 ? Double.NaN : sum_ / (double) count_;
            }
        }
    }

    /**
     * Combiner instance that just counts submissions.
     */
    private static class CountCombiner extends Combiner {

        /**
         * Constructor.
         */
        CountCombiner() {
            super( "count", "the number of non-blank values" );
        }

        public BinList createArrayBinList( int size ) {
            final int[] counts = new int[ size ];
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double value ) {
                    counts[ index ]++;
                }
                public double getBinResultInt( int index ) {
                    int count = counts[ index ];
                    return count == 0 ? Double.NaN : count;
                }
            };
        }

        public Container createContainer() {
            return new CountContainer();
        }

        /**
         * Container that holds a count.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class CountContainer implements Container {
            int count_;
            public void submit( double datum ) {
                count_++;
            }
            public double getResult() {
                return count_ == 0 ? Double.NaN : count_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the sum.
     */
    private static class SumCombiner extends Combiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (sum).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineSum( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum : oldValue + datum;
        }

        /**
         * Constructor.
         */
        SumCombiner() {
            super( "sum", "the sum of all the combined values" );
        }

        public BinList createArrayBinList( int size ) {
            final double[] sums = new double[ size ];
            Arrays.fill( sums, Double.NaN );
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double datum ) {
                    sums[ index ] = combineSum( sums[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return sums[ index ];
                }
            };
        }

        public Container createContainer() {
            return new SumContainer();
        }

        /**
         * Container that holds a sum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class SumContainer implements Container {
            double sum_ = Double.NaN;
            public void submit( double datum ) {
                sum_ = combineSum( sum_, datum );
            }
            public double getResult() {
                return sum_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the minimum submitted value.
     */
    private static class MinCombiner extends Combiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (min).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineMin( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum
                                            : Math.min( oldValue, datum );
        }

        /**
         * Constructor.
         */
        MinCombiner() {
            super( "min", "the minimum of all the combined values" );
        }

        public BinList createArrayBinList( int size ) {
            final double[] mins = new double[ size ];
            Arrays.fill( mins, Double.NaN );
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double datum ) {
                    mins[ index ] = combineMin( mins[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return mins[ index ];
                }
            };
        }

        public Container createContainer() {
            return new MinContainer();
        }

        /**
         * Container that accumulates a minimum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MinContainer implements Container {
            private double min_ = Double.NaN;
            public void submit( double datum ) {
                min_ = combineMin( min_, datum );
            }
            public double getResult() {
                return min_;
            }
        }
    }

    /**
     * Combiner implementation that calculates the maximum submitted value.
     */
    private static class MaxCombiner extends Combiner {

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (max).
         *
         * @param  oldValue  previous value
         * @param  datum   newly submitted value
         * @return  new state
         */
        private static double combineMax( double oldValue, double datum ) {
            return Double.isNaN( oldValue ) ? datum
                                            : Math.max( oldValue, datum );
        }

        /**
         * Constructor.
         */
        MaxCombiner() {
            super( "max", "the maximum of all the combined values" );
        }

        public BinList createArrayBinList( int size ) {
            final double[] maxs = new double[ size ];
            Arrays.fill( maxs, Double.NaN );
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double datum ) {
                    maxs[ index ] = combineMax( maxs[ index ], datum );
                }
                public double getBinResultInt( int index ) {
                    return maxs[ index ];
                }
            };
        }

        public Container createContainer() {
            return new MaxContainer();
        }

        /**
         * Container that accumulates a maximum.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MaxContainer implements Container {
            private double max_ = Double.NaN;
            public void submit( double datum ) {
                max_ = combineMax( max_, datum );
            }
            public double getResult() {
                return max_;
            }
        }
    }

    /**
     * Combiner that just registers whether any data have been submitted.
     */
    private static class HitCombiner extends Combiner {

        /**
         * Constructor.
         */
        HitCombiner() {
            super( "hit", "1 if any values present, NaN otherwise" );
        }

        public BinList createArrayBinList( int size ) {
            final BitSet mask = new BitSet();
            return new ArrayBinList( size, this ) {
                public void submitToBinInt( int index, double datum ) {
                    mask.set( index );
                }
                public double getBinResultInt( int index ) {
                    return mask.get( index ) ? 1 : Double.NaN;
                }
            };
        }

        public Container createContainer() {
            return new HitContainer();
        }

        private static class HitContainer implements Container {
            boolean hit_;
            public void submit( double datum ) {
                hit_ = true;
            }
            public double getResult() {
                return hit_ ? 1 : Double.NaN;
            }
        }
    }
}