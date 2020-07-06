package uk.ac.starlink.table.join;

import cds.healpix.FlatHashIterator;
import cds.healpix.Healpix;
import cds.healpix.HealpixNestedBMOC;
import uk.ac.starlink.table.Tables;

/**
 * HEALPix sky pixellator using the CDS-HEALPIX-JAVA library
 * written by F-X Pineau at CDS.
 *
 * <p>The Nested indexing scheme is used, and the largest permissible
 * k-value (depth) is 29.
 *
 * @author   Mark Taylor
 * @since    21 May 2020
 * @see   <a href="https://github.com/cds-astro/cds-healpix-java"
 *                >https://github.com/cds-astro/cds-healpix-java</a>
 */
public class CdsHealpixSkyPixellator extends HealpixSkyPixellator {

    private int depth_;

    /**
     * Scale factor which determines the sky pixel size to use,
     * as a multiple of the angular scale, if no k value is set explicitly.
     * This is a tuning factor (any value will give correct results,
     * but performance may be affected).
     * The current value may not be optimal.
     */
    private static final int DEFAULT_SCALE_FACTOR = 8;

    public CdsHealpixSkyPixellator() {
        super( Healpix.DEPTH_MAX );
    }

    protected void configureK( int k ) {
        depth_ = k;
    }

    public Object[] getPixels( double alpha, double delta, double radius ) {
        HealpixNestedBMOC bmoc =
            Healpix.getNested( depth_ )
                   .newConeComputerApprox( radius )
                   .overlappingCells( alpha, delta );
        assert bmoc.getDepthMax() == depth_;
        int npix = Tables.checkedLongToInt( bmoc.computeDeepSize() );
        Long[] pixels = new Long[ npix ];
        FlatHashIterator flit = bmoc.flatHashIterator();
        for ( int ipix = 0; ipix < npix; ipix++ ) {
            assert flit.hasNext();
            pixels[ ipix ] = new Long( flit.next() );
        }
        assert ! flit.hasNext();
        return pixels;
    }

    /**
     * Determines a default value to use for the HEALPix k parameter
     * based on a given scale.
     *
     * @param   scale   distance scale, in radians
     */
    public int calculateDefaultK( double scale ) {

        /* Calculate the HEALPix map resolution parameter appropriate for
         * the requested scale.  Any value is correct, the scale is just
         * a tuning parameter. */
        return Math.max( 0,
                         Healpix.getBestStartingDepth( DEFAULT_SCALE_FACTOR *
                                                       scale ) );
    }
}
