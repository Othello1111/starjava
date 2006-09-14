package uk.ac.starlink.ttools.task;

/**
 * Task which implements a table pipeline.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public class TablePipe extends SingleMapperTask {
    public TablePipe() {
        super( new PipeMapper(), new ChoiceMode(), false, false );
    }
}
