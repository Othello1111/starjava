

package diva.sketch.toolbox;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;

/**
 * Native class that defines a collection of TextLine's. It also defines 
 * the avg x-height and character width. 
 *
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @rating Red
 */
public class BulletedItem implements TypedData {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(BulletedItem.class);
    
    private double _charHeight;
    private double _charWidth;
	
    public BulletedItem(double ch, double cw) {
        _charHeight = ch;
        _charWidth = cw;
    }
	
    public Type getType() {
        return BulletedItem.type;
    }
	
    public double getCharHeight() {
        return _charHeight;
    }
	
    public double getCharWidth() {
        return _charWidth;
    }
	
    public String toString() {
        return "BulletedItem[ charHeight = " + _charHeight + 
		  ", charWidth = " + _charWidth + "]";
    }
}
